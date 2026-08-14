# Application deployment

This document describes how to set up a server for the current deployment approach.

For routine health checks, low-memory constraints, and OOM recovery, see
[`OPERATIONS.md`](OPERATIONS.md).

> **Warning:** This is a deliberately simple deployment and can be improved in the future.

The application is distributed as `no-thanks.jar` and run by a systemd service. The GitHub Actions workflow in `.github/workflows/build-and-deploy.yaml` builds the JAR, transfers it to the server with `rsync`, and restarts the service.

Replace these placeholders in the commands:

- `<server-ip>`: the server's IP address
- `<username>`: the dedicated deployment/service user, such as `githubbot`
- `<groupname>`: that user's group, usually the same as `<username>`

The Linux user in `deploy/no-thanks.service`, the owner of `/opt/apps`, and the `GITHUBBOT_USER` GitHub secret must refer to the same account. Do not run the application as `root` permanently.

## Set up SSH access from your host

SSH keys let the server authenticate a private key held by your host instead of requesting the server account's password. Never copy or disclose the private key.

### Windows (PowerShell)

Generate a modern Ed25519 key:

```powershell
ssh-keygen -t ed25519
```

Press Enter to accept `C:\Users\<you>\.ssh\id_ed25519`. An empty passphrase allows completely prompt-free login; using a passphrase with `ssh-agent` is safer.

Display and copy the public key:

```powershell
Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub
```

### Linux or macOS

```bash
ssh-keygen -t ed25519
cat ~/.ssh/id_ed25519.pub
```

### Authorize the host key for root

Log in once using the server password:

```bash
ssh root@<server-ip>
```

On the server, add the public key copied above:

```bash
install -d -m 700 /root/.ssh
echo 'PASTE_YOUR_PUBLIC_KEY_HERE' >> /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
```

Keep this session open and test `ssh root@<server-ip>` in another terminal before closing it. Run `exit` or press Ctrl+D to return to the host terminal.

## Prepare the server environment

These steps assume a clean Debian/Ubuntu server. Connect as root:

```bash
ssh root@<server-ip>
```

Install Java and `rsync`:

```bash
apt update
apt install default-jre rsync
```

Create the deployment group and user before installing or starting the service:

```bash
groupadd --system <groupname>
useradd --system --create-home --shell /bin/bash --gid <groupname> <username>
install -d -o <username> -g <groupname> /opt/apps
```

`--create-home` and an SSH-capable shell are required because GitHub Actions connects as this account. If the account already exists, do not recreate it.

Set the same account in `deploy/no-thanks.service`:

```ini
User=<username>
```

The repository template uses `User=githubbot`. A nonexistent user causes systemd to fail with `status=217/USER`.

## Build the initial JAR

Run this from the repository root on your host.

### Windows (PowerShell)

```powershell
.\gradlew.bat clean build
```

### Linux or macOS

```bash
./gradlew clean build
```

The Gradle configuration always creates `build/libs/no-thanks.jar`, which is also the path used by GitHub Actions.

## Transfer the initial deployment

The original deployment uses `rsync`. It is available directly on Linux/macOS and through WSL (or another environment where `rsync` is installed) on Windows. Native Windows PowerShell can use `scp` for the one-time setup.

### Windows with WSL (`rsync`)

Run from the repository root, using paths appropriate to that shell:

```bash
rsync -avz ./build/libs/no-thanks.jar root@<server-ip>:/opt/apps/
rsync -avz ./deploy/no-thanks.service root@<server-ip>:/etc/systemd/system/
```

### Windows PowerShell (`scp` alternative)

```powershell
scp .\build\libs\no-thanks.jar root@<server-ip>:/opt/apps/
scp .\deploy\no-thanks.service root@<server-ip>:/etc/systemd/system/
```

### Linux or macOS (`rsync`)

```bash
rsync -avz ./build/libs/no-thanks.jar root@<server-ip>:/opt/apps/
rsync -avz ./deploy/no-thanks.service root@<server-ip>:/etc/systemd/system/
```

On the server, assign ownership, load the unit, start it, and enable startup after reboot:

```bash
chown -R <username>:<groupname> /opt/apps
systemctl daemon-reload
systemctl enable --now no-thanks.service
systemctl status no-thanks.service
```

After changing the unit file, always run:

```bash
systemctl daemon-reload
systemctl restart no-thanks.service
```

### Optional small-server memory profile

The default `no-thanks.service` is a general-purpose profile. Do not use its 512
MiB heap on a host whose total RAM is similarly small. For the current single-core
server with approximately 371 MiB of usable RAM and 1 GiB of swap, use the
alternative [`no-thanks-low-memory.service`](no-thanks-low-memory.service).

The alternative uses a 128 MiB heap, Serial GC, a 96 MiB metaspace ceiling, and a
280 MiB systemd cgroup limit. `MemoryHigh=220M` starts reclaim pressure before the
hard limit is reached, while its start-rate limit prevents a failing JVM from
restarting continuously and starving essential operating-system services.

The profile assumes that the host has at least 1 GiB of active swap. Verify that
first; `swapon --show` must list a swap device or file:

```bash
swapon --show
free -h
```

On this small server, keep swappiness at 20 so the kernel uses that swap before
physical RAM is exhausted. A value of zero previously allowed a global OOM while
the entire swap file remained unused:

```bash
printf 'vm.swappiness=20\n' > /etc/sysctl.d/90-no-thanks-memory.conf
sysctl -w vm.swappiness=20
sysctl vm.swappiness
```

`vm.swappiness` controls when active swap is used; it does not create or enable a
swap file by itself. See [`OPERATIONS.md`](OPERATIONS.md) for the incident history
and small-server maintenance policy.

Install the alternative under the canonical unit name expected by the deployment
workflow and operational commands:

```bash
scp deploy/no-thanks-low-memory.service root@<server-ip>:/etc/systemd/system/no-thanks.service
ssh root@<server-ip>
systemctl daemon-reload
systemctl enable --now no-thanks.service
```

Do not install it as a second active `no-thanks-low-memory.service`; two enabled
units would start two JVMs competing for port 8080 and memory.

After installing or updating it, verify the effective unit, JVM command, memory
limits, swap policy, and application health:

```bash
systemctl cat no-thanks.service
systemctl status no-thanks.service --no-pager
systemctl show no-thanks.service \
  -p FragmentPath -p DropInPaths -p MemoryHigh -p MemoryMax -p NRestarts
sysctl vm.swappiness
swapon --show
free -h
ps -o pid,rss,vsz,%mem,etime,cmd -C java
curl -I --max-time 10 http://127.0.0.1:8080/
```

If the server already has `/etc/systemd/system/no-thanks.service.d/memory.conf`,
keep it until the alternative unit has been copied and verified. It can then be
removed to avoid maintaining duplicate settings:

```bash
rm /etc/systemd/system/no-thanks.service.d/memory.conf
rmdir --ignore-fail-on-non-empty /etc/systemd/system/no-thanks.service.d
systemctl daemon-reload
systemctl restart no-thanks.service
```

## Configure the deployment user for GitHub Actions

The workflow connects to the server as `<username>`, uploads
`/opt/apps/no-thanks.jar` and the unit selected by its top-level `SERVICE_PROFILE`
setting, verifies that the root-installed unit matches that profile byte-for-byte,
and restarts the systemd service. Unit installation remains a one-time root
operation: granting the deployment account permission to install arbitrary systemd
units would allow a compromised deployment key to execute commands as root.

Generate a separate deployment key on a trusted machine. The original RSA form remains valid and is widely compatible:

```bash
ssh-keygen -t rsa -b 4096 -C "githubbot@github.com" -f githubbot
```

This creates private key `githubbot` and public key `githubbot.pub`. Do not give the private key to the server.

Copy the public key to the server.

### Windows PowerShell

```powershell
scp .\githubbot.pub root@<server-ip>:/tmp/githubbot.pub
```

### Windows with WSL, Linux, or macOS

```bash
rsync -avz ./githubbot.pub root@<server-ip>:/tmp/githubbot.pub
```

On the server, authorize it for the deployment user:

```bash
install -d -m 700 -o <username> -g <groupname> /home/<username>/.ssh
install -m 600 -o <username> -g <groupname> /tmp/githubbot.pub /home/<username>/.ssh/authorized_keys
rm /tmp/githubbot.pub
```

Allow the exact restart command used by `.github/workflows/build-and-deploy.yaml`:

```bash
echo '<username> ALL=(root) NOPASSWD: /bin/systemctl restart no-thanks' > /etc/sudoers.d/no-thanks-deploy
chmod 440 /etc/sudoers.d/no-thanks-deploy
visudo -cf /etc/sudoers.d/no-thanks-deploy
```

Confirm that `/bin/systemctl` is the correct path with `command -v systemctl`. The sudoers command must match the workflow command exactly.

Test the deployment account:

```bash
ssh <username>@<server-ip>
test -w /opt/apps
sudo -n /bin/systemctl restart no-thanks
exit
```

## Configure GitHub Actions secrets

Add these repository secrets under **Settings → Secrets and variables → Actions**:

- `GITHUBBOT_PRIVATE_KEY`: the complete contents of the private `githubbot` key
- `GITHUBBOT_SSH_HOST`: `<server-ip>` or the server hostname
- `GITHUBBOT_USER`: `<username>`

The existing workflow runs for pushes to `master`. It can also be started manually with **Run workflow** in the GitHub Actions UI; the workflow file containing `workflow_dispatch` must first exist on the repository's default branch. It performs:

1. A Java 11 Gradle build with `./gradlew build --no-daemon`.
2. An upload of `deploy/no-thanks-low-memory.service` to `/opt/apps/`.
3. A byte-for-byte comparison with `/etc/systemd/system/no-thanks.service`.
4. An `rsync` upload of `./build/libs/no-thanks.jar` to `/opt/apps/` only when the
   profiles match.
5. `sudo -n /bin/systemctl restart no-thanks`.

Because the workflow uploads as `<username>`, `/opt/apps` must remain writable by that user. The service also runs as that same non-root user.

Before the first deployment using this workflow, copy and install the selected
profile once as root. From the repository root on the host machine:

```bash
scp deploy/no-thanks-low-memory.service root@<server-ip>:/tmp/no-thanks-low-memory.service
ssh root@<server-ip>
```

Then, on the server, install it and remove the now-redundant temporary drop-in:

```bash
install -o root -g root -m 0644 /tmp/no-thanks-low-memory.service /etc/systemd/system/no-thanks.service
rm /tmp/no-thanks-low-memory.service
rm -f /etc/systemd/system/no-thanks.service.d/memory.conf
rmdir --ignore-fail-on-non-empty /etc/systemd/system/no-thanks.service.d
systemctl daemon-reload
systemctl restart no-thanks.service
```

### Switching service profiles

Activating another profile is intentionally a manual root operation. The workflow
selects and verifies a profile, but cannot install an arbitrary systemd unit. This
prevents a compromised deployment key from changing the unit to execute as root.

1. Change the single top-level workflow setting. Use
   `no-thanks-low-memory.service` for the constrained VPS or `no-thanks.service`
   for the default profile:

   ```yaml
   env:
     SERVICE_PROFILE: no-thanks.service
   ```

2. From a trusted checkout, copy the same selected file to the server. Replace
   `<selected-profile>` with the exact filename from `SERVICE_PROFILE`:

   ```bash
   scp deploy/<selected-profile> root@<server-ip>:/tmp/<selected-profile>
   ssh root@<server-ip>
   ```

3. On the server, inspect the change before installing it:

   ```bash
   diff -u \
     /etc/systemd/system/no-thanks.service \
     /tmp/<selected-profile> || true
   ```

4. Install the selected file under the canonical unit name, validate it, reload
   systemd, and restart the application:

   ```bash
   install -o root -g root -m 0644 \
     /tmp/<selected-profile> \
     /etc/systemd/system/no-thanks.service
   systemd-analyze verify /etc/systemd/system/no-thanks.service
   systemctl daemon-reload
   systemctl restart no-thanks.service
   systemctl status no-thanks.service --no-pager
   curl -I --max-time 10 http://127.0.0.1:8080/
   rm /tmp/<selected-profile>
   ```

5. Merge or push the workflow change. Its byte-for-byte comparison will now pass,
   allowing the JAR deployment and restart to proceed.

The installed path is always `/etc/systemd/system/no-thanks.service`, regardless
of which source profile is selected. If the profile contents change later, repeat
the manual installation before deploying that revision.

If a workflow is run before the manual installation, it safely fails before
uploading the JAR. The selected candidate is left in `/opt/apps/`; inspect it with
`diff`, install it as root if expected, and rerun the failed workflow.

## Logs and diagnostics

Run these on the server:

```bash
# Status and recent messages
systemctl status no-thanks.service

# Complete service log from the current boot, including the Spring banner
journalctl -u no-thanks.service -b --no-pager

# Follow new messages live; press Ctrl+C to stop
journalctl -u no-thanks.service -f

# Show the unit systemd actually loaded
systemctl cat no-thanks.service

# Confirm the process listening on port 8080
ss -ltnp 'sport = :8080'
```

Plain `journalctl -u no-thanks.service` includes retained history and starts at the oldest entry, so failed deployments from earlier boots may appear first.

### Recover from memory pressure or an OOM restart loop

Typical evidence is `code=killed, signal=KILL` in the service status and `Out of
memory: Killed process ... (java)` in the kernel journal. Stop the restart loop
first, then inspect the host while Java is not running:

```bash
systemctl stop no-thanks.service
systemctl reset-failed no-thanks.service
free -h
swapon --show
ps -eo pid,user,comm,rss,%mem --sort=-rss | head -20
journalctl -k -b --no-pager | grep -Ei 'oom|out of memory|killed process|memory cgroup'
```

If SSH is unresponsive, use the provider console to run the first command. If the
restart loop resumes during boot, disable the service and reboot:

```bash
systemctl disable --now no-thanks.service
reboot
```

Once the machine is responsive, confirm that the low-memory unit is installed,
reload it, and start it once:

```bash
systemctl daemon-reload
systemctl enable --now no-thanks.service
sleep 60
systemctl status no-thanks.service --no-pager
curl -I --max-time 10 http://127.0.0.1:8080/
free -h
```

Do not repeatedly restart a failed service. Read its last messages instead:

```bash
journalctl -u no-thanks.service -n 100 --no-pager
```

## Set up NGINX and HTTPS

Connect as root and install NGINX and Certbot:

```bash
apt update
apt install nginx snapd
snap install --classic certbot
ln -s /snap/bin/certbot /usr/bin/certbot
```

Update the domain in `deploy/nginx/no-thanks.conf`, then transfer the NGINX configuration.

### Windows with WSL, Linux, or macOS (`rsync`)

```bash
rsync -avz ./deploy/nginx/ root@<server-ip>:/etc/nginx/sites-available/
```

### Windows PowerShell (`scp` alternative)

```powershell
scp .\deploy\nginx\default.conf .\deploy\nginx\no-thanks.conf root@<server-ip>:/etc/nginx/sites-available/
```

On the server, enable the supplied configurations:

```bash
rm -f /etc/nginx/sites-enabled/default
ln -s /etc/nginx/sites-available/default.conf /etc/nginx/sites-enabled/default.conf
ln -s /etc/nginx/sites-available/no-thanks.conf /etc/nginx/sites-enabled/no-thanks.conf
mkdir -p /etc/nginx/ssl
openssl req -nodes -new -x509 -subj '/CN=localhost' -keyout /etc/nginx/ssl/default.key -out /etc/nginx/ssl/default.crt
nginx -t
systemctl reload nginx
```

Request the production certificate after DNS points to the server and the domain in the NGINX config is correct:

```bash
certbot --nginx
```

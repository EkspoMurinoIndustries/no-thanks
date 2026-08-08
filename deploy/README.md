# Application deployment

This document describes how to set up a server for the current deployment approach.

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

## Configure the deployment user for GitHub Actions

The workflow connects to the server as `<username>`, writes `/opt/apps/no-thanks.jar` with `rsync`, and restarts the systemd service.

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
2. An `rsync` upload of `./build/libs/no-thanks.jar` to `/opt/apps/`.
3. `sudo systemctl restart no-thanks` over SSH.

Because the workflow uploads as `<username>`, `/opt/apps` must remain writable by that user. The service also runs as that same non-root user.

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

# Server and CI setup

This is the first-time setup for the current simple systemd deployment. Replace
`<server-ip>`, `<username>`, and `<groupname>` in commands. The service template
uses `githubbot`; the unit's `User`, `/opt/apps` owner, and `GITHUBBOT_USER` secret
must name the same account.

## Prepare the server

Connect as root and install the runtime and transfer tool:

```bash
ssh root@<server-ip>
apt update
apt install default-jre rsync
groupadd --system <groupname>
useradd --system --create-home --shell /bin/bash --gid <groupname> <username>
install -d -o <username> -g <groupname> /opt/apps
```

Do not recreate an existing user or group. The home directory and shell are needed
because GitHub Actions connects as the service account.

## Install the application initially

Build locally, then copy the JAR and service unit:

```bash
scp build/libs/no-thanks.jar root@<server-ip>:/opt/apps/
scp deploy/no-thanks.service root@<server-ip>:/etc/systemd/system/no-thanks.service
```

On the server:

```bash
chown -R <username>:<groupname> /opt/apps
systemctl daemon-reload
systemctl enable --now no-thanks.service
systemctl status no-thanks.service --no-pager
curl -I --max-time 10 http://127.0.0.1:8080/
```

## Configure the deployment key

Generate a dedicated key on a trusted machine:

```bash
ssh-keygen -t rsa -b 4096 -C "githubbot@github.com" -f githubbot
```

Copy only `githubbot.pub` to the server. Never copy the private key there. As root
from the trusted machine, then install it as root on the server:

```bash
scp githubbot.pub root@<server-ip>:/tmp/githubbot.pub
ssh root@<server-ip>
install -d -m 700 -o <username> -g <groupname> /home/<username>/.ssh
install -m 600 -o <username> -g <groupname> /tmp/githubbot.pub /home/<username>/.ssh/authorized_keys
rm /tmp/githubbot.pub
```

Allow only the restart command used by the workflow:

```bash
echo '<username> ALL=(root) NOPASSWD: /bin/systemctl restart no-thanks' > /etc/sudoers.d/no-thanks-deploy
chmod 440 /etc/sudoers.d/no-thanks-deploy
visudo -cf /etc/sudoers.d/no-thanks-deploy
```

Confirm the path using `command -v systemctl`, then test as the deployment user:

```bash
test -w /opt/apps
sudo -n /bin/systemctl restart no-thanks
```

## Configure GitHub

Add these repository secrets:

- `GITHUBBOT_PRIVATE_KEY` — complete private deployment key.
- `GITHUBBOT_SSH_HOST` — server IP or hostname.
- `GITHUBBOT_USER` — deployment/service username.

The workflow runs on pushes to `master` and can be started manually through
`workflow_dispatch`. It:

1. Builds the JAR.
2. Uploads the JAR.
3. Restarts `no-thanks.service`.

The workflow deliberately cannot install arbitrary systemd units as root.

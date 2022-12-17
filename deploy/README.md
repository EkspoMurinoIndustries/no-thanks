# Application deployment

This doc contains the instructions how to setup server for the current deploy approach.

> :warning: This approach is not perfect and should be significantly improved in the future.

We use a simple systemd daemon to run the application on the server, as a way to distribute the application - `.jar` archive. For deployment, we have set up a Github Action, the essence of which is to send a new `.jar` archive to the server, and restart the daemon to run a new `.jar`.

## Prepare system environment

> ℹ️ At this step, we assume that we have a completely clean system environment

1. Login as root user to the server via ssh: 
    ```
    $ ssh root@<ip-address>
    root@<ip-address>'s password:
    ```
2. Install JRE:
    ```
    $ sudo apt install default-jre
    ```
3. Create directory for the application archives:
    ```bash
    mkdir /opt/apps
    ```

4. Prepare initial application archive and copy to the server via `rsync` from your machine:
    ```bash
    $ rsync no-thanks.jar root@193.168.48.222:/opt/apps
    ```

5. Prepare systemd daemon config and transfer it via `rsync` from your machine:
    ```bash
    $ rsync no-thanks.service root@<ip-address>:/etc/systemd/system/
    ```

6. Launch `no-thanks` daemon:
    ```bash
    sudo systemctl start no-thanks.service
    ```

7. Enable daemon autorun after server restart:
    ```bash
    sudo systemctl enable no-thanks
    ```

## Create a user for deployment control:

1. Log in as root user to the server via ssh:
    ```
    $ ssh root@<ip-address>
    root@<ip-address>'s password:
    ```
2. Create user group:
    ```bash
    $ sudo groupadd -r <groupname>
    ```
3. Create a user:
    ```bash
    $ sudo useradd -r -s /bin/false -g <groupname> <username>
    ```
4. Make the user the owner of the app archives directory:
    ```bash
    $ sudo chown -R <username>:<groupname> /opt/apps
    ```
5. Allow user to restart `no-thanks` daemon:
    ```bash
    $ echo "<username> ALL=(root) NOPASSWD: /bin/systemctl restart no-thanks" > /etc/sudoers.d/username
    visudo -c /etc/sudoers.d/githubbot
    ```
## Setup user access via ssh keys:
1. Generate pair on your machine with:
    ```bash
    $ ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
    ssh-keygen -t rsa -b 4096 -C "your_email@github.com"
    Generating public/private rsa key pair.
    Enter file in which to save the key (/Users/******/.ssh/id_rsa): <filename>
    Enter passphrase (empty for no passphrase):
    Enter same passphrase again:
    Your identification has been saved in dddd
    Your public key has been saved in dddd.pub
    The key fingerprint is:
    SHA256:xK4KbJpUCFDwVl6zZTZG88vvcJxk2uisdLloL13KAPI githubbot@github.com
    The key's randomart image is:
    +---[RSA 4096]----+
    |oo. . o.O        |
    |.. o . O +       |
    |. o . . o .      |
    |... . .o . .     |
    | . . o .S o o    |
    | ..   E..  X..   |
    | .+   . .+*o*    |
    |.+ . . .o=+=     |
    |o   .  .o+= .    |

    ```
2. Copy public key to the server:
    ```bash
    rsync <filename>.pub root@<ip-address>:~
    ```
3. Set public key as authorized_keys:
    ```bash
    $ ssh root@<ip-address>
    $ mkdir /home/<username>/.ssh
    $ mv <filename>.pub /home/<username>/.ssh/authorized_keys
    $ chmod 700 /home/<username>/.ssh && chmod 600 /home/<username>/.ssh/authorized_keys
    ```
4. Make the user the owner of `/home/<username>/.ssh/`:
    ```bash
    $ chown -R <username>:<username> /home/<username>/.ssh
    ```
Now the private key can be used to gain access to the server as a <username> user.
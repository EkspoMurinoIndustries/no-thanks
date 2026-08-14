# Linux notes

This document is a growing reference for general Linux and shell concepts. It is
deliberately separate from application and deployment documentation.

## Reading a shell prompt

A prompt such as:

```text
githubbot@server:~$
```

means:

- `githubbot` is the current user.
- `server` is the machine's hostname.
- `~` is the current user's home directory.
- `$` normally indicates an unprivileged user.

A root prompt commonly ends in `#`:

```text
root@server:~#
```

Do not include the prompt itself when copying a documented command unless the
instructions explicitly say otherwise.

## Root and `sudo`

The root user can modify the whole system. An ordinary user has limited
permissions. `sudo` runs one command with elevated privileges when the user is
authorized:

```bash
sudo systemctl restart example.service
```

Use elevated privileges only for commands that need them. Running an application
as a dedicated unprivileged user limits damage if that application is compromised.

## Multiline shell commands

A backslash at the very end of a line continues the same command on the next line:

```bash
systemctl show example.service \
  -p MemoryCurrent \
  -p MemoryPeak
```

There must be no spaces after the backslash. Blank lines normally separate
commands, and multiple complete commands pasted together execute sequentially.

## Exit status and silent success

Linux commands report success or failure through an exit status:

- `0` means success.
- A non-zero value means failure.

Many successful administrative commands print nothing. Silence from commands such
as `rm`, `install`, `systemctl daemon-reload`, or `systemctl restart` is normal.

Display the previous command's exit status with:

```bash
echo $?
```

This is useful immediately after a silent command; running another command first
replaces the saved status.

## Pagers

A pager displays long terminal output one screen at a time instead of allowing it
to scroll past. The most common pager is `less`. Text such as this indicates that
a pager is active:

```text
lines 1-20/20 (END)
```

Useful pager keys:

- `q` quits. Press the lowercase key by itself; Enter is not required.
- `Space` or `Page Down` advances one page.
- `b` or `Page Up` moves back one page.
- Arrow keys move one line.
- `g` goes to the beginning.
- `G` goes to the end.
- `/text`, followed by Enter, searches forward.
- `n` goes to the next search result.
- `N` goes to the previous search result.
- `h` opens pager help.

`Ctrl+C` may interrupt a pager or the command producing its input, but `q` is the
normal way to leave `less`.

Commands such as `systemctl` and `journalctl` may open a pager automatically. Many
support `--no-pager`, which is convenient for scripts and copied diagnostics:

```bash
systemctl status example.service --no-pager
journalctl -u example.service --no-pager
```

## Pipes

The pipe operator, `|`, passes the standard output of one command into the standard
input of another:

```bash
ps -ef | grep java
```

This does not mean “then run.” It connects the commands into one pipeline. Common
filters include `grep`, `head`, `tail`, and `less`.

## Services and systemd

On many Linux distributions, systemd manages background services. A unit commonly
has a name such as `example.service`.

Common operations:

```bash
systemctl status example.service --no-pager
systemctl start example.service
systemctl stop example.service
systemctl restart example.service
systemctl enable example.service
systemctl disable example.service
```

`start` and `stop` affect the current boot. `enable` and `disable` control whether
the service is started automatically on future boots. `enable --now` does both:

```bash
systemctl enable --now example.service
```

After editing a unit file, tell systemd to reload unit definitions:

```bash
systemctl daemon-reload
```

This reloads systemd configuration; it does not itself restart the service.

## Logs and the journal

`journalctl` reads logs stored by systemd-journald:

```bash
journalctl -u example.service -n 100 --no-pager
journalctl -u example.service -f
journalctl -k -b --no-pager
```

- `-u` selects a service unit.
- `-n 100` shows the latest 100 entries.
- `-f` follows new entries until interrupted with `Ctrl+C`.
- `-k` selects kernel messages.
- `-b` selects the current boot.

## Processes

A process is a running program. Its PID is its process identifier:

```bash
ps -eo pid,user,comm,rss,%mem --sort=-rss | head -20
```

This lists large resident-memory consumers first. Useful terms:

- PID identifies a particular process instance and changes after restart.
- RSS is physical memory currently resident in RAM.
- VSZ is virtual address space, not physical memory consumption.
- A service may create multiple processes or threads.

Send a normal termination request with `TERM`; reserve `KILL` for processes that
cannot shut down normally:

```bash
kill -TERM <pid>
kill -KILL <pid>
```

`KILL` cannot be handled by the process, so it prevents cleanup.

## RAM, cache, and swap

Inspect memory with:

```bash
free -h
swapon --show
```

In `free -h`, `available` is usually more meaningful than `free`. Linux uses idle
RAM for reclaimable filesystem cache, so very little completely free RAM is not
automatically a problem.

Swap is disk space used to hold inactive memory pages when RAM is under pressure.
It is slower than RAM but can prevent an out-of-memory event. Swap remaining in
use after pressure ends is normal; Linux does not move every cold page back merely
to make the swap-used number zero.

`vm.swappiness` influences how readily Linux uses active swap:

```bash
sysctl vm.swappiness
```

It does not create or enable swap. A very low value makes the kernel reluctant to
swap; an appropriate value depends on the workload and machine size.

## SSH sessions

SSH provides a remote shell. Leave a session cleanly with:

```bash
exit
```

or `Ctrl+D`. Inspect logged-in sessions with:

```bash
who
w
tty
```

`tty` identifies the current terminal, often `/dev/pts/0`. One SSH connection can
have multiple associated `sshd` processes, so process count alone does not prove
that multiple users are logged in.

## Disk usage and safe cleanup

Inspect usage before deleting anything:

```bash
df -h /
journalctl --disk-usage
du -xhd1 /var 2>/dev/null | sort -h
du -xhd1 /opt 2>/dev/null | sort -h
```

- `df` reports filesystem-level capacity.
- `du` reports space attributable to files under a directory.
- `-x` prevents `du` from crossing into other mounted filesystems.
- `-h` uses human-readable units, and `-d1` limits output to one level.

### systemd journal

Rotate the active journal and delete archived journal files until retained usage is
below a chosen ceiling:

```bash
journalctl --rotate
journalctl --vacuum-size=100M
journalctl --disk-usage
```

Alternatively, retain only a period of history:

```bash
journalctl --rotate
journalctl --vacuum-time=14d
```

Vacuuming targets archived journal files; rotating first makes current journal data
eligible. Keep enough history to diagnose recent incidents.

Set a persistent ceiling with a journald drop-in:

```ini
# /etc/systemd/journald.conf.d/storage-limits.conf
[Journal]
SystemMaxUse=100M
```

After creating or changing it:

```bash
systemctl restart systemd-journald
```

### Package cache

APT keeps downloaded package archives. Inspect and clear them with:

```bash
du -sh /var/cache/apt/archives
apt clean
```

`apt clean` removes cached downloads, not installed packages.

Preview packages that APT considers unnecessary before approving removal:

```bash
apt autoremove
```

Read the proposed package list carefully. Answer `N` if anything important or
unfamiliar appears; automatic dependency removal can affect system functionality.

### Temporary files and crash dumps

Clean only temporary files whose configured age has expired:

```bash
systemd-tmpfiles --clean
```

This follows system cleanup policies and is safer than recursively deleting `/tmp`.

Check for stored core dumps:

```bash
coredumpctl list --no-pager
du -sh /var/lib/systemd/coredump 2>/dev/null
```

Do not manually delete arbitrary files under `/var/lib`, `/usr`, `/boot`, or
`/etc`. These directories contain managed state, software, kernels, and system
configuration.

## Markdown for commands and logs

Use one backtick pair for short inline text such as `systemctl status`. Use fenced
blocks for multiline content:

````markdown
```bash
systemctl status example.service --no-pager
```

```text
Active: active (running)
```
````

Use `bash` for Linux commands and `text` for raw output or logs.

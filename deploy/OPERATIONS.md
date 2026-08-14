# NoThanks operations runbook

This runbook is for the current small VPS: one CPU, approximately 371 MiB of
usable RAM, and 1 GiB of swap. It assumes that the repository's optional
`no-thanks-low-memory.service` has been installed on the server under the canonical
name `/etc/systemd/system/no-thanks.service`.

## If the application becomes unavailable

Check the service without restarting it first:

```bash
systemctl status no-thanks.service --no-pager
free -h
systemctl show no-thanks.service -p MemoryCurrent -p MemoryPeak -p NRestarts
```

`code=killed, signal=KILL` together with a kernel message such as `Out of memory:
Killed process ... (java)` indicates that the kernel OOM killer terminated Java.

If SSH is responsive, stop any restart loop and collect evidence:

```bash
systemctl stop no-thanks.service
systemctl reset-failed no-thanks.service

free -h
swapon --show
ps -eo pid,user,comm,rss,%mem --sort=-rss | head -20
journalctl -k -b --no-pager | grep -Ei 'oom|out of memory|killed process|memory cgroup'
journalctl -u no-thanks.service -n 100 --no-pager
```

If SSH hangs, open the VPS provider's console and run:

```bash
systemctl stop no-thanks.service
systemctl disable no-thanks.service
systemctl reset-failed no-thanks.service
reboot
```

After the reboot, connect through SSH and verify that the effective Java command
still contains `-Xmx128m`, not the old `-Xmx512m` value:

```bash
systemctl cat no-thanks.service
```

Start the service once and give the weak CPU approximately one minute to initialize
Spring Boot:

```bash
systemctl enable --now no-thanks.service
sleep 60
systemctl status no-thanks.service --no-pager
curl -I --max-time 10 http://127.0.0.1:8080/
free -h
```

Do not repeatedly restart a failed service. If startup fails, inspect its log:

```bash
journalctl -u no-thanks.service -n 100 --no-pager
```

## Normal health checks

The application is healthy when systemd reports `active (running)`, the journal
contains `Started NoThanksApplicationKt`, and localhost port 8080 returns an HTTP
response:

```bash
systemctl status no-thanks.service --no-pager
journalctl -u no-thanks.service -n 40 --no-pager
ss -ltnp 'sport = :8080'
curl -I --max-time 10 http://127.0.0.1:8080/
```

Check current and peak service memory occasionally:

```bash
systemctl show no-thanks.service -p MemoryCurrent -p MemoryPeak -p NRestarts
free -h
ps -o pid,rss,vsz,%mem,etime,cmd -C java
```

Less than approximately 50 MiB in the `available` column of `free -h` is a warning
on this host. `free` alone is less useful than `available`, because Linux normally
uses otherwise-idle RAM for reclaimable filesystem cache.

## Constraints and recommendations

- Keep the existing 1 GiB swap enabled. Confirm it with `swapon --show` after a
  reboot.
- Keep `vm.swappiness=20` in `/etc/sysctl.d/90-no-thanks-memory.conf`. The August
  2026 OOM incident occurred with swappiness set to zero: the kernel killed Java
  at about 171 MiB RSS while the complete 1 GiB swap file was still free. An
  automatic APT upgrade reached a 191.6 MiB memory peak at the same time. With
  swappiness 20, Linux moved cold pages to swap and available RAM recovered from
  about 33 MiB to more than 140 MiB.
- Keep the optional low-memory unit's `-Xmx128m` setting. Do not replace it with
  the default unit on this host. A Java heap limit is not a limit on the complete
  JVM: metaspace, thread stacks, direct buffers, code cache, and other native
  allocations need additional RAM.
- Keep `MemoryHigh=220M` and `MemoryMax=280M`. They contain the application before
  it can consume the entire host. Hitting the hard limit may still restart the
  application, but should leave SSH and essential system services responsive.
- Keep the start-rate limit. It prevents a crash/restart loop from repeatedly
  exhausting the machine.
- Install NGINX when HTTPS is needed, then check `free -h` again. Once NGINX is the
  public reverse proxy, expose only SSH, HTTP, and HTTPS (normally ports 22, 80,
  and 443); do not expose application port 8080 publicly.
- Avoid adding memory-heavy services, containers, monitoring stacks, or databases
  to this VPS without measuring their resident memory.
- Fix abandoned-lobby retention in the application eventually. It was probably
  not responsible for the low-traffic OOM incident, but it remains a source of
  long-term memory growth.
- A VPS with at least 1 GiB RAM would provide a substantially safer operating
  margin. Until then, the low-memory systemd profile and periodic checks make the
  current server more viable.

## Package updates on the small VPS

Automatic APT upgrades are disabled deliberately on this temporary, dedicated
game server. Their transient memory use can exceed the game JVM's resident memory.
This trades automatic security patching for predictable resource use, so perform
the manual procedure below regularly, ideally every week.

Disable and verify the automatic timers:

```bash
systemctl disable --now apt-daily.timer apt-daily-upgrade.timer
systemctl is-enabled apt-daily.timer apt-daily-upgrade.timer
systemctl list-timers --all | grep -E 'apt-daily|apt-daily-upgrade' || true
```

Both `is-enabled` results should be `disabled`, and neither timer should have a
future trigger time. Leave `unattended-upgrades.service` installed; it also
provides shutdown coordination.

For controlled maintenance, stop the game before allowing APT to use the host's
limited RAM:

```bash
systemctl stop no-thanks.service
apt update
apt upgrade
dpkg --audit
free -h
```

An empty `dpkg --audit` result is healthy. If a reboot is required, reboot and let
the enabled game service start during boot:

```bash
if test -f /var/run/reboot-required; then
    cat /var/run/reboot-required
    reboot
else
    systemctl start no-thanks.service
    sleep 60
    systemctl status no-thanks.service --no-pager
    curl -I --max-time 10 http://127.0.0.1:8080/
fi
```

APT may report that a package such as `cloud-init` has a configuration-file prompt
and must be upgraded manually. This does not mean `dpkg` is corrupt; handle it
during a controlled maintenance window rather than while the game is running.

## Deploying the repository unit

The server currently may have a temporary drop-in at
`/etc/systemd/system/no-thanks.service.d/memory.conf`. A JAR-only deployment does
not remove it, so the low-memory protection remains active.

When installing the updated repository unit on this VPS, copy the low-memory
alternative under systemd's canonical service name, then reload systemd:

```bash
scp deploy/no-thanks-low-memory.service root@<server-ip>:/etc/systemd/system/no-thanks.service
ssh root@<server-ip>
systemctl daemon-reload
systemctl cat no-thanks.service
```

Only after confirming that the main unit contains the low-memory settings may the
duplicate drop-in be removed:

```bash
rm /etc/systemd/system/no-thanks.service.d/memory.conf
rmdir --ignore-fail-on-non-empty /etc/systemd/system/no-thanks.service.d
systemctl daemon-reload
systemctl restart no-thanks.service
```

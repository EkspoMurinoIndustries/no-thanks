# Service profiles

Both repository profiles are installed under the canonical path
`/etc/systemd/system/no-thanks.service`. Do not enable them as two separate units;
two JVMs would compete for memory and port 8080.

## Available profiles

- `no-thanks.service` is the default profile with a 512 MiB heap.
- `no-thanks-low-memory.service` is the constrained-host profile with a 128 MiB heap,
  Serial GC, capped metaspace, restart throttling, and systemd memory limits.

## Low-memory host preparation

The constrained profile assumes at least 1 GiB of active swap:

```bash
swapon --show
free -h
```

For constrained hosts, a swappiness value of 20 is recommended. With a value of
zero, the kernel may invoke the OOM killer while swap remains unused:

```bash
printf 'vm.swappiness=20\n' > /etc/sysctl.d/90-no-thanks-memory.conf
sysctl -w vm.swappiness=20
sysctl vm.swappiness
```

Swappiness controls use of active swap; it does not create a swap file.

## Switch profiles

Switching is intentionally a manual root action. GitHub Actions selects and
verifies a profile but cannot install it.

1. Change the workflow's single selection:

   ```yaml
   env:
     SERVICE_PROFILE: no-thanks.service
   ```

   Use `no-thanks-low-memory.service` to switch in the other direction.

2. From a trusted checkout, replace `<selected-profile>` with that exact filename:

   ```bash
   scp deploy/<selected-profile> root@<server-ip>:/tmp/<selected-profile>
   ssh root@<server-ip>
   ```

3. Inspect and install it on the server:

   ```bash
   diff -u /etc/systemd/system/no-thanks.service /tmp/<selected-profile> || true
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

4. Merge or push the workflow selection. Its comparison will now pass.

Repeat the manual installation whenever the selected profile's contents change.

## Profile mismatch

A mismatch means the uploaded selection differs byte-for-byte from the installed
unit. This occurs after selecting or editing a profile, provisioning a new server,
or manually editing the installed unit. The workflow fails before uploading the
JAR and leaves the candidate in `/opt/apps/`.

Inspect it first:

```bash
diff -u \
  /etc/systemd/system/no-thanks.service \
  /opt/apps/<selected-profile>
```

If expected, install it as root using the procedure above and rerun the workflow.

## Verify the low-memory profile

```bash
systemctl cat no-thanks.service --no-pager
systemctl show no-thanks.service \
  -p FragmentPath -p DropInPaths -p MemoryHigh -p MemoryMax -p NRestarts
sysctl vm.swappiness
swapon --show
free -h
ps -ww -o pid,rss,vsz,%mem,etime,cmd -C java
curl -I --max-time 10 http://127.0.0.1:8080/
```

Expected limits are `MemoryHigh=220M`, `MemoryMax=280M`, and `-Xmx128m` in the
Java command. `DropInPaths` should be empty when the profile is installed directly.

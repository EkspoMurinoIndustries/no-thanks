# Deployment

NoThanks is deployed as `no-thanks.jar`, run by systemd, and updated by the
GitHub Actions workflow in `.github/workflows/build-and-deploy.yaml`.

## Files

- [`no-thanks.service`](no-thanks.service) — default service profile.
- [`no-thanks-low-memory.service`](no-thanks-low-memory.service) — constrained
  profile for low-memory hosts.
- [`docs/SETUP.md`](docs/SETUP.md) — first-time server and GitHub Actions setup.
- [`docs/SERVICE_PROFILES.md`](docs/SERVICE_PROFILES.md) — selecting, installing, and
  switching systemd profiles.
- [`docs/OPERATIONS.md`](docs/OPERATIONS.md) — health checks, updates, and OOM recovery.
- [`docs/HTTPS.md`](docs/HTTPS.md) — optional NGINX and HTTPS setup.

## Build

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The output is `build/libs/no-thanks.jar`.

## Selected deployment profile

The workflow currently selects `no-thanks-low-memory.service` through its
top-level `SERVICE_PROFILE` setting. The selected profile must also be installed
manually as `/etc/systemd/system/no-thanks.service`; the workflow verifies an
exact match before uploading a new JAR.

See [`docs/SERVICE_PROFILES.md`](docs/SERVICE_PROFILES.md) before changing profiles and
[`docs/OPERATIONS.md`](docs/OPERATIONS.md) for maintenance and recovery procedures.

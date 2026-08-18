# Deployment

NoThanks is deployed as `no-thanks.jar`, run by systemd, and updated by the
GitHub Actions workflow in `.github/workflows/build-and-deploy.yaml`.

## Files

- [`no-thanks.service`](no-thanks.service) — systemd service unit sized for the
  game's current observed usage.
- [`docs/SETUP.md`](docs/SETUP.md) — first-time server and GitHub Actions setup.
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

## Service unit installation

The repository's `no-thanks.service` must be installed manually as
`/etc/systemd/system/no-thanks.service`. The deployment workflow updates only the
JAR and restarts the service.

The current JVM and systemd memory limits have proved sufficient for the game's
present single-lobby load. Increase them in `no-thanks.service` if future usage
requires more capacity.

See [`docs/OPERATIONS.md`](docs/OPERATIONS.md) for maintenance and recovery procedures.

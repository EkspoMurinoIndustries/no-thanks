## Mobile reconnection

Temporary disconnects keep a player's seat and game state. The browser reconnects
automatically and refreshes the game when returning from the background.
`no-thanks.reconnect-grace-ms` in `application.yml` defaults to five minutes.
If the host remains disconnected beyond that period, the lobby closes; cleanup
runs every five seconds. Disconnected guests without scores are removed from a
pre-game lobby after the same grace period. Active-round participants and score
history are retained while the host's lobby remains open. A new round waits for
all retained players to reconnect.

## Tests

Run the Kotlin/Spring tests:

```powershell
.\gradlew.bat test
```

The end-to-end test uses Playwright and the real application. Install its dependencies once:

```powershell
Set-Location e2e
npm.cmd ci
npx.cmd playwright install chromium
```

Then run:

```powershell
npm.cmd run test:e2e
```

On Linux or macOS, run `cd e2e`, then use `npm` and `npx` instead of their Windows command names.

The Playwright configuration starts the Spring Boot application automatically when port 8080 is free. The player-name scenario creates two isolated browser contexts, so each player has independent cookies. It covers registration, the initial player greeting, lobby creation and joining, case-preserving names, rejection and recovery from a blank rename, form submission with Enter, lobby renaming, game startup, in-game renaming, cross-player name updates, and a 360-pixel-wide player viewport.

When maintaining the test:

- Update its locators when the corresponding element IDs or accessible button names change.
- Keep assertions focused on behavior visible to a player rather than internal WebSocket details.
- Run `npx playwright install chromium` from `e2e/` again after upgrading `@playwright/test`.
- Inspect the retained trace in `e2e/test-results/` when a run fails.

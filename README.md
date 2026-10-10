## Card artwork builds

Builds require Java 11 and Node.js 20.9+ with npm (CI uses Node.js 24).
Gradle installs the pinned Sharp encoder using `npm ci`, then generates WebP
cards automatically for `build`, `bootRun`, and `test`. Subsequent builds skip
installation and generation when their inputs and outputs are unchanged.

Keep the original numbered PNGs in `src/main/resources/static/img/cards/`.
They remain unchanged and are excluded from the application JAR. Generated
files live under `build/generated/card-artwork/` and are not committed.
The encoder preserves aspect ratio, fits within 488 × 732 pixels (twice the
desktop display size), and uses WebP quality 85. Settings live in
`tools/card-artwork/generate.cjs`. Run `./gradlew generateCardArtwork` to
regenerate independently, or use `gradlew.bat` on Windows.

Use Gradle builds/runs in the IDE so generated resources are included.
Production still runs only the JAR; Node.js is required on the build machine,
not on the application server. The first build requires npm registry access.

## Game settings limits

`no-thanks.game-limits` in `application.yml` controls the smallest allowed card,
the minimum and maximum allowed values for the biggest card, and the maximum
initial tokens. `min-biggest-card` is the lower bound of the "Biggest card"
setting, not the smallest card in the deck. Default game settings remain under
`no-thanks.default-game-params` and must fit within these limits.
The server sends the limits to the settings form, so changing them requires no
HTML or JavaScript edits. Configuration changes take effect after restarting.

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

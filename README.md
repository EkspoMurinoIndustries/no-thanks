## Tests

Run the Kotlin/Spring tests:

```powershell
.\gradlew.bat test
```

The end-to-end test uses Playwright and the real application. Install its dependencies once:

```powershell
npm.cmd ci
npx.cmd playwright install chromium
```

Then run:

```powershell
npm.cmd run test:e2e
```

On Linux or macOS, use `./gradlew`, `npm`, and `npx` instead of their Windows command names.

The Playwright configuration starts the Spring Boot application automatically when port 8080 is free. The player-name scenario creates two isolated browser contexts, so each player has independent cookies. It covers registration, the initial player greeting, lobby creation and joining, case-preserving names, rejection and recovery from a blank rename, form submission with Enter, lobby renaming, game startup, in-game renaming, cross-player name updates, and a 360-pixel-wide player viewport.

When maintaining the test:

- Update its locators when the corresponding element IDs or accessible button names change.
- Keep assertions focused on behavior visible to a player rather than internal WebSocket details.
- Run `npx playwright install chromium` again after upgrading `@playwright/test`.
- Inspect the retained trace in `test-results/` when a run fails.

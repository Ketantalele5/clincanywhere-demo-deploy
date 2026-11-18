# ClincyAnywhere runtime validation

## Validation refresh — 2026-10-24

| Check | Result |
|---|---|
| `cd backend; mvn clean test` | **PASS** — 16 tests, 0 failures, 0 errors. A clean compile exposed and fixed two Java lambda parameter name collisions in `LegacyRecordEncryptionMigration`. |
| `cd frontend; npm run build` | **PASS** — Vite production build completed. |
| `cd frontend; npm run test:e2e -- --grep "marketing site opens accessible clinic sign in"` | **PASS** — 1 browser test passed after correcting its stale locator from link to button. |
| Full `npm run test:e2e` | **NOT RUN TO COMPLETION** — initial run timed out on the stale locator; remaining API-backed tests require the unavailable demo API. |
| Local API at `http://127.0.0.1:8080/api/health` | **NOT AVAILABLE** — no listener. Starting the demo API after `mvn clean` required downloading the Spring Boot parent POM, but network access was denied. |

The browser run did not complete and does not establish the status of API-backed workflows. The prior validation record below has been removed because its date and relationship to this refresh could not be established.


# Frontend guide

This folder contains the ClincyAnywhere browser app. It uses React, Vite, JavaScript modules, and plain CSS. Read this guide before adding a page or moving files.

## Start here

1. `index.html` provides the browser document and `#root` mount point.
2. `src/main.jsx` starts React, installs the global toast provider, and loads `styles.css`.
3. `src/app/ClincyAnywhere.jsx` is the application shell. It currently owns sign-in state, clinic and role selection, navigation, shared data loading, and page actions. It also selects which feature page is visible.
4. `src/features/` contains user-facing pages grouped by feature.
5. Feature pages import reusable controls from `src/components/` and shared browser/API helpers from `src/lib/`.
6. `src/lib/apiClient.js` is the shared HTTP client. Feature pages should use `apiRequest` rather than calling `fetch` directly for API requests.

## Folder map (current code)

```text
frontend/
|-- index.html                 Browser entry document
|-- vite.config.js             Vite dev server and API/WebSocket proxy
|-- package.json               Commands and frontend dependencies
|-- e2e/                       Playwright browser tests
`-- src/
    |-- main.jsx               React entry point
    |-- app/                   App shell and top-level navigation
    |-- features/              Pages grouped by user task/domain
    |   |-- auth/              Login and registration
    |   |-- dashboard/         Dashboard, pharmacy, recovery, voice assistant
    |   |-- marketing/         Public website
    |   |-- messaging/         Messages and patient support
    |   |-- platform/          Clinic onboarding and platform team
    |   |-- records/           Reusable record list and editor
    |   |-- reports/           Reports and analytics
    |   `-- settings/          Profile, clinic settings, digital health, parity
    |-- components/
    |   |-- ui/                Reusable visual controls (Button, Card, Table...)
    |   |-- shared/            Reusable domain display pieces, e.g. record badges
    |   `-- OfflineBanner.jsx  App-wide offline status banner
    |-- hooks/                 Reusable React hooks; currently realtime messaging
    |-- lib/                   API client, app configuration, formatting, i18n
    `-- styles.css             Global styles and page/component styling
```

The map describes the files that exist today. `context/` and `pages/` are not current source folders; do not add them just because a template suggests them. Prefer the existing `features/` convention.

## Where should my change go?

| If you are changing... | Start in... | Keep in mind... |
|---|---|---|
| A screen or user workflow | `src/features/<feature>/` | Put the page and feature-only components together. |
| A button, dialog, table, or other visual primitive | `src/components/ui/` | Keep it domain-neutral and export it through `index.js` when used. |
| A piece used by multiple product features | `src/components/shared/` | Use a clear domain name; do not put feature-specific UI here. |
| REST request behavior | `src/lib/apiClient.js` | It attaches the in-memory bearer token, handles timeouts/errors, and signals expired sessions. |
| File exports/downloads | `src/lib/downloadFile.js` | Use `downloadRows` for CSV/JSON downloads; it protects CSV cells from spreadsheet formulas. |
| WebSocket or server-sent events | `src/hooks/useRealtime.js` | Keep connection setup and cleanup in hooks. |
| Shared role, navigation, or record form metadata | `src/lib/appConfig.js` | The app shell and record pages both use this configuration. |
| Translation strings and language switching | `src/lib/i18n.js` | Add keys to each supported language dictionary. |
| Global visual tokens/layout styles | `src/styles.css` | Prefer existing class patterns; avoid adding inline style systems for shared design. |

## Add a page

1. Find the feature folder that owns the workflow. If there is no good fit, create `src/features/<plain-feature-name>/`.
2. Add a descriptively named page component, for example `AppointmentsPage.jsx`. Keep feature-only subcomponents in the same folder when the page grows.
3. Import shared controls from `src/components/ui/index.js`; import domain UI from `src/components/shared/` only when more than one feature uses it.
4. Call backend endpoints through `apiRequest` from `src/lib/apiClient.js`. Do not store access tokens in page state or browser storage.
5. Add styling to `src/styles.css` using feature-specific class names.
6. Register the page in `src/app/ClincyAnywhere.jsx` and navigation/role visibility in `src/lib/appConfig.js` as required by the existing pattern. The shell renders the selected page; config supplies navigation and role metadata. Frontend visibility is for navigation; the backend remains responsible for authorization.
7. If the page introduces a user-facing string, add translations in `src/lib/i18n.js` when that string belongs to the translated interface.

## Import direction

Use this simple dependency direction when adding code:

```text
main.jsx -> app -> features -> shared components / lib -> UI primitives
```

In practice, shared components and `lib` modules must not import feature pages. Feature-only code should stay inside its feature folder. The app shell may compose features, but a feature should not import `ClincyAnywhere.jsx`. This keeps dependencies one-way and makes a page easier to understand in isolation.

## Current boundaries and improvement direction

The current app is organized by feature for pages, but not every concern is fully separated yet. `ClincyAnywhere.jsx` is a large shell that also owns shared fetching and mutations; `appConfig.js` combines navigation, roles, data access metadata, and record schemas; and the global stylesheet contains styles for many features. A small example of a safe extraction is `downloadRows`, now in `lib/downloadFile.js` so the shell focuses on coordinating the app. Follow existing boundaries for small changes. When refactoring, extract one cohesive responsibility at a time, keep behavior and exports stable, and update imports in the same change. Avoid moving the whole tree as a first step.

New code should use one clear responsibility per module, descriptive names (`useNotifications`, `AppointmentsPage`, `apiClient`), and explicit imports. Keep feature-specific API/data logic close to its feature when it is not shared. Promote code to `components/shared` or `lib` only after another feature needs it. Do not create generic `utils.js`, `helpers.js`, or `common/` dumping grounds.

## Commands

Run these from `frontend/`:

```powershell
npm install
npm run dev
npm run build
npm run test:e2e
```

The Vite dev server proxies `/api` and `/ws` to `http://127.0.0.1:8080` by default. Set `CLINCANYWHERE_API_TARGET` to override the API target. `VITE_API_URL` can set a frontend API base URL where needed. See the root `README.md` for starting the local demo API and test prerequisites.


## Install ClincyAnywhere

The production frontend is a Progressive Web App (PWA). Serve it over HTTPS (or localhost during development), then:

- **Desktop/laptop (Chrome or Edge):** open the site and choose the install icon in the address bar, or use the in-app **Install app** button when available.
- **Android (Chrome/Edge):** open the HTTPS site and tap **Install app** or choose **Add to Home screen** from the browser menu.
- **iPhone/iPad (Safari):** open the HTTPS site, tap **Share**, then **Add to Home Screen**.

The installed app shell can reopen offline after a successful visit. Clinic API requests and patient data are never cached by the service worker; sign-in, live records, and messaging still require a network connection to the configured backend. Set `VITE_API_URL` to a backend origin reachable from the installed device when the frontend and API are deployed separately. A Vite development proxy at `127.0.0.1` is only suitable for development on the same computer, not for a phone installation.

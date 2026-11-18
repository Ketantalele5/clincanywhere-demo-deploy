# Free-tier deployment guide

## What can run on Cloudflare Pages

Cloudflare Pages can host this repository's built React/Vite frontend. It cannot
run the Spring Boot Java API, PostgreSQL, or the RabbitMQ/STOMP services that
the production profile requires. Deploying the frontend alone will show the
public website, but sign-in, clinic data, and messaging will not work until a
reachable API and its dependencies are deployed separately.

This project handles clinical information. Do not put real patient data on a
free/demo deployment. Hosting a container or passing a health check is not
healthcare production approval; read [PRODUCTION-RELEASE-GATES.md](./PRODUCTION-RELEASE-GATES.md)
and [ARCHITECTURE.md](../ARCHITECTURE.md) before considering any production
use. Free plans may sleep, have limited database retention, or change their
limits. Verify each provider's current terms and backup behavior before use.

## Deploy the frontend to Cloudflare Pages

1. Push the repository to a Git provider connected to Cloudflare Pages.
2. Create a Pages project and set:
   - **Root directory:** `frontend`
   - **Build command:** `npm run build`
   - **Build output directory:** `dist`
3. Add the build environment variable `VITE_API_URL` with the HTTPS origin of
   the separately deployed API, for example `https://api.hspmamanagement.eu.org`.
   Use the API origin only (no `/api` suffix). Vite embeds this value at build
   time, so rebuild after changing it.
4. After the first Pages deploy, add `hspmamanagement.eu.org` as a custom
   domain in the Pages project. Complete the DNS instructions Cloudflare
   displays. The domain must be delegated/configured to use the DNS provider
   required by that setup; the project cannot change domain registration or
   nameservers on your behalf.
5. Once the final frontend URL is known, set the API's
   `CORS_ALLOWED_ORIGINS` to that exact HTTPS origin, for example
   `https://hspmamanagement.eu.org`, then redeploy/restart the API.

The existing frontend reads `VITE_API_URL` for HTTP requests and WebSockets.
Keep the API HTTPS-enabled: browsers on the HTTPS Pages site block insecure
HTTP API and WebSocket connections.

## Backend requirements

Deploy `backend/Dockerfile` on a container-capable Java host and provide durable
PostgreSQL plus RabbitMQ services with both AMQP and STOMP endpoints accessible
to the API. Set the API's environment variables/secrets from the backend
configuration documented in [README.md](../README.md) and `.env.example`.
At minimum, configure the database, strong independent JWT/data-encryption/
phone-fingerprint keys, CORS origin, strong AMQP and STOMP credentials, and
the shared-events setting. Do not expose broker ports publicly.

The `prod` profile intentionally refuses to start without the required
services, strong secrets, and release-gate approval/evidence reference. Do not
disable these checks or substitute the local H2 demo profile for a deployment.
The host's container runtime must pass its assigned port through as `PORT` or
`SERVER_PORT`; keep the management port private.

There is no backend deployable on Cloudflare Pages, and no provider accounts,
DNS access, database/broker services, or production secrets are available from
this repository. Therefore this guide prepares the frontend/backend
configuration but does not claim that a public deployment has been completed.

## Separate public test/demo deployment

For role testing, use the backend's `demo` profile on a separate public demo
service—not on the production API. It seeds synthetic records and the existing
shared demo accounts, uses an in-memory H2 database, disables WhatsApp and
RabbitMQ fanout, and uses the in-process WebSocket broker. The database is
volatile and resets when the service restarts. Never enter real patient data.

Set `SPRING_PROFILES_ACTIVE=demo`, allow only the frontend's HTTPS origin in
`CORS_ALLOWED_ORIGINS`, and set the frontend build variables `VITE_API_URL` to
that demo API origin and `VITE_DEMO_MODE=true`. The demo login panel then shows
the seeded staff/platform roles and shared test password. Keep
`VITE_DEMO_MODE=false` for production builds. Patient login remains invitation
based and has no shared seeded patient account.

This repository does not contain a connected source-code remote or an
authorized Java hosting service, so the demo backend has not been published.
The current Pages URL is only a static frontend until a backend host is
provisioned and the API origin is configured.

The root `render.yaml` provides a starting point for a free Render Docker web
service using the demo profile. Render must be connected to a Git repository
containing this project before it can build the backend. After Render deploys,
set the frontend's `VITE_API_URL` to the Render service HTTPS origin and
`VITE_DEMO_MODE=true`, then rebuild and redeploy Pages. Free services can sleep
and their ephemeral filesystem/in-memory H2 database is not durable.

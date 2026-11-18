# ClincyAnywhere clinic operations workspace

ClincyAnywhere is a React + Spring Boot multi-clinic application with a H2 development database and PostgreSQL production profile. The UI includes clinic operations, authenticated messaging, SSE notifications, and a queued WhatsApp integration.

> **Development/demo system:** This repository is not production-ready for real patient information or clinical decisions. The H2 profile and seeded records are for local development only. The production-like Compose profile uses PostgreSQL, but deployment alone does not make this application suitable for healthcare production. Complete the governance, security, privacy, clinical safety, and operational controls listed in [ARCHITECTURE.md](ARCHITECTURE.md) and obtain the required organizational approvals before any real patient use.

## Requirements

- Java 17+
- Maven 3.9+
- Node.js 20.19+ or 22.12+ (required by Vite 8)

## Run locally

The public ClincyAnywhere product site is served at the Vite root and includes a clinic onboarding request form. Submissions are validated and stored in the `clinic_leads` table through `POST /api/public/clinic-onboarding`. The platform owner reviews and assigns each enquiry in the platform dashboard; an onboarding teammate follows up and can provision the workspace and first clinic admin after the clinic is ready.

In VS Code, run **Terminal → Run Task → ClincyAnywhere API (demo)** to start the local API with the seeded demo accounts. This uses a separate local H2 database at `backend/data/clincanywhere-demo` and does not target the production PostgreSQL settings in `.env`.

Start the Spring Boot API with the local demo profile and H2 database (so production `.env` values do not override the demo settings):

```powershell
cd backend
.\run-demo.cmd
```

In another terminal, start the React app:

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal. The Vite development server proxies `/api` requests to the local-only API at `http://127.0.0.1:8080`. H2 persists data in `backend/data/clincanywhere`.

Demo password for all roles: `demo123`. Clinic role usernames: `admin`, `reception`, `doctor`, `labtech`, `pharmacy`, `billing`, and `store`. Platform role usernames: `owner` and `onboarder`.

### Isolated public demo profile

The `demo` Spring profile seeds the same synthetic demo records/accounts as `dev` and `test`, uses an in-memory H2 database, disables external WhatsApp and RabbitMQ fanout, and uses the simple in-process WebSocket broker. Start it only on a separate demo service, never on a production service with real customer data:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

In the frontend build environment for that separate demo deployment, set `VITE_DEMO_MODE=true` and `VITE_API_URL` to the demo API's HTTPS origin. Existing staff and platform demo usernames and their shared `demo123` password are listed above; the patient role is invitation-based and is not a seeded shared account. Anyone can use a public shared demo login, so enter only synthetic data. The in-memory database resets when the backend restarts and must never be treated as durable storage.

### Clinic onboarding workflow

- Sign in as `owner` to see onboarded-clinic totals, the full prospect pipeline, and outreach-team performance. The owner can create onboarding teammate accounts and assign public enquiries to them.
- Sign in as `onboarder` to record clinics found through outreach and work on leads assigned to that teammate. Onboarding accounts cannot access clinic patient, billing, or staff data.
- Move a lead through New, Contacted, and Qualified. **Set up clinic** creates its workspace and its first clinic-admin login together. That admin signs in separately and manages staff for that clinic only.
- Website enquiries are visible to the platform owner until assigned. “Onboarded clinics” counts converted customer leads, not the bundled sample clinics.

### Clinic billing payments

The Billing & Payments page supports staff-recorded cash receipts and online Razorpay Checkout. A payment receipt is stored in a separate clinic-scoped ledger and updates the invoice balance; invoice edits cannot overwrite payment totals. Existing demo invoices keep their current paid amount, but those historical amounts do not appear as new ledger receipts.

Online checkout stays disabled until `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` are configured on the server. Start with Razorpay test-mode credentials, configure the `payment.captured` and `payment.failed` webhook events to call `/api/payments/webhooks/razorpay`, and test the public webhook URL before using live credentials. The key secret and webhook secret must remain server-side. Cash receipt recording works without gateway credentials.

The H2 console is disabled; inspect local development data through the application or a trusted database tool. Demo credentials and records are test fixtures and must never be enabled or copied into a production environment.

## Source and test layout

- `frontend/src/features/` contains one folder per user-facing feature; shared UI lives in `frontend/src/components/`, with reusable hooks and utilities in `frontend/src/hooks/` and `frontend/src/lib/`.
- `frontend/e2e/` contains the current browser tests. Playwright reports, screenshots, and traces are written under `tests/artifacts/`.
- `backend/src/main/` contains the production API source and resources. Backend tests are isolated under `backend/src/test/`.
- `tests/legacy/` preserves older browser tests for historical reference; they are not part of the current Playwright run.

## Backend platform configuration

The API uses Liquibase-managed schema (`backend/src/main/resources/db/changelog`) and Hibernate schema validation. Profiles are `dev` (default), `test`, and `prod`. Copy `.env.example` to `.env`; Compose injects its values into the production-like stack, and Spring Boot imports `.env` when running locally from the repository or backend directory. Replace every `CHANGE_ME` value and generate separate 32-byte base64 keys for `APP_DATA_ENCRYPTION_KEY` and stable `APP_PHONE_FINGERPRINT_KEY` with `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`. Never commit production credentials. Production requires PostgreSQL credentials, a strong `JWT_SECRET`, explicit `CORS_ALLOWED_ORIGINS`, and an approved release checklist evidence reference; the `prod` profile fails startup until that gate is set. Patient registration requires an invitation; staff must be provisioned by the operator. Do not enable `BOOTSTRAP_ADMIN_ENABLED`: that legacy setting cannot safely identify a clinic and now fails closed. Provision the first clinic administrator by converting an authorized onboarding lead, which creates the clinic profile and a single-clinic admin membership transactionally. `BOOTSTRAP_PLATFORM_OWNER_ENABLED` is the one-time mechanism for creating the platform owner; disable/remove it after startup. Demo accounts receive sample clinics only in dev/test.

Authentication is a modular `auth` feature: `POST /api/auth/login` accepts username/password and returns a short-lived signed JWT. Protected API calls must send `Authorization: Bearer <accessToken>`. Demo accounts (`admin`, `reception`, `doctor`, `labtech`, `pharmacy`, `billing`, `store`; password `demo123`) are seeded only in `dev` and `test`. Production has no default accounts. For a first platform owner, temporarily set `BOOTSTRAP_PLATFORM_OWNER_ENABLED=true`, `BOOTSTRAP_PLATFORM_OWNER_USERNAME`, and a 16-72 byte `BOOTSTRAP_PLATFORM_OWNER_PASSWORD`; disable/remove those settings after the first successful startup. The platform owner can provision onboarding teammates from the platform dashboard. Set `JWT_SECRET` to Base64-encoded random key material of at least 32 bytes and rotate it using your secret manager. Role-based write checks run on the backend, and clinic reads/writes/chat are gated by clinic membership on the account. The platform-owner account is a cross-hospital administrator: it can select any registered clinic and use clinic administrator features, including patient records. Onboarding teammates have no clinic membership and can access only leads assigned to them or created by them, not clinic data. Patient accounts require an invitation token issued by a clinic doctor/admin for a specific patient record (`POST /api/{clinicId}/patient-invitations`); patients can view only that record and message clinic doctors/admins. The invitation is one-use, expires after 24 hours, and only its hash is stored.

Authenticated users can read/update their own display name, email and phone at `/api/account/profile`; they can change their password at `/api/account/password` after confirming the current password. Clinic admins can list clinic-scoped team profiles and create staff accounts at `/api/clinics/{clinicId}/team`. Staff creation does not permit ADMIN authority; initial production admin access uses the one-time bootstrap settings above. The Settings page includes personal profile and password forms, plus team provisioning for admins. Patient invitation signup collects a profile name and contact details.

Spring Security is stateless and validates Bearer JWTs in all profiles. CSRF is disabled for this stateless JSON API because the browser sends bearer tokens in the Authorization header rather than cookies. Enable CSRF if cookie-based browser sessions are introduced. CORS does not allow credentialed cookies. Avoid exposing Actuator beyond trusted monitoring networks. API failures use RFC 7807 problem responses and include a server-generated `X-Correlation-ID` for support and log lookup.

`GET /api/{clinicId}/notifications/stream` streams authenticated, clinic-scoped server-sent events; recent message notifications are available at `GET /api/{clinicId}/notifications`. Direct messages are persisted and emitted after transaction commit to private `/user/queue/messages` subscriptions. The Docker Compose production-like stack provides RabbitMQ STOMP relay for cross-instance WebSocket user destinations and RabbitMQ fanout for local SSE emitters. Notifications remain persisted and can replay after an SSE reconnect. WebSocket clients may subscribe to their private queues but cannot publish messages.

Docker Compose includes an internal RabbitMQ STOMP broker and notification fanout for multiple API replicas. Set unique high-entropy `RABBITMQ_DEFAULT_USER` (at least 16 characters) and `RABBITMQ_DEFAULT_PASS` (at least 32 characters) values in `.env`; Compose requires them, production startup validates them, and broker ports are not published publicly. To add API replicas, run `docker compose up --build --scale api=3`. Nginx resolves the API service dynamically, STOMP user destinations are broadcast between API nodes, and SSE notifications fan out through RabbitMQ; disconnected SSE clients replay persisted notifications after reconnect. Prometheus discovers each API replica. Keep the aggregate Hikari pool below the PostgreSQL connection budget: `replicas × DB_POOL_MAX_SIZE`, leaving headroom for migrations, monitoring and administration. Choose pool/thread sizes from measured load, not replica count alone.

Health/readiness endpoints are under `/actuator/health`; Prometheus metrics are at `/actuator/prometheus`. Production runs management endpoints on internal port 8081; the compose file does not publish that port to the host. `docker compose up --build` starts PostgreSQL, the API, Prometheus, Grafana, and an OTLP collector. Grafana is at port 3000 (admin password from `GRAFANA_ADMIN_PASSWORD`) with a provisioned Prometheus data source and ClincyAnywhere API/JVM dashboard. The collector accepts OTLP metrics/traces and logs to its debug exporter. API JSON logs are also written to a shared local volume; enable `docker compose --profile splunk up --build` with `SPLUNK_HEC_HOST`, `SPLUNK_HEC_PORT`, and `SPLUNK_HEC_TOKEN` to start the Fluent Bit HEC forwarder. Do not send clinical content in logs.

`POST /api/{clinicId}/whatsapp` queues delivery through Meta WhatsApp Cloud API when `WHATSAPP_ENABLED=true` and Meta credentials are set. This integration is a fixed Meta client, not the generic HTTP notification-provider contract described above. A client checkbox alone is insufficient: doctor/admin must first record an opaque external consent evidence reference through `/api/{clinicId}/whatsapp/consents`; the send path and worker both check active consent. Verified inbound `STOP`/`UNSUBSCRIBE`/`END`/`CANCEL`/`QUIT` webhooks revoke consent and suppress queued sends. Set `WHATSAPP_CLINIC_ID` to the clinic mapped to this configured Meta number. Signed provider status callbacks persist the provider message ID and delivery state. Delivery remains at-least-once around provider acceptance/process failure; validate the consent evidence source, templates, opt-out operation, and callback behavior before patient use. The in-app chat is separate.

To build the executable jar: `cd backend; mvn clean package`; run it with `java -XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=70 -XX:+UseG1GC -Xlog:gc*:stdout:time,level,tags -jar target/clincanywhere-api-0.0.1-SNAPSHOT.jar`. Tune JVM flags from measured container limits and GC telemetry. Async work uses a bounded executor with caller-runs backpressure; avoid blocking network operations while holding database transactions.

## Validate the application

The current verification results and any unavailable checks are recorded in [runtime-validation-report.md](runtime-validation-report.md). Historical results must be labeled with their verification date and must not be presented as current results.

With the API and Vite development server running, execute the browser end-to-end suite:

```powershell
cd frontend
npm run test:e2e
```

The current Playwright suite checks the marketing-to-sign-in flow, admin login and H2-backed clinic loading, and invalid-credential handling. The older broader workflow scripts are archived under `tests/legacy/` until they are updated for the current onboarding and account flows. Run the backend integration tests and frontend production build with `cd backend; mvn test` and `cd frontend; npm run build`.

## Production readiness

The production profile is a deployment baseline, not a healthcare production certification. Before using real patient data, complete the controls in [ARCHITECTURE.md](ARCHITECTURE.md), including verified tenant isolation, formal audit and retention, encryption and key management, tested backup/restore and disaster recovery, abuse protection, privacy and threat reviews, clinical validation, and monitoring/incident response. Use a supported external database for production; H2 is intended for local development and automated tests.

For Cloudflare Pages frontend setup and the separate Spring Boot backend requirements, see [docs/FREE-TIER-DEPLOYMENT.md](docs/FREE-TIER-DEPLOYMENT.md).

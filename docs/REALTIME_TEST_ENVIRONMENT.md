# StayEasy integrated test environment

This workflow is for local development and test data only. Seeded listings are explicitly marked **DEMO DATA**; they are not real rentals or verified availability. Online checkout must use Razorpay test-mode credentials. Never use production credentials in local development.

## Start the backend and database

Prerequisites: Docker Desktop with Compose v2, Python 3.12+ (for helper scripts), and Android Studio / API 35 emulator for the mobile app.

From the repository root:

```bash
python tools/start_test_stack.py
```

The helper creates a local-only `.env` with random database/JWT/dev-token secrets if one does not exist, then builds and starts PostgreSQL + FastAPI and enables the development seed fixtures. It prints the dev token password once; keep it private. The API is at http://localhost:8000, health is http://localhost:8000/health and interactive API documentation is http://localhost:8000/docs. The service is bound to loopback by default; do not expose this development backend to the public internet.

Seed data includes three Pune demo listings, room/bed inventory, one active demo tenant stay, and an open rent charge. Rerunning the seed is idempotent. These records are for end-to-end testing only and are not real/live housing supply.

## Connect Android to the API

1. Start the Android API 35 emulator and install the latest `stayeasy-debug-apk` artifact from the successful Android APK workflow.
2. Open StayEasy → Profile / feature hub → **API connection & sync**.
3. Set the base URL to `http://10.0.2.2:8000` (Android emulator alias for the host computer).
4. Tap the connection check and load server listings. The app will also refresh published server listings while the foreground app is open, about every 30 seconds. Server rows and local preview rows have different provenance labels.
5. For development-token testing, request a token at `POST /auth/dev-token` with the subject `stayeasy-demo-owner` or `stayeasy-demo-tenant`, matching role `owner` or `customer`, and the generated `DEV_TOKEN_PASSWORD`. Use the token with the matching role; do not paste it into screenshots or issues.

For a physical phone, use a trusted HTTPS development tunnel to the local API and enter its HTTPS URL in the app; do not enable plain HTTP for arbitrary LAN/public hosts. The default loopback-only Compose bind intentionally avoids exposing the dev API.

## Inspect integration readiness from the Android app

From **API connection & sync**, use a short-lived owner/admin token and tap **Test connection**. The **Integration readiness** section shows whether a partner feed is configured, the worker's last attempt and last successful sync, the last successful batch's created/updated/skipped counts, the number of imported feed listings, and whether Razorpay checkout/webhook configuration flags are present.

The authenticated diagnostic endpoint is `GET /owner/integrations/status`. It never returns the property-feed URL, feed bearer token, Razorpay key ID, API secret, or webhook secret. A configured URL does not prove that the worker is running; use the last-success timestamp and batch counts to confirm delivery. `not_run` means the worker has not recorded a sync yet; `error` includes only a safe error category. Razorpay configuration flags are not an end-to-end checkout test.

The feed worker records status in the `integration_sync_state` table. Apply Alembic migrations before enabling the worker in staging/production. In local development, the worker creates local schema as needed.

## End-to-end rent/payment checks

- Tenant token: `stayeasy-demo-tenant`, role `customer` → `GET /payments/charges` should return the seeded open charge.
- Owner token: `stayeasy-demo-owner`, role `owner` → owner dashboard and online payments/refund history must be property-scoped.
- Set `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` in local `.env` only with **Razorpay test-mode** values. Configure the webhook to the API tunnel's HTTPS `/payments/webhooks/razorpay` route.
- Without test keys, the backend, charge ledger and payment status/recovery screens can be tested, but no checkout order or provider-confirmed refund can succeed. StayEasy does not include a fake "paid" button.
- Verify a charge becomes paid only after provider capture is confirmed, interrupted checkout can be refreshed, duplicate webhook events do not double-post, and a refund is not marked processed until provider confirmation.

## Configure a real external property feed

Real rental inventory must come from a provider or owner-authorised feed; StayEasy does not scrape marketplaces or fabricate availability. The optional polling worker accepts an authorised JSON feed endpoint configured with `PROPERTY_FEED_URL`, `PROPERTY_FEED_SOURCE`, optional `PROPERTY_FEED_TOKEN`, and `PROPERTY_FEED_POLL_SECONDS` (minimum 60 seconds). Start it with:

```bash
docker compose --profile ingestion up --build -d
```

The provider must return either a JSON array or `{ "items": [ ... ] }`. Each item must include `external_id`, `name`, `locality`, `city`, and `rent_from`; optional fields include `deposit_from`, `food_monthly`, `utilities_estimate`, `description`, `address`, `pin_code`, `latitude`, `longitude`, `eligibility`, and `couples_welcome`. Ingestion upserts on provider + external ID, labels rows as unverified, and reports failures without logging the auth token or feed payload. A real provider endpoint/API key is required before actual external listings can appear.

## Reset local test data

To wipe the dev database and recreate test fixtures:

```bash
docker compose down -v
python tools/start_test_stack.py
```

Warning: `docker compose down -v` permanently deletes the local test database and its records.

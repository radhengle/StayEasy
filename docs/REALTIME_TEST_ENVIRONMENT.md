# StayEasy integrated test environment

This workflow is for local development and test data only. Seeded listings are explicitly marked **DEMO DATA**; they are not real rentals or verified availability. Online checkout must use Razorpay test-mode credentials. Never use production credentials in local development.

## Start the backend and database

Prerequisites: Docker Desktop with Compose v2, Python 3.12+ (for helper scripts), and Android Studio / API 35 emulator for the mobile app.

From the repository root:

```bash
python tools/start_test_stack.py
```

The helper creates a local-only `.env` with random database/JWT/dev-token secrets if one does not exist, then builds and starts PostgreSQL + FastAPI, a local Mailpit verification-email inbox, and the development seed fixtures. It prints the dev token password once; keep it private. The API is at http://localhost:8000, health is http://localhost:8000/health and interactive API documentation is http://localhost:8000/docs. The Mailpit inbox is at http://127.0.0.1:8025. The service is bound to loopback by default; do not expose this development backend to the public internet.

Seed data includes three Pune demo listings, room/bed inventory, one active demo tenant stay, and an open rent charge. Rerunning the seed is idempotent. These records are for end-to-end testing only and are not real/live housing supply.

## Register and sign in with real accounts

1. Start the local stack with `python tools/start_test_stack.py`.
2. In the emulator, open **API connection & sync** and set the base URL to `http://10.0.2.2:8000`.
3. From the first screen, choose **Tenant / Customer → Register** or **Property Owner → Register** and submit a real email address plus a password of at least 12 characters.
4. Open `http://127.0.0.1:8025) on the host computer, open the StayEasy verification message, copy its six-digit code into the app, and verify the account. This is a local email-catcher, not a real outbound email service.
5. Sign in with the newly registered email and password. The account/token are stored in the backend; closing/reopening the app should restore the account session while it remains valid.
6. Owner sign-up deliberately does **not** grant Owner permissions. It creates a verified customer account with an Owner-access request awaiting admin approval. To test the protected Owner workspace, use a local development admin session to approve it; production Owner access must remain approval-gated.

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

## Real data: what is available and what is not

The local database is connected to the real PostgreSQL-backed API and supports real account-created owner listings, tenant enquiries, reservations and rent charges. The three preloaded Pune listings are clearly labelled demo fixtures and must not be presented as actual vacant rental inventory. The polling ingestion worker can load live rental records from an external provider only after `PROPERTY_FEED_URL` (and any required `PROPERTY_FEED_TOKEN`) is configured for a feed you are authorised to use. No authorised property-feed URL is supplied in this repository, so a live rental marketplace feed is not yet connected. Do not scrape rental portals or display test fixtures as real available homes.

For now, create a listing through the signed-in Owner workflow (after local admin approval) and verify it appears in the customer listing search via the backend. This tests the complete owner-to-tenant data path using user-entered listing data rather than fabricated “live” supply.

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

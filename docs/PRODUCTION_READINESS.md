# StayEasy — production-readiness gates

## Authentication foundation currently implemented (not launch-ready)

- The backend exposes `/auth/register`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/login`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/me` and `/auth/logout`.
- Passwords are stored as scrypt hashes. Verification/reset codes are short-lived (15 minutes), hashed with a server-secret HMAC, limited to five attempts, and never returned in API responses or written to application logs. Sign-in locks after five failed passwords for 15 minutes.
- Real account access tokens carry an auth version checked against the database on each request. Password resets and logout invalidate existing account tokens. Tokens are still short-lived bearer credentials, not refresh tokens.
- Registration always creates a `customer` role. Requesting the Owner workspace records intent only; it cannot confer owner permissions. An approved owner-onboarding/review route is still needed before owner accounts can be promoted.
- Configure email through environment variables: `SMTP_HOST`, `SMTP_FROM_EMAIL`, `SMTP_PORT` (587 with STARTTLS, or 465 with TLS), and optional `SMTP_USERNAME` / `SMTP_PASSWORD`. Registration and recovery fail clearly when SMTP is unavailable; no email provider has been configured by this code change.
- Alembic bootstrap is added. Run `python -m alembic -c backend/alembic.ini upgrade head`. The first revision uses the current SQLAlchemy metadata to create missing tables; before public production, freeze/review it as an explicit schema baseline and compare it against any existing database. Do not treat this bootstrap as a completed migration audit.
- Android API-client wrappers are added, but no interactive sign-in/verification/recovery screen is wired yet, and the current client stores the token in memory only. Secure on-device storage, refresh-token rotation, device/session management, external rate limiting, MFA, and security review remain release gates.

## Must be completed before public launch

- Integrate Android with the API using a typed network client, TLS, safe token storage, expiry/refresh/revocation and clear server-confirmed UI state. Do not assume local demo records have synced.
- The account API now includes password registration, expiring email verification codes, sign-in, reset codes, account profile and server-side session invalidation. Before launch, wire the Android screens to these routes, configure and verify production email delivery, implement approved owner onboarding and operator MFA, and add edge/rate-limit protections.
- Apply Alembic migrations; do not rely on ORM `create_all()` after real customer data exists. Back up the database and rehearse restore/PITR.
- Add secure object storage with signed uploads/downloads, image type/size validation, malware screening, compression, moderation and access controls for documents.
- Configure Maps SDK, Places API and routing only with restricted keys, billing controls, data minimization and attribution. Never display uncomputed distances as facts.
- Integrate a payment provider and verify webhooks server-side. Idempotency, settlement, refunds, chargebacks and provider reconciliation must have automated tests. Manual ledger rows must remain visibly distinct from provider-confirmed payments.
- Add notification delivery workers, push tokens, email/SMS provider templates, retries, idempotency and delivery status.
- Add scheduled billing/expiry/availability reminders and idempotent jobs; test time zones, pro-rata policies, notice periods and edge dates.
- Build complete privacy requests, data export, account deletion, retention schedule, consent records and secure document handling. Obtain local legal/privacy review before production.
- Add abuse reporting, fraud escalation, verified-review moderation, appeal workflow, evidence retention, admin action logs and staff offboarding.
- Add rate limits, input/output limits, request IDs, safe error responses, audit monitoring, dependency scanning, vulnerability review, incident response and HTTPS reverse proxy.
- Test tenant isolation and cross-property staff scopes; test reservation races against PostgreSQL with parallel requests.
- Complete Android UI tests, API tests, migration tests, accessibility test, small/large device test, low-connectivity test, crash analytics and staged release checks.

## Scope explicitly not claimed as complete

The current package is an implementation foundation, not a public launch-ready marketplace: Google Maps/Places, real payment processing, push notification delivery, document/object storage, Android↔API syncing, production authentication, full multi-lingual resources, iOS app and subscription billing are not active integrations.

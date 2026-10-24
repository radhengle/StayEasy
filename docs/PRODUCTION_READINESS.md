# StayEasy — production-readiness gates

## Must be completed before public launch

- Finish production Android/API integration using the server-backed finance ledger as the current vertical slice. Add secure token storage, expiry/refresh/revocation, explicit session/account state and consistent loading/error/offline reconciliation across every workflow. Do not assume local demo records have synced.
- Replace dev-token bootstrapping with verified phone/email identity, account recovery, a Tenant/Owner role-selection and onboarding flow (for example, “Looking for a place to stay?” versus “Have a property to rent?”), MFA for operators and least-privilege admin access.
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

The current package is an implementation foundation, not a public launch-ready marketplace. A development-session-backed finance sync and manual ledger flow exist, but production identity/session management and end-to-end sync for every workflow are not complete. Google Maps/Places, real payment processing/webhooks, push notification delivery, document/object storage, database migrations, production deployment, full multilingual resources, iOS and subscription billing are not active integrations.

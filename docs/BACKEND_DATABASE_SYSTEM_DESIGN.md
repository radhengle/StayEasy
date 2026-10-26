# StayEasy — Backend, Database & System Design

## Status and scope

This document describes the current architecture and the target production boundaries. StayEasy already has a Kotlin/Jetpack Compose Android client, a FastAPI backend, SQLAlchemy models, PostgreSQL, Alembic migrations, a development Compose stack, account verification, owner-access approval, property/inventory/reservation workflows, a property-feed worker, and a Razorpay payment foundation. These are not equivalent to a production deployment: provider credentials, authorized real listing supply, staging acceptance, operational controls, and deployment still need to be completed and verified.

## High-level architecture

```text
Android (Kotlin / Jetpack Compose)       Admin web console
                | HTTPS / JSON                  | HTTPS / JSON
                +---------------+---------------+
                                |
                         FastAPI application
       +------------------------+-------------------------+
       |                        |                         |
 Authentication &          Marketplace and          Payments / finance
 authorization             accommodation domain    Razorpay adapter
       |                        |                         |
       +------------------------+-------------------------+
                                |
                    SQLAlchemy transaction boundary
                                |
                         PostgreSQL database
                                ^
                                |
               Authorized feed-ingestion worker
               (separate Compose ingestion profile)

External dependencies: SMTP provider, Razorpay, authorized property-feed provider,
and (when implemented/configured) object storage, maps and push delivery.
```

The API is the source of truth for identity, roles, permissions, prices, availability, booking state, and payment status. Clients must not grant themselves Owner/Admin privileges, decide final prices, or mark charges paid.

## Runtime components

- **Android client:** UI, secure token storage, input validation, and API calls. Local/demo previews must be visibly distinguished from API-backed records. API base URL is configurable for emulator and staging.
- **FastAPI:** versioned business API, authentication dependencies, role checks, validation, transactional domain operations, and OpenAPI docs.
- **PostgreSQL:** durable source of truth. Use PostgreSQL—not SQLite—for integration tests of constraints, transaction behavior, and reservation concurrency.
- **Alembic:** schema changes are explicit, ordered migrations. Production startup must run reviewed migrations as a deployment step; do not rely on automatic table creation.
- **Feed worker:** optional separate process that only imports data from a permitted provider endpoint/dataset. Store source/provenance and sync diagnostics; never label fixtures or unverified scraped data as real availability.
- **Payment adapter:** Razorpay order creation, signature verification, server-side payment status lookup, signed webhook handling, idempotency, refund handling, and ledger reconciliation. Live collection requires configured credentials and end-to-end staging acceptance.
- **Admin console:** restricted operational interface. Administrative actions must be API-authorized and audited; production admin accounts require MFA and least privilege.

## Core relational model

The authoritative SQLAlchemy model definitions and migrations remain the executable schema. The following is the conceptual domain map; use actual model/migration files when implementing schema changes.

| Domain | Principal records | Important invariants |
|---|---|---|
| Identity | `users`, `user_credentials`, `tenant_profiles`, owner-access request state | Unique normalized email; password hashes only; server-controlled role; verification and account status enforced by API |
| Property supply | properties, property media/amenities, owner association, listing review | Every listing has an accountable owner/source; only approved/published listings are public |
| Inventory | units/beds/rooms and unit status | A unit cannot be simultaneously available and occupied; ownership scope is checked on every write |
| Tenant journey | enquiries, visits, reservations, stays | Separate lifecycle records; tenant and owner access is scoped to the relevant record; reservation expiry and status transitions are enforced |
| Finance | rent charges, payment attempts/orders, ledger entries, refunds and webhook/idempotency references | Amounts computed server-side in integer paise; unique provider identifiers; payment state only advances from verified provider evidence |
| Operations | maintenance issues, notices, notifications, staff, meal plans, housekeeping, visitors | Property-scoped authorization and auditability for sensitive state changes |
| Quality and integrations | reviews/moderation, audit events, feed sync state | Moderation is role-protected; feed diagnostics exclude secrets; source provenance is retained |

Use foreign keys, unique constraints, check constraints, indexes for common filters, and transactions for multi-record state changes. Avoid trusting a client-supplied owner ID, price, payment state, or arbitrary record ID without authorization checks. Before importing legacy production data, audit existing rows and repair orphaned references before enforcing foreign keys.

## API and transaction rules

1. Validate all request bodies and normalize email and provider identifiers before database operations.
2. Authenticate the caller and authorize both the role and record/property scope.
3. Read current state and calculate pricing on the server.
4. Commit related state changes atomically; translate known uniqueness/conflict failures into accurate HTTP errors rather than generic messages.
5. Use idempotency keys for retried booking/payment operations and webhook event processing.
6. Treat external calls as fallible. Persist a safe pending/unknown state when a provider timeout makes the outcome ambiguous; reconcile with the provider rather than charging again blindly.
7. Return structured errors with request IDs; never return secrets, verification codes, password hashes, or raw provider credentials.
8. Record audit events for owner approvals, listing publication, payment/refund changes, and administrative decisions.

## Property data ingestion and provenance

- **Local test fixtures:** clearly labelled demo data used to exercise search, inventory, booking and payment flows. They are not real rental offers.
- **Owner-entered listings:** persist in PostgreSQL with an owner identity and moderation state.
- **External supply:** enable the feed worker only after a provider authorizes access and supplies a stable endpoint or approved dataset and credentials. Configure `PROPERTY_FEED_URL`, `PROPERTY_FEED_SOURCE`, and `PROPERTY_FEED_TOKEN` via deployment secrets. Do not commit tokens.
- Validate feed envelopes, row types, coordinates, required fields, source IDs and duplicate/upsert behavior. Track received/created/updated/skipped counts and last success/failure without storing secrets in diagnostics.
- A successful sync means a batch was processed, not that rent, availability, or property legitimacy was independently verified. Show source and freshness to users and define stale-listing expiry rules.

## Payment flow

1. Tenant selects a server-issued billable charge.
2. API verifies ownership/tenant scope and charge status, computes amount/currency, creates a local payment attempt, then creates the provider order.
3. Android checkout uses the provider's public key and order ID; secret keys remain on the server.
4. API verifies the checkout signature and independently checks provider payment/capture status, or processes a verified signed webhook.
5. A database transaction records the provider event/payment idempotently and posts the ledger entry once.
6. Refunds are server-authorized, bounded by captured amount and prior refunds, and reflected in the ledger after provider confirmation.
7. Ambiguous outcomes are reconciled; never infer success from the Android callback alone.

Until test credentials, webhook configuration and staging acceptance are complete, payment integration must be reported as implemented foundation—not live money movement.

## Security and privacy baseline

- Use HTTPS in all non-local environments, strong environment-specific secrets, restricted CORS, least-privilege database credentials, and private database networking.
- Keep passwords hashed with a suitable password KDF. Store bearer/session credentials using platform secure storage and use short expiry, revocation/version checks, and refresh-token rotation before production.
- Apply rate limits to registration, login, verification, reset, owner approval, and payment endpoints; use generic account-recovery responses where appropriate.
- Require MFA for administrators and document an owner-approval operating procedure.
- Keep personal data and payment metadata to the minimum required. Define retention, account deletion, backup retention, access logging, incident response, and legal/privacy review.
- Never log passwords, one-time codes, access tokens, payment secrets, or full sensitive provider payloads.

## Environments and deployment

- **Local:** Docker Compose with PostgreSQL, FastAPI, migrations, and clearly marked fixtures. Bind ports to loopback by default.
- **CI:** static validation, backend unit/contract tests, PostgreSQL integration tests, migration checks, Compose API smoke tests, Android emulator UI assertions, and APK artifacts.
- **Staging:** managed PostgreSQL, HTTPS API, SMTP test relay, Razorpay test mode/webhook, authorized test feed, non-production secrets, and cross-device end-to-end acceptance.
- **Production:** managed PostgreSQL with encrypted backups and tested restore, private networking, secret manager, HTTPS, monitoring/alerting, migration rollback plan, rate limiting/WAF, incident procedures, admin MFA, and signed Android release artifact.

Never promote a local database or demo seed as production data. Use explicit deployment migrations and a tested backup/restore process.

## Release gates and test evidence

A release is not complete solely because the app compiles. Track these gates independently:

- [ ] Fresh PostgreSQL database can migrate from zero to head; upgrade path tested against a representative prior schema.
- [ ] Registration, verification, login, reset, logout/revocation, duplicate email and lockout paths pass PostgreSQL integration tests.
- [ ] Tenant and Owner authorization boundaries are tested, including pending/rejected Owner requests and cross-owner record access.
- [ ] Property creation, moderation, publication, inventory and tenant discovery persist across process restarts and separate devices.
- [ ] Reservation concurrency/expiry and lifecycle transitions are tested on PostgreSQL.
- [ ] Feed contract tests pass; a real feed is only declared active after authorized credentials, a successful sync, provenance checks and stale-data handling are verified.
- [ ] Razorpay test order, captured payment, duplicate webhook, invalid signature, timeout/reconciliation and refund scenarios pass in staging.
- [ ] API smoke tests, Android emulator login tests and APK build all pass; latest APK and screenshots are linked to the exact CI run/commit.
- [ ] Production hosting, TLS, monitoring, backup restore, MFA, privacy/retention review and signed release are accepted.

## Immediate implementation priority

1. Resolve the PostgreSQL registration foreign-key insert-order defect and ensure integrity failures are classified accurately (duplicate email only when it actually is duplicate email).
2. Complete and merge the Tenant/Owner authentication + local Mailpit/PostgreSQL end-to-end test milestone only after CI passes.
3. Complete PostgreSQL cross-device integration tests for property/inventory/reservations and payment/webhook idempotency.
4. Configure an authorized rental feed and Razorpay test credentials; verify real provider behavior in staging. Do not fabricate real inventory or claim live payments before this gate.
5. Deploy a staging environment with migration, secrets, observability, backup/restore, and acceptance procedures before production.

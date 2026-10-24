# StayEasy release checklist

This is the prioritized release backlog. A green build is necessary, but it is not a production-readiness sign-off. Keep this file updated when a release gate changes state.

## P0 — required before a public production launch

- [ ] **Authenticated mobile-to-API acceptance:** test the owner flows against a staging API with approved owner credentials, including property scoping, resident candidate loading, successful stay creation, stale/occupied unit rejection, duplicate submission protection, deposit recording, and failure/retry behavior.
- [ ] **Complete resident/stay synchronization:** hydrate owner stay history from the server after app restart and on a second device; add server-backed notice/move-out lifecycle actions and consistent loading, empty, offline, retry, and conflict states.
- [ ] **Production account and owner approval:** configure real email delivery, approved owner onboarding/role promotion, operator MFA, token/session policy, and external rate limiting. A workspace selection or development token must never grant production permissions.
- [ ] **Database and operations:** review/freeze Alembic baseline for the actual target database; prove backup and restore/PITR; test PostgreSQL transaction/race behavior; configure secrets, HTTPS ingress, request tracing, logs, alerts and incident response.
- [ ] **Security/privacy review:** verify property/staff/customer authorization and tenant isolation, audit sensitive operations, complete retention/export/deletion/consent decisions, document access controls and obtain the required legal/privacy review.
- [ ] **Accessible, resilient Android acceptance:** test TalkBack, text scaling, contrast, touch targets, small/large screens, process recreation, permissions, poor connectivity, retries, duplicate taps and real-device regression.
- [ ] **Installable production release:** configure secure release signing and versioning, build and verify a signed APK/AAB, confirm artifact provenance, and rehearse the store/internal-track release. The current CI release APK is unsigned and is for build validation only.

## P1 — required before enabling the dependent features

- [ ] **Payments:** integrate a provider only if payment collection is in launch scope; verify webhooks, idempotency, settlement, refunds and reconciliation. Otherwise keep all manual entries visibly distinct and do not imply that money was collected or bank-verified.
- [ ] **Maps and location:** configure restricted Maps/Places/routing credentials, permissions, attribution, billing limits and real geospatial calculations. Keep demo pins and distances clearly labelled until then.
- [ ] **Media and documents:** implement secure object storage, signed upload/download, file type/size validation, malware screening, moderation and authorization.
- [ ] **Notifications and jobs:** configure email/push/SMS delivery, retries and idempotency; schedule reservation expiry, rent billing and reminders with timezone-safe tests.
- [ ] **Trust and moderation:** complete abuse reporting, listing verification, review moderation/appeals, evidence retention and administrative audit.
- [ ] **Automated coverage:** add staging/API integration for the server-backed resident journey and key owner/resident lifecycle flows. Preserve Android emulator screenshots (Customer Home, Explore search, Owner workspace preview) as a review artifact on every CI run.

## P2 — post-launch / separate scope decisions

- [ ] Full owner web portal and iOS client.
- [ ] Subscription billing and paid promotional placement.
- [ ] Broader multilingual resource coverage and non-Android platform parity.

## Current CI baseline

- [x] Backend contract tests, clean-database migration validation and project contract validation passed on PR #12.
- [x] Android API 35 emulator smoke tests passed on PR #12.
- [x] Debug APK and unsigned release APK builds passed on PR #12.
- [ ] CI screenshot artifact: capture/upload workflow is under implementation; verify the artifact on the current pull request before marking complete.
- [ ] Staging server-backed resident assignment acceptance remains unverified.

**Release rule:** do not call StayEasy production-ready until all P0 items are closed, the relevant P1 items for enabled functionality are complete, and a signed release candidate passes the end-to-end acceptance suite.

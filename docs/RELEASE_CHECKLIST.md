# StayEasy release checklist

This is the prioritized release backlog. A green build is necessary, but it is not a production-readiness sign-off. Keep this file updated when a release gate changes state.

## P0 — required before a public production launch

- [ ] **Authenticated mobile-to-API acceptance:** test the owner flows against a staging API with approved owner credentials, including property scoping, resident candidate loading, successful stay creation, stale/occupied unit rejection, duplicate submission protection, deposit recording, and failure/retry behavior.
- [ ] **Complete resident/stay synchronization:** owner stay history, customer My Stay, notice/move-out lifecycle and tenant move-out request/review are server-backed. Still prove cross-device/reinstall restoration and authenticated staging behavior; complete consistent offline, retry and conflict handling for all remaining workflows.
- [ ] **Production account and owner approval:** admin-only Owner requests, audited approve/reject decisions and a secure first-admin bootstrap are implemented. Still verify the full journey on staging; configure real email delivery, operator MFA, token/session policy, and external rate limiting. A workspace selection or development token must never grant production permissions.
- [ ] **Database and operations:** review/freeze Alembic baseline for the actual target database; prove backup and restore/PITR; test PostgreSQL transaction/race behavior; configure secrets, HTTPS ingress, request tracing, logs, alerts and incident response.
- [ ] **Security/privacy review:** verify property/staff/customer authorization and tenant isolation, audit sensitive operations, complete export/account-deletion/consent decisions, validate document access controls and obtain the required legal/privacy review. An expiry-aware encrypted-document purge command now exists; it also sweeps old unreferenced generated blobs after a 24-hour default grace period. The deployment still must schedule it daily, confirm `DOCUMENT_STORAGE_ROOT` matches the API's private storage path, monitor failures, and document recovery/retention policy.
- [ ] **Accessible, resilient Android acceptance:** test TalkBack, text scaling, contrast, touch targets, small/large screens, process recreation, permissions, poor connectivity, retries, duplicate taps and real-device regression.
- [ ] **Installable production release:** configure secure release signing and versioning, build and verify a signed APK/AAB, confirm artifact provenance, and rehearse the store/internal-track release. The current CI release APK is unsigned and is for build validation only.

## P1 — required before enabling the dependent features

- [ ] **Payments:** integrate a provider only if payment collection is in launch scope; verify webhooks, idempotency, settlement, refunds and reconciliation. Otherwise keep all manual entries visibly distinct and do not imply that money was collected or bank-verified.
- [ ] **Maps and location:** configure restricted Maps/Places/routing credentials, permissions, attribution, billing limits and real geospatial calculations. Keep demo pins and distances clearly labelled until then.
- [ ] **Media and documents:** implement secure object storage, signed upload/download, file type/size validation, malware screening, moderation and authorization.
- [ ] **Notifications and jobs:** configure email/push/SMS delivery, retries and idempotency; schedule reservation expiry, rent billing and reminders with timezone-safe tests. Schedule `python -m backend.scripts.purge_expired_invitation_documents` daily after configuring private storage, keep `DOCUMENT_ORPHAN_GRACE_HOURS` at a safe 24-hour minimum unless operations justify otherwise, and monitor its expired/orphan purge and failure counts.
- [ ] **Trust and moderation:** complete abuse reporting, listing verification, review moderation/appeals, evidence retention and administrative audit.
- [ ] **Automated coverage:** backend contracts cover resident assignment, customer/owner stay isolation, tenant move-out submission, notice period, duplicate requests, approve/reject, unit occupancy and encrypted invitation-document access/retention. PostgreSQL 16 CI now runs a concurrent same-bed reservation test (one success, one conflict). Still add authenticated end-to-end staging journeys and real-device/offline regression. CI run #318 passed the expanded document-security suite, run #317 passed PostgreSQL concurrency plus APK builds, and run #316 passed the auth-entry emulator assertions plus APK builds.

## P2 — post-launch / separate scope decisions

- [ ] Full owner web portal and iOS client.
- [ ] Subscription billing and paid promotional placement.
- [ ] Broader multilingual resource coverage and non-Android platform parity.

## Current CI baseline

- [x] Backend contract tests, clean-database Alembic migration validation and project contract validation passed in [run #318](https://github.com/radhengle/StayEasy/actions/runs/40141437086); expanded suite reported **40 passed**.
- [x] Android API 35 emulator UI tests and both debug/unsigned-release APK builds passed in [run #318](https://github.com/radhengle/StayEasy/actions/runs/40141437086); APKs and screenshots/UI-test reports were uploaded as artifacts.
- [x] Focused login-entry emulator tests and both APK builds passed in [run #316](https://github.com/radhengle/StayEasy/actions/runs/40140999715).
- [x] PostgreSQL 16 same-bed race test passed (1 passed), and both APK builds passed in [run #317](https://github.com/radhengle/StayEasy/actions/runs/40141117891). Emulator tests also passed before the workflow finished successfully.
- [ ] Authenticated staging acceptance for resident assignment, customer My Stay, Owner approval and tenant move-out request/review remains unverified. Retention purge scheduling, production signing, database restore/PITR and production integrations remain deployment gates.

**Release rule:** do not call StayEasy production-ready until all P0 items are closed, the relevant P1 items for enabled functionality are complete, and a signed release candidate passes the end-to-end acceptance suite.

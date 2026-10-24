# StayEasy release checklist

This is the prioritized release backlog. A green build is necessary, but it is not a production-readiness sign-off. Keep this file updated when a release gate changes state.

## P0 — required before a public production launch

- [ ] **Authenticated mobile-to-API acceptance:** test the owner flows against a staging API with approved owner credentials, including property scoping, resident candidate loading, successful stay creation, stale/occupied unit rejection, duplicate submission protection, deposit recording, and failure/retry behavior.
- [ ] **Complete resident/stay synchronization:** owner stay history, customer My Stay, notice/move-out lifecycle and tenant move-out request/review are server-backed. Still prove cross-device/reinstall restoration and authenticated staging behavior; complete consistent offline, retry and conflict handling for all remaining workflows.
- [ ] **Production account and owner approval:** admin-only Owner requests, audited approve/reject decisions and a secure first-admin bootstrap are implemented. Still verify the full journey on staging; configure real email delivery, operator MFA, token/session policy, and external rate limiting. A workspace selection or development token must never grant production permissions.
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
- [ ] **Automated coverage:** backend contracts cover resident assignment, customer/owner stay isolation, tenant move-out submission, notice period, duplicate requests, approve/reject and unit occupancy. Still add authenticated end-to-end staging journeys, PostgreSQL concurrency tests and real-device/offline regression. The CI workflow captures Customer Home, Explore search and Owner workspace preview screenshots; run #312 uploaded the screenshot and UI-test-result artifacts successfully. Additional tenant Login and Owner Register action assertions are in PR #30 and still require green CI.

## P2 — post-launch / separate scope decisions

- [ ] Full owner web portal and iOS client.
- [ ] Subscription billing and paid promotional placement.
- [ ] Broader multilingual resource coverage and non-Android platform parity.

## Current CI baseline

- [x] Backend contract tests, clean-database Alembic migration validation and project contract validation passed on [PR #28 CI run #312](https://github.com/radhengle/StayEasy/actions/runs/40139821954).
- [x] Android API 35 emulator UI tests passed on [run #312](https://github.com/radhengle/StayEasy/actions/runs/40139821954).
- [x] Debug APK (`stayeasy-debug-apk`) and unsigned release APK (`stayeasy-release-unsigned-apk`) built and uploaded successfully in [run #312](https://github.com/radhengle/StayEasy/actions/runs/40139821954). Download them from the run's **Artifacts** section; the release artifact is unsigned and not a store-ready release.
- [x] Screenshot and Android UI-test-result artifacts uploaded successfully in [run #312](https://github.com/radhengle/StayEasy/actions/runs/40139821954). Artifacts are retained for 30 days.
- [ ] Authenticated staging acceptance for resident assignment, customer My Stay, Owner approval, tenant move-out request/review, document authorization and real PostgreSQL concurrency remains unverified.

**Release rule:** do not call StayEasy production-ready until all P0 items are closed, the relevant P1 items for enabled functionality are complete, and a signed release candidate passes the end-to-end acceptance suite.

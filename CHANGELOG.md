# Changelog

User-facing and release-relevant changes are recorded here. Unreleased entries document merged functionality and current release checks; they do not certify production readiness.

## Unreleased

### Added
- Gated Razorpay Route owner settlement: admin-linked provider accounts with audit records, owner-routed order transfers, transfer/settlement reconciliation, owner-visible transfer states, and linked-account transfer reversal on refunds. This remains blocked until provider approval, account onboarding/KYC, and end-to-end test-mode acceptance are complete.
- Server-backed owner resident assignment: select an existing verified customer, assign an available server room/bed, record rent, deposit and joining date, and refresh occupancy after API confirmation.
- Owner Residents/Stays history reads from property-scoped server records, including resident, property, room/unit, rent, deposit, notice and move-out details.
- Tenant **My Stay** reads current and previous stays from the authenticated customer endpoint, with loading, refresh, error and empty states.
- Tenant move-out request workflow: submit a requested date that honours the property's notice period, track the server request and owner note, and retry after a rejection.
- Owner move-out review queue: approve or reject a tenant request with a response note; approval records notice and the expected date without freeing the unit prematurely.
- CI captures Customer Home, Explore search and Owner workspace preview as verified emulator PNGs under `stayeasy-android-screenshots`.

### Changed
- Owner access is granted through an admin-reviewed server workflow, not the Tenant/Owner choice screen. Admin decisions are audited and invalidate the applicant's existing customer session.
- README and release checklist now distinguish automated build validation from authenticated staging acceptance and public-release requirements.
- Android CI fails if expected screenshot PNGs are missing or invalid and avoids duplicate builds for successive commits on a single PR ref.

### Validation
- PR #13 screenshot workflow passed backend/project validation, Android emulator UI tests, debug APK and unsigned release APK builds; all three genuine PNGs were verified in [workflow run #274](https://github.com/radhengle/StayEasy/actions/runs/40130310803).
- PR #17 Owner access approval passed all three required jobs in [workflow run #266](https://github.com/radhengle/StayEasy/actions/runs/40129245337).
- PR #18 customer My Stay sync passed all three required jobs in [workflow run #261](https://github.com/radhengle/StayEasy/actions/runs/40128947415).
- PR #20 tenant move-out request and owner review passed backend contracts, clean-database migration validation, Android emulator UI tests, debug APK and unsigned release APK builds in [workflow run #276](https://github.com/radhengle/StayEasy/actions/runs/40130773832).
- Authenticated staging acceptance, production SMTP, signed release artifacts, PostgreSQL operations and independent security review remain open release gates.

# StayEasy feature checklist — implementation and acceptance matrix

This matrix maps the full requested checklist to the current deliverable. “Local UI” means it is interactive and saved on the same Android device using SharedPreferences; it is not shared with other accounts/devices. “API foundation” means an endpoint/model exists in the FastAPI service but the Android client does not yet synchronize it. External integrations are deliberately not simulated as live services.

## Current status summary

- **Android:** native Kotlin/Jetpack Compose screens, local sample discovery, filters, favourites, details, owner listing wizard, local draft record, theme settings, persisted generic operational records, bed state board, customer booking request flow, and comparison up to three properties.
- **Backend:** FastAPI/SQLAlchemy/PostgreSQL service foundation, role/permission checks, property review queue, inventory, enquiry/visit/reservation/stay lifecycle, ledger, maintenance, notices, saved searches, staff, meal plans, housekeeping, visitor log, reviews, notifications and audit/report endpoints.
- **Admin:** browser-based listing review console for admins with decision actions, platform counts and audit log.
- **Not yet integrated:** email/phone verification, password recovery, rate limiting/abuse controls, MFA/OIDC and session revocation; full account-data deletion; actual Google Maps SDK/Places API, media object storage, push provider, payment gateway/reconciliation, full web owner portal, iOS and complete Android↔API sync for all workflows. These need integration and security/legal review.

## 1. Customer app — finding a PG

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| 1.1 Home | Location search entry, quick discovery choices, recommendations/sample listings, recent browsing/favourites paths, owner entry point | Local UI; promotional placement is not an actual paid ad product |
| 1.1 Location search | Pune area suggestions, search typing, saved recent locations and opt-in nearby demo action | Local UI; Google Places autocomplete/GPS needs credentials and permission flow |
| 1.2 Location filters | City/locality/PIN/query filters, proximity field in advanced filters, map/list navigation | Local filtering; real distance/radius requires geocoded coordinates and location provider |
| 1.2 Eligibility | Boys only, Girls only, Mixed/all genders, couples welcome; property-level policy visible in details | Local UI; backend property fields support eligibility/couple policy; room/unit eligibility overrides are only partly surfaced in owner UI |
| 1.2 Room type | Single/private and sharing filters; individual-bed versus room concept in detail and booking flows | Local UI; API models bed/room units and capacity |
| 1.2 Budget/cost | Rent, deposit, food, utilities estimates, move-in totals/recurring costs and filter ranges | Local UI; API ledger stores financial entries; pro-rata billing automation is not built |
| 1.2 Amenities | AC, Wi-Fi and bathroom/furnished/cooking/food preferences, structured amenity scopes and catalogue API | Local UI partial; API stores scoped amenities and included/extra-charge metadata |
| 1.2 Availability | Local available bed status, selected move-in preference and expected vacancy fields | Local UI; API reservation/inventory supports server state; stale-availability scheduled reminders are not running |
| 1.2 Living preferences | Furnishing, cooking, curfew/visitor policy, food and stay rules captured/displayed | Local UI; some filters and fields are ready, but not every preference has a dedicated UI control |
| 1.2 Trust/quality | Verified/unverified labels, listing quality record, reported listings category | Demo labels are not real verification; backend moderation queue available |
| 1.3 Search results | Cards, list/map toggle, sort/filter logic, favourites, save-search workflow | Local UI; server search endpoint supports basic criteria only |
| 1.3 Comparison | Up to three properties compared across rent/deposit/cost/sharing/policy/amenities/trust | Local UI |
| 1.3 Alerts and reports | Saved-search record and local notification log; report/misleading-listing record can be created | Local-only until backend notifications and moderation case workflow are connected |
| 1.4 Google Maps | Visual demo map with interactive sample pins and list selection | **Mock only**; live map tiles, real coordinates, marker clustering, routes, GPS and Google Places are not connected |

## 2. Property details and customer decisions

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| 2.1 Property details | Description, image, location, listing/verification markers, pricing, room and bed availability, amenities, policy and vacancy details | Local UI; some demo listing values are illustrative |
| 2.1 Pricing & move-in cost | Rent, deposit, food and utility estimate separated; first-month indicative total | Local UI; charges are estimates, not a confirmed offer/contract |
| 2.1 Rules/location/trust | Gender and couple policy, curfew/visitors/cooking/stay/notice fields and location disclaimers | Local UI; actual routing and real verified-stay reviews are not active |
| 2.2 Amenities catalogue | Structured categories for room, food/kitchen, utilities, cleaning/convenience and safety/building; applies at property, room, or unit scope; included/extra/unavailable statuses in API model | Local owner amenity selectors; API catalogue and scoped assignments are present; photo evidence/verification is not automated |
| 2.3 Enquiries/visits | Enquiry, messaging notes and visit requests are distinct persisted local records; visit reminders/history can be recorded | Local UI; API has create/list/status routes, in-app notice queue only, no push delivery |
| 2.4 Reservations | Reservation request UI, local terms/status, local active-unit collision guard and expiry fields; no false “confirmed” claim | Local demo; API adds expiry, snapshot and DB-level collision protection. Android/API sync is not connected |
| 2.5 Account/personalisation | Local favorites, search recents, theme/mode/language/large-text/notification preferences | Local persistence; phone OTP, identity, consent and account deletion are not implemented end to end |

## 3. Customer app — after moving into a PG

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| 3.1 My Stay | Stay lifecycle, move-in/renewal/notice/move-out records and contact/detail access paths | Local records UI; API resident stay records support history |
| 3.2 Rent/financial records | Payment, charges, deposits and receipt/ledger records; CSV export/share | Local records are manually entered; backend ledger distinguishes manual/provider-confirmed/reconciled values; no live payment integration |
| 3.3 Maintenance/support | Customer issue submission, property/unit scope, priority, owner queue and server-confirmed status updates | API-backed when the customer has a valid session and active server stay, or when an owner/staff member has property-scoped maintenance access; otherwise explicitly local-only. Photo upload and SLA escalation automation not included |
| 3.4 Resident lifecycle | Bed change, renewal, notice and settlement can be tracked as stay records; final statement concept supported by ledger | Local workflow foundation; server lifecycle endpoints exist; agreement signing and automated pro-rata settlement are not integrated |

## 4. Owner app — property onboarding and management

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| 4.1 Owner onboarding | Owner/manager mode, property creation wizard, saved draft record and preview/publish-to-local-catalogue flow | Local UI only; backend property review route exists but Android publishing does not submit to API |
| 4.2 Add/edit property | Name, locality/address text, PIN, owner-entered nearby places, photos, policy, room/sharing, prices, furnishing, bathroom, food, cooking, curfew, visitors, minimum stay and notice period | Local UI; map pin/lat-lon and real owner verification not connected |
| 4.2 Listing generation/quality | Description can be generated from entered facts; quality/draft record; missing details can be added in the wizard | Local UI; does not claim unentered benefits/distances |
| 4.3 Rooms/beds | Local room/bed board with available/reserved/occupied/maintenance state and allowed state actions; owner inventory routes in API | Local UI + API foundation; local conflict checks are not a substitute for DB transactions across devices |
| 4.4 Residents | Local resident and stay history records, assignment/move-out workflow; API owner stays and customer stay views | Local UI + API foundation; resident identity/document access still needs ID/permission integration |
| 4.5 Owner dashboard/tasks | Operational destinations and category hubs for enquiries, reservations, due payments, issues, reports and tasks | Local UI record-based queue; API dashboard counts implemented. This is not a live unified analytics dashboard until records sync |

## 5. Owner financial and operational management

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| 5.1 Rent/payments | Generic charge/payment records with amount/method/reference/status and exports | Local UI; API ledger/audit exists; recurring monthly invoice generation and late-fee policy engine are not scheduled |
| 5.2 Deposits | Separate deposit, deduction/refund/settlement record categories | Local UI; ledger supports category records; legal dispute/receipt and approval workflow requires refinement |
| 5.3 Expenses/reports | Categorized expense entries, report records, CSV export/share; owner analytics destination | Local UI; API ledger/dashboard. Report calculations need accounting validation before formal use |
| 5.4 Maintenance | Category, priority, status, assigned person, record history and unit state actions | Server-backed issue queue and status transitions with property-scoped staff visibility; unrelated maintenance schedules and record types remain local until integrated |
| 5.5 Staff/access | Role/permission records on device; server staff-assignment CRUD with property-specific permission checks | Local records are not authorization; API is the enforcement layer but authentication provider is a production prerequisite |
| 5.6 Communications | Enquiry/message/notice/notification categories; API notices and notification inbox with read status | Local UI + API queue; no SMS, email, push provider, delivery retry worker or read receipt from an external provider |

## 6. Automated/intelligent features

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| Description generation | Rule-based copy generated from confirmed owner-entered facts | Local implementation; no fabricated distance/amenity claims |
| Availability management | Local inventory status, bed-level actions and status notifications; API active reservation conflict constraint | Cross-device atomic behavior only through API; Android currently uses local mode |
| Location intelligence | Owner-entered nearby places explicitly labelled as unverified; coordinate fields exist | Google Places/Routes and true distance calculations remain external integration work |
| Owner task assistant | Task record hub for due items/follow-ups, status & priority | Record-based, not AI agent automation |
| Notifications | Local notification records; backend notifications are queued and readable/markable | External push delivery, retry workers and user-device token management not implemented |
| Listing quality checks | Quality records and wizard fields; API review queue and admin moderation decisions | Some validation/queue UI; image fraud/moderation and advanced contradiction detection are not automated |
| Suggested pricing | No production-ready dynamic recommendation implemented | Future; requires trustworthy comparison data and calibration |
| Smart recommendations | Basic local query/filter/sort; no explainable model-based ranking yet | Future iteration |

## 7. Shared platform and administration

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| Authentication/roles | Tenant/Owner email-password registration and sign-in, salted PBKDF2 password hashes, API-issued JWT sessions and server-side role checks; dev-token bootstrap remains development-only | Email/phone verification, password reset, rate limiting/lockout, MFA/OIDC, session revocation and production security review are still required. Not ready for public production |
| Shared property database | PostgreSQL backend schema and endpoints with property/inventory/record models | Foundation implemented; Android app not connected yet; no multi-device sync claim |
| Search | API filters for city, locality, text/PIN, rent, eligibility, couples, verified and availability | Basic API search; advanced geospatial search and full amenities joins require expansion |
| Media storage | Android image picker remembers selected content URI locally; database supports listing photo metadata | No production object storage, upload/access-control/compression pipeline yet |
| Notifications | API `Notification` records with read status; request events add owner notifications | Queue only; background delivery/retry is not deployed |
| Admin console | `admin/index.html` can connect, list review queue, approve/request changes/reject/pause, show reports and audit log | Working browser UI when served with CORS and admin token; complete investigations/complaint workflow remains |
| Security/privacy | server-side authorization, scoped staff permissions, audit trail, account fields and data structures | Production privacy request workflow, retention jobs, encryption/storage policies, independent penetration testing and legal review remain |
| Quality/monitoring | backend `/health`, test scaffold, migration/ops docs and Compose health check | Crash-reporting/APM, alerting, backup/restore drills and release gates still required |

## 8. Design, themes and usability

| Checklist item | Delivered behaviour / location | Status / acceptance condition |
|---|---|---|
| Appearance | System/Light/Dark and Midnight Cobalt, Ocean, Violet, Graphite, Porcelain, Copper themes persist | Local UI; no green primary theme |
| Visual consistency | Shared colour tokens, cards, fields, chips, buttons and layouts in Compose | Broadly themed; continue replacing any hard-coded colors in any remaining view |
| Interaction | Material Compose controls, confirmation dialogs, empty states, saved records and status transitions | Local UI; loading/error states for real network sync depend on API client integration |
| Accessibility | Semantics on core controls, large-text preference stored, contrast-oriented palettes | Screen-reader and dynamic large-text audit needs actual device accessibility pass |
| Localization | English UI and language preference stored | Hindi/Marathi translations are not present yet |
| Performance/reliability | local state persistence, URI permission retention, record timestamps and empty state fallback | offline queue/retry and server reconciliation not yet implemented |

## 9. Future expansion

| Checklist item | Status / acceptance condition |
|---|---|
| iOS and owner web portal | Not implemented; API and product structure provide a future foundation |
| Online rent/booking payments | Not implemented. Integrate a payment provider, signed webhooks, idempotency, reconciliation and dispute/refund paths |
| Digital rental agreement | Not implemented; requires jurisdiction-appropriate workflows and e-sign integration |
| Verified reviews | API requires completed stay and moderation; Android UI submission and admin review browsing are not yet integrated |
| Referrals, subscriptions, promoted listings | Not implemented; requires pricing/billing and clear paid-placement disclosure |
| Multi-property analytics | Record-based local reports and API owner dashboard counts; finance-grade aggregation requires validation |
| Food/housekeeping/visitors | Local categories plus API CRUD endpoints; enforce privacy, role scope and applicable policies before production |
| Corporate/college partnerships and advanced commute recs | Not implemented |

## Release acceptance gates

1. Connect Android login and API client; all create/update actions must wait for API confirmation before showing server-confirmed success.
2. Configure Google Maps/Places only with appropriately restricted credentials, billing/quota alerts, attribution and optional-location permission rationale.
3. Add migrations, identity provider, object storage, push delivery and payment-provider webhooks before real customer data or money is used.
4. Test reservation races under PostgreSQL using parallel requests; test permissions with customer/owner/manager/staff/admin roles.
5. Complete privacy/deletion/retention and review/moderation flows; security/legal review is required for Maharashtra rental procedures and India data-protection obligations.
6. Perform Gradle build, emulator/device QA, accessibility check, Android release-signing and API deployment verification. This packaging environment could not run Android Gradle build because it lacked a working Gradle distribution/Android SDK and network access.

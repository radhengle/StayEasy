# StayEasy UI prototype — acceptance checklist

## Tenant discovery
- [ ] Home renders on a small phone and can scroll to all sections.
- [ ] Selecting Boys/Girls/Mixed/Couples updates property results according to the demo policy.
- [ ] Couples filter only returns properties explicitly marked couples welcome.
- [ ] Typing/selecting a Pune locality narrows results; choosing Pune returns city-wide demo inventory.
- [ ] Budget slider filters results by listed monthly rent.
- [ ] Map/list actions preserve selected guest policy and budget.
- [ ] Every card opens the matching property's detail page.
- [ ] Save/remove favourite updates the Saved screen.
- [ ] Details show rent and deposit separately and clearly label demo listings.
- [ ] Visit request screen shows room preference, move-in date, visit time and a no-reservation-yet explanation.
- [ ] Chat entry, message entry and My Stay actions show clear demo feedback.
- [ ] Empty state offers a clear recovery action.

## Owner onboarding and management
- [ ] Owner mode opens from the profile/home entry and tenant mode can be reopened.
- [ ] Property wizard progresses through Basics, Stay policy, Rooms & pricing and Review.
- [ ] The form supports Boys only, Girls only and Mixed; enabling couples automatically selects Mixed.
- [ ] Numeric rent/deposit/food/room/bed fields accept digits and preview displays clear costs.
- [ ] Optional cover image can be selected from device storage.
- [ ] Publishing a named property displays a confirmation and returns to the property portfolio.
- [ ] Newly created property appears in tenant search/results and remains after app restart on the same device.
- [ ] Owner property list shows room, bed and available-bed counts.
- [ ] Bed board lets a user cycle demo states and indicates actions are prototype-only.
- [ ] Enquiry, resident, payment, issue and analytics screens scroll and their main actions give feedback.
- [ ] For a server-backed property, assign an eligible verified resident to an available room/bed and verify server stay creation, occupied-unit refresh, deposit ledger entry and duplicate/failure handling against an authorized staging API.
- [ ] Confirm resident/stay history is restored from server data after reinstall or on a second device; local summaries alone do not satisfy cross-device acceptance.

## Reliability / security before production
- [ ] Move availability and reservation-conflict rules to a transactional backend before accepting live reservations.
- [ ] Ensure tenant/owner/staff/administrator authorization is enforced by the server, not by UI hiding alone.
- [ ] Do not expose sample payment data as actual transactions.
- [ ] Validate every numeric value server-side; prevent invalid rent, deposit, occupancy and room capacity.
- [ ] Audit changes to stays, bed assignments, charges, payments, deposits and refunds.
- [ ] Run Android accessibility checks for contrast, TalkBack, text scaling and touch targets.
- [ ] Test process recreation, permission denial, image URI persistence, offline state, poor connections and duplicate actions.
- [ ] Configure and test real Maps/Places credentials, restrictions, attribution, quota and billing before enabling live maps.

## Automated CI coverage and remaining acceptance

The `Android APK` GitHub Actions workflow runs backend contract tests, validates a clean-database Alembic upgrade, runs project contract checks, executes Compose UI smoke tests on an Android API 35 emulator, and builds debug plus unsigned release APKs. PR #12's run passed all three jobs: [workflow run #187](https://github.com/radhengle/StayEasy/actions/runs/40124058013).

The baseline smoke suite covers customer home, Explore search controls, system Back behavior, owner workspace preview, theme controls, account entry, API-origin token binding and malformed date validation. It does **not** yet exercise authenticated server-backed resident assignment end-to-end against a real/staging API.

This checklist is not production certification. Manual staging acceptance, accessibility checks, API authorization/tenant-isolation tests, offline/retry testing, database restore drills, real device regression and release signing remain required. CI screenshots are being added for Customer Home, Explore search and Owner workspace preview; confirm the screenshot artifact is present on the current PR run before considering that evidence complete.

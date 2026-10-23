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

## Reliability / security before production
- [ ] Move availability and reservation-conflict rules to a transactional backend before accepting live reservations.
- [ ] Ensure tenant/owner/staff/administrator authorization is enforced by the server, not by UI hiding alone.
- [ ] Do not expose sample payment data as actual transactions.
- [ ] Validate every numeric value server-side; prevent invalid rent, deposit, occupancy and room capacity.
- [ ] Audit changes to stays, bed assignments, charges, payments, deposits and refunds.
- [ ] Run Android accessibility checks for contrast, TalkBack, text scaling and touch targets.
- [ ] Test process recreation, permission denial, image URI persistence, offline state, poor connections and duplicate actions.
- [ ] Configure and test real Maps/Places credentials, restrictions, attribution, quota and billing before enabling live maps.

## Test status for this packaged ZIP

- Kotlin syntax scan completed with no parser-level syntax errors.
- Full Gradle build and emulator interaction test **not run** in the packaging environment because Android SDK / Gradle distribution was not installed and external dependency download was unavailable.
- All screen-level demo actions should be verified in Android Studio after Gradle sync. This checklist is not a claim of production certification.

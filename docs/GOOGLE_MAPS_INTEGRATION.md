# Google Maps and Places integration plan

The present Android map is a labelled interactive demo illustration. It must not be represented as live geographic data, current GPS, true distance or route estimates.

## Required setup

1. Create a Google Cloud project and configure billing.
2. Enable Maps SDK for Android and Places API (New). Enable Routes only if commute time/route geometry is required.
3. Restrict the Android key by application ID and signing certificate fingerprint, split keys by environment and keep web service keys server-side.
4. Inject secrets through local configuration/CI secrets; never commit keys.
5. Implement Maps Compose map camera/markers and a Places Autocomplete (New) flow using session tokens and minimum requested fields.
6. Store verified place IDs and coordinates for properties. Owner-entered nearby-place notes remain labelled unverified until backed by source data.
7. Compute distance using coordinates and a documented method. Compute driving/walking commute time only from a configured routing service. Show “estimate” and attribution where required.
8. Make current location optional with runtime permission and a manual city/locality fallback.
9. Sync selected map bounds with results, cluster markers at low zoom, handle denied permissions, quota errors and offline mode, and avoid making ungrounded nearby-place claims.
10. Monitor quotas/cost and validate service terms before release.

Official references:
- https://developers.google.com/maps/documentation/places/android-sdk
- https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete
- https://developers.google.com/maps/api-security-best-practices

# StayEasy location search and map design

## Demo now

The current Compose UI contains:
- A manually editable location field.
- A suggested-localities screen with example Pune localities.
- A stylised demo map canvas with clickable monthly-rent pins.
- A property preview that selects the listing associated with the selected pin.
- A list/map switch and a separate guest policy/budget filter surface.

The current geometry is illustrative. It must not be presented as real roads, the user's current location, actual distances, Google data or live map pins.

## Production implementation

### Google services
- Maps SDK for Android / Maps Compose for real map rendering and camera state.
- Places SDK for Android (New) for location/place autocomplete and Place Details.
- Optional Routes API or another licensed routing provider if drive/walk time is part of ranking.
- Android location permission only after a user-initiated “Near me” action. Manual locality entry remains available.

### StayEasy-owned data
A Google Place is a search anchor, not a PG listing. The backend stores StayEasy's own properties with verified address, coordinates, publication/verification status and inventory/availability. Search queries places predictions, resolves a selected area or landmark to coordinates, then queries the StayEasy backend for PGs within a configured radius or viewport. The results list and map share one result set, selected property and filters.

### Search behaviour
1. User types an area, landmark, office or college.
2. Debounce keystrokes and request Places Autocomplete (New) predictions after a sensible minimum input length.
3. Bias results toward Pune or the visible map region, but avoid an overly strict location restriction unless the user explicitly chooses one.
4. Show full place name plus secondary locality, recent searches and manual entry fallback.
5. On selection, use the place ID to obtain only necessary place fields, then query StayEasy properties by area/radius.
6. Let users switch between list and map without losing filters or scroll/search context.
7. On map movement, optionally offer “Search this area” rather than issuing requests on every camera frame.
8. Price pins open a concise preview card; selecting the card opens the full listing.

### Key security / product rules
- Keep API keys out of version control; restrict Android keys to the expected package and signing certificate, and limit enabled APIs.
- Use only relevant Places fields and session tokens; monitor usage and configure quota/budget alerts.
- Display required Google attributions wherever Google-sourced data requires them.
- Never infer that a property is available because a map result exists. Availability comes from StayEasy inventory/reservation records.
- Do not expose exact resident or sensitive property details on map pins.

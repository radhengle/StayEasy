# StayEasy — Comprehensive Android + API Foundation

StayEasy is a PG discovery, booking, and accommodation-operations product. This project expands the original Kotlin/Jetpack Compose Android codebase with customer and owner workflows, local persistence, a configurable theme system, a FastAPI/PostgreSQL backend foundation, and a small browser-based moderation console.

**This is a comprehensive development foundation, not a claim that every external integration is production-live.** The Android UI and local workflows are implemented, several public discovery actions can call the API, and API foundations are covered by automated tests. Google Maps/Places, online payments, OTP delivery, push/email/SMS, object storage, production identity, and real multi-device synchronisation for all records still require credentials and integration work. See [`docs/IMPLEMENTATION_MATRIX.md`](docs/IMPLEMENTATION_MATRIX.md) for the feature-by-feature status and [`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md) for release gates.

## Project layout

```text
stayeasy_android/
├── app/                          # Kotlin + Jetpack Compose Android application
├── backend/                      # FastAPI + SQLAlchemy + PostgreSQL API
│   ├── app/main.py                # API routes, auth dependencies, business rules
│   ├── app/models.py              # Relational domain models and constraints
│   ├── app/database.py            # Database configuration/session management
│   ├── app/security.py            # Signed development/session-token primitives
│   └── tests/                     # API contract tests
├── admin/index.html              # Browser-based moderation/reports console
├── docs/                          # Implementation matrix, test plan and integration plans
├── docker-compose.yml             # PostgreSQL + API local environment
└── tools/validate_project.py      # Static project validation
```

## Run the Android app

1. Clone this repository and open its root folder in Android Studio.
2. Install JDK 17 and an Android SDK platform compatible with `compileSdk = 35`.
3. Allow Android Studio to sync dependencies and download the pinned Gradle 8.9 distribution.
4. Run the `app` configuration on an emulator or Android phone.

The packaging environment used for this build did not have an Android SDK/Gradle distribution and could not resolve external download hosts. The GitHub Actions workflow is the definitive CI check: Compose UI smoke tests run on an Android API 35 emulator before APK artifacts are released. Passing these smoke tests does not replace real-device acceptance, full accessibility review, or end-to-end tests against a deployed backend.

## Get the APK from GitHub Actions

Every push to `main` or a `feat/**` branch runs the **Android APK** workflow ([`.github/workflows/android-apk.yml`](.github/workflows/android-apk.yml)). CI first runs backend contract tests and static project validation. After those pass, APK packaging and Compose UI smoke tests on an Android API 35 emulator run as separate jobs in parallel. APK artifacts are retained for 30 days, so a slow emulator cannot block packaging; however, treat a build as fully verified only when both the APK and emulator-test jobs pass. Test reports are uploaded even if emulator tests fail:

- `stayeasy-debug-apk` — debug build (`app-debug.apk`), installable directly on a phone.
- `stayeasy-release-unsigned-apk` — release build (`app-release-unsigned.apk`). It is **not signed**, so Android will not install it until it is signed with a release key.

### Download from the website

1. Open the repo on GitHub and click the **Actions** tab.
2. Select the **Android APK** workflow in the left sidebar.
3. Open the latest run with a green check mark.
4. Scroll to **Artifacts** and click **stayeasy-debug-apk** to download a `.zip`. You must be signed in to GitHub to download artifacts.
5. Unzip it to get `app-debug.apk`.
6. Copy the `.apk` to your Android device (minimum Android 8.0) and open it. If asked, allow **Install unknown apps** for the app you're opening it from (Files, Chrome, etc.), then tap **Install**.

### Run the build manually

- On GitHub: **Actions** → **Android APK** → **Run workflow** → choose `main` → **Run workflow**.
- With the GitHub CLI:

```bash
gh workflow run android-apk.yml -R radhengle/StayEasy
gh run watch -R radhengle/StayEasy   # pick the new run to follow it
```

### Download with the GitHub CLI

```bash
# Debug APK from the latest successful run on main, into ./apk
RUN_ID=$(gh run list -R radhengle/StayEasy -w android-apk.yml -b main -s success -L 1 --json databaseId -q '.[0].databaseId')
gh run download "$RUN_ID" -R radhengle/StayEasy -n stayeasy-debug-apk -D apk

# Or a specific run
gh run list -R radhengle/StayEasy -w android-apk.yml
gh run download <run-id> -R radhengle/StayEasy -n stayeasy-debug-apk -D apk
```

Install it on a USB-connected device with `adb install -r apk/app-debug.apk`.

## Android capability overview

### Customer discovery and account

- Location and property search, recent/saved locations, filter and sort controls, list/map-style demo presentation, property details, favourites, comparison of up to three listings, enquiries, visit requests and a booking-request flow.
- Gender eligibility and couples policy, room/sharing options, budget and cost fields, amenities, availability preferences and local listing quality signals.
- My Stay, payment/charge records, issue reporting, resident lifecycle, theme and accessibility preferences.
- System, Light and Dark appearance plus Midnight Cobalt, Ocean, Violet, Graphite, Porcelain and Copper palettes. Themes use cool professional colours; green is not used as the primary theme colour.

### Owner and manager workflows

- Property wizard with drafts and preview-oriented listing creation, room/unit inventory, unit statuses, resident assignment/lifecycle, enquiries and visits.
- Local operations screens for rent/charges, payments, deposits, expenses, maintenance, notices, staff and reports.
- Persisted feature records use the app's local store. Until the relevant endpoint is integrated, they remain device-local and are not shared with another account/device.

### API and admin

- FastAPI service with role-aware route dependencies and PostgreSQL data models for properties, units, scoped amenities, enquiries, visits, reservations, stays, ledger entries, maintenance, notices, staff access, saved searches, reviews, moderation, privacy requests, documents metadata, reports, meal plans, housekeeping and visitor logs.
- Server-side reservation constraints, audit events, notification records, billing-charge generation, public property discovery, owner inventory/dashboard routes and administrative listing/report workflows.
- A basic browser admin console in `admin/index.html`. It requires an admin bearer token; it does not provide a production identity provider by itself.

## Run the backend locally

Requires Docker Desktop or Docker Engine with Compose.

1. Copy the root `.env.example` to `.env`. Change `POSTGRES_PASSWORD`, `JWT_SECRET` (at least 32 characters), and the local-only `DEV_TOKEN_PASSWORD`. Keep `.env` out of source control. The separate `backend/.env.example` is for direct non-Compose development.
2. Start services from the project root:

   ```bash
   docker compose up --build
   ```

3. Open API documentation at `http://localhost:8000/docs` and health at `http://localhost:8000/health`.
4. Serve the admin console from the project root with `python -m http.server 8080` and open `http://127.0.0.1:8080/admin/`. Configure the API base URL and paste a development/admin bearer token for that session. For a local token, use the `/auth/dev-token` endpoint documented in `backend/README.md`; that endpoint is only for development.

The provided token/bootstrap mode is for local development only. Configure a strong random JWT secret and production identity/role provisioning before exposing the API. The API uses `create_all()` for local foundations; use proper versioned database migrations before production deployments.

## Run backend tests

From the project root, with Python and the backend requirements installed:

```bash
python -m pip install -r backend/requirements.txt
python -m pytest backend/tests -q
python tools/validate_project.py
```

The tested API contracts use a temporary/test database. Passing tests verifies the covered contracts; it does not certify the service for production traffic, security, compliance, or a real payment environment.

## API connection from Android

In the app, open Profile → Owner/management features → API connection & sync. Enter a reachable API base URL and run the health/listing check. The screen can fetch public listings from `/properties?city=Pune`. Use HTTPS except for explicitly permitted local development hosts (`localhost`, `127.0.0.1`, `10.0.2.2`). A token entered for an optional owner-dashboard check is kept in screen state rather than persisted as an app preference. Beyond public discovery and health checks, Android now supports role-token sessions for development, owner property creation with room/bed inventory and amenity submission, customer enquiry + visit requests, server-backed reservation requests for a selected server unit, and owner status updates for server-side enquiries, visits, and reservations. Enquiry, visit, and reservation views refresh from the API when a session is active. Drafts and finance, resident and maintenance records remain device-local until their individual workflows are connected and reconciled. Development-token minting is for a configured development environment only and must be disabled in production.

## Google Maps and location accuracy

The existing map view remains a visual map-style discovery UI until a Google Maps SDK/Places key and service integration are supplied. No live route/commute metric is fabricated. See [`docs/GOOGLE_MAPS_INTEGRATION.md`](docs/GOOGLE_MAPS_INTEGRATION.md) for the integration steps. Distances are treated as trustworthy only when backed by configured coordinates/location calculations.

## Important production release gates

- Complete an Android Gradle build, automated UI tests, accessibility checks and device/emulator regression tests.
- Connect each owner/resident workflow to authenticated API endpoints and establish conflict-safe synchronisation, retries, and offline-pending states.
- Add database migrations, backups/recovery, secrets management, HTTPS deployment, rate limiting, observability, and security testing.
- Configure and restrict Maps/Places and distance-service credentials; integrate object storage and media scanning.
- Integrate a real OTP/identity provider, push notifications, and any payment provider. Reconcile provider-confirmed payments; never label a manually recorded payment as provider-confirmed.
- Implement and verify consent, account/data deletion, resident-document storage and access, listing verification, UGC/review moderation, and applicable legal review.
- Complete operational pilot testing and app signing/release workflows.

See `docs/IMPLEMENTATION_MATRIX.md`, `docs/PRODUCTION_READINESS.md`, and `docs/TEST_PLAN.md` before treating the product as release-ready.

# StayEasy API (foundation build)

FastAPI + SQLAlchemy backend for a PG marketplace and owner operations. It uses PostgreSQL in the supplied Compose setup and SQLite by default for an isolated development run. API documentation is generated at `/docs` when the service is running.

## Run with Docker Compose

From the project root:

```bash
cp .env.example .env
```

Create a root `.env` file from this safe template (use generated values, do not commit it):

```dotenv
APP_ENV=development
POSTGRES_DB=stayeasy
POSTGRES_USER=stayeasy
POSTGRES_PASSWORD=REPLACE_WITH_LONG_HEX_PASSWORD
JWT_SECRET=REPLACE_WITH_AT_LEAST_32_RANDOM_CHARACTERS
JWT_ISSUER=stayeasy-api
ACCESS_TOKEN_MINUTES=120
DEV_TOKEN_PASSWORD=SET_A_LOCAL_ONLY_PASSWORD
CORS_ORIGINS=http://localhost:8080,http://127.0.0.1:8080
API_PORT=8000
```

Use different strong secrets in every environment. Hex-encoded random strings avoid URL escaping issues in the Compose PostgreSQL URL. Then run:

```bash
docker compose up --build -d
docker compose logs -f api
```

Health check: `http://localhost:8000/health`. Swagger/OpenAPI: `http://localhost:8000/docs`.

Serve the administrator UI separately:

```bash
python -m http.server 8080 --directory admin
```

Open `http://localhost:8080`. Alternatively, serve from the project root with `python -m http.server 8080` and open `/admin/`. The admin UI keeps the bearer token in page memory only. It intentionally does not implement a login provider or save the token to browser storage.

## Local development without Docker

Install Python 3.12+, then:

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate
pip install -r backend/requirements.txt
cd backend
uvicorn app.main:app --reload --port 8000
```

Use `DATABASE_URL=sqlite:///./stayeasy_dev.db` for local SQLite. Configure `JWT_SECRET` and `DEV_TOKEN_PASSWORD`. The optional `POST /auth/dev-token` endpoint is enabled only for `APP_ENV=development` and only when a non-empty `DEV_TOKEN_PASSWORD` matches. Do not expose it outside a local dev network. Example request:

```bash
curl -X POST http://localhost:8000/auth/dev-token \
  -H 'Content-Type: application/json' \
  -d '{"subject":"local-owner-01","role":"owner","dev_password":"YOUR_LOCAL_DEV_PASSWORD"}'
```

This developer-token flow is a bootstrap aid, not real account registration. Replace it with an organization identity provider/OIDC, proper account lifecycle and MFA before production. The token subject becomes the user ID used for ownership and resident linkage; use stable, server-issued IDs, not client-supplied arbitrary IDs.

## API capability groups

| Capability | Main routes |
|---|---|
| Health and dev-only token | `GET /health`, `POST /auth/dev-token` |
| Published property search/details | `GET /properties`, `GET /properties/{id}`, `GET /properties/{id}/amenities`, `GET /properties/{id}/reviews`, `GET /amenities/catalog` |
| Owner property setup and moderation | `POST /owner/properties`, `PATCH /owner/properties/{id}`, `POST /owner/properties/{id}/submit-review`, `GET /admin/listing-review`, `POST /admin/properties/{id}/decision` |
| Inventory and stay lifecycle | `POST /owner/properties/{id}/units`, `GET /owner/units`, `PATCH /owner/units/{id}/status`, `POST /owner/stays`, `GET /owner/stays`, `GET /stays/me`, `PATCH /owner/stays/{id}` |
| Enquiries and visits | `POST/GET /enquiries`, `PATCH /owner/enquiries/{id}`, `POST/GET /visits` |
| Reservation requests and status | `POST/GET /reservations`, `PATCH /owner/reservations/{id}` |
| Financial ledger | `POST/GET /owner/ledger` |
| Maintenance | `POST/GET /issues`, `PATCH /owner/issues/{id}` |
| Notices, notifications, saved searches | `POST/GET/PATCH /owner/notices`, `GET /notifications`, `PATCH /notifications/{id}/read`, `POST/GET /saved-searches` |
| Staff and operations | `GET/POST/PATCH /owner/staff`, `GET/POST/PATCH /owner/meal-plans`, `GET/POST/PATCH /owner/housekeeping`, `GET/POST/PATCH /owner/visitors` |
| Amenities | `GET /properties/{id}/amenities`, `PUT /owner/properties/{id}/amenities` |
| Reviews | `GET /properties/{id}/reviews`, `POST /reviews`, `PATCH /owner/reviews/{id}`, `PATCH /admin/reviews/{id}/moderation` |
| Owner/admin reporting and audit | `GET /owner/dashboard`, `GET /admin/reports`, `GET /admin/audit-log` |

## Domain rules encoded

- Enquiries, visits, reservations and resident stays are distinct records.
- Reservation requests receive an expiry and pricing/deposit snapshots. An active-unit partial unique database index and transaction-level checks help prevent overlapping live reservations. PostgreSQL is required for the intended production concurrency behaviour; SQLite is for local development only.
- Inventory cannot be marked available while there is an active resident stay.
- A completed stay requires a move-out date.
- Ledger entries distinguish manually entered amounts from payment-provider-confirmed/reconciled states. No payment is charged or bank-verified by this API.
- Reviews are limited to one per completed resident stay and remain pending moderation until approved.
- Staff access checks a property-specific permission rather than accepting a generic staff role alone.
- Moderation/finance/inventory mutations write an audit record.

## Known boundaries before production

The Android app currently stores its operational records locally and is **not yet wired to this API**. The admin browser uses the API directly. A proper Android API client/session, identity provider, refresh/revocation strategy and cross-device synchronization remain integration work.

This foundation does not yet implement production-grade object storage or image upload, account deletion requests and retention jobs, provider webhooks for UPI/card payments, scheduled rent billing and notification delivery workers, image/content moderation, Google Maps/Places/Routes integration, legal document signing, or verified-distance calculations. `create_all()` is convenient for a fresh prototype database; replace it with Alembic migrations before real data is stored. Configure HTTPS/TLS, restrictive CORS, reverse-proxy limits, rate limiting, secure secrets, backups/PITR, monitoring, audit retention, database migrations, threat modelling and an independent security review before launch.

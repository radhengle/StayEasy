# Production configuration gate

The API permits SQLite and missing SMTP only in `development` and `test`. Set `APP_ENV=staging` or `APP_ENV=production` for deployed environments; startup fails when required production settings are missing or unsafe.

| Variable | Requirement |
|---|---|
| `APP_ENV` | `development`, `test`, `staging`, or `production`; set explicitly in deployment |
| `DATABASE_URL` | Explicit PostgreSQL SQLAlchemy URL for staging/production; SQLite is rejected |
| `JWT_SECRET` | Unique secret of at least 32 characters; unsafe placeholders are rejected |
| `SMTP_HOST` | Configured SMTP relay for verification and recovery |
| `SMTP_FROM_EMAIL` | Verified sender address |
| `SMTP_PORT` | Valid TCP port; defaults to 587 |
| `CORS_ORIGINS` | Optional comma-separated browser origins; if configured in staging/production they must use HTTPS and cannot contain `*` |

Keep secrets in your hosting platform's secret manager. Do not commit secrets, place passwords in shell arguments, or expose production credentials in CI. The API's development token endpoint is unavailable unless `APP_ENV=development`.

Local development/test still initializes tables through SQLAlchemy metadata. Staging/production no longer auto-creates tables; run `python -m alembic -c backend/alembic.ini upgrade head` as a deployment step. Before public launch, freeze/review the initial metadata-based baseline as explicit migrations, rehearse backup/restore/PITR, and validate concurrency on the actual PostgreSQL version.

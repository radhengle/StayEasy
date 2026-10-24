"""Fail-fast validation for deployment environments."""
from collections.abc import Mapping
import os

ALLOWED_ENVIRONMENTS = {"development", "test", "staging", "production"}


def validate_database_configuration(environ: Mapping[str, str] | None = None) -> None:
    env = os.environ if environ is None else environ
    app_env = env.get("APP_ENV", "development").strip().lower()
    if app_env not in {"staging", "production"}:
        return
    url = env.get("DATABASE_URL", "").strip()
    if not url:
        raise RuntimeError("DATABASE_URL must be explicitly configured for staging/production.")
    driver = url.partition("://")[0]
    # requirements.txt installs psycopg 3, not psycopg2. Requiring an explicit
    # driver prevents a bare postgresql:// URL from selecting the absent psycopg2 driver.
    if driver != "postgresql+psycopg":
        raise RuntimeError("Staging/production requires a postgresql+psycopg:// URL (psycopg 3).")


def validate_runtime_configuration(environ: Mapping[str, str] | None = None) -> None:
    env = os.environ if environ is None else environ
    app_env = env.get("APP_ENV", "development").strip().lower()
    if app_env not in ALLOWED_ENVIRONMENTS:
        raise RuntimeError("APP_ENV must be one of development, test, staging, or production.")
    if app_env not in {"staging", "production"}:
        return

    validate_database_configuration(env)
    jwt_secret = env.get("JWT_SECRET", "")
    if len(jwt_secret) < 32 or "unsafe" in jwt_secret.lower() or "change-before-deployment" in jwt_secret.lower():
        raise RuntimeError("Set a unique JWT_SECRET of at least 32 characters for staging/production.")

    missing = [key for key in ("SMTP_HOST", "SMTP_FROM_EMAIL") if not env.get(key, "").strip()]
    if missing:
        raise RuntimeError("Configure SMTP_HOST and SMTP_FROM_EMAIL for account verification and recovery.")
    try:
        smtp_port = int(env.get("SMTP_PORT", "587"))
    except ValueError as exc:
        raise RuntimeError("SMTP_PORT must be a valid TCP port number.") from exc
    if not 1 <= smtp_port <= 65535:
        raise RuntimeError("SMTP_PORT must be between 1 and 65535.")

    raw_origins = env.get("CORS_ORIGINS", "").strip()
    if not raw_origins:
        raise RuntimeError("CORS_ORIGINS must be explicitly configured for staging/production.")
    origins = [origin.strip() for origin in raw_origins.split(",") if origin.strip()]
    if not origins:
        raise RuntimeError("CORS_ORIGINS must contain at least one browser origin.")
    if "*" in origins:
        raise RuntimeError("Wildcard CORS origins are not allowed in staging/production.")
    if any(not origin.startswith("https://") for origin in origins):
        raise RuntimeError("Staging/production CORS origins must use HTTPS.")

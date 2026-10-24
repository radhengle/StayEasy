import os
import hmac
import hashlib
import secrets
from datetime import datetime, timedelta, timezone
from typing import Any

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

APP_ENV = os.getenv("APP_ENV", "development").lower()
JWT_SECRET = os.getenv("JWT_SECRET", "dev-only-unsafe-secret-change-before-deployment")
JWT_ISSUER = os.getenv("JWT_ISSUER", "stayeasy-api")
TOKEN_MINUTES = int(os.getenv("ACCESS_TOKEN_MINUTES", "120"))
if APP_ENV == "production" and ("unsafe" in JWT_SECRET or len(JWT_SECRET) < 32):
    raise RuntimeError("Set a strong JWT_SECRET (32+ characters) before running in production")

bearer = HTTPBearer(auto_error=False)
ALLOWED_ROLES = {"customer", "owner", "manager", "staff", "admin"}

def create_access_token(subject: str, role: str, scopes: list[str] | None = None) -> str:
    if role not in ALLOWED_ROLES:
        raise ValueError("Unsupported role")
    now = datetime.now(timezone.utc)
    payload = {"sub": subject, "role": role, "scopes": scopes or [], "iss": JWT_ISSUER,
               "iat": now, "exp": now + timedelta(minutes=TOKEN_MINUTES)}
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")

def get_actor(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)) -> dict[str, Any]:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Bearer token required", headers={"WWW-Authenticate": "Bearer"})
    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=["HS256"], issuer=JWT_ISSUER,
                             options={"require": ["sub", "role", "exp", "iss"]})
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail="Invalid or expired access token", headers={"WWW-Authenticate": "Bearer"}) from exc
    role = payload.get("role")
    if role not in ALLOWED_ROLES:
        raise HTTPException(status_code=403, detail="Unsupported role")
    return {"subject": str(payload["sub"]), "role": role, "scopes": payload.get("scopes", [])}

def require_roles(*roles: str):
    def dependency(actor: dict[str, Any] = Depends(get_actor)) -> dict[str, Any]:
        if actor["role"] not in roles:
            raise HTTPException(status_code=403, detail="Role is not permitted for this action")
        return actor
    return dependency

def check_dev_password(candidate: str) -> bool:
    expected = os.getenv("DEV_TOKEN_PASSWORD", "")
    return bool(expected) and hmac.compare_digest(expected.encode("utf-8"), candidate.encode("utf-8"))


PASSWORD_HASH_ITERATIONS = 310_000


def hash_password(password: str) -> str:
    """Hash a password with a unique random salt using PBKDF2-HMAC-SHA256."""
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PASSWORD_HASH_ITERATIONS)
    return f"pbkdf2_sha256${PASSWORD_HASH_ITERATIONS}${salt.hex()}${digest.hex()}"


def verify_password(password: str, encoded_hash: str) -> bool:
    """Verify a stored PBKDF2 hash without leaking comparison timing."""
    try:
        algorithm, iterations_text, salt_hex, expected_hex = encoded_hash.split("$", 3)
        iterations = int(iterations_text)
        if algorithm != "pbkdf2_sha256" or not 100_000 <= iterations <= 2_000_000:
            return False
        salt = bytes.fromhex(salt_hex)
        expected = bytes.fromhex(expected_hex)
        actual = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iterations)
        return hmac.compare_digest(actual, expected)
    except (ValueError, TypeError):
        return False

import hashlib
import hmac
import os
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
_SCRYPT_N = 16384
_SCRYPT_R = 8
_SCRYPT_P = 1


def hash_password(password: str) -> str:
    """Hash passwords with scrypt; never store a raw password."""
    salt = secrets.token_bytes(16)
    digest = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=_SCRYPT_N,
                            r=_SCRYPT_R, p=_SCRYPT_P, dklen=32)
    return "scrypt$" + str(_SCRYPT_N) + "$" + str(_SCRYPT_R) + "$" + str(_SCRYPT_P) + "$" + salt.hex() + "$" + digest.hex()


def verify_password(password: str, encoded: str) -> bool:
    try:
        scheme, n, r, p, salt_hex, digest_hex = encoded.split("$")
        if scheme != "scrypt" or (int(n), int(r), int(p)) != (_SCRYPT_N, _SCRYPT_R, _SCRYPT_P):
            return False
        expected = bytes.fromhex(digest_hex)
        actual = hashlib.scrypt(password.encode("utf-8"), salt=bytes.fromhex(salt_hex),
                                n=_SCRYPT_N, r=_SCRYPT_R, p=_SCRYPT_P, dklen=len(expected))
        return hmac.compare_digest(actual, expected)
    except (ValueError, TypeError, MemoryError):
        return False


def hash_one_time_code(subject: str, code: str, purpose: str) -> str:
    """Key one-time codes with the server secret so a DB leak does not permit offline code guessing."""
    material = f"{purpose}:{subject}:{code}".encode("utf-8")
    return hmac.new(JWT_SECRET.encode("utf-8"), material, hashlib.sha256).hexdigest()


def create_access_token(subject: str, role: str, scopes: list[str] | None = None,
                        auth_version: int | None = None) -> str:
    if role not in ALLOWED_ROLES:
        raise ValueError("Unsupported role")
    now = datetime.now(timezone.utc)
    payload = {"sub": subject, "role": role, "scopes": scopes or [], "iss": JWT_ISSUER,
               "iat": now, "exp": now + timedelta(minutes=TOKEN_MINUTES)}
    if auth_version is not None:
        payload["av"] = auth_version
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def get_actor(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)) -> dict[str, Any]:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Bearer token required",
                            headers={"WWW-Authenticate": "Bearer"})
    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=["HS256"], issuer=JWT_ISSUER,
                             options={"require": ["sub", "role", "exp", "iss"]})
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail="Invalid or expired access token",
                            headers={"WWW-Authenticate": "Bearer"}) from exc
    role = payload.get("role")
    if role not in ALLOWED_ROLES:
        raise HTTPException(status_code=403, detail="Unsupported role")

    # Real account tokens are checked against current DB state on every request. This
    # makes account suspension, role changes, password resets and logout take effect
    # immediately. Prototype tokens have no "av" claim and remain dev-only.
    if "av" in payload:
        from .database import SessionLocal
        from . import models as m
        with SessionLocal() as db:
            user = db.get(m.User, str(payload["sub"]))
            credential = db.get(m.UserCredential, str(payload["sub"]))
            if (user is None or credential is None or not user.is_active or
                    credential.email_verified_at is None or credential.auth_version != payload.get("av")):
                raise HTTPException(status_code=401, detail="Session is no longer valid",
                                    headers={"WWW-Authenticate": "Bearer"})
            return {"subject": user.id, "role": user.role, "scopes": payload.get("scopes", []),
                    "account_session": True}
    return {"subject": str(payload["sub"]), "role": role, "scopes": payload.get("scopes", []),
            "account_session": False}


def require_roles(*roles: str):
    def dependency(actor: dict[str, Any] = Depends(get_actor)) -> dict[str, Any]:
        if actor["role"] not in roles:
            raise HTTPException(status_code=403, detail="Role is not permitted for this action")
        return actor
    return dependency


def check_dev_password(candidate: str) -> bool:
    expected = os.getenv("DEV_TOKEN_PASSWORD", "")
    return bool(expected) and hmac.compare_digest(expected.encode("utf-8"), candidate.encode("utf-8"))

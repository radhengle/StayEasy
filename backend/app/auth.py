"""Verified email/password account routes.

Email delivery is only enabled when an SMTP relay is configured. Account roles are
assigned by the server: a workspace request for Owner is not owner authorization.
"""
import hmac
import os
import secrets
import smtplib
import ssl
from datetime import datetime, timedelta, timezone
from email.message import EmailMessage
from typing import Any
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from . import models as m
from .database import get_db
from .security import create_access_token, get_actor, hash_password, hash_one_time_code, verify_password

router = APIRouter(prefix="/auth", tags=["authentication"])


class AccountRegister(BaseModel):
    email: str = Field(min_length=5, max_length=255, pattern=r"^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    display_name: str = Field(min_length=2, max_length=180)
    password: str = Field(min_length=12, max_length=128)
    requested_workspace: str = Field(default="tenant", pattern="^(tenant|owner)$")


class EmailCodeRequest(BaseModel):
    email: str = Field(min_length=5, max_length=255, pattern=r"^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    code: str = Field(pattern=r"^\\d{6}$")


class AccountLogin(BaseModel):
    email: str = Field(min_length=5, max_length=255, pattern=r"^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    password: str = Field(min_length=1, max_length=128)


class PasswordResetConfirm(EmailCodeRequest):
    new_password: str = Field(min_length=12, max_length=128)


def _utc(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value.astimezone(timezone.utc)


def _email(value: str) -> str:
    return value.strip().lower()


def _mail_ready() -> bool:
    return bool(os.getenv("SMTP_HOST", "").strip() and os.getenv("SMTP_FROM_EMAIL", "").strip())


def send_security_code(recipient: str, subject: str, code: str, body_text: str) -> None:
    """Deliver a short-lived code via SMTP without logging it or exposing provider errors."""
    if not _mail_ready():
        raise HTTPException(status_code=503, detail="Email delivery is not configured on this server")
    message = EmailMessage()
    message["From"] = os.environ["SMTP_FROM_EMAIL"].strip()
    message["To"] = recipient
    message["Subject"] = subject
    message.set_content(body_text)
    host = os.environ["SMTP_HOST"].strip()
    port = int(os.getenv("SMTP_PORT", "587"))
    username = os.getenv("SMTP_USERNAME", "")
    password = os.getenv("SMTP_PASSWORD", "")
    try:
        if port == 465:
            with smtplib.SMTP_SSL(host, port, timeout=10, context=ssl.create_default_context()) as server:
                if username:
                    server.login(username, password)
                server.send_message(message)
        else:
            with smtplib.SMTP(host, port, timeout=10) as server:
                server.ehlo()
                server.starttls(context=ssl.create_default_context())
                server.ehlo()
                if username:
                    server.login(username, password)
                server.send_message(message)
    except (OSError, smtplib.SMTPException, ValueError) as exc:
        raise HTTPException(status_code=503, detail="Email delivery is temporarily unavailable") from exc


def _account_payload(user: m.User, credential: m.UserCredential) -> dict[str, Any]:
    return {
        "id": user.id,
        "email": user.email,
        "display_name": user.display_name,
        "role": user.role,
        "requested_workspace": credential.requested_workspace,
        "email_verified": credential.email_verified_at is not None,
        "owner_access_status": (
            "pending_approval" if credential.requested_workspace == "owner" and user.role != "owner"
            else "active" if user.role == "owner" else "not_requested"
        ),
    }


def _configured_mail_or_503() -> None:
    if not _mail_ready():
        raise HTTPException(status_code=503, detail="Email delivery is not configured on this server")


@router.post("/register", status_code=status.HTTP_202_ACCEPTED)
def register_account(body: AccountRegister, db: Session = Depends(get_db)):
    _configured_mail_or_503()
    email = _email(body.email)
    if db.scalar(select(m.User).where(func.lower(m.User.email) == email)) is not None:
        raise HTTPException(status_code=409, detail="An account with this email already exists")
    user_id = str(uuid4())
    code = f"{secrets.randbelow(1_000_000):06d}"
    user = m.User(id=user_id, email=email, display_name=body.display_name.strip(), role="customer", is_active=True)
    credential = m.UserCredential(
        user_id=user_id,
        password_hash=hash_password(body.password),
        requested_workspace=body.requested_workspace,
        verification_code_hash=hash_one_time_code(user_id, code, "email-verification"),
        verification_expires_at=datetime.now(timezone.utc) + timedelta(minutes=15),
    )
    db.add(user)
    db.add(credential)
    try:
        db.flush()
        send_security_code(email, "Verify your StayEasy account", code,
                           f"Your StayEasy verification code is {code}. It expires in 15 minutes. If you did not request this, ignore this email.")
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="An account with this email already exists") from exc
    return {"message": "Verification code sent. Verify your email before signing in.", "email": email}


@router.post("/verify-email")
def verify_account_email(body: EmailCodeRequest, db: Session = Depends(get_db)):
    email = _email(body.email)
    user = db.scalar(select(m.User).where(func.lower(m.User.email) == email))
    credential = db.get(m.UserCredential, user.id) if user else None
    now = datetime.now(timezone.utc)
    if (user is None or credential is None or credential.email_verified_at is not None or
            credential.verification_expires_at is None or _utc(credential.verification_expires_at) <= now or
            not hmac.compare_digest(
                credential.verification_code_hash or "",
                hash_one_time_code(user.id, body.code, "email-verification")
            )):
        raise HTTPException(status_code=400, detail="Verification code is invalid or expired")
    credential.email_verified_at = now
    credential.verification_code_hash = None
    credential.verification_expires_at = None
    db.commit()
    return {"message": "Email verified. You can now sign in."}


@router.post("/resend-verification", status_code=status.HTTP_202_ACCEPTED)
def resend_verification(body: AccountLogin, db: Session = Depends(get_db)):
    _configured_mail_or_503()
    email = _email(body.email)
    user = db.scalar(select(m.User).where(func.lower(m.User.email) == email))
    credential = db.get(m.UserCredential, user.id) if user else None
    if (user is not None and credential is not None and credential.email_verified_at is None
            and verify_password(body.password, credential.password_hash)):
        code = f"{secrets.randbelow(1_000_000):06d}"
        credential.verification_code_hash = hash_one_time_code(user.id, code, "email-verification")
        credential.verification_expires_at = datetime.now(timezone.utc) + timedelta(minutes=15)
        send_security_code(email, "Verify your StayEasy account", code,
                           f"Your StayEasy verification code is {code}. It expires in 15 minutes.")
        db.commit()
    return {"message": "If the account needs verification, a new code has been sent."}


@router.post("/login")
def login_account(body: AccountLogin, db: Session = Depends(get_db)):
    email = _email(body.email)
    user = db.scalar(select(m.User).where(func.lower(m.User.email) == email))
    credential = db.get(m.UserCredential, user.id) if user else None
    if user is None or credential is None or not user.is_active:
        raise HTTPException(status_code=401, detail="Email or password is incorrect")
    now = datetime.now(timezone.utc)
    locked_until = _utc(credential.locked_until)
    if locked_until is not None and locked_until > now:
        raise HTTPException(status_code=429, detail="Too many sign-in attempts. Try again later.")
    if not verify_password(body.password, credential.password_hash):
        credential.failed_login_attempts += 1
        if credential.failed_login_attempts >= 5:
            credential.locked_until = now + timedelta(minutes=15)
            credential.failed_login_attempts = 0
        db.commit()
        raise HTTPException(status_code=401, detail="Email or password is incorrect")
    if credential.email_verified_at is None:
        raise HTTPException(status_code=403, detail="Verify your email before signing in")
    credential.failed_login_attempts = 0
    credential.locked_until = None
    db.commit()
    token = create_access_token(user.id, user.role, scopes=["account"], auth_version=credential.auth_version)
    return {"access_token": token, "token_type": "bearer",
            "expires_in_minutes": int(os.getenv("ACCESS_TOKEN_MINUTES", "120")),
            "user": _account_payload(user, credential)}


@router.get("/me")
def current_account(actor: dict[str, Any] = Depends(get_actor), db: Session = Depends(get_db)):
    if not actor.get("account_session"):
        raise HTTPException(status_code=403, detail="Account authentication is required")
    user = db.get(m.User, actor["subject"])
    credential = db.get(m.UserCredential, actor["subject"])
    if user is None or credential is None:
        raise HTTPException(status_code=401, detail="Account session is no longer valid")
    return _account_payload(user, credential)


@router.post("/logout")
def logout_account(actor: dict[str, Any] = Depends(get_actor), db: Session = Depends(get_db)):
    if not actor.get("account_session"):
        raise HTTPException(status_code=403, detail="Account authentication is required")
    credential = db.get(m.UserCredential, actor["subject"])
    if credential is None:
        raise HTTPException(status_code=401, detail="Account session is no longer valid")
    credential.auth_version += 1
    db.commit()
    return {"message": "Signed out successfully"}


@router.post("/forgot-password", status_code=status.HTTP_202_ACCEPTED)
def forgot_password(body: EmailCodeRequest, db: Session = Depends(get_db)):
    _configured_mail_or_503()
    email = _email(body.email)
    user = db.scalar(select(m.User).where(func.lower(m.User.email) == email))
    credential = db.get(m.UserCredential, user.id) if user else None
    if user is not None and credential is not None and credential.email_verified_at is not None:
        code = f"{secrets.randbelow(1_000_000):06d}"
        credential.reset_code_hash = hash_one_time_code(user.id, code, "password-reset")
        credential.reset_expires_at = datetime.now(timezone.utc) + timedelta(minutes=15)
        send_security_code(email, "Reset your StayEasy password", code,
                           f"Your StayEasy password reset code is {code}. It expires in 15 minutes. If you did not request this, ignore this email.")
        db.commit()
    return {"message": "If a verified account exists for that email, a reset code has been sent."}


@router.post("/reset-password")
def reset_password(body: PasswordResetConfirm, db: Session = Depends(get_db)):
    email = _email(body.email)
    user = db.scalar(select(m.User).where(func.lower(m.User.email) == email))
    credential = db.get(m.UserCredential, user.id) if user else None
    now = datetime.now(timezone.utc)
    if (user is None or credential is None or credential.email_verified_at is None or
            credential.reset_expires_at is None or _utc(credential.reset_expires_at) <= now or
            not hmac.compare_digest(
                credential.reset_code_hash or "",
                hash_one_time_code(user.id, body.code, "password-reset")
            )):
        raise HTTPException(status_code=400, detail="Password reset code is invalid or expired")
    credential.password_hash = hash_password(body.new_password)
    credential.reset_code_hash = None
    credential.reset_expires_at = None
    credential.failed_login_attempts = 0
    credential.locked_until = None
    credential.auth_version += 1
    db.commit()
    return {"message": "Password changed. Sign in again with your new password."}

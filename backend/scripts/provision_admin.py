"""Provision the initial platform administrator from a trusted deployment shell.

Run only after applying the database migrations:
    python -m backend.scripts.provision_admin --email admin@example.com --display-name "StayEasy Admin"

The password is entered interactively and is never passed as a command-line argument.
"""
import argparse
import getpass
import re
from datetime import datetime, timezone
from uuid import uuid4

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from backend.app import models as m
from backend.app.database import SessionLocal
from backend.app.security import hash_password


EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def provision_admin(db: Session, email: str, display_name: str, password: str) -> m.User:
    """Create a new verified admin without exposing an API-based role promotion route."""
    normalized_email = email.strip().lower()
    normalized_name = display_name.strip()
    if not EMAIL_PATTERN.fullmatch(normalized_email):
        raise ValueError("Enter a valid administrator email address.")
    if len(normalized_name) < 2 or len(normalized_name) > 180:
        raise ValueError("Display name must contain 2 to 180 characters.")
    if len(password) < 12 or len(password) > 128:
        raise ValueError("Administrator password must contain 12 to 128 characters.")
    if db.scalar(select(m.User.id).where(func.lower(m.User.email) == normalized_email)) is not None:
        raise ValueError("An account with this email already exists; no account was changed.")

    now = datetime.now(timezone.utc)
    user = m.User(
        id=str(uuid4()),
        email=normalized_email,
        display_name=normalized_name,
        role="admin",
        is_active=True,
    )
    credential = m.UserCredential(
        user_id=user.id,
        password_hash=hash_password(password),
        requested_workspace="tenant",
        owner_access_status="not_requested",
        email_verified_at=now,
    )
    db.add_all([user, credential])
    db.flush()
    db.add(m.AuditLog(
        actor_id="system-bootstrap",
        actor_role="system",
        entity_type="user",
        entity_id=user.id,
        action="admin_account_bootstrapped",
        old_value={},
        new_value={"role": "admin", "email": normalized_email},
    ))
    try:
        db.commit()
        db.refresh(user)
    except IntegrityError as exc:
        db.rollback()
        raise ValueError("Administrator account creation conflicted with an existing account.") from exc
    return user


def main() -> int:
    parser = argparse.ArgumentParser(description="Provision a platform administrator in the configured StayEasy database.")
    parser.add_argument("--email", required=True, help="Verified administrator email address")
    parser.add_argument("--display-name", required=True, help="Administrator display name")
    args = parser.parse_args()

    password = getpass.getpass("New administrator password (12+ characters): ")
    confirmation = getpass.getpass("Confirm password: ")
    if password != confirmation:
        parser.error("Passwords do not match.")
    try:
        with SessionLocal() as db:
            user = provision_admin(db, args.email, args.display_name, password)
        print(f"Administrator account created for {user.email} (id: {user.id}).")
        print("Sign in through StayEasy. No password was printed or stored in command history.")
        return 0
    except ValueError as exc:
        parser.error(str(exc))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())

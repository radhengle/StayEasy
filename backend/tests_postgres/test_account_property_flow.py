"""PostgreSQL end-to-end account and rental-data flow.

Run against a disposable PostgreSQL database with:
  DATABASE_URL=postgresql+psycopg://... python -m pytest backend/tests_postgres/test_account_property_flow.py -q

Unlike the SQLite contract suite, this verifies real SQL persistence and JWT session
validation across registration, email verification, Owner approval, property publishing,
and Tenant discovery.
"""
from __future__ import annotations

import os
from datetime import datetime, timezone

import pytest

DATABASE_URL = os.getenv("DATABASE_URL", "")
if not DATABASE_URL.startswith(("postgresql+psycopg://", "postgresql://")):
    pytest.skip("PostgreSQL account-flow test requires DATABASE_URL pointing at PostgreSQL", allow_module_level=True)

os.environ.setdefault("APP_ENV", "test")
os.environ.setdefault("JWT_SECRET", "postgres-account-flow-test-secret-at-least-32-chars")
os.environ.setdefault("DEV_TOKEN_PASSWORD", "local-only-postgres-account-flow-password")

from fastapi.testclient import TestClient
from sqlalchemy import select

from backend.app import auth as auth_routes
from backend.app import main as api
from backend.app import models as m
from backend.app.database import Base, SessionLocal, engine
from backend.app.security import create_access_token
from backend.scripts.provision_admin import provision_admin


def _reset_database() -> None:
    # Only run against the disposable database supplied to the PostgreSQL test job.
    with engine.begin() as connection:
        Base.metadata.drop_all(bind=connection)
        Base.metadata.create_all(bind=connection)


def _register_and_verify(client: TestClient, monkeypatch, *, email: str, name: str,
                         workspace: str, password: str, delivered: dict) -> dict:
    response = client.post("/auth/register", json={
        "email": email,
        "display_name": name,
        "password": password,
        "requested_workspace": workspace,
    })
    assert response.status_code == 202, response.text
    assert response.json()["email"] == email.lower()

    # Tests capture the code at the mail boundary; no code is returned by the public API.
    code = delivered["Verify your StayEasy account"][email.lower()]["code"]
    verified = client.post("/auth/verify-email", json={"email": email, "code": code})
    assert verified.status_code == 200, verified.text
    return client.post("/auth/login", json={"email": email, "password": password})


def test_postgres_tenant_and_owner_registration_to_published_listing(monkeypatch) -> None:
    """A verified Owner can publish a listing only after approval; Tenant then discovers it."""
    _reset_database()
    api.app.dependency_overrides.clear()

    delivered: dict[str, dict[str, dict[str, str]]] = {}
    monkeypatch.setattr(auth_routes, "_mail_ready", lambda: True)

    def fake_send(recipient: str, subject: str, code: str, body_text: str) -> None:
        delivered.setdefault(subject, {})[recipient.lower()] = {
            "code": code,
            "body": body_text,
        }

    monkeypatch.setattr(auth_routes, "send_security_code", fake_send)

    try:
        with TestClient(api.app) as client:
            tenant_login = _register_and_verify(
                client, monkeypatch,
                email="tenant.flow@example.test",
                name="Tenant Flow Test",
                workspace="tenant",
                password="Tenant-Flow-Password-2026",
                delivered=delivered,
            )
            assert tenant_login.status_code == 200, tenant_login.text
            tenant_body = tenant_login.json()
            assert tenant_body["user"]["role"] == "customer"
            tenant_headers = {"Authorization": f"Bearer {tenant_body['access_token']}"}
            assert client.get("/auth/me", headers=tenant_headers).json()["email"] == "tenant.flow@example.test"

            owner_login = _register_and_verify(
                client, monkeypatch,
                email="owner.flow@example.test",
                name="Owner Flow Test",
                workspace="owner",
                password="Owner-Flow-Password-2026",
                delivered=delivered,
            )
            assert owner_login.status_code == 200, owner_login.text
            owner_body = owner_login.json()
            assert owner_body["user"]["role"] == "customer"
            assert owner_body["user"]["owner_access_status"] == "pending_approval"
            old_owner_headers = {"Authorization": f"Bearer {owner_body['access_token']}"}

            forbidden = client.post("/owner/properties", headers=old_owner_headers, json={
                "name": "Must Not Be Created", "locality": "Wakad", "city": "Pune",
            })
            assert forbidden.status_code == 403, forbidden.text

            # Create a verified DB-backed administrator and issue a real account-session JWT.
            with SessionLocal() as db:
                admin = provision_admin(
                    db,
                    "flow.admin@example.test",
                    "Flow Test Administrator",
                    "Admin-Flow-Password-2026",
                )
                admin_id = admin.id
                admin_credential = db.get(m.UserCredential, admin_id)
                assert admin_credential is not None
                admin_token = create_access_token(
                    admin_id, "admin", scopes=["account"], auth_version=admin_credential.auth_version
                )
                admin_email = admin.email
            admin_headers = {"Authorization": f"Bearer {admin_token}"}
            requests = client.get("/admin/owner-requests?status=pending_approval", headers=admin_headers)
            assert requests.status_code == 200, requests.text
            applicant = next(row for row in requests.json() if row["email"] == "owner.flow@example.test")
            assert applicant["email_verified"] is True

            approved = client.post(
                f"/admin/owner-requests/{applicant['id']}/decision",
                headers=admin_headers,
                json={"decision": "approve", "note": "Approved for PostgreSQL end-to-end test"},
            )
            assert approved.status_code == 200, approved.text
            assert approved.json()["role"] == "owner"

            # Approval increments auth_version; the pre-approval JWT must no longer work.
            stale_session = client.get("/auth/me", headers=old_owner_headers)
            assert stale_session.status_code == 401, stale_session.text

            owner_relogin = client.post("/auth/login", json={
                "email": "owner.flow@example.test",
                "password": "Owner-Flow-Password-2026",
            })
            assert owner_relogin.status_code == 200, owner_relogin.text
            assert owner_relogin.json()["user"]["role"] == "owner"
            owner_headers = {"Authorization": f"Bearer {owner_relogin.json()['access_token']}"}

            created = client.post("/owner/properties", headers=owner_headers, json={
                "name": "PostgreSQL End-to-End Test Property",
                "description": "Disposable test data persisted in PostgreSQL; not a real rental advertisement.",
                "locality": "Wakad",
                "city": "Pune",
                "rent_from": 11500,
                "deposit_from": 20000,
                "eligibility": "Mixed",
            })
            assert created.status_code == 201, created.text
            property_id = created.json()["id"]

            unit_response = client.post(
                f"/owner/properties/{property_id}/units",
                headers=owner_headers,
                json={
                    "floor_label": "First",
                    "room_code": "PG-TEST-101",
                    "unit_code": "PG-TEST-101-A",
                    "rentable_type": "bed",
                    "sharing_capacity": 1,
                    "monthly_rent": 11500,
                    "deposit": 20000,
                },
            )
            assert unit_response.status_code == 201, unit_response.text

            submitted = client.post(f"/owner/properties/{property_id}/submit-review", headers=owner_headers)
            assert submitted.status_code == 200, submitted.text
            approved_listing = client.post(
                f"/admin/properties/{property_id}/decision",
                headers=admin_headers,
                json={"decision": "approve", "note": "Published by PostgreSQL flow test"},
            )
            assert approved_listing.status_code == 200, approved_listing.text

            # Customer-facing discovery reads the committed property and inventory from PostgreSQL.
            discovered = client.get("/properties", params={"city": "Pune", "q": "PostgreSQL End-to-End Test Property"})
            assert discovered.status_code == 200, discovered.text
            matches = [row for row in discovered.json()["items"] if row["id"] == property_id]
            assert len(matches) == 1
            assert matches[0]["status"] == "published"
            assert matches[0]["rent_from"] == 11500
            assert matches[0]["inventory"]["available"] == 1
            assert matches[0]["verification_status"] == "verified"

            # Verify relational persistence and the authenticated account states independently of response bodies.
            with SessionLocal() as db:
                owner_user = db.scalar(select(m.User).where(m.User.email == "owner.flow@example.test"))
                tenant_user = db.scalar(select(m.User).where(m.User.email == "tenant.flow@example.test"))
                prop = db.get(m.Property, property_id)
                units = list(db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.property_id == property_id)))
                assert owner_user is not None and owner_user.role == "owner"
                assert tenant_user is not None and tenant_user.role == "customer"
                assert prop is not None and prop.owner_id == owner_user.id
                assert len(units) == 1 and units[0].status == "available"
                assert db.get(m.UserCredential, owner_user.id).email_verified_at is not None
    finally:
        api.app.dependency_overrides.clear()

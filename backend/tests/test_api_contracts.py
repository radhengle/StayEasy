"""API contract tests against an isolated SQLite database.

Run from the project root with: python -m pytest backend/tests -q
SQLite verifies API contracts; PostgreSQL concurrency testing is still required before production.
"""
import base64
import os
import tempfile
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

DB_PATH = Path(tempfile.gettempdir()) / f"stayeasy_test_{os.getpid()}.db"
os.environ["DATABASE_URL"] = f"sqlite:///{DB_PATH.as_posix()}"
os.environ["APP_ENV"] = "development"
os.environ["JWT_SECRET"] = "test-secret-long-enough-for-stayeasy-api-contract-tests"
os.environ["DEV_TOKEN_PASSWORD"] = "local-test-only-password"

from fastapi.testclient import TestClient
from sqlalchemy import delete, select

from backend.app import main as api
from backend.app.database import Base, engine, SessionLocal
from backend.app.security import create_access_token, get_actor, verify_password
from backend.app import models as m
from backend.app import auth as auth_routes
from backend.app.config import validate_runtime_configuration
from backend.scripts.provision_admin import provision_admin
from backend.scripts.purge_expired_invitation_documents import purge_expired_invitation_documents

client = TestClient(api.app)


def set_actor(subject: str, role: str):
    api.app.dependency_overrides[get_actor] = lambda: {"subject": subject, "role": role, "scopes": ["test"]}


def clear_db():
    with engine.begin() as connection:
        Base.metadata.drop_all(connection)
        Base.metadata.create_all(connection)
    api.app.dependency_overrides.clear()


def setup_property(owner_id="owner-1"):
    set_actor(owner_id, "owner")
    response = client.post("/owner/properties", json={
        "name": "Test House", "description": "Test property", "locality": "Wakad", "city": "Pune",
        "rent_from": 9000, "deposit_from": 18000, "eligibility": "Mixed", "couples_welcome": True,
    })
    assert response.status_code == 201, response.text
    prop = response.json()
    unit = client.post(f"/owner/properties/{prop['id']}/units", json={
        "floor_label": "First", "room_code": "101", "unit_code": "101-A", "rentable_type": "bed",
        "sharing_capacity": 2, "monthly_rent": 9000, "deposit": 18000,
    })
    assert unit.status_code == 201, unit.text
    submitted = client.post(f"/owner/properties/{prop['id']}/submit-review")
    assert submitted.status_code == 200, submitted.text
    set_actor("admin-1", "admin")
    approved = client.post(f"/admin/properties/{prop['id']}/decision", json={"decision": "approve", "note": "test"})
    assert approved.status_code == 200, approved.text
    return prop, unit.json()


def create_test_stay(payload):
    """Create the verified test account required by the real resident-assignment contract."""
    resident_id = payload["resident_id"]
    with SessionLocal() as db:
        if db.get(m.User, resident_id) is None:
            db.add(m.User(id=resident_id, email=f"{resident_id}@example.test", display_name=resident_id,
                          role="customer", is_active=True))
            db.add(m.UserCredential(user_id=resident_id, password_hash="test-only-hash",
                                    email_verified_at=datetime.now(timezone.utc)))
            db.commit()
    return client.post("/owner/stays", json=payload)


def setup_function():
    clear_db()


def teardown_function():
    api.app.dependency_overrides.clear()


def test_production_configuration_fails_closed_and_development_defaults_remain_usable():
    valid = {
        "APP_ENV": "production",
        "DATABASE_URL": "postgresql+psycopg://stayeasy:placeholder@db.example.test/stayeasy",
        "JWT_SECRET": "prod-token-signing-secret-with-more-than-32-characters",
        "SMTP_HOST": "smtp.example.test",
        "SMTP_FROM_EMAIL": "noreply@example.test",
        "SMTP_PORT": "587",
        "CORS_ORIGINS": "https://app.example.test,https://admin.example.test",
    }
    validate_runtime_configuration(valid)

    invalid_cases = [
        ({k: v for k, v in valid.items() if k != "DATABASE_URL"}, "DATABASE_URL must be explicitly configured"),
        ({**valid, "DATABASE_URL": "sqlite:///./stayeasy.db"}, "postgresql+psycopg:// URL"),
        ({**valid, "DATABASE_URL": "postgresql://stayeasy:placeholder@db.example.test/stayeasy"}, "postgresql+psycopg:// URL"),
        ({**valid, "DATABASE_URL": "postgresql+unknown://db.example.test/stayeasy"}, "postgresql+psycopg:// URL"),
        ({k: v for k, v in valid.items() if k != "CORS_ORIGINS"}, "CORS_ORIGINS must be explicitly configured"),
        ({**valid, "JWT_SECRET": "short"}, "unique JWT_SECRET"),
        ({k: v for k, v in valid.items() if k != "SMTP_HOST"}, "Configure SMTP_HOST and SMTP_FROM_EMAIL"),
        ({**valid, "SMTP_PORT": "not-a-port"}, "SMTP_PORT must be a valid"),
        ({**valid, "SMTP_PORT": "70000"}, "between 1 and 65535"),
        ({**valid, "CORS_ORIGINS": "*"}, "Wildcard CORS origins"),
        ({**valid, "CORS_ORIGINS": "http://app.example.test"}, "must use HTTPS"),
        ({**valid, "APP_ENV": "unknown"}, "APP_ENV must be one of"),
    ]
    for environment, expected_message in invalid_cases:
        try:
            validate_runtime_configuration(environment)
            assert False, f"Invalid deployment configuration was accepted: {expected_message}"
        except RuntimeError as exc:
            assert expected_message in str(exc)

    validate_runtime_configuration({"APP_ENV": "development", "DATABASE_URL": "sqlite:///./local.db"})


def test_health_and_admin_authorization():
    assert client.get("/health").status_code == 200
    api.app.dependency_overrides.clear()
    assert client.get("/admin/reports").status_code == 401
    set_actor("customer-1", "customer")
    assert client.get("/admin/reports").status_code == 403


def test_property_review_publish_and_search():
    prop, unit = setup_property()
    result = client.get("/properties", params={"city": "Pune", "available_only": "true"})
    assert result.status_code == 200, result.text
    assert result.json()["count"] == 1
    item = result.json()["items"][0]
    assert item["id"] == prop["id"]
    assert item["inventory"]["available"] == 1
    assert item["verification_status"] == "verified"



def test_owner_resident_candidates_only_returns_verified_customers_related_to_property():
    prop, _unit = setup_property()
    verified = m.User(id="verified-customer", email="verified@example.test", display_name="Verified Customer", role="customer", is_active=True)
    credential = m.UserCredential(user_id=verified.id, password_hash="test-only-hash", email_verified_at=datetime.now(timezone.utc))
    unverified = m.User(id="unverified-customer", email="unverified@example.test", display_name="Unverified Customer", role="customer", is_active=True)
    unverified_credential = m.UserCredential(user_id=unverified.id, password_hash="test-only-hash")
    with SessionLocal() as db:
        db.add_all([verified, credential, unverified, unverified_credential])
        db.flush()
        db.add(m.Enquiry(property_id=prop["id"], customer_id=verified.id, message="Interested"))
        db.add(m.Enquiry(property_id=prop["id"], customer_id=unverified.id, message="Interested"))
        db.commit()
    set_actor("owner-1", "owner")
    response = client.get("/owner/resident-candidates", params={"property_id": prop["id"]})
    assert response.status_code == 200, response.text
    assert [row["id"] for row in response.json()] == ["verified-customer"]
    assert response.json()[0]["email_verified"] is True


def test_owner_cannot_assign_unverified_or_unknown_resident_account():
    prop, unit = setup_property()
    set_actor("owner-1", "owner")
    payload = {
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "not-a-real-account",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 0,
    }
    rejected = client.post("/owner/stays", json=payload)
    assert rejected.status_code == 422, rejected.text

def test_owner_stay_history_includes_resident_and_unit_labels_and_is_property_scoped():
    property_a, unit_a = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    stay_a = create_test_stay({
        "property_id": property_a["id"], "unit_id": unit_a["id"], "resident_id": "resident-alpha",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9100, "deposit_held": 12000,
    })
    assert stay_a.status_code == 201, stay_a.text

    property_b, unit_b = setup_property(owner_id="owner-2")
    set_actor("owner-2", "owner")
    stay_b = create_test_stay({
        "property_id": property_b["id"], "unit_id": unit_b["id"], "resident_id": "resident-beta",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 8200, "deposit_held": 8000,
    })
    assert stay_b.status_code == 201, stay_b.text

    set_actor("owner-1", "owner")
    response = client.get("/owner/stays")
    assert response.status_code == 200, response.text
    rows = response.json()
    assert [row["id"] for row in rows] == [stay_a.json()["id"]]
    row = rows[0]
    assert row["resident_name"] == "resident-alpha"
    assert row["resident_email"] == "resident-alpha@example.test"
    assert row["property_name"] == "Test House"
    assert row["room_code"] == "101"
    assert row["unit_code"] == "101-A"
    assert row["agreed_monthly_rent"] == 9100
    assert row["deposit_held"] == 12000

    scoped = client.get("/owner/stays", params={"property_id": property_a["id"]})
    assert scoped.status_code == 200, scoped.text
    assert [row["id"] for row in scoped.json()] == [stay_a.json()["id"]]
    forbidden = client.get("/owner/stays", params={"property_id": property_b["id"]})
    assert forbidden.status_code == 403


def test_owner_stay_notice_and_moveout_dates_are_validated_and_unit_is_released():
    prop, unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    joining_date = date.today()
    stay = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "lifecycle-resident",
        "joining_date": joining_date.isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay.status_code == 201, stay.text
    stay_id = stay.json()["id"]

    missing_notice_date = client.patch(f"/owner/stays/{stay_id}", json={"status": "notice_given"})
    assert missing_notice_date.status_code == 422, missing_notice_date.text

    notice_date = joining_date + timedelta(days=10)
    notice = client.patch(f"/owner/stays/{stay_id}", json={
        "status": "notice_given", "notice_date": notice_date.isoformat(),
    })
    assert notice.status_code == 200, notice.text
    assert notice.json()["status"] == "notice_given"
    assert notice.json()["notice_date"] == notice_date.isoformat()

    missing_moveout_date = client.patch(f"/owner/stays/{stay_id}", json={"status": "completed"})
    assert missing_moveout_date.status_code == 422, missing_moveout_date.text

    invalid_moveout_date = client.patch(f"/owner/stays/{stay_id}", json={
        "status": "completed", "move_out_date": (joining_date - timedelta(days=1)).isoformat(),
    })
    assert invalid_moveout_date.status_code == 422, invalid_moveout_date.text

    move_out_date = notice_date + timedelta(days=30)
    completed = client.patch(f"/owner/stays/{stay_id}", json={
        "status": "completed", "move_out_date": move_out_date.isoformat(),
    })
    assert completed.status_code == 200, completed.text
    assert completed.json()["status"] == "completed"
    assert completed.json()["notice_date"] == notice_date.isoformat()
    assert completed.json()["move_out_date"] == move_out_date.isoformat()

    stays = client.get("/owner/stays", params={"property_id": prop["id"]})
    assert stays.status_code == 200, stays.text
    assert stays.json()[0]["notice_date"] == notice_date.isoformat()
    assert stays.json()[0]["move_out_date"] == move_out_date.isoformat()
    units = client.get("/owner/units", params={"property_id": prop["id"]})
    assert units.status_code == 200, units.text
    assert units.json()[0]["status"] == "available"

    cannot_reactivate = client.patch(f"/owner/stays/{stay_id}", json={"status": "active"})
    assert cannot_reactivate.status_code == 409, cannot_reactivate.text


def test_owner_cancelling_an_active_stay_releases_its_unit():
    prop, unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    stay = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "cancelled-resident",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 0,
    })
    assert stay.status_code == 201, stay.text

    cancelled = client.patch(f"/owner/stays/{stay.json()['id']}", json={"status": "cancelled"})
    assert cancelled.status_code == 200, cancelled.text
    assert cancelled.json()["status"] == "cancelled"
    units = client.get("/owner/units", params={"property_id": prop["id"]})
    assert units.status_code == 200, units.text
    assert units.json()[0]["status"] == "available"


def test_customer_stay_history_has_property_labels_and_is_scoped_to_customer():
    property_a, unit_a = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    stay_a = create_test_stay({
        "property_id": property_a["id"], "unit_id": unit_a["id"], "resident_id": "resident-alpha",
        "joining_date": date(2026, 10, 1).isoformat(), "agreed_monthly_rent": 9200, "deposit_held": 15000,
    })
    assert stay_a.status_code == 201, stay_a.text

    property_b, unit_b = setup_property(owner_id="owner-2")
    set_actor("owner-2", "owner")
    stay_b = create_test_stay({
        "property_id": property_b["id"], "unit_id": unit_b["id"], "resident_id": "resident-beta",
        "joining_date": date(2026, 11, 1).isoformat(), "agreed_monthly_rent": 8700, "deposit_held": 10000,
    })
    assert stay_b.status_code == 201, stay_b.text

    set_actor("resident-alpha", "customer")
    response = client.get("/stays/me")
    assert response.status_code == 200, response.text
    rows = response.json()
    assert [row["id"] for row in rows] == [stay_a.json()["id"]]
    row = rows[0]
    assert row["property_id"] == property_a["id"]
    assert row["property_name"] == "Test House"
    assert row["locality"] == "Wakad"
    assert row["city"] == "Pune"
    assert row["notice_period_days"] == 30
    assert row["room_code"] == "101"
    assert row["unit_code"] == "101-A"
    assert row["joining_date"] == "2026-10-01"
    assert row["agreed_monthly_rent"] == 9200
    assert row["deposit_held"] == 15000

    set_actor("owner-1", "owner")
    forbidden = client.get("/stays/me")
    assert forbidden.status_code == 403


def _set_account_actor(subject: str, role: str = "customer"):
    api.app.dependency_overrides[get_actor] = lambda: {
        "subject": subject,
        "role": role,
        "scopes": ["account"],
        "account_session": True,
    }


def test_tenant_moveout_request_is_property_scoped_and_owner_approval_does_not_release_unit():
    prop, unit = setup_property(owner_id="owner-1")
    joining_date = date.today() - timedelta(days=60)
    set_actor("owner-1", "owner")
    stay = create_test_stay({
        "property_id": prop["id"],
        "unit_id": unit["id"],
        "resident_id": "moveout-resident",
        "joining_date": joining_date.isoformat(),
        "agreed_monthly_rent": 9200,
        "deposit_held": 16000,
    })
    assert stay.status_code == 201, stay.text
    stay_id = stay.json()["id"]
    earliest_date = date.today() + timedelta(days=30)
    requested_date = earliest_date + timedelta(days=15)
    payload = {
        "requested_move_out_date": requested_date.isoformat(),
        "reason": "Moving to a new city for work.",
    }

    # A development/role token is not enough to submit a tenant request.
    set_actor("moveout-resident", "customer")
    assert client.post(f"/stays/me/{stay_id}/move-out-requests", json=payload).status_code == 403

    _set_account_actor("moveout-resident")
    too_early = client.post(f"/stays/me/{stay_id}/move-out-requests", json={
        **payload, "requested_move_out_date": (date.today() + timedelta(days=5)).isoformat(),
    })
    assert too_early.status_code == 422, too_early.text

    created = client.post(f"/stays/me/{stay_id}/move-out-requests", json=payload)
    assert created.status_code == 201, created.text
    request_id = created.json()["id"]
    assert created.json()["status"] == "pending"
    assert created.json()["property_name"] == "Test House"
    assert created.json()["room_code"] == "101"
    assert created.json()["unit_code"] == "101-A"
    assert created.json()["requested_move_out_date"] == requested_date.isoformat()

    duplicate = client.post(f"/stays/me/{stay_id}/move-out-requests", json=payload)
    assert duplicate.status_code == 409, duplicate.text
    listed = client.get("/stays/me/move-out-requests")
    assert listed.status_code == 200, listed.text
    assert len(listed.json()) == 1
    assert listed.json()[0]["id"] == request_id

    # Another resident cannot submit against or read this stay's move-out request.
    _set_account_actor("another-resident")
    forbidden = client.post(f"/stays/me/{stay_id}/move-out-requests", json=payload)
    assert forbidden.status_code == 404, forbidden.text
    assert client.get("/stays/me/move-out-requests").json() == []

    # Other property owners cannot list or decide this request.
    other_prop, _ = setup_property(owner_id="owner-2")
    set_actor("owner-2", "owner")
    foreign_review = client.patch(f"/owner/move-out-requests/{request_id}", json={
        "decision": "approve", "owner_note": "Unauthorized",
    })
    assert foreign_review.status_code == 403, foreign_review.text
    foreign_list = client.get("/owner/move-out-requests")
    assert foreign_list.status_code == 200, foreign_list.text
    assert all(row["property_id"] == other_prop["id"] for row in foreign_list.json())

    set_actor("owner-1", "owner")
    owner_list = client.get("/owner/move-out-requests")
    assert owner_list.status_code == 200, owner_list.text
    assert [row["id"] for row in owner_list.json()] == [request_id]
    assert owner_list.json()[0]["resident_email"] == "moveout-resident@example.test"

    approved = client.patch(f"/owner/move-out-requests/{request_id}", json={
        "decision": "approve", "owner_note": "Approved. Please return the keys on the agreed date.",
    })
    assert approved.status_code == 200, approved.text
    assert approved.json()["status"] == "approved"
    assert approved.json()["owner_note"].startswith("Approved.")
    assert approved.json()["reviewed_at"] is not None

    # Approval records notice and target move-out date, but the bed remains occupied
    # until the owner confirms the actual move-out by completing the stay.
    _set_account_actor("moveout-resident")
    customer_requests = client.get("/stays/me/move-out-requests")
    assert customer_requests.status_code == 200, customer_requests.text
    assert customer_requests.json()[0]["status"] == "approved"
    assert customer_requests.json()[0]["owner_note"].startswith("Approved.")

    set_actor("owner-1", "owner")
    stays = client.get("/owner/stays", params={"property_id": prop["id"]})
    assert stays.status_code == 200, stays.text
    assert stays.json()[0]["status"] == "notice_given"
    assert stays.json()[0]["notice_date"] == date.today().isoformat()
    assert stays.json()[0]["move_out_date"] == requested_date.isoformat()
    units = client.get("/owner/units", params={"property_id": prop["id"]})
    assert units.status_code == 200, units.text
    assert units.json()[0]["status"] == "occupied"

    set_actor("owner-1", "owner")
    completed = client.patch(f"/owner/stays/{stay_id}", json={
        "status": "completed", "move_out_date": requested_date.isoformat(),
    })
    assert completed.status_code == 200, completed.text
    units_after_moveout = client.get("/owner/units", params={"property_id": prop["id"]})
    assert units_after_moveout.status_code == 200, units_after_moveout.text
    assert units_after_moveout.json()[0]["status"] == "available"

    with SessionLocal() as db:
        saved = db.get(m.ResidentMoveOutRequest, request_id)
        assert saved is not None
        assert saved.status == "approved"
        assert saved.reviewed_by == "owner-1"
        assert db.scalar(select(m.AuditLog.id).where(
            m.AuditLog.entity_type == "resident_move_out_request",
            m.AuditLog.entity_id == str(request_id),
            m.AuditLog.action == "reviewed",
        )) is not None


def test_owner_can_reject_moveout_request_and_resident_can_submit_again():
    prop, unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    stay = create_test_stay({
        "property_id": prop["id"],
        "unit_id": unit["id"],
        "resident_id": "moveout-reject-resident",
        "joining_date": (date.today() - timedelta(days=45)).isoformat(),
        "agreed_monthly_rent": 9000,
        "deposit_held": 0,
    })
    assert stay.status_code == 201, stay.text
    stay_id = stay.json()["id"]
    first_date = date.today() + timedelta(days=45)
    _set_account_actor("moveout-reject-resident")
    payload = {"requested_move_out_date": first_date.isoformat(), "reason": "Need to relocate."}
    first = client.post(f"/stays/me/{stay_id}/move-out-requests", json=payload)
    assert first.status_code == 201, first.text

    set_actor("owner-1", "owner")
    rejected = client.patch(f"/owner/move-out-requests/{first.json()['id']}", json={
        "decision": "reject", "owner_note": "Please discuss the contractual notice date with the manager.",
    })
    assert rejected.status_code == 200, rejected.text
    assert rejected.json()["status"] == "rejected"

    duplicate_decision = client.patch(f"/owner/move-out-requests/{first.json()['id']}", json={
        "decision": "approve", "owner_note": "This second decision must fail.",
    })
    assert duplicate_decision.status_code == 409, duplicate_decision.text

    stays = client.get("/owner/stays", params={"property_id": prop["id"]})
    assert stays.status_code == 200, stays.text
    assert stays.json()[0]["status"] == "active"
    units = client.get("/owner/units", params={"property_id": prop["id"]})
    assert units.status_code == 200, units.text
    assert units.json()[0]["status"] == "occupied"

    _set_account_actor("moveout-reject-resident")
    customer_requests = client.get("/stays/me/move-out-requests")
    assert customer_requests.status_code == 200, customer_requests.text
    assert customer_requests.json()[0]["status"] == "rejected"
    assert customer_requests.json()[0]["owner_note"].startswith("Please discuss")

    second_date = date.today() + timedelta(days=50)
    second = client.post(f"/stays/me/{stay_id}/move-out-requests", json={
        "requested_move_out_date": second_date.isoformat(),
        "reason": "Updated date after speaking with manager.",
    })
    assert second.status_code == 201, second.text
    assert second.json()["status"] == "pending"
    assert second.json()["id"] != first.json()["id"]


def test_owner_property_inventory_is_scoped_and_contains_server_units():
    first_property, first_unit = setup_property(owner_id="owner-1")
    second_property, _ = setup_property(owner_id="owner-2")

    set_actor("owner-1", "owner")
    response = client.get("/owner/properties")
    assert response.status_code == 200, response.text
    rows = response.json()
    assert [row["id"] for row in rows] == [first_property["id"]]
    assert rows[0]["units"][0]["id"] == first_unit["id"]
    assert rows[0]["units"][0]["status"] == "available"
    assert rows[0]["inventory"]["total"] == 1

    set_actor("customer-1", "customer")
    assert client.get("/owner/properties").status_code == 403

    set_actor("admin-1", "admin")
    admin_rows = client.get("/owner/properties")
    assert admin_rows.status_code == 200, admin_rows.text
    assert {row["id"] for row in admin_rows.json()} == {first_property["id"], second_property["id"]}


def test_owner_inventory_tree_derives_floor_room_bed_state_from_reservation_and_stay_records():
    prop, unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    initial = client.get(f"/owner/properties/{prop['id']}/inventory-tree")
    assert initial.status_code == 200, initial.text
    tree = initial.json()
    assert tree["inventory"] == {"total": 1, "available": 1, "reserved": 0, "occupied": 0, "unavailable": 0}
    assert tree["floors"][0]["floor_label"] == "First"
    assert tree["floors"][0]["rooms"][0]["room_code"] == "101"
    assert tree["floors"][0]["rooms"][0]["beds"][0]["unit_code"] == "101-A"
    assert tree["floors"][0]["rooms"][0]["beds"][0]["status"] == "available"

    set_actor("customer-1", "customer")
    reservation = client.post("/reservations", json={
        "property_id": prop["id"], "unit_id": unit["id"],
        "move_in_date": (date.today() + timedelta(days=7)).isoformat(),
        "terms_snapshot": {"currency": "INR"}
    })
    assert reservation.status_code == 201, reservation.text

    set_actor("owner-1", "owner")
    reserved_tree = client.get(f"/owner/properties/{prop['id']}/inventory-tree").json()
    reserved_bed = reserved_tree["floors"][0]["rooms"][0]["beds"][0]
    assert reserved_bed["status"] == "reserved"
    assert reserved_bed["reserved_for"]["user_id"] == "customer-1"
    assert reserved_tree["inventory"]["available"] == 0
    assert reserved_tree["inventory"]["reserved"] == 1

    stay = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "customer-1",
        "reservation_id": reservation.json()["id"], "joining_date": date.today().isoformat(),
        "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay.status_code == 201, stay.text
    occupied_tree = client.get(f"/owner/properties/{prop['id']}/inventory-tree").json()
    occupied_bed = occupied_tree["floors"][0]["rooms"][0]["beds"][0]
    assert occupied_bed["status"] == "occupied"
    assert occupied_bed["occupant"]["user_id"] == "customer-1"
    assert occupied_bed["occupant"]["display_name"] == "customer-1"
    assert occupied_tree["inventory"]["occupied"] == 1

    completed = client.patch(f"/owner/stays/{stay.json()['id']}", json={
        "status": "completed", "move_out_date": date.today().isoformat()
    })
    assert completed.status_code == 200, completed.text
    released_tree = client.get(f"/owner/properties/{prop['id']}/inventory-tree").json()
    assert released_tree["floors"][0]["rooms"][0]["beds"][0]["status"] == "available"
    with SessionLocal() as db:
        closed_reservation = db.get(m.Reservation, reservation.json()["id"])
        assert closed_reservation.status == "completed"


def test_owner_can_reassign_pending_reservation_and_old_bed_is_released():
    prop, first_unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    second = client.post(f"/owner/properties/{prop['id']}/units", json={
        "floor_label": "Second", "room_code": "202", "unit_code": "202-B",
        "rentable_type": "bed", "sharing_capacity": 3, "monthly_rent": 10500, "deposit": 21000,
    })
    assert second.status_code == 201, second.text

    set_actor("customer-1", "customer")
    reservation = client.post("/reservations", json={
        "property_id": prop["id"], "unit_id": first_unit["id"],
        "move_in_date": (date.today() + timedelta(days=7)).isoformat(),
        "terms_snapshot": {"currency": "INR"}
    })
    assert reservation.status_code == 201, reservation.text

    set_actor("owner-1", "owner")
    moved = client.post(f"/owner/reservations/{reservation.json()['id']}/reassign", json={
        "new_unit_id": second.json()["id"],
        "reason": "The original room is temporarily unavailable; this bed has the same confirmed terms."
    })
    assert moved.status_code == 200, moved.text
    assert moved.json()["unit_id"] == second.json()["id"]
    assert moved.json()["tenant_notified"] is True
    reservations = client.get("/reservations")
    assert reservations.status_code == 200
    assert reservations.json()[0]["unit_id"] == second.json()["id"]

    units = client.get("/owner/units", params={"property_id": prop["id"]})
    assert units.status_code == 200, units.text
    by_id = {row["id"]: row for row in units.json()}
    assert by_id[first_unit["id"]]["status"] == "available"
    assert by_id[second.json()["id"]]["status"] == "reserved"
    set_actor("customer-1", "customer")
    notifications = client.get("/notifications")
    assert notifications.status_code == 200
    assert any(row["title"] == "Proposed bed changed" for row in notifications.json())


def test_owner_inventory_status_update_is_server_confirmed_and_property_scoped():
    prop, unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    updated = client.patch(f"/owner/units/{unit['id']}/status", json={"status": "maintenance"})
    assert updated.status_code == 200, updated.text
    assert updated.json()["status"] == "maintenance"
    rows = client.get("/owner/units", params={"property_id": prop["id"]})
    assert rows.status_code == 200, rows.text
    assert rows.json()[0]["status"] == "maintenance"

    set_actor("owner-2", "owner")
    denied = client.patch(f"/owner/units/{unit['id']}/status", json={"status": "available"})
    assert denied.status_code == 403


def test_enquiry_and_visit_creation_emit_owner_notifications():
    prop, _ = setup_property()
    set_actor("customer-1", "customer")
    enquiry = client.post("/enquiries", json={"property_id": prop["id"], "preferred_room_type": "2 sharing", "message": "Is a bed available?"})
    assert enquiry.status_code == 201, enquiry.text
    visit = client.post("/visits", json={"property_id": prop["id"], "enquiry_id": enquiry.json()["id"],
                                         "preferred_at": (date.today() + timedelta(days=2)).isoformat() + "T15:00:00"})
    assert visit.status_code == 201, visit.text
    enquiry_rows = client.get("/enquiries")
    assert enquiry_rows.status_code == 200, enquiry_rows.text
    assert enquiry_rows.json()[0]["property_name"] == "Test House"
    visit_rows = client.get("/visits")
    assert visit_rows.status_code == 200, visit_rows.text
    assert visit_rows.json()[0]["property_name"] == "Test House"
    set_actor("owner-1", "owner")
    invalid_enquiry_transition = client.post(f"/owner/enquiries/{enquiry.json()['id']}/status", json={"status": "converted"})
    assert invalid_enquiry_transition.status_code == 409
    enquiry_status = client.post(f"/owner/enquiries/{enquiry.json()['id']}/status", json={"status": "contacted", "owner_response": "We can arrange a visit."})
    assert enquiry_status.status_code == 200, enquiry_status.text
    assert enquiry_status.json()["status"] == "contacted"
    invalid_visit_transition = client.post(f"/owner/visits/{visit.json()['id']}/status", json={"status": "completed"})
    assert invalid_visit_transition.status_code == 409
    visit_status = client.post(f"/owner/visits/{visit.json()['id']}/status", json={"status": "confirmed", "outcome": "Confirmed for the requested time."})
    assert visit_status.status_code == 200, visit_status.text
    assert visit_status.json()["status"] == "confirmed"
    invalid_visit_status = client.post(f"/owner/visits/{visit.json()['id']}/status", json={"status": "requested"})
    assert invalid_visit_status.status_code == 422
    messages = client.get("/notifications")
    assert messages.status_code == 200
    assert len(messages.json()) >= 2


def _create_verified_customer(user_id: str, email: str, name: str = "Invited Tenant"):
    with SessionLocal() as db:
        if db.get(m.User, user_id) is None:
            db.add(m.User(
                id=user_id, email=email, display_name=name, phone="5550101",
                role="customer", is_active=True,
            ))
            db.add(m.UserCredential(
                user_id=user_id,
                password_hash="test-only-hash",
                email_verified_at=datetime.now(timezone.utc),
            ))
            db.commit()


def test_owner_invites_specific_tenant_and_acceptance_reserves_only_once_with_acknowledged_proration():
    prop, unit = setup_property(owner_id="owner-1")
    _create_verified_customer("invite-tenant-1", "invite-tenant-1@example.test", "Taylor Tenant")
    _create_verified_customer("invite-tenant-2", "invite-tenant-2@example.test", "Jordan Tenant")

    set_actor("owner-1", "owner")
    first = client.post("/owner/invitations", json={
        "property_id": prop["id"], "unit_id": unit["id"],
        "recipient_email": "invite-tenant-1@example.test", "recipient_name": "Taylor Tenant",
        "recipient_phone": "5550101", "move_in_date": (date.today() + timedelta(days=10)).isoformat(),
    })
    assert first.status_code == 201, first.text
    first_body = first.json()
    assert first_body["status"] == "pending"
    assert first_body["unit_id"] == unit["id"]
    assert first_body["invitation_url"].startswith("stayeasy://invitation/")
    token = first_body["invitation_url"].split("/")[-1]
    assert first_body["token_returned_once"] is True

    # A second tenant may be invited, but the bed is not held until a tenant submits registration.
    second = client.post("/owner/invitations", json={
        "property_id": prop["id"], "unit_id": unit["id"],
        "recipient_email": "invite-tenant-2@example.test", "recipient_name": "Jordan Tenant",
        "move_in_date": (date.today() + timedelta(days=12)).isoformat(),
    })
    assert second.status_code == 201, second.text

    with SessionLocal() as db:
        stored = db.get(m.PropertyInvitation, first_body["id"])
        assert stored.token_hash != token
        assert len(stored.token_hash) == 64
        assert token not in stored.token_hash

    set_actor("invite-tenant-1", "customer")
    invitation_list = client.get("/tenant/invitations")
    assert invitation_list.status_code == 200, invitation_list.text
    assert any(item["id"] == first_body["id"] for item in invitation_list.json())
    assert all("invitation_url" not in item for item in invitation_list.json())

    resolved = client.get("/invitations/resolve", params={"token": token})
    assert resolved.status_code == 200, resolved.text
    resolved_body = resolved.json()
    assert resolved_body["property_name"] == prop["name"]
    assert resolved_body["unit_code"] == unit["unit_code"]
    assert resolved_body["recipient_email"] == "invite-tenant-1@example.test"
    quote = resolved_body["billable_quote"]
    assert quote["billable_start_date"] == date.today().isoformat()
    assert quote["days_in_billing_month"] in {28, 29, 30, 31}
    assert quote["billable_days"] == quote["days_in_billing_month"] - date.today().day + 1
    assert quote["first_period_rent_paise"] >= 0

    missing_ack = client.post("/invitations/accept", json={
        "token": token, "phone": "9876543210", "profile_completion_acknowledged": True,
        "billable_start_date": date.today().isoformat(), "billable_terms_acknowledged": False,
    })
    assert missing_ack.status_code == 422, missing_ack.text

    accepted = client.post("/invitations/accept", json={
        "token": token, "phone": "9876543210", "profile_completion_acknowledged": True,
        "billable_start_date": date.today().isoformat(), "billable_terms_acknowledged": True,
    })
    assert accepted.status_code == 201, accepted.text
    accepted_body = accepted.json()
    assert accepted_body["status"] == "pending"
    assert accepted_body["billing_status"] == "rent_due_recorded_registration_completed"
    assert accepted_body["charge_created"] is True
    assert accepted_body["charge_amount_paise"] == quote["first_period_rent_paise"]
    assert accepted_body["billable_quote"]["first_period_rent_paise"] == quote["first_period_rent_paise"]

    set_actor("owner-1", "owner")
    ledger = client.get("/owner/ledger", params={"property_id": prop["id"]})
    assert ledger.status_code == 200, ledger.text
    rent_charge = next(row for row in ledger.json() if row["reservation_id"] == accepted_body["reservation_id"] and row["category"] == "rent_charge")
    assert rent_charge["amount_paise"] == quote["first_period_rent_paise"]
    assert rent_charge["amount_precise"] == quote["first_period_rent"]
    assert rent_charge["status"] == "open"
    assert rent_charge["due_date"] == quote["billable_start_date"]
    assert "No money has been collected" in rent_charge["description"]

    unit_rows = client.get("/owner/units", params={"property_id": prop["id"]})
    assert unit_rows.status_code == 200, unit_rows.text
    assert next(row for row in unit_rows.json() if row["id"] == unit["id"])["status"] == "reserved"

    set_actor("invite-tenant-2", "customer")
    competing_acceptance = client.post("/invitations/accept", json={
        "token": second.json()["invitation_url"].split("/")[-1], "phone": "9876543210",
        "profile_completion_acknowledged": True, "billable_start_date": date.today().isoformat(),
        "billable_terms_acknowledged": True,
    })
    assert competing_acceptance.status_code == 409, competing_acceptance.text

    set_actor("owner-1", "owner")
    notifications = client.get("/notifications")
    assert notifications.status_code == 200, notifications.text
    assert any("Tenant registration submitted" in row["title"] for row in notifications.json())


def test_cancelling_unpaid_invitation_reservation_voids_rent_charge():
    prop, unit = setup_property(owner_id="owner-1")
    _create_verified_customer("cancel-tenant-1", "cancel-tenant-1@example.test", "Cancel Tenant")
    set_actor("owner-1", "owner")
    invitation = client.post("/owner/invitations", json={
        "property_id": prop["id"], "unit_id": unit["id"],
        "recipient_email": "cancel-tenant-1@example.test", "recipient_name": "Cancel Tenant",
        "move_in_date": (date.today() + timedelta(days=4)).isoformat(),
    })
    assert invitation.status_code == 201, invitation.text
    token = invitation.json()["invitation_url"].split("/")[-1]

    set_actor("cancel-tenant-1", "customer")
    accepted = client.post("/invitations/accept", json={
        "token": token, "phone": "9876543210", "profile_completion_acknowledged": True,
        "billable_start_date": date.today().isoformat(), "billable_terms_acknowledged": True,
    })
    assert accepted.status_code == 201, accepted.text
    reservation_id = accepted.json()["reservation_id"]

    set_actor("owner-1", "owner")
    cancelled = client.post(f"/owner/reservations/{reservation_id}/status", json={"status": "cancelled"})
    assert cancelled.status_code == 200, cancelled.text
    ledger = client.get("/owner/ledger", params={"property_id": prop["id"]})
    assert ledger.status_code == 200, ledger.text
    invoice = next(row for row in ledger.json() if row["reservation_id"] == reservation_id and row["category"] == "rent_charge")
    assert invoice["status"] == "cancelled"
    assert cancelled.json()["cancelled_rent_charge_ids"] == [invoice["id"]]
    units = client.get("/owner/units", params={"property_id": prop["id"]})
    assert next(row for row in units.json() if row["id"] == unit["id"])["status"] == "available"


def test_registration_charge_cannot_be_voided_after_a_payment_was_recorded():
    prop, unit = setup_property(owner_id="owner-1")
    _create_verified_customer("paid-tenant-1", "paid-tenant-1@example.test", "Paid Tenant")
    set_actor("owner-1", "owner")
    invitation = client.post("/owner/invitations", json={
        "property_id": prop["id"], "unit_id": unit["id"],
        "recipient_email": "paid-tenant-1@example.test", "recipient_name": "Paid Tenant",
        "move_in_date": (date.today() + timedelta(days=4)).isoformat(),
    })
    assert invitation.status_code == 201, invitation.text
    token = invitation.json()["invitation_url"].split("/")[-1]
    set_actor("paid-tenant-1", "customer")
    accepted = client.post("/invitations/accept", json={
        "token": token, "phone": "9876543210", "profile_completion_acknowledged": True,
        "billable_start_date": date.today().isoformat(), "billable_terms_acknowledged": True,
    })
    assert accepted.status_code == 201, accepted.text
    reservation_id = accepted.json()["reservation_id"]

    set_actor("owner-1", "owner")
    payment = client.post("/owner/ledger", json={
        "property_id": prop["id"], "reservation_id": reservation_id,
        "category": "payment", "amount": 250, "billing_period": date.today().strftime("%Y-%m"),
        "payment_method": "manual", "payment_reference": "test receipt",
        "description": "Test payment entry; provider settlement is not implied.",
    })
    assert payment.status_code == 201, payment.text
    cancelled = client.post(f"/owner/reservations/{reservation_id}/status", json={"status": "cancelled"})
    assert cancelled.status_code == 409, cancelled.text
    with SessionLocal() as db:
        reservation = db.get(m.Reservation, reservation_id)
        charge = db.scalar(select(m.LedgerEntry).where(
            m.LedgerEntry.reservation_id == reservation_id,
            m.LedgerEntry.category == "rent_charge",
        ))
        assert reservation.status == "pending"
        assert charge.status == "open"


def test_owner_can_reassign_pending_invitation_and_tenant_sees_updated_bed_terms():
    prop, original_unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    replacement = client.post(f"/owner/properties/{prop['id']}/units", json={
        "floor_label": "Second", "room_code": "202", "unit_code": "202-B",
        "rentable_type": "bed", "sharing_capacity": 3, "monthly_rent": 10500, "deposit": 21000,
    })
    assert replacement.status_code == 201, replacement.text
    created = client.post("/owner/invitations", json={
        "property_id": prop["id"], "unit_id": original_unit["id"],
        "recipient_email": "not-yet-registered@example.test", "recipient_name": "New Tenant",
        "move_in_date": (date.today() + timedelta(days=14)).isoformat(),
    })
    assert created.status_code == 201, created.text
    token = created.json()["invitation_url"].split("/")[-1]

    reassigned = client.post(f"/owner/invitations/{created.json()['id']}/reassign", json={
        "new_unit_id": replacement.json()["id"],
        "reason": "The original bed is being repaired; this replacement is available.",
    })
    assert reassigned.status_code == 200, reassigned.text
    assert reassigned.json()["unit_id"] == replacement.json()["id"]
    assert reassigned.json()["monthly_rent"] == 10500
    assert reassigned.json()["reassignment_reason"].startswith("The original bed")

    public = client.get("/invitations/resolve", params={"token": token})
    assert public.status_code == 200, public.text
    assert public.json()["unit_code"] == "202-B"
    assert public.json()["monthly_rent"] == 10500
    assert public.json()["reassignment_reason"].startswith("The original bed")



def test_reservations_block_duplicate_active_unit():
    prop, unit = setup_property()
    set_actor("customer-1", "customer")
    payload = {"property_id": prop["id"], "unit_id": unit["id"], "move_in_date": (date.today() + timedelta(days=7)).isoformat(), "terms_snapshot": {"currency": "INR"}}
    first = client.post("/reservations", json=payload)
    assert first.status_code == 201, first.text
    reservation_rows = client.get("/reservations")
    assert reservation_rows.status_code == 200, reservation_rows.text
    assert reservation_rows.json()[0]["property_name"] == "Test House"
    second = client.post("/reservations", json=payload)
    assert second.status_code == 409, second.text
    set_actor("owner-1", "owner")
    manual_release = client.patch(f"/owner/units/{unit['id']}/status", json={"status": "available"})
    assert manual_release.status_code == 409, manual_release.text
    invalid_status_update = client.post(f"/owner/reservations/{first.json()['id']}/status", json={"status": "checked_in"})
    assert invalid_status_update.status_code == 409
    status_update = client.post(f"/owner/reservations/{first.json()['id']}/status", json={"status": "confirmed"})
    assert status_update.status_code == 200, status_update.text
    assert status_update.json()["status"] == "confirmed"
    confirmed_rows = client.get("/reservations")
    assert confirmed_rows.status_code == 200, confirmed_rows.text
    assert confirmed_rows.json()[0]["status"] == "confirmed"
    assert confirmed_rows.json()[0]["expires_at"] is None


def test_expired_pending_reservation_releases_unit():
    prop, unit = setup_property()
    set_actor("customer-1", "customer")
    payload = {
        "property_id": prop["id"], "unit_id": unit["id"],
        "move_in_date": (date.today() + timedelta(days=7)).isoformat(),
        "terms_snapshot": {"currency": "INR"}
    }
    first = client.post("/reservations", json=payload)
    assert first.status_code == 201, first.text

    with SessionLocal() as db:
        held = db.get(m.Reservation, first.json()["id"])
        held.expires_at = datetime.now(timezone.utc) - timedelta(minutes=5)
        db.commit()

    set_actor("customer-2", "customer")
    second = client.post("/reservations", json=payload)
    assert second.status_code == 201, second.text
    with SessionLocal() as db:
        assert db.get(m.Reservation, first.json()["id"]).status == "expired"


def test_assigned_manager_reads_only_scoped_marketplace_records():
    property_a, unit_a = setup_property(owner_id="owner-a")
    property_b, unit_b = setup_property(owner_id="owner-b")

    def create_customer_records(customer_id, prop, unit):
        set_actor(customer_id, "customer")
        enquiry = client.post("/enquiries", json={
            "property_id": prop["id"], "preferred_room_type": "2 sharing", "message": "Is a bed available?"
        })
        assert enquiry.status_code == 201, enquiry.text
        visit = client.post("/visits", json={
            "property_id": prop["id"], "enquiry_id": enquiry.json()["id"],
            "preferred_at": (date.today() + timedelta(days=2)).isoformat() + "T15:00:00"
        })
        assert visit.status_code == 201, visit.text
        reservation = client.post("/reservations", json={
            "property_id": prop["id"], "unit_id": unit["id"],
            "move_in_date": (date.today() + timedelta(days=7)).isoformat(),
            "terms_snapshot": {"currency": "INR"}
        })
        assert reservation.status_code == 201, reservation.text
        return enquiry.json(), visit.json(), reservation.json()

    enquiry_a, visit_a, reservation_a = create_customer_records("customer-a", property_a, unit_a)
    create_customer_records("customer-b", property_b, unit_b)

    with SessionLocal() as db:
        db.add(m.StaffAssignment(
            owner_id="owner-a", staff_user_id="manager-1", property_id=property_a["id"],
            role="manager", permissions=["enquiries.view", "visits.view", "reservations.view", "dashboard.view"], status="active"
        ))
        db.commit()

    set_actor("manager-1", "manager")
    assert client.get("/enquiries").json()[0]["property_id"] == property_a["id"]
    assert client.get("/visits").json()[0]["property_id"] == property_a["id"]
    assert client.get("/reservations").json()[0]["property_id"] == property_a["id"]
    assert client.get("/owner/dashboard").json()["property_count"] == 1

    denied_update = client.post(
        f"/owner/enquiries/{enquiry_a['id']}/status",
        json={"status": "contacted"}
    )
    assert denied_update.status_code == 403


def test_ledger_notice_staff_and_amenity_contracts():
    prop, unit = setup_property()
    set_actor("owner-1", "owner")
    ledger = client.post("/owner/ledger", json={"property_id": prop["id"], "category": "rent_charge", "amount": 9000,
                                                 "billing_period": "2026-11", "description": "Monthly rent", "status": "open"})
    assert ledger.status_code == 201, ledger.text
    notice = client.post("/owner/notices", json={"property_id": prop["id"], "title": "Water maintenance", "body": "Water supply interruption tomorrow", "status": "draft"})
    assert notice.status_code == 201, notice.text
    staff = client.post("/owner/staff", json={"property_id": prop["id"], "staff_user_id": "caretaker-1", "role": "caretaker", "permissions": ["inventory.view", "issues.manage"]})
    assert staff.status_code == 201, staff.text
    amenities = client.put(f"/owner/properties/{prop['id']}/amenities", json={"amenities": [
        {"code": "wifi", "label": "Wi-Fi", "category": "utilities", "scope": "property", "availability": "available", "pricing": "included"},
        {"code": "ac-room-101", "label": "Room AC", "category": "room", "scope": "unit", "unit_id": unit["id"], "availability": "available", "pricing": "extra_charge", "extra_charge": 800},
    ]})
    assert amenities.status_code == 200, amenities.text
    readback = client.get(f"/properties/{prop['id']}/amenities")
    assert readback.status_code == 200, readback.text
    assert len(readback.json()) == 2


def test_review_requires_completed_stay():
    prop, unit = setup_property()
    set_actor("customer-1", "customer")
    # A customer cannot submit a review simply by knowing a property's ID.
    review = client.post("/reviews", json={"resident_stay_id": 9999, "rating": 5, "body": "Great"})
    assert review.status_code == 403


def test_photo_metadata_and_admin_misleading_listing_reports():
    prop, _ = setup_property()
    set_actor("owner-1", "owner")
    photo = client.post(f"/owner/properties/{prop['id']}/photos", json={"storage_key": "uploads/property/test-photo-01", "caption": "Front view", "sort_order": 1})
    assert photo.status_code == 201, photo.text
    assert client.get(f"/properties/{prop['id']}/photos").json()[0]["caption"] == "Front view"
    # Device file/content links and public URLs must not be accepted as storage keys.
    blocked = client.post(f"/owner/properties/{prop['id']}/photos", json={"storage_key": "https://example.invalid/photo.jpg"})
    assert blocked.status_code == 422
    set_actor("customer-1", "customer")
    report = client.post("/reports", json={"target_type": "property", "target_id": str(prop["id"]), "property_id": prop["id"], "category": "inaccurate_listing", "details": "Availability is not accurate."})
    assert report.status_code == 201, report.text
    set_actor("admin-1", "admin")
    rows = client.get("/admin/moderation-reports?status=open")
    assert rows.status_code == 200 and rows.json()[0]["id"] == report.json()["id"]
    decision = client.patch(f"/admin/moderation-reports/{report.json()['id']}", json={"status": "investigating", "moderator_note": "Check availability"})
    assert decision.status_code == 200 and decision.json()["status"] == "investigating"


def test_privacy_requests_and_idempotent_pro_rata_billing():
    prop, unit = setup_property()
    set_actor("owner-1", "owner")
    stay_response = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "customer-1",
        "joining_date": "2026-11-16", "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay_response.status_code == 201, stay_response.text
    bill_payload = {"property_id": prop["id"], "billing_period": "2026-11", "due_date": "2026-11-20", "pro_rata_policy": "actual_days"}
    first = client.post("/owner/billing/generate", json=bill_payload)
    assert first.status_code == 200, first.text
    assert first.json()["created_count"] == 1 and first.json()["entries"][0]["amount"] == 4500
    second = client.post("/owner/billing/generate", json=bill_payload)
    assert second.status_code == 200 and second.json()["created_count"] == 0 and second.json()["skipped_existing"] == 1
    set_actor("customer-1", "customer")
    privacy = client.post("/privacy/requests", json={"request_type": "export", "details": "Please provide my data."})
    assert privacy.status_code == 201, privacy.text
    mine = client.get("/privacy/requests/me")
    assert mine.status_code == 200 and mine.json()[0]["id"] == privacy.json()["id"]


def test_resident_cannot_read_another_stays_documents():
    prop, unit = setup_property()
    set_actor("owner-1", "owner")
    stay = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "customer-1",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay.status_code == 201, stay.text
    set_actor("someone-else", "customer")
    assert client.get(f"/stays/{stay.json()['id']}/documents").status_code == 403


def test_urgent_maintenance_report_notifies_owner_and_blocks_unit():
    prop, unit = setup_property()
    set_actor("owner-1", "owner")
    stay = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "customer-1",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay.status_code == 201, stay.text
    set_actor("customer-1", "customer")
    issue = client.post("/issues", json={
        "property_id": prop["id"], "unit_id": unit["id"], "category": "plumbing",
        "title": "Bathroom leak", "description": "Water is leaking near the shower.", "priority": "urgent",
    })
    assert issue.status_code == 201, issue.text
    inventory = client.get("/properties", params={"city": "Pune"}).json()["items"][0]["inventory"]
    assert inventory["maintenance"] == 1
    set_actor("owner-1", "owner")
    messages = client.get("/notifications")
    assert messages.status_code == 200
    assert any("Maintenance issue reported" in n["title"] for n in messages.json())


def test_maintenance_issues_are_scoped_to_staff_permissions():
    property_a, unit_a = setup_property(owner_id="owner-a")
    property_b, unit_b = setup_property(owner_id="owner-b")

    def create_resident_issue(owner_id, customer_id, prop, unit):
        set_actor(owner_id, "owner")
        stay = create_test_stay({
            "property_id": prop["id"], "unit_id": unit["id"], "resident_id": customer_id,
            "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 18000,
        })
        assert stay.status_code == 201, stay.text
        set_actor(customer_id, "customer")
        issue = client.post("/issues", json={
            "property_id": prop["id"], "unit_id": unit["id"], "category": "plumbing",
            "title": "Bathroom leak", "description": "Water is leaking around the shower.",
            "priority": "normal",
        })
        assert issue.status_code == 201, issue.text
        return issue.json()

    issue_a = create_resident_issue("owner-a", "resident-a", property_a, unit_a)
    issue_b = create_resident_issue("owner-b", "resident-b", property_b, unit_b)

    with SessionLocal() as db:
        db.add(m.StaffAssignment(
            owner_id="owner-a", staff_user_id="maintenance-a", property_id=property_a["id"],
            role="maintenance", permissions=["issues.view", "issues.manage"], status="active"
        ))
        db.commit()

    set_actor("maintenance-a", "staff")
    visible = client.get("/issues")
    assert visible.status_code == 200, visible.text
    assert len(visible.json()) == 1
    assert visible.json()[0]["property_id"] == property_a["id"]

    updated = client.patch(f"/owner/issues/{issue_a['id']}", json={"status": "in_progress"})
    assert updated.status_code == 200, updated.text
    assert updated.json()["status"] == "in_progress"

    denied = client.patch(f"/owner/issues/{issue_b['id']}", json={"status": "resolved"})
    assert denied.status_code == 403

    with SessionLocal() as db:
        db.add(m.StaffAssignment(
            owner_id="owner-a", staff_user_id="maintenance-viewer", property_id=property_a["id"],
            role="maintenance", permissions=["maintenance.view"], status="active"
        ))
        db.commit()

    set_actor("maintenance-viewer", "staff")
    view_only = client.get("/issues")
    assert view_only.status_code == 200, view_only.text
    assert len(view_only.json()) == 1
    assert view_only.json()[0]["property_id"] == property_a["id"]

    view_only_update = client.patch(f"/owner/issues/{issue_a['id']}", json={"status": "resolved"})
    assert view_only_update.status_code == 403


def test_resident_ledger_is_scoped_to_the_authenticated_resident():
    property_a, unit_a = setup_property(owner_id="owner-a")
    property_b, unit_b = setup_property(owner_id="owner-b")

    set_actor("owner-a", "owner")
    stay_a = create_test_stay({
        "property_id": property_a["id"], "unit_id": unit_a["id"], "resident_id": "resident-a",
        "joining_date": "2026-10-01", "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay_a.status_code == 201, stay_a.text

    set_actor("owner-b", "owner")
    stay_b = create_test_stay({
        "property_id": property_b["id"], "unit_id": unit_b["id"], "resident_id": "resident-b",
        "joining_date": "2026-10-01", "agreed_monthly_rent": 8500, "deposit_held": 15000,
    })
    assert stay_b.status_code == 201, stay_b.text

    with SessionLocal() as db:
        for prop, stay_id, amount, creator in [
            (property_a, stay_a.json()["id"], 9000, "owner-a"),
            (property_b, stay_b.json()["id"], 8500, "owner-b"),
        ]:
            db.add(m.LedgerEntry(
                property_id=prop["id"], resident_stay_id=stay_id, category="rent_charge",
                amount=amount, billing_period="2026-11", description="November rent",
                status="open", provider_status="manual_record", created_by=creator,
            ))
        db.commit()

    set_actor("resident-a", "customer")
    resident_a_rows = client.get("/ledger/me")
    assert resident_a_rows.status_code == 200, resident_a_rows.text
    assert resident_a_rows.json()
    assert {row["resident_stay_id"] for row in resident_a_rows.json()} == {stay_a.json()["id"]}
    assert {row["property_id"] for row in resident_a_rows.json()} == {property_a["id"]}

    set_actor("resident-b", "customer")
    resident_b_rows = client.get("/ledger/me")
    assert resident_b_rows.status_code == 200, resident_b_rows.text
    assert {row["resident_stay_id"] for row in resident_b_rows.json()} == {stay_b.json()["id"]}


def test_owner_ledger_is_scoped_and_manual_payment_cannot_claim_provider_verification():
    property_a, _ = setup_property(owner_id="owner-a")
    property_b, _ = setup_property(owner_id="owner-b")

    set_actor("owner-a", "owner")
    manually_recorded = client.post("/owner/ledger", json={
        "property_id": property_a["id"], "category": "payment", "amount": 9000,
        "provider_status": "manual_record", "status": "recorded",
        "paid_at": "2026-11-15T00:00:00+00:00",
        "description": "Cash payment noted by owner",
    })
    assert manually_recorded.status_code == 201, manually_recorded.text
    assert manually_recorded.json()["provider_status"] == "manual_record"
    assert manually_recorded.json()["paid_at"].startswith("2026-11-15")

    falsely_confirmed = client.post("/owner/ledger", json={
        "property_id": property_a["id"], "category": "payment", "amount": 9000,
        "provider_status": "provider_confirmed", "status": "paid",
    })
    assert falsely_confirmed.status_code == 422, falsely_confirmed.text

    with SessionLocal() as db:
        db.add(m.LedgerEntry(
            property_id=property_b["id"], category="rent_charge", amount=8500,
            billing_period="2026-11", description="Other owner's rent",
            status="open", provider_status="manual_record", created_by="owner-b",
        ))
        db.commit()

    owner_rows = client.get("/owner/ledger")
    assert owner_rows.status_code == 200, owner_rows.text
    assert owner_rows.json()
    assert {row["property_id"] for row in owner_rows.json()} == {property_a["id"]}
    assert any(row["id"] == manually_recorded.json()["id"] for row in owner_rows.json())
    scoped_rows = client.get("/owner/ledger", params={"property_id": property_b["id"]})
    assert scoped_rows.status_code == 403, scoped_rows.text


def test_move_in_deposit_is_added_to_the_resident_ledger_as_manual_record():
    prop, unit = setup_property(owner_id="owner-1")
    set_actor("owner-1", "owner")
    stay = create_test_stay({
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "resident-deposit",
        "joining_date": "2026-10-01", "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay.status_code == 201, stay.text

    set_actor("resident-deposit", "customer")
    response = client.get("/ledger/me")
    assert response.status_code == 200, response.text
    deposits = [row for row in response.json() if row["category"] == "deposit"]
    assert len(deposits) == 1
    assert deposits[0]["amount"] == 18000
    assert deposits[0]["provider_status"] == "manual_record"
    assert deposits[0]["status"] == "held"


def test_email_account_registration_login_role_boundary_logout_and_password_reset(monkeypatch):
    delivered = {}

    monkeypatch.setattr(auth_routes, "_mail_ready", lambda: True)
    def fake_send(recipient, subject, code, body_text):
        delivered[subject] = {"recipient": recipient, "code": code, "body": body_text}
    monkeypatch.setattr(auth_routes, "send_security_code", fake_send)

    payload = {
        "email": "New.Owner@Example.com", "display_name": "New Owner",
        "password": "Correct-Horse-Battery-9", "requested_workspace": "owner",
    }
    registered = client.post("/auth/register", json=payload)
    assert registered.status_code == 202, registered.text
    assert registered.json()["email"] == "new.owner@example.com"
    with SessionLocal() as db:
        account = db.scalar(select(m.User).where(m.User.email == "new.owner@example.com"))
        credential = db.get(m.UserCredential, account.id)
        assert account.role == "customer"
        assert credential.requested_workspace == "owner"
        assert credential.email_verified_at is None
    assert client.post("/auth/login", json={"email": payload["email"], "password": payload["password"]}).status_code == 403

    generated_code = delivered["Verify your StayEasy account"]["code"]
    wrong_code = "000000" if generated_code != "000000" else "000001"
    bad_code = client.post("/auth/verify-email", json={"email": payload["email"], "code": wrong_code})
    assert bad_code.status_code == 400
    code = generated_code
    verified = client.post("/auth/verify-email", json={"email": payload["email"], "code": code})
    assert verified.status_code == 200, verified.text
    logged_in = client.post("/auth/login", json={"email": payload["email"], "password": payload["password"]})
    assert logged_in.status_code == 200, logged_in.text
    token = logged_in.json()["access_token"]
    assert logged_in.json()["user"]["role"] == "customer"
    assert logged_in.json()["user"]["owner_access_status"] == "pending_approval"
    auth_header = {"Authorization": f"Bearer {token}"}
    assert client.get("/auth/me", headers=auth_header).json()["requested_workspace"] == "owner"
    # Asking for an Owner workspace must never self-assign owner privileges.
    denied = client.post("/owner/properties", headers=auth_header, json={
        "name": "Not Yet An Owner", "locality": "Wakad", "city": "Pune"
    })
    assert denied.status_code == 403, denied.text

    requested = client.post("/auth/forgot-password", json={"email": payload["email"]})
    assert requested.status_code == 202, requested.text
    reset_code = delivered["Reset your StayEasy password"]["code"]
    reset = client.post("/auth/reset-password", json={
        "email": payload["email"], "code": reset_code, "new_password": "New-Secure-Password-2026"
    })
    assert reset.status_code == 200, reset.text
    assert client.get("/auth/me", headers=auth_header).status_code == 401
    assert client.post("/auth/login", json={"email": payload["email"], "password": payload["password"]}).status_code == 401

    new_login = client.post("/auth/login", json={
        "email": payload["email"], "password": "New-Secure-Password-2026"
    })
    assert new_login.status_code == 200, new_login.text
    new_header = {"Authorization": f"Bearer {new_login.json()['access_token']}"}
    assert client.post("/auth/logout", headers=new_header).status_code == 200
    assert client.get("/auth/me", headers=new_header).status_code == 401


def test_admin_bootstrap_creates_verified_hashed_account_and_audit_event():
    with SessionLocal() as db:
        user = provision_admin(
            db,
            " First.Admin@Example.test ",
            " First StayEasy Admin ",
            "Admin-Secret-Only-For-Tests-2026",
        )
        user_id = user.id
        assert user.email == "first.admin@example.test"
        assert user.display_name == "First StayEasy Admin"
        assert user.role == "admin"
        assert user.is_active is True

    with SessionLocal() as db:
        user = db.get(m.User, user_id)
        credential = db.get(m.UserCredential, user_id)
        assert credential is not None
        assert credential.email_verified_at is not None
        assert credential.owner_access_status == "not_requested"
        assert credential.password_hash != "Admin-Secret-Only-For-Tests-2026"
        assert verify_password("Admin-Secret-Only-For-Tests-2026", credential.password_hash)
        event = db.scalar(select(m.AuditLog).where(
            m.AuditLog.entity_type == "user",
            m.AuditLog.entity_id == user_id,
            m.AuditLog.action == "admin_account_bootstrapped",
        ))
        assert event is not None
        assert event.actor_role == "system"

    try:
        with SessionLocal() as db:
            provision_admin(db, "FIRST.ADMIN@example.test", "Duplicate Admin", "Another-Long-Secret-2026")
        assert False, "Duplicate email must not create or replace an administrator"
    except ValueError as exc:
        assert "already exists" in str(exc)

    try:
        with SessionLocal() as db:
            provision_admin(db, "short-password@example.test", "Short Password", "short")
        assert False, "Weak administrator password must be rejected"
    except ValueError as exc:
        assert "12 to 128" in str(exc)


def _create_owner_access_applicant(user_id, email, *, verified=True, request_status="pending_approval"):
    requested_at = datetime.now(timezone.utc)
    user = m.User(
        id=user_id, email=email, display_name=user_id.replace("-", " ").title(),
        role="customer", is_active=True,
    )
    credential = m.UserCredential(
        user_id=user_id,
        password_hash="test-only-hash",
        requested_workspace="owner",
        owner_access_status=request_status,
        owner_access_requested_at=requested_at,
        email_verified_at=requested_at if verified else None,
    )
    with SessionLocal() as db:
        db.add_all([user, credential])
        db.commit()
    return requested_at


def test_admin_owner_access_review_requires_verification_and_audits_decision():
    verified_requested_at = _create_owner_access_applicant(
        "owner-applicant-1", "owner-applicant-1@example.test", verified=True
    )
    prior_customer_token = create_access_token(
        "owner-applicant-1", "customer", scopes=["account"], auth_version=0
    )
    _create_owner_access_applicant(
        "owner-applicant-unverified", "unverified-owner@example.test", verified=False
    )

    set_actor("customer-1", "customer")
    assert client.get("/admin/owner-requests").status_code == 403

    set_actor("admin-1", "admin")
    listing = client.get("/admin/owner-requests")
    assert listing.status_code == 200, listing.text
    requests = {row["id"]: row for row in listing.json()}
    assert set(requests) == {"owner-applicant-1", "owner-applicant-unverified"}
    assert requests["owner-applicant-1"]["email_verified"] is True
    assert requests["owner-applicant-unverified"]["email_verified"] is False
    assert requests["owner-applicant-1"]["owner_access_status"] == "pending_approval"

    denied_approval = client.post("/admin/owner-requests/owner-applicant-unverified/decision", json={
        "decision": "approve", "note": "Email verification required",
    })
    assert denied_approval.status_code == 422, denied_approval.text

    rejected = client.post("/admin/owner-requests/owner-applicant-1/decision", json={
        "decision": "reject", "note": "Please provide business registration details.",
    })
    assert rejected.status_code == 200, rejected.text
    assert rejected.json()["role"] == "customer"
    assert rejected.json()["owner_access_status"] == "rejected"

    with SessionLocal() as db:
        user = db.get(m.User, "owner-applicant-1")
        credential = db.get(m.UserCredential, "owner-applicant-1")
        assert user.role == "customer"
        assert credential.owner_access_status == "rejected"
        assert credential.owner_access_review_note == "Please provide business registration details."
        assert credential.owner_access_reviewed_at is not None
        assert credential.auth_version == 0
        audit_row = db.scalar(select(m.AuditLog).where(
            m.AuditLog.entity_type == "user",
            m.AuditLog.entity_id == "owner-applicant-1",
            m.AuditLog.action == "owner_access_decision",
        ))
        assert audit_row is not None
        assert audit_row.actor_role == "admin"

    # A customer can re-request after rejection, but the role remains customer.
    api.app.dependency_overrides[get_actor] = lambda: {
        "subject": "owner-applicant-1", "role": "customer", "scopes": ["account"], "account_session": True,
    }
    re_request = client.post("/auth/owner-access-request")
    assert re_request.status_code == 200, re_request.text
    assert re_request.json()["owner_access_status"] == "pending_approval"
    assert re_request.json()["role"] == "customer"
    assert re_request.json()["owner_access_review_note"] == ""
    assert re_request.json()["owner_access_requested_at"] is not None

    # Return to the verified admin actor and approve the re-submitted request.
    set_actor("admin-1", "admin")
    approved = client.post("/admin/owner-requests/owner-applicant-1/decision", json={
        "decision": "approve", "note": "Verified owner identity.",
    })
    assert approved.status_code == 200, approved.text
    assert approved.json()["role"] == "owner"
    assert approved.json()["owner_access_status"] == "active"

    with SessionLocal() as db:
        user = db.get(m.User, "owner-applicant-1")
        credential = db.get(m.UserCredential, "owner-applicant-1")
        assert user.role == "owner"
        assert credential.owner_access_status == "approved"
        assert credential.auth_version == 1
        assert credential.owner_access_review_note == "Verified owner identity."
        assert credential.owner_access_reviewed_at is not None

    already_decided = client.post("/admin/owner-requests/owner-applicant-1/decision", json={
        "decision": "reject", "note": "A second decision must not be accepted.",
    })
    assert already_decided.status_code == 409

    # Approval increments auth_version so the applicant's prior customer token is no longer valid.
    api.app.dependency_overrides.clear()
    stale_customer_session = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {prior_customer_token}"},
    )
    assert stale_customer_session.status_code == 401, stale_customer_session.text


def test_owner_access_request_endpoint_requires_account_session_and_is_idempotent():
    _create_owner_access_applicant(
        "customer-owner-request", "customer-owner-request@example.test", verified=True, request_status="rejected"
    )
    set_actor("customer-owner-request", "customer")
    assert client.post("/auth/owner-access-request").status_code == 403

    api.app.dependency_overrides[get_actor] = lambda: {
        "subject": "customer-owner-request", "role": "customer", "scopes": ["account"], "account_session": True,
    }
    first = client.post("/auth/owner-access-request")
    assert first.status_code == 200, first.text
    assert first.json()["owner_access_status"] == "pending_approval"
    assert first.json()["role"] == "customer"

    second = client.post("/auth/owner-access-request")
    assert second.status_code == 200, second.text
    assert second.json()["owner_access_status"] == "pending_approval"
    assert second.json()["owner_access_requested_at"] == first.json()["owner_access_requested_at"]

    with SessionLocal() as db:
        user = db.get(m.User, "customer-owner-request")
        credential = db.get(m.UserCredential, "customer-owner-request")
        assert user.role == "customer"
        assert credential.requested_workspace == "owner"
        assert credential.owner_access_status == "pending_approval"
        assert credential.owner_access_reviewed_at is None


def test_registration_requires_configured_email_delivery(monkeypatch):
    monkeypatch.setattr(auth_routes, "_mail_ready", lambda: False)
    response = client.post("/auth/register", json={
        "email": "missing.smtp@example.com", "display_name": "SMTP Missing",
        "password": "Correct-Horse-Battery-9", "requested_workspace": "tenant",
    })
    assert response.status_code == 503
    with SessionLocal() as db:
        assert db.scalar(select(m.User).where(m.User.email == "missing.smtp@example.com")) is None


def _create_tenant_document_invitation(owner_id: str, tenant_id: str, email: str):
    prop, unit = setup_property(owner_id=owner_id)
    _create_verified_customer(tenant_id, email, "Document Test Tenant")
    reason = "Verify identity for the rental agreement and property registration."
    set_actor(owner_id, "owner")
    invitation = client.post("/owner/invitations", json={
        "property_id": prop["id"],
        "unit_id": unit["id"],
        "recipient_email": email,
        "recipient_name": "Document Test Tenant",
        "recipient_phone": "9876543210",
        "move_in_date": (date.today() + timedelta(days=10)).isoformat(),
        "required_document_categories": ["government_id"],
        "document_request_reason": reason,
    })
    assert invitation.status_code == 201, invitation.text
    return prop, unit, invitation.json(), reason


def test_invitation_documents_are_encrypted_scoped_and_retention_limited(monkeypatch, tmp_path):
    owner_id = "document-owner"
    tenant_id = "document-tenant"
    email = "document-tenant@example.test"
    prop, _unit, invitation, purpose = _create_tenant_document_invitation(owner_id, tenant_id, email)

    storage_root = tmp_path / "encrypted-documents"
    key = base64.urlsafe_b64encode(b"stayeasy-document-test-key-00001").decode("ascii").rstrip("=")
    monkeypatch.setenv("DOCUMENT_STORAGE_ROOT", str(storage_root))
    monkeypatch.setenv("DOCUMENT_ENCRYPTION_KEY", key)
    monkeypatch.setenv("DOCUMENT_RETENTION_DAYS", "30")

    plaintext = b"%PDF-1.7\\nPRIVATE TENANT ID TEST CONTENT 3fc12d\\n%%EOF"
    set_actor(tenant_id, "customer")
    uploaded = client.post(
        f"/tenant/invitations/{invitation['id']}/documents",
        data={
            "category": "government_id",
            "purpose": purpose,
            "consent_confirmed": "true",
        },
        files={"file": ("identity.pdf", plaintext, "application/pdf")},
    )
    assert uploaded.status_code == 201, uploaded.text
    metadata = uploaded.json()
    assert metadata["status"] == "submitted"
    assert metadata["category"] == "government_id"
    assert metadata["purpose"] == purpose
    assert metadata["content_url"] == f"/invitations/{invitation['id']}/documents/{metadata['id']}/content"
    assert "storage_key" not in metadata

    with SessionLocal() as db:
        document = db.get(m.InvitationDocument, metadata["id"])
        assert document is not None
        storage_key = document.storage_key
        retention_until = document.retention_until
        assert len(storage_key) == 32
        assert all(char in "0123456789abcdef" for char in storage_key)
        encrypted_path = storage_root / f"{storage_key}.enc"
        encrypted = encrypted_path.read_bytes()
        assert encrypted.startswith(b"SE1")
        assert plaintext not in encrypted
        assert encrypted_path.stat().st_mode & 0o777 == 0o600
        assert storage_root.stat().st_mode & 0o777 == 0o700

    # The invited tenant and the property owner can retrieve the document; an unrelated
    # tenant and an unrelated owner cannot use the invitation/document IDs as a bearer link.
    tenant_download = client.get(metadata["content_url"])
    assert tenant_download.status_code == 200
    assert tenant_download.content == plaintext
    assert tenant_download.headers["cache-control"] == "no-store, private"
    assert tenant_download.headers["x-content-type-options"] == "nosniff"

    _create_verified_customer("document-intruder", "intruder@example.test", "Unrelated Tenant")
    set_actor("document-intruder", "customer")
    intruder_download = client.get(metadata["content_url"])
    assert intruder_download.status_code == 404

    set_actor(owner_id, "owner")
    owner_download = client.get(metadata["content_url"])
    assert owner_download.status_code == 200
    assert owner_download.content == plaintext

    set_actor("unrelated-owner", "owner")
    unrelated_owner = client.get(f"/owner/invitations/{invitation['id']}/documents")
    assert unrelated_owner.status_code in {403, 404}

    with SessionLocal() as db:
        document = db.get(m.InvitationDocument, metadata["id"])
        assert document is not None
        document.retention_until = datetime.now(timezone.utc) - timedelta(days=1)
        db.commit()
    set_actor(tenant_id, "customer")
    expired_download = client.get(metadata["content_url"])
    assert expired_download.status_code == 410
    assert retention_until is not None


def test_invitation_document_upload_requires_disclosed_purpose_consent_and_valid_file(monkeypatch, tmp_path):
    owner_id = "document-owner-2"
    tenant_id = "document-tenant-2"
    email = "document-tenant-2@example.test"
    _prop, _unit, invitation, purpose = _create_tenant_document_invitation(owner_id, tenant_id, email)
    monkeypatch.setenv("DOCUMENT_STORAGE_ROOT", str(tmp_path / "encrypted-documents"))
    key = base64.urlsafe_b64encode(b"stayeasy-document-test-key-00001").decode("ascii").rstrip("=")
    monkeypatch.setenv("DOCUMENT_ENCRYPTION_KEY", key)

    url = f"/tenant/invitations/{invitation['id']}/documents"
    file = {"file": ("identity.pdf", b"%PDF-1.7\\ncontent\\n%%EOF", "application/pdf")}
    set_actor(tenant_id, "customer")

    no_consent = client.post(url, data={
        "category": "government_id", "purpose": purpose, "consent_confirmed": "false",
    }, files=file)
    assert no_consent.status_code == 422, no_consent.text

    wrong_purpose = client.post(url, data={
        "category": "government_id", "purpose": "Generic document upload.", "consent_confirmed": "true",
    }, files=file)
    assert wrong_purpose.status_code == 422, wrong_purpose.text

    unrequested_category = client.post(url, data={
        "category": "other", "purpose": purpose, "consent_confirmed": "true",
    }, files=file)
    assert unrequested_category.status_code == 403, unrequested_category.text

    invalid_signature = client.post(url, data={
        "category": "government_id", "purpose": purpose, "consent_confirmed": "true",
    }, files={"file": ("identity.pdf", b"This is not a PDF or image.", "application/pdf")})
    assert invalid_signature.status_code == 415, invalid_signature.text
    assert not list((tmp_path / "encrypted-documents").glob("*.enc")) if (tmp_path / "encrypted-documents").exists() else True


def test_retention_purge_deletes_expired_blobs_and_metadata_but_keeps_future_documents(tmp_path):
    owner_id = "retention-owner"
    tenant_id = "retention-tenant"
    email = "retention-tenant@example.test"
    _prop, _unit, invitation, purpose = _create_tenant_document_invitation(owner_id, tenant_id, email)

    storage_root = tmp_path / "encrypted-documents"
    storage_root.mkdir(mode=0o700)
    now = datetime.now(timezone.utc)
    expired_key = "a" * 32
    missing_key = "b" * 32
    invalid_key = "invalid-storage-key"
    future_key = "c" * 32

    def make_doc(storage_key: str, retention_until: datetime, filename: str):
        return m.InvitationDocument(
            invitation_id=invitation["id"],
            uploaded_by=tenant_id,
            category="government_id",
            purpose=purpose,
            storage_key=storage_key,
            original_filename=filename,
            media_type="application/pdf",
            size_bytes=128,
            consent_at=now - timedelta(days=31),
            status="approved",
            retention_until=retention_until,
        )

    with SessionLocal() as db:
        expired_doc = make_doc(expired_key, now - timedelta(minutes=1), "expired-private.pdf")
        missing_doc = make_doc(missing_key, now - timedelta(hours=1), "already-missing-private.pdf")
        invalid_doc = make_doc(invalid_key, now - timedelta(hours=2), "invalid-key-private.pdf")
        future_doc = make_doc(future_key, now + timedelta(days=1), "future-private.pdf")
        db.add_all([expired_doc, missing_doc, invalid_doc, future_doc])
        db.commit()
        expired_id = expired_doc.id
        missing_id = missing_doc.id
        invalid_id = invalid_doc.id
        future_id = future_doc.id

    expired_blob = storage_root / f"{expired_key}.enc"
    future_blob = storage_root / f"{future_key}.enc"
    orphan_key = "d" * 32
    fresh_orphan_key = "e" * 32
    aged_orphan_blob = storage_root / f"{orphan_key}.enc"
    fresh_orphan_blob = storage_root / f"{fresh_orphan_key}.enc"
    unrelated_file = storage_root / "operator-notes.txt"
    expired_blob.write_bytes(b"SE1encrypted-expired-document")
    future_blob.write_bytes(b"SE1encrypted-future-document")
    aged_orphan_blob.write_bytes(b"SE1orphaned-encrypted-document")
    fresh_orphan_blob.write_bytes(b"SE1fresh-orphaned-encrypted-document")
    unrelated_file.write_text("Do not delete unrelated files.", encoding="utf-8")
    expired_blob.chmod(0o600)
    future_blob.chmod(0o600)
    aged_orphan_blob.chmod(0o600)
    fresh_orphan_blob.chmod(0o600)
    old_timestamp = (now - timedelta(hours=48)).timestamp()
    os.utime(aged_orphan_blob, (old_timestamp, old_timestamp))

    with SessionLocal() as db:
        dry_run = purge_expired_invitation_documents(db, storage_root, now=now, dry_run=True)
        assert dry_run["dry_run"] is True
        assert dry_run["expired_found"] == 3
        assert dry_run["purged"] == 0
        assert dry_run["orphan_found"] == 1
        assert dry_run["orphan_deleted"] == 0
        assert expired_blob.exists()
        assert aged_orphan_blob.exists()

        result = purge_expired_invitation_documents(db, storage_root, now=now, batch_size=2)

    assert result["dry_run"] is False
    assert result["expired_found"] == 3
    assert result["purged"] == 2
    assert result["content_deleted"] == 1
    assert result["content_missing"] == 1
    assert result["orphan_found"] == 1
    assert result["orphan_deleted"] == 1
    assert result["failed"] == 1
    assert {error["reason"] for error in result["errors"]} == {"storage_path_invalid"}
    assert not expired_blob.exists()
    assert not aged_orphan_blob.exists()
    assert future_blob.exists()
    assert fresh_orphan_blob.exists()
    assert unrelated_file.exists()

    with SessionLocal() as db:
        assert db.get(m.InvitationDocument, expired_id) is None
        assert db.get(m.InvitationDocument, missing_id) is None
        assert db.get(m.InvitationDocument, invalid_id) is not None
        assert db.get(m.InvitationDocument, future_id) is not None
        audit_rows = list(db.scalars(select(m.AuditLog).where(
            m.AuditLog.entity_type == "invitation_document",
            m.AuditLog.action == "retention_purge",
        )))
        assert len(audit_rows) == 2
        for row in audit_rows:
            assert row.actor_id == "system-retention-purge"
            assert row.actor_role == "system"
            assert "filename" not in str(row.old_value).lower()
            assert "purpose" not in row.old_value
            assert row.new_value["policy"] == "retention_expired"
        orphan_audits = list(db.scalars(select(m.AuditLog).where(
            m.AuditLog.entity_type == "invitation_document_blob",
            m.AuditLog.action == "orphan_blob_purge",
        )))
        assert len(orphan_audits) == 1
        assert orphan_audits[0].entity_id != orphan_key
        assert orphan_audits[0].new_value["policy"] == "orphan_grace_period"

def test_retention_purge_fails_closed_without_document_storage_root(monkeypatch):
    monkeypatch.delenv("DOCUMENT_STORAGE_ROOT", raising=False)
    with SessionLocal() as db:
        try:
            purge_expired_invitation_documents(db, now=datetime.now(timezone.utc))
        except ValueError as exc:
            assert "DOCUMENT_STORAGE_ROOT" in str(exc)
        else:
            raise AssertionError("Retention purge must refuse to run without an explicit storage root.")

def test_pre_registration_document_delete_keeps_metadata_when_blob_unlink_fails(monkeypatch, tmp_path):
    owner_id = "delete-failure-owner"
    tenant_id = "delete-failure-tenant"
    email = "delete-failure-tenant@example.test"
    _prop, _unit, invitation, purpose = _create_tenant_document_invitation(owner_id, tenant_id, email)
    storage_root = tmp_path / "encrypted-documents"
    storage_root.mkdir(mode=0o700)
    key = "f" * 32
    blob = storage_root / f"{key}.enc"
    blob.write_bytes(b"SE1encrypted-private-document")
    blob.chmod(0o600)
    monkeypatch.setenv("DOCUMENT_STORAGE_ROOT", str(storage_root))
    encryption_key = base64.urlsafe_b64encode(b"stayeasy-document-test-key-00001").decode("ascii").rstrip("=")
    monkeypatch.setenv("DOCUMENT_ENCRYPTION_KEY", encryption_key)

    with SessionLocal() as db:
        document = m.InvitationDocument(
            invitation_id=invitation["id"],
            uploaded_by=tenant_id,
            category="government_id",
            purpose=purpose,
            storage_key=key,
            original_filename="private.pdf",
            media_type="application/pdf",
            size_bytes=128,
            consent_at=datetime.now(timezone.utc),
            status="submitted",
            retention_until=datetime.now(timezone.utc) + timedelta(days=30),
        )
        db.add(document)
        db.commit()
        document_id = document.id

    original_unlink = Path.unlink
    fail_once = {"pending": True}

    def fail_document_unlink(path, *args, **kwargs):
        if path == blob and fail_once["pending"]:
            fail_once["pending"] = False
            raise PermissionError("simulated private storage failure")
        return original_unlink(path, *args, **kwargs)

    monkeypatch.setattr(Path, "unlink", fail_document_unlink)
    set_actor(tenant_id, "customer")
    response = client.delete(f"/tenant/invitations/{invitation['id']}/documents/{document_id}")
    assert response.status_code == 503, response.text
    assert blob.exists()
    with SessionLocal() as db:
        assert db.get(m.InvitationDocument, document_id) is not None


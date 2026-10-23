"""API contract tests against an isolated SQLite database.

Run from the project root with: python -m pytest backend/tests -q
SQLite verifies API contracts; PostgreSQL concurrency testing is still required before production.
"""
import os
import tempfile
from datetime import date, timedelta
from pathlib import Path

DB_PATH = Path(tempfile.gettempdir()) / f"stayeasy_test_{os.getpid()}.db"
os.environ["DATABASE_URL"] = f"sqlite:///{DB_PATH.as_posix()}"
os.environ["APP_ENV"] = "development"
os.environ["JWT_SECRET"] = "test-secret-long-enough-for-stayeasy-api-contract-tests"
os.environ["DEV_TOKEN_PASSWORD"] = "local-test-only-password"

from fastapi.testclient import TestClient
from sqlalchemy import delete

from backend.app import main as api
from backend.app.database import Base, engine, SessionLocal
from backend.app.security import get_actor
from backend.app import models as m

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


def setup_function():
    clear_db()


def teardown_function():
    api.app.dependency_overrides.clear()


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
    set_actor("owner-1", "owner")
    messages = client.get("/notifications")
    assert messages.status_code == 200
    assert len(messages.json()) >= 2


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
    stay_response = client.post("/owner/stays", json={
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
    stay = client.post("/owner/stays", json={
        "property_id": prop["id"], "unit_id": unit["id"], "resident_id": "customer-1",
        "joining_date": date.today().isoformat(), "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay.status_code == 201, stay.text
    set_actor("someone-else", "customer")
    assert client.get(f"/stays/{stay.json()['id']}/documents").status_code == 403


def test_urgent_maintenance_report_notifies_owner_and_blocks_unit():
    prop, unit = setup_property()
    set_actor("owner-1", "owner")
    stay = client.post("/owner/stays", json={
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

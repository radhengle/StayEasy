"""API contract tests against an isolated SQLite database.

Run from the project root with: python -m pytest backend/tests -q
SQLite verifies API contracts; PostgreSQL concurrency testing is still required before production.
"""
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
from backend.app.security import get_actor
from backend.app import models as m
from backend.app import auth as auth_routes

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


def test_maintenance_issues_are_scoped_to_staff_permissions():
    property_a, unit_a = setup_property(owner_id="owner-a")
    property_b, unit_b = setup_property(owner_id="owner-b")

    def create_resident_issue(owner_id, customer_id, prop, unit):
        set_actor(owner_id, "owner")
        stay = client.post("/owner/stays", json={
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
    stay_a = client.post("/owner/stays", json={
        "property_id": property_a["id"], "unit_id": unit_a["id"], "resident_id": "resident-a",
        "joining_date": "2026-10-01", "agreed_monthly_rent": 9000, "deposit_held": 18000,
    })
    assert stay_a.status_code == 201, stay_a.text

    set_actor("owner-b", "owner")
    stay_b = client.post("/owner/stays", json={
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
    stay = client.post("/owner/stays", json={
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


def test_registration_requires_configured_email_delivery(monkeypatch):
    monkeypatch.setattr(auth_routes, "_mail_ready", lambda: False)
    response = client.post("/auth/register", json={
        "email": "missing.smtp@example.com", "display_name": "SMTP Missing",
        "password": "Correct-Horse-Battery-9", "requested_workspace": "tenant",
    })
    assert response.status_code == 503
    with SessionLocal() as db:
        assert db.scalar(select(m.User).where(m.User.email == "missing.smtp@example.com")) is None

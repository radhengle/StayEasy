"""PostgreSQL integration tests for reservation races.

Run with DATABASE_URL pointing at a disposable PostgreSQL database:
  python -m pytest backend/tests_postgres -q

These tests intentionally run outside backend/tests/test_api_contracts.py, whose
module setup overrides DATABASE_URL with SQLite for the contract-test suite.
"""
import os
from concurrent.futures import ThreadPoolExecutor
from datetime import date, timedelta
from threading import Barrier

import pytest

DATABASE_URL = os.getenv("DATABASE_URL", "")
if not DATABASE_URL.startswith(("postgresql+psycopg://", "postgresql://")):
    pytest.skip(
        "PostgreSQL integration tests require DATABASE_URL to point at PostgreSQL",
        allow_module_level=True,
    )

os.environ.setdefault("APP_ENV", "test")
os.environ.setdefault("JWT_SECRET", "postgres-reservation-race-test-secret-at-least-32-chars")
os.environ.setdefault("DEV_TOKEN_PASSWORD", "local-only-postgres-test-password")

from fastapi import Request
from fastapi.testclient import TestClient
from sqlalchemy import select

from backend.app import main as api
from backend.app import models as m
from backend.app.database import Base, SessionLocal, engine
from backend.app.security import get_actor


OWNER_HEADERS = {"X-Test-Role": "owner", "X-Test-Subject": "race-owner"}
ADMIN_HEADERS = {"X-Test-Role": "admin", "X-Test-Subject": "race-admin"}


def actor_from_test_headers(request: Request) -> dict:
    """Supply a distinct actor per request without shared mutable test state."""
    return {
        "subject": request.headers.get("X-Test-Subject", "race-customer"),
        "role": request.headers.get("X-Test-Role", "customer"),
        "scopes": ["test"],
    }


def reset_database() -> None:
    with engine.begin() as connection:
        Base.metadata.drop_all(bind=connection)
        Base.metadata.create_all(bind=connection)


def create_published_property(client: TestClient) -> tuple[int, int]:
    created = client.post(
        "/owner/properties",
        headers=OWNER_HEADERS,
        json={
            "name": "Concurrency Test House",
            "description": "Disposable property for a PostgreSQL concurrency test",
            "locality": "Wakad",
            "city": "Pune",
            "rent_from": 9000,
            "deposit_from": 18000,
            "eligibility": "Mixed",
            "couples_welcome": True,
        },
    )
    assert created.status_code == 201, created.text
    property_id = created.json()["id"]

    created_unit = client.post(
        f"/owner/properties/{property_id}/units",
        headers=OWNER_HEADERS,
        json={
            "floor_label": "First",
            "room_code": "101",
            "unit_code": "101-A",
            "rentable_type": "bed",
            "sharing_capacity": 1,
            "monthly_rent": 9000,
            "deposit": 18000,
        },
    )
    assert created_unit.status_code == 201, created_unit.text
    unit_id = created_unit.json()["id"]

    submitted = client.post(f"/owner/properties/{property_id}/submit-review", headers=OWNER_HEADERS)
    assert submitted.status_code == 200, submitted.text
    approved = client.post(
        f"/admin/properties/{property_id}/decision",
        headers=ADMIN_HEADERS,
        json={"decision": "approve", "note": "approved for PostgreSQL concurrency test"},
    )
    assert approved.status_code == 200, approved.text
    return property_id, unit_id


def test_two_customers_cannot_reserve_the_same_bed_concurrently() -> None:
    """One request wins; the other receives conflict, with one active DB row."""
    reset_database()
    api.app.dependency_overrides[get_actor] = actor_from_test_headers

    try:
        with TestClient(api.app) as setup_client:
            property_id, unit_id = create_published_property(setup_client)

        start_together = Barrier(2)
        move_in_date = (date.today() + timedelta(days=1)).isoformat()

        def reserve_as(customer_id: str):
            with TestClient(api.app) as client:
                start_together.wait(timeout=10)
                return client.post(
                    "/reservations",
                    headers={"X-Test-Role": "customer", "X-Test-Subject": customer_id},
                    json={
                        "property_id": property_id,
                        "unit_id": unit_id,
                        "preferred_room_type": "sharing",
                        "move_in_date": move_in_date,
                        "terms_snapshot": {"test": "postgres-concurrency"},
                    },
                )

        with ThreadPoolExecutor(max_workers=2) as pool:
            responses = list(pool.map(reserve_as, ["race-customer-a", "race-customer-b"]))

        observed_statuses = sorted(response.status_code for response in responses)
        assert observed_statuses == [201, 409], [
            {"status": response.status_code, "body": response.text}
            for response in responses
        ]

        with SessionLocal() as db:
            active = list(
                db.scalars(
                    select(m.Reservation).where(
                        m.Reservation.unit_id == unit_id,
                        m.Reservation.status.in_(["pending", "confirmed", "checked_in"]),
                    )
                )
            )
            assert len(active) == 1
            assert active[0].customer_id in {"race-customer-a", "race-customer-b"}
            unit = db.get(m.PropertyUnit, unit_id)
            assert unit is not None
            assert unit.status == "reserved"
    finally:
        api.app.dependency_overrides.clear()

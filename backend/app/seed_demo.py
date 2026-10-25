"""Idempotent development fixtures for end-to-end StayEasy testing.

Never run against production. The records are explicitly test fixtures, not real
rental inventory or proof of real availability.
"""
from __future__ import annotations

import os
from datetime import date, timedelta

from sqlalchemy import select
from app.database import Base, SessionLocal, engine
from app import models as m


OWNER_ID = "stayeasy-demo-owner"
TENANT_ID = "stayeasy-demo-tenant"


def seed() -> dict[str, int]:
    if os.getenv("APP_ENV", "development").lower() != "development":
        raise RuntimeError("Demo seeding is permitted only when APP_ENV=development.")
    if os.getenv("SEED_DEMO_DATA", "true").strip().lower() not in {"1", "true", "yes", "on"}:
        return {"owners": 0, "tenants": 0, "properties": 0, "units": 0, "stays": 0, "charges": 0, "skipped": 1}

    Base.metadata.create_all(bind=engine)
    counts = {"owners": 0, "tenants": 0, "properties": 0, "units": 0, "stays": 0, "charges": 0}
    db = SessionLocal()
    try:
        for uid, email, name, role in [
            (OWNER_ID, "demo-owner@stayeasy.test", "StayEasy Demo Owner", "owner"),
            (TENANT_ID, "demo-tenant@stayeasy.test", "StayEasy Demo Tenant", "customer"),
        ]:
            user = db.get(m.User, uid)
            if user is None:
                db.add(m.User(id=uid, email=email, display_name=name, role=role, is_active=True))
                counts["owners" if role == "owner" else "tenants"] += 1
        db.flush()

        property_specs = [
            {
                "name": "Aster House — Baner",
                "locality": "Baner",
                "address": "Baner Road, near Pancard Club Road",
                "rent": 10500,
                "deposit": 20000,
                "food": 2500,
                "eligibility": "Mixed",
                "description": "DEMO DATA — furnished shared accommodation in Baner. Availability is a test fixture and must be confirmed with an actual owner.",
                "pin": "411045",
                "units": [("A-201", 10500, "occupied"), ("A-202", 10500, "available"), ("A-203", 10500, "available")],
            },
            {
                "name": "Bluebell Residency — Wakad",
                "locality": "Wakad",
                "address": "Datta Mandir Road, Wakad",
                "rent": 8900,
                "deposit": 18000,
                "food": 2200,
                "eligibility": "Girls only",
                "description": "DEMO DATA — furnished accommodation in Wakad with Wi-Fi and housekeeping. Not a verified public listing.",
                "pin": "411057",
                "units": [("B-101", 8900, "available"), ("B-102", 8900, "available")],
            },
            {
                "name": "Foundry Living — Kothrud",
                "locality": "Kothrud",
                "address": "Paud Road, Kothrud",
                "rent": 12500,
                "deposit": 25000,
                "food": 3000,
                "eligibility": "Boys only",
                "description": "DEMO DATA — private and shared room test listing in Kothrud. Rent and occupancy are sample values.",
                "pin": "411038",
                "units": [("C-301", 12500, "available"), ("C-302", 12500, "available")],
            },
        ]
        properties = {}
        for spec in property_specs:
            prop = db.scalar(select(m.Property).where(
                m.Property.name == spec["name"],
                m.Property.locality == spec["locality"],
                m.Property.city == "Pune",
            ))
            if prop is None:
                prop = m.Property(
                    owner_id=OWNER_ID,
                    name=spec["name"],
                    description=spec["description"],
                    address=spec["address"],
                    locality=spec["locality"],
                    city="Pune",
                    pin_code=spec["pin"],
                    eligibility=spec["eligibility"],
                    couples_welcome=False,
                    furnishing="Furnished",
                    bathroom_policy="Shared",
                    food_policy="Available as extra",
                    rent_from=spec["rent"],
                    deposit_from=spec["deposit"],
                    food_monthly=spec["food"],
                    utilities_estimate=700,
                    status="published",
                    verification_status="unverified",
                    data_source="demo-fixture",
                )
                db.add(prop)
                db.flush()
                counts["properties"] += 1
            properties[spec["locality"]] = (prop, spec)
            for room_code, rent, unit_status in spec["units"]:
                unit = db.scalar(select(m.PropertyUnit).where(
                    m.PropertyUnit.property_id == prop.id,
                    m.PropertyUnit.unit_code == room_code,
                ))
                if unit is None:
                    db.add(m.PropertyUnit(
                        property_id=prop.id,
                        floor_label=room_code.split("-")[0],
                        room_code=room_code,
                        unit_code=room_code,
                        rentable_type="bed",
                        sharing_capacity=2,
                        monthly_rent=rent,
                        deposit=spec["deposit"],
                        status=unit_status,
                        last_availability_confirmed_at=None,
                    ))
                    counts["units"] += 1
            db.flush()

        # One server-backed resident stay and open rent charge exercise tenant finance.
        baner, baner_spec = properties["Baner"]
        occupied_unit = db.scalar(select(m.PropertyUnit).where(
            m.PropertyUnit.property_id == baner.id,
            m.PropertyUnit.unit_code == "A-201",
        ))
        stay = db.scalar(select(m.ResidentStay).where(
            m.ResidentStay.property_id == baner.id,
            m.ResidentStay.resident_id == TENANT_ID,
            m.ResidentStay.status == "active",
        ))
        if stay is None and occupied_unit is not None:
            stay = m.ResidentStay(
                property_id=baner.id,
                unit_id=occupied_unit.id,
                resident_id=TENANT_ID,
                joining_date=date.today() - timedelta(days=75),
                agreed_monthly_rent=baner_spec["rent"],
                deposit_held=baner_spec["deposit"],
                status="active",
                agreement_reference="DEMO-NOT-A-LEGAL-AGREEMENT",
            )
            db.add(stay)
            db.flush()
            counts["stays"] += 1

        if stay is not None:
            period = date.today().strftime("%Y-%m")
            charge = db.scalar(select(m.LedgerEntry).where(
                m.LedgerEntry.resident_stay_id == stay.id,
                m.LedgerEntry.category == "rent_charge",
                m.LedgerEntry.billing_period == period,
            ))
            if charge is None:
                db.add(m.LedgerEntry(
                    property_id=baner.id,
                    resident_stay_id=stay.id,
                    category="rent_charge",
                    amount=baner_spec["rent"],
                    amount_paise=baner_spec["rent"] * 100,
                    currency="INR",
                    billing_period=period,
                    due_date=date.today() - timedelta(days=2),
                    payment_method="online",
                    provider_status="not_paid",
                    description=f"DEMO TEST CHARGE — rent for {period}",
                    status="open",
                    created_by=OWNER_ID,
                ))
                counts["charges"] += 1

        db.commit()
        return counts
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    print("StayEasy development fixtures:", seed())

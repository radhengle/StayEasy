import os
from datetime import date, datetime, timedelta, timezone
from typing import Any
from uuid import uuid4

from fastapi import Depends, FastAPI, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import and_, func, or_, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from .database import Base, engine, get_db
from . import models as m
from .security import create_access_token, get_actor, require_roles, check_dev_password, APP_ENV

# Auto-create tables only for local/test development. Production schemas must be migrated explicitly.
if APP_ENV != "production":
    Base.metadata.create_all(bind=engine)
app = FastAPI(title="StayEasy API", version="0.2.0", description="Role-protected PG marketplace and property operations API")
from .auth import router as auth_router
from .owner_access import router as owner_access_router
app.include_router(auth_router)
app.include_router(owner_access_router)
origins = [x.strip() for x in os.getenv("CORS_ORIGINS", "http://localhost:3000,http://localhost:8080").split(",") if x.strip()]
app.add_middleware(CORSMiddleware, allow_origins=origins, allow_credentials=True, allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"], allow_headers=["Authorization", "Content-Type", "X-Request-ID"])

class DevTokenRequest(BaseModel):
    subject: str = Field(min_length=2, max_length=255)
    role: str = Field(pattern="^(customer|owner|manager|staff|admin)$")
    dev_password: str = Field(min_length=1)

class PropertyCreate(BaseModel):
    name: str = Field(min_length=2, max_length=180)
    description: str = ""
    address: str = ""
    locality: str = Field(min_length=1, max_length=120)
    city: str = Field(min_length=1, max_length=100)
    pin_code: str = ""
    latitude: float | None = None
    longitude: float | None = None
    nearby_places: list[dict[str, Any]] = Field(default_factory=list)
    eligibility: str = Field(default="Mixed", pattern="^(Boys only|Girls only|Mixed|All genders)$")
    couples_welcome: bool = False
    furnishing: str = "Furnished"
    bathroom_policy: str = "Shared"
    food_policy: str = "Not offered"
    visitor_policy: str = "Ask owner"
    curfew_policy: str = "Ask owner"
    cooking_allowed: bool = False
    minimum_stay_months: int = Field(default=1, ge=0, le=120)
    notice_period_days: int = Field(default=30, ge=0, le=365)
    rent_from: int = Field(default=0, ge=0)
    deposit_from: int = Field(default=0, ge=0)
    food_monthly: int = Field(default=0, ge=0)
    utilities_estimate: int = Field(default=0, ge=0)

class PropertyPatch(BaseModel):
    name: str | None = None
    description: str | None = None
    address: str | None = None
    locality: str | None = None
    city: str | None = None
    pin_code: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    nearby_places: list[dict[str, Any]] | None = None
    eligibility: str | None = None
    couples_welcome: bool | None = None
    furnishing: str | None = None
    bathroom_policy: str | None = None
    food_policy: str | None = None
    visitor_policy: str | None = None
    curfew_policy: str | None = None
    cooking_allowed: bool | None = None
    minimum_stay_months: int | None = Field(default=None, ge=0, le=120)
    notice_period_days: int | None = Field(default=None, ge=0, le=365)
    rent_from: int | None = Field(default=None, ge=0)
    deposit_from: int | None = Field(default=None, ge=0)
    food_monthly: int | None = Field(default=None, ge=0)
    utilities_estimate: int | None = Field(default=None, ge=0)

class UnitCreate(BaseModel):
    floor_label: str = "Ground"
    room_code: str = Field(min_length=1, max_length=70)
    unit_code: str = Field(min_length=1, max_length=90)
    rentable_type: str = Field(default="bed", pattern="^(bed|room)$")
    sharing_capacity: int = Field(default=1, ge=1, le=30)
    eligibility_override: str = ""
    couples_override: bool | None = None
    monthly_rent: int = Field(default=0, ge=0)
    deposit: int = Field(default=0, ge=0)
    expected_vacancy: date | None = None

class EnquiryCreate(BaseModel):
    property_id: int
    move_in_date: date | None = None
    preferred_room_type: str = ""
    message: str = Field(default="", max_length=4000)

class VisitCreate(BaseModel):
    property_id: int
    enquiry_id: int | None = None
    preferred_at: datetime

class VisitStatus(BaseModel):
    status: str = Field(pattern="^(confirmed|completed|cancelled|no_show)$")
    outcome: str = Field(default="", max_length=4000)

class ReservationCreate(BaseModel):
    property_id: int
    unit_id: int | None = None
    preferred_room_type: str = ""
    move_in_date: date
    terms_snapshot: dict[str, Any] = Field(default_factory=dict)

class ReservationStatus(BaseModel):
    status: str = Field(pattern="^(confirmed|cancelled|expired|checked_in)$")
    expires_at: datetime | None = None
    cancellation_reason: str = ""

class StayCreate(BaseModel):
    property_id: int
    unit_id: int
    resident_id: str = Field(min_length=1, max_length=36)
    reservation_id: int | None = None
    joining_date: date
    agreed_monthly_rent: int = Field(ge=0)
    deposit_held: int = Field(default=0, ge=0)
    agreement_reference: str = ""

class StayPatch(BaseModel):
    status: str = Field(pattern="^(active|notice_given|completed|cancelled)$")
    notice_date: date | None = None
    move_out_date: date | None = None

class MoveOutRequestCreate(BaseModel):
    requested_move_out_date: date
    reason: str = Field(default="", max_length=1200)


class MoveOutRequestReview(BaseModel):
    decision: str = Field(pattern="^(approve|reject)$")
    owner_note: str = Field(default="", max_length=1000)

class LedgerCreate(BaseModel):
    property_id: int
    resident_stay_id: int | None = None
    category: str = Field(pattern="^(rent_charge|payment|deposit|refund|deduction|expense|food|utility|late_fee)$")
    amount: int = Field(ge=0)
    billing_period: str = ""
    due_date: date | None = None
    paid_at: datetime | None = None
    payment_method: str = "manual"
    payment_reference: str = ""
    provider_status: str = Field(default="manual_record", pattern="^(manual_record|provider_pending|provider_confirmed|reconciled|failed|refunded)$")
    description: str = ""
    status: str = "open"

class IssueCreate(BaseModel):
    property_id: int
    unit_id: int | None = None
    category: str = "other"
    title: str = Field(min_length=2, max_length=180)
    description: str = Field(min_length=2, max_length=5000)
    priority: str = Field(default="normal", pattern="^(low|normal|high|urgent)$")

class IssuePatch(BaseModel):
    status: str = Field(pattern="^(open|in_progress|waiting_for_resident|resolved|reopened|escalated)$")
    assigned_staff_id: str | None = None
    resolution_note: str | None = None

class NoticeCreate(BaseModel):
    property_id: int
    title: str = Field(min_length=2, max_length=180)
    body: str = Field(min_length=2, max_length=10000)
    audience: str = "residents"
    status: str = Field(default="draft", pattern="^(draft|published|archived)$")

class SavedSearchCreate(BaseModel):
    name: str = Field(min_length=2, max_length=150)
    criteria: dict[str, Any] = Field(default_factory=dict)
    alerts_enabled: bool = True

class StaffCreate(BaseModel):
    property_id: int
    staff_user_id: str = Field(min_length=2, max_length=36)
    role: str = Field(default="caretaker", pattern="^(manager|caretaker|maintenance|finance|front_desk)$")
    permissions: list[str] = Field(default_factory=lambda: ["inventory.view", "issues.manage"])
    status: str = Field(default="invited", pattern="^(invited|active|suspended|revoked)$")

class ReviewCreate(BaseModel):
    resident_stay_id: int
    rating: int = Field(ge=1, le=5)
    body: str = Field(default="", max_length=5000)

class MealPlanCreate(BaseModel):
    property_id: int
    day_of_week: int = Field(ge=0, le=6)
    meal_type: str = Field(min_length=2, max_length=30)
    menu: str = Field(default="", max_length=3000)
    dietary_options: list[str] = Field(default_factory=list)
    charge_per_month: int = Field(default=0, ge=0)
    status: str = Field(default="available", pattern="^(available|unavailable|optional|included)$")

class HousekeepingCreate(BaseModel):
    property_id: int
    unit_id: int | None = None
    task_type: str = Field(min_length=2, max_length=70)
    description: str = Field(default="", max_length=3000)
    due_at: datetime | None = None
    assigned_staff_id: str | None = None
    status: str = Field(default="scheduled", pattern="^(scheduled|in_progress|completed|cancelled|overdue)$")

class VisitorCreate(BaseModel):
    property_id: int
    resident_stay_id: int | None = None
    visitor_name: str = Field(min_length=1, max_length=150)
    purpose: str = Field(default="", max_length=180)
    expected_at: datetime | None = None

class PropertyAmenityBatch(BaseModel):
    amenities: list["AmenityAssignment"] = Field(default_factory=list)

class AmenityAssignment(BaseModel):
    code: str = Field(min_length=2, max_length=70)
    label: str = Field(min_length=2, max_length=120)
    category: str = Field(default="general", max_length=80)
    scope: str = Field(default="property", pattern="^(property|room|unit)$")
    unit_id: int | None = None
    availability: str = Field(default="available", pattern="^(available|temporarily_unavailable|not_offered)$")
    pricing: str = Field(default="included", pattern="^(included|extra_charge|not_applicable)$")
    extra_charge: int = Field(default=0, ge=0)
    notes: str = Field(default="", max_length=2000)

class PhotoMetadataCreate(BaseModel):
    storage_key: str = Field(min_length=3, max_length=500)
    caption: str = Field(default="", max_length=200)
    sort_order: int = Field(default=0, ge=0, le=10000)

class PlatformReportCreate(BaseModel):
    target_type: str = Field(default="property", pattern="^(property|review|other)$")
    target_id: str = Field(default="", max_length=70)
    property_id: int | None = None
    category: str = Field(default="inaccurate_listing", max_length=70)
    details: str = Field(min_length=3, max_length=5000)

class PlatformReportDecision(BaseModel):
    status: str = Field(pattern="^(open|investigating|action_taken|dismissed|closed)$")
    moderator_note: str = Field(default="", max_length=3000)

class PrivacyRequestCreate(BaseModel):
    request_type: str = Field(pattern="^(export|correct|delete)$")
    details: str = Field(default="", max_length=5000)

class PrivacyRequestDecision(BaseModel):
    status: str = Field(pattern="^(submitted|under_review|approved|rejected|completed)$")
    admin_note: str = Field(default="", max_length=5000)

class DocumentMetadataCreate(BaseModel):
    category: str = Field(default="other", max_length=50)
    filename: str = Field(min_length=1, max_length=255)
    storage_key: str = Field(min_length=3, max_length=600)

class BillingRunRequest(BaseModel):
    property_id: int
    billing_period: str = Field(pattern=r"^\d{4}-(0[1-9]|1[0-2])$")
    due_date: date
    pro_rata_policy: str = Field(default="actual_days", pattern="^(actual_days|fixed_30)$")

class StatusPatch(BaseModel):
    status: str

PropertyAmenityBatch.model_rebuild()


def actor_user_id(actor: dict[str, Any]) -> str:
    return str(actor["subject"])

def audit(db: Session, actor: dict[str, Any], entity_type: str, entity_id: Any, action: str, old: dict | None = None, new: dict | None = None):
    db.add(m.AuditLog(actor_id=actor_user_id(actor), actor_role=actor["role"], entity_type=entity_type,
                      entity_id=str(entity_id), action=action, old_value=old or {}, new_value=new or {}))

def add_notification(db: Session, recipient_id: str, title: str, body: str, channel: str = "in_app") -> None:
    db.add(m.Notification(recipient_id=recipient_id, title=title[:180], body=body[:4000], channel=channel, status="queued"))

def property_or_404(db: Session, property_id: int) -> m.Property:
    item = db.get(m.Property, property_id)
    if item is None:
        raise HTTPException(404, "Property not found")
    return item

def can_manage_property(db: Session, actor: dict[str, Any], prop: m.Property, permission: str = "property.manage") -> bool:
    if actor["role"] == "admin":
        return True
    if actor["role"] == "owner" and prop.owner_id == actor_user_id(actor):
        return True
    assignment = db.scalar(select(m.StaffAssignment).where(
        m.StaffAssignment.staff_user_id == actor_user_id(actor),
        m.StaffAssignment.property_id == prop.id, m.StaffAssignment.status == "active"))
    permissions = set(assignment.permissions or []) if assignment else set()
    accepted = {permission, "*"}
    if permission == "maintenance.manage": accepted.add("issues.manage")
    if permission == "maintenance.view": accepted.add("issues.view")
    return bool(permissions.intersection(accepted))

def require_property_manager(db: Session, actor: dict[str, Any], property_id: int, permission: str = "property.manage") -> m.Property:
    prop = property_or_404(db, property_id)
    if not can_manage_property(db, actor, prop, permission):
        raise HTTPException(403, "You do not have access to this property for that operation")
    return prop

def visible_property_ids(db: Session, actor: dict[str, Any], resource: str) -> list[int] | None:
    """Return property scope for list endpoints; None means the actor is an admin."""
    if actor["role"] == "admin":
        return None
    subject = actor_user_id(actor)
    if actor["role"] == "owner":
        return list(db.scalars(select(m.Property.id).where(m.Property.owner_id == subject)))
    allowed = {f"{resource}.view", f"{resource}.manage", "*"}
    if resource == "issues": allowed.update({"maintenance.view", "maintenance.manage"})
    if resource == "maintenance": allowed.update({"issues.view", "issues.manage"})
    assignments = db.scalars(select(m.StaffAssignment).where(
        m.StaffAssignment.staff_user_id == subject,
        m.StaffAssignment.status == "active"
    ))
    return sorted({assignment.property_id for assignment in assignments if allowed.intersection(assignment.permissions or [])})

def expire_pending_reservations_for_unit(db: Session, unit_id: int, actor: dict[str, Any]) -> int:
    """Expire timed-out pending holds before checking the active-reservation unique index."""
    now = datetime.now(timezone.utc)
    expired = list(db.scalars(select(m.Reservation).where(
        m.Reservation.unit_id == unit_id,
        m.Reservation.status == "pending",
        m.Reservation.expires_at.is_not(None),
        m.Reservation.expires_at <= now
    ).with_for_update()))
    for reservation in expired:
        old = {"status": reservation.status}
        reservation.status = "expired"
        audit(db, actor, "reservation", reservation.id, "expired_due_to_timeout", old=old, new={"status": "expired"})
    if expired:
        db.commit()
    return len(expired)

def property_dict(p: m.Property, db: Session | None = None) -> dict:
    out = {"id": p.id, "owner_id": p.owner_id, "name": p.name, "description": p.description, "address": p.address,
           "locality": p.locality, "city": p.city, "pin_code": p.pin_code, "latitude": p.latitude, "longitude": p.longitude,
           "nearby_places": p.nearby_places or [], "eligibility": p.eligibility, "couples_welcome": p.couples_welcome,
           "furnishing": p.furnishing, "bathroom_policy": p.bathroom_policy, "food_policy": p.food_policy,
           "visitor_policy": p.visitor_policy, "curfew_policy": p.curfew_policy, "cooking_allowed": p.cooking_allowed,
           "minimum_stay_months": p.minimum_stay_months, "notice_period_days": p.notice_period_days,
           "rent_from": p.rent_from, "deposit_from": p.deposit_from, "food_monthly": p.food_monthly,
           "utilities_estimate": p.utilities_estimate, "status": p.status, "verification_status": p.verification_status,
           "availability_confirmed_at": p.availability_confirmed_at.isoformat() if p.availability_confirmed_at else None,
           "created_at": p.created_at.isoformat(), "updated_at": p.updated_at.isoformat()}
    if db is not None:
        units = list(db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.property_id == p.id)))
        out["inventory"] = {"total": len(units), "available": sum(u.status == "available" for u in units),
                             "reserved": sum(u.status == "reserved" for u in units), "occupied": sum(u.status == "occupied" for u in units),
                             "maintenance": sum(u.status == "maintenance" for u in units)}
        out["units"] = [{"id": u.id, "room_code": u.room_code, "unit_code": u.unit_code, "rentable_type": u.rentable_type,
                         "sharing_capacity": u.sharing_capacity, "monthly_rent": u.monthly_rent, "deposit": u.deposit, "status": u.status,
                         "expected_vacancy": u.expected_vacancy.isoformat() if u.expected_vacancy else None} for u in units]
        amenity_rows = list(db.scalars(select(m.PropertyAmenity).where(m.PropertyAmenity.property_id == p.id)))
        amenity_values = []
        for link in amenity_rows:
            amenity = db.get(m.Amenity, link.amenity_id)
            if amenity and link.availability == "available":
                amenity_values.append({"code": amenity.code, "label": amenity.label, "category": amenity.category,
                                       "scope": link.scope, "unit_id": link.unit_id, "pricing": link.pricing,
                                       "extra_charge": link.extra_charge, "notes": link.notes})
        out["amenities"] = amenity_values
        reviews = list(db.scalars(select(m.Review).where(m.Review.property_id == p.id, m.Review.moderation_status == "approved")))
        out["rating"] = round(sum(r.rating for r in reviews) / len(reviews), 2) if reviews else 0
        out["verified_review_count"] = len(reviews)
    return out

@app.get("/health")
def health():
    return {"status": "ok", "service": "stayeasy-api", "environment": APP_ENV}

@app.post("/auth/dev-token")
def dev_token(body: DevTokenRequest):
    if APP_ENV != "development" or not check_dev_password(body.dev_password):
        raise HTTPException(404, "Not found")
    token = create_access_token(body.subject, body.role, scopes=["prototype"])
    return {"access_token": token, "token_type": "bearer", "expires_in_minutes": int(os.getenv("ACCESS_TOKEN_MINUTES", "120")),
            "warning": "Development-only token endpoint. Disable it and use the organization's identity provider before deployment."}

@app.get("/properties")
def search_properties(city: str | None = None, locality: str | None = None, q: str | None = None,
                      min_rent: int | None = Query(default=None, ge=0), max_rent: int | None = Query(default=None, ge=0),
                      eligibility: str | None = None, couples_welcome: bool | None = None, available_only: bool = False,
                      verified_only: bool = False, db: Session = Depends(get_db)):
    stmt = select(m.Property).where(m.Property.status == "published")
    if city: stmt = stmt.where(m.Property.city.ilike(f"%{city.strip()}%"))
    if locality: stmt = stmt.where(m.Property.locality.ilike(f"%{locality.strip()}%"))
    if q: stmt = stmt.where((m.Property.name.ilike(f"%{q.strip()}%")) | (m.Property.description.ilike(f"%{q.strip()}%")) | (m.Property.pin_code == q.strip()))
    if min_rent is not None: stmt = stmt.where(m.Property.rent_from >= min_rent)
    if max_rent is not None: stmt = stmt.where(m.Property.rent_from <= max_rent)
    if eligibility: stmt = stmt.where(m.Property.eligibility == eligibility)
    if couples_welcome is not None: stmt = stmt.where(m.Property.couples_welcome == couples_welcome)
    if verified_only: stmt = stmt.where(m.Property.verification_status == "verified")
    props = list(db.scalars(stmt.order_by(m.Property.updated_at.desc()).limit(100)))
    response = [property_dict(p, db) for p in props]
    if available_only: response = [p for p in response if (p.get("inventory") or {}).get("available", 0) > 0]
    return {"items": response, "count": len(response), "distance_note": "Distance and commute time are omitted unless computed by a configured location service."}

@app.get("/properties/{property_id}")
def get_public_property(property_id: int, db: Session = Depends(get_db)):
    prop = property_or_404(db, property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    return property_dict(prop, db)

@app.get("/owner/properties")
def list_owner_properties(actor: dict = Depends(require_roles("owner", "admin")), db: Session = Depends(get_db)):
    """List the caller's own full property/inventory records, including drafts and pending review."""
    stmt = select(m.Property)
    if actor["role"] != "admin":
        stmt = stmt.where(m.Property.owner_id == actor_user_id(actor))
    rows = list(db.scalars(stmt.order_by(m.Property.id.desc())))
    return [property_dict(prop, db) for prop in rows]


@app.post("/owner/properties", status_code=201)
def create_property(body: PropertyCreate, actor: dict = Depends(require_roles("owner", "admin")), db: Session = Depends(get_db)):
    prop = m.Property(owner_id=actor_user_id(actor), **body.model_dump(), status="draft", verification_status="unverified")
    db.add(prop); db.flush(); audit(db, actor, "property", prop.id, "created", new={"name": prop.name, "status": prop.status}); db.commit(); db.refresh(prop)
    return property_dict(prop, db)

@app.patch("/owner/properties/{property_id}")
def update_property(property_id: int, body: PropertyPatch, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    prop = require_property_manager(db, actor, property_id)
    old = {"name": prop.name, "status": prop.status}
    for key, value in body.model_dump(exclude_unset=True).items(): setattr(prop, key, value)
    if prop.status == "published": prop.status = "pending_review"  # edited live listings must be re-reviewed
    audit(db, actor, "property", prop.id, "updated", old=old, new={"name": prop.name, "status": prop.status})
    db.commit(); db.refresh(prop); return property_dict(prop, db)

@app.post("/owner/properties/{property_id}/submit-review")
def submit_property_review(property_id: int, actor: dict = Depends(require_roles("owner", "manager", "admin")), db: Session = Depends(get_db)):
    prop = require_property_manager(db, actor, property_id)
    units = list(db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.property_id == prop.id)))
    if not prop.name or not prop.locality or not prop.city or prop.rent_from < 0 or not units:
        raise HTTPException(422, "Add required location/pricing details and at least one rentable unit before review")
    old = prop.status; prop.status = "pending_review"; prop.verification_status = "pending"
    audit(db, actor, "property", prop.id, "submitted_for_review", old={"status": old}, new={"status": prop.status})
    db.commit(); db.refresh(prop); return property_dict(prop, db)

@app.post("/owner/properties/{property_id}/units", status_code=201)
def create_unit(property_id: int, body: UnitCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, property_id, "inventory.manage")
    unit = m.PropertyUnit(property_id=property_id, **body.model_dump())
    db.add(unit)
    try:
        db.flush(); audit(db, actor, "unit", unit.id, "created", new={"unit_code": unit.unit_code, "status": unit.status}); db.commit(); db.refresh(unit)
    except IntegrityError as exc:
        db.rollback(); raise HTTPException(409, "This room / unit code already exists for the property") from exc
    return {"id": unit.id, "property_id": unit.property_id, "room_code": unit.room_code, "unit_code": unit.unit_code, "status": unit.status,
            "monthly_rent": unit.monthly_rent, "deposit": unit.deposit, "expected_vacancy": unit.expected_vacancy.isoformat() if unit.expected_vacancy else None}

@app.patch("/owner/units/{unit_id}/status")
def update_unit_status(unit_id: int, body: StatusPatch, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    unit = db.get(m.PropertyUnit, unit_id)
    if not unit: raise HTTPException(404, "Unit not found")
    require_property_manager(db, actor, unit.property_id, "inventory.manage")
    allowed = {"available", "reserved", "occupied", "maintenance", "inactive"}
    if body.status not in allowed: raise HTTPException(422, "Invalid unit status")
    if body.status == "available" and db.scalar(select(m.ResidentStay.id).where(m.ResidentStay.unit_id == unit.id, m.ResidentStay.status.in_(["active", "notice_given"]))):
        raise HTTPException(409, "An active stay still uses this unit. Complete move-out and settlement first.")
    old = unit.status; unit.status = body.status
    audit(db, actor, "unit", unit.id, "status_changed", old={"status": old}, new={"status": body.status}); db.commit(); db.refresh(unit)
    return {"id": unit.id, "status": unit.status}

@app.get("/owner/units")
def list_units(property_id: int, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, property_id, "inventory.view")
    units = list(db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.property_id == property_id).order_by(m.PropertyUnit.floor_label, m.PropertyUnit.room_code, m.PropertyUnit.unit_code)))
    return [{"id": u.id, "floor_label": u.floor_label, "room_code": u.room_code, "unit_code": u.unit_code, "rentable_type": u.rentable_type,
             "sharing_capacity": u.sharing_capacity, "eligibility_override": u.eligibility_override, "couples_override": u.couples_override,
             "monthly_rent": u.monthly_rent, "deposit": u.deposit, "status": u.status,
             "expected_vacancy": u.expected_vacancy.isoformat() if u.expected_vacancy else None} for u in units]

@app.get("/owner/resident-candidates")
def list_owner_resident_candidates(property_id: int, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    """Return verified customer accounts already connected to this property's enquiries/reservations."""
    require_property_manager(db, actor, property_id, "residents.view")
    related_customer_ids = select(m.Reservation.customer_id).where(
        m.Reservation.property_id == property_id,
        m.Reservation.status.in_(["pending", "confirmed"])
    ).union(
        select(m.Enquiry.customer_id).where(m.Enquiry.property_id == property_id)
    )
    rows = db.execute(
        select(m.User, m.UserCredential)
        .join(m.UserCredential, m.UserCredential.user_id == m.User.id)
        .where(
            m.User.id.in_(related_customer_ids),
            m.User.role == "customer",
            m.User.is_active.is_(True),
            m.UserCredential.email_verified_at.is_not(None)
        )
        .order_by(m.User.display_name, m.User.email)
    ).all()
    return [{
        "id": user.id,
        "display_name": user.display_name,
        "email": user.email,
        "email_verified": True
    } for user, _credential in rows]


@app.get("/owner/stays")
def list_owner_stays(property_id: int | None = None, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    """Return stay rows only for properties where the actor can view resident records."""
    stmt = (
        select(m.ResidentStay, m.User, m.Property, m.PropertyUnit)
        .join(m.User, m.User.id == m.ResidentStay.resident_id)
        .join(m.Property, m.Property.id == m.ResidentStay.property_id)
        .join(m.PropertyUnit, m.PropertyUnit.id == m.ResidentStay.unit_id)
    )
    visible_ids = visible_property_ids(db, actor, "residents")
    if visible_ids is not None:
        stmt = stmt.where(m.ResidentStay.property_id.in_(visible_ids))
    if property_id is not None:
        require_property_manager(db, actor, property_id, "residents.view")
        stmt = stmt.where(m.ResidentStay.property_id == property_id)
    rows = db.execute(stmt.order_by(m.ResidentStay.created_at.desc()).limit(1000)).all()
    return [{
        "id": stay.id,
        "property_id": stay.property_id,
        "property_name": prop.name,
        "unit_id": stay.unit_id,
        "room_code": unit.room_code,
        "unit_code": unit.unit_code,
        "resident_id": stay.resident_id,
        "resident_name": resident.display_name,
        "resident_email": resident.email,
        "reservation_id": stay.reservation_id,
        "joining_date": stay.joining_date.isoformat(),
        "notice_date": stay.notice_date.isoformat() if stay.notice_date else None,
        "move_out_date": stay.move_out_date.isoformat() if stay.move_out_date else None,
        "agreed_monthly_rent": stay.agreed_monthly_rent,
        "deposit_held": stay.deposit_held,
        "status": stay.status,
        "agreement_reference": stay.agreement_reference,
        "created_at": stay.created_at.isoformat() if stay.created_at else None,
    } for stay, resident, prop, unit in rows]

@app.get("/stays/me")
def list_my_stays(actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    """Return only the authenticated customer's stays, joined with labels suitable for their app."""
    stmt = (
        select(m.ResidentStay, m.Property, m.PropertyUnit)
        .join(m.Property, m.Property.id == m.ResidentStay.property_id)
        .join(m.PropertyUnit, m.PropertyUnit.id == m.ResidentStay.unit_id)
        .where(m.ResidentStay.resident_id == actor_user_id(actor))
        .order_by(m.ResidentStay.joining_date.desc())
    )
    rows = db.execute(stmt).all()
    return [{
        "id": stay.id,
        "property_id": stay.property_id,
        "property_name": prop.name,
        "property_address": prop.address,
        "locality": prop.locality,
        "city": prop.city,
        "notice_period_days": prop.notice_period_days,
        "unit_id": stay.unit_id,
        "room_code": unit.room_code,
        "unit_code": unit.unit_code,
        "joining_date": stay.joining_date.isoformat(),
        "notice_date": stay.notice_date.isoformat() if stay.notice_date else None,
        "move_out_date": stay.move_out_date.isoformat() if stay.move_out_date else None,
        "agreed_monthly_rent": stay.agreed_monthly_rent,
        "deposit_held": stay.deposit_held,
        "status": stay.status,
        "agreement_reference": stay.agreement_reference,
    } for stay, prop, unit in rows]

def move_out_request_payload(
    request: m.ResidentMoveOutRequest,
    property_name: str = "",
    resident_name: str = "",
    resident_email: str = "",
    room_code: str = "",
    unit_code: str = "",
    joining_date: date | None = None,
) -> dict[str, Any]:
    return {
        "id": request.id,
        "resident_stay_id": request.resident_stay_id,
        "property_id": request.property_id,
        "property_name": property_name,
        "resident_id": request.resident_id,
        "resident_name": resident_name,
        "resident_email": resident_email,
        "room_code": room_code,
        "unit_code": unit_code,
        "joining_date": joining_date.isoformat() if joining_date else None,
        "requested_move_out_date": request.requested_move_out_date.isoformat(),
        "reason": request.reason,
        "status": request.status,
        "owner_note": request.owner_note,
        "created_at": request.created_at.isoformat() if request.created_at else None,
        "reviewed_at": request.reviewed_at.isoformat() if request.reviewed_at else None,
    }


@app.post("/stays/me/{stay_id}/move-out-requests", status_code=201)
def create_my_move_out_request(
    stay_id: int,
    body: MoveOutRequestCreate,
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    """Create a request for the authenticated resident; never releases the unit."""
    if not actor.get("account_session"):
        raise HTTPException(403, "Sign in with your verified tenant account before requesting move-out")

    stay = db.scalar(
        select(m.ResidentStay)
        .where(m.ResidentStay.id == stay_id, m.ResidentStay.resident_id == actor_user_id(actor))
        .with_for_update()
    )
    if stay is None:
        raise HTTPException(404, "Active stay not found")
    if stay.status != "active":
        raise HTTPException(409, "A move-out request can only be submitted for an active stay")

    existing = db.scalar(select(m.ResidentMoveOutRequest).where(
        m.ResidentMoveOutRequest.resident_stay_id == stay.id,
        m.ResidentMoveOutRequest.status == "pending",
    ))
    if existing:
        raise HTTPException(409, "A move-out request is already awaiting the owner's response")

    prop = property_or_404(db, stay.property_id)
    earliest_date = max(date.today(), stay.joining_date) + timedelta(days=max(0, prop.notice_period_days))
    if body.requested_move_out_date < earliest_date:
        raise HTTPException(
            422,
            f"The requested date must honor this property's {prop.notice_period_days}-day notice period. "
            f"Earliest allowed date: {earliest_date.isoformat()}",
        )

    request = m.ResidentMoveOutRequest(
        resident_stay_id=stay.id,
        property_id=stay.property_id,
        resident_id=stay.resident_id,
        requested_move_out_date=body.requested_move_out_date,
        reason=body.reason.strip(),
        status="pending",
    )
    try:
        db.add(request)
        db.flush()
        resident = db.get(m.User, stay.resident_id)
        resident_label = resident.display_name if resident else "A resident"
        add_notification(
            db,
            prop.owner_id,
            "Move-out request received",
            f"{resident_label} requested move-out from {prop.name} on {body.requested_move_out_date.isoformat()}.",
        )
        audit(
            db,
            actor,
            "resident_move_out_request",
            request.id,
            "submitted",
            new={
                "resident_stay_id": stay.id,
                "property_id": stay.property_id,
                "requested_move_out_date": body.requested_move_out_date.isoformat(),
            },
        )
        db.commit()
        db.refresh(request)
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(409, "A move-out request is already awaiting the owner's response") from exc
    resident = db.get(m.User, stay.resident_id)
    unit = db.get(m.PropertyUnit, stay.unit_id)
    return move_out_request_payload(
        request,
        property_name=prop.name,
        resident_name=resident.display_name if resident else "",
        resident_email=resident.email if resident else "",
        room_code=unit.room_code if unit else "",
        unit_code=unit.unit_code if unit else "",
        joining_date=stay.joining_date,
    )


@app.get("/stays/me/move-out-requests")
def list_my_move_out_requests(
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    if not actor.get("account_session"):
        raise HTTPException(403, "Sign in with your verified tenant account to view move-out requests")
    rows = db.execute(
        select(m.ResidentMoveOutRequest, m.Property, m.ResidentStay, m.User, m.PropertyUnit)
        .join(m.Property, m.Property.id == m.ResidentMoveOutRequest.property_id)
        .join(m.ResidentStay, m.ResidentStay.id == m.ResidentMoveOutRequest.resident_stay_id)
        .join(m.User, m.User.id == m.ResidentMoveOutRequest.resident_id)
        .join(m.PropertyUnit, m.PropertyUnit.id == m.ResidentStay.unit_id)
        .where(m.ResidentMoveOutRequest.resident_id == actor_user_id(actor))
        .order_by(m.ResidentMoveOutRequest.created_at.desc())
        .limit(250)
    ).all()
    return [
        move_out_request_payload(
            request,
            property_name=prop.name,
            resident_name=resident.display_name,
            resident_email=resident.email,
            room_code=unit.room_code,
            unit_code=unit.unit_code,
            joining_date=stay.joining_date,
        )
        for request, prop, stay, resident, unit in rows
    ]


@app.get("/owner/move-out-requests")
def list_owner_move_out_requests(
    property_id: int | None = None,
    request_status: str = Query(default="pending", pattern="^(pending|approved|rejected|cancelled|all)$"),
    actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")),
    db: Session = Depends(get_db),
):
    visible_ids = visible_property_ids(db, actor, "residents")
    stmt = (
        select(m.ResidentMoveOutRequest, m.Property, m.ResidentStay, m.User, m.PropertyUnit)
        .join(m.Property, m.Property.id == m.ResidentMoveOutRequest.property_id)
        .join(m.ResidentStay, m.ResidentStay.id == m.ResidentMoveOutRequest.resident_stay_id)
        .join(m.User, m.User.id == m.ResidentMoveOutRequest.resident_id)
        .join(m.PropertyUnit, m.PropertyUnit.id == m.ResidentStay.unit_id)
    )
    if visible_ids is not None:
        stmt = stmt.where(m.ResidentMoveOutRequest.property_id.in_(visible_ids))
    if property_id is not None:
        require_property_manager(db, actor, property_id, "residents.view")
        stmt = stmt.where(m.ResidentMoveOutRequest.property_id == property_id)
    if request_status != "all":
        stmt = stmt.where(m.ResidentMoveOutRequest.status == request_status)
    rows = db.execute(stmt.order_by(m.ResidentMoveOutRequest.created_at.desc()).limit(1000)).all()
    return [
        move_out_request_payload(
            request,
            property_name=prop.name,
            resident_name=resident.display_name,
            resident_email=resident.email,
            room_code=unit.room_code,
            unit_code=unit.unit_code,
            joining_date=stay.joining_date,
        )
        for request, prop, stay, resident, unit in rows
    ]


@app.patch("/owner/move-out-requests/{request_id}")
def review_owner_move_out_request(
    request_id: int,
    body: MoveOutRequestReview,
    actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")),
    db: Session = Depends(get_db),
):
    request = db.scalar(
        select(m.ResidentMoveOutRequest)
        .where(m.ResidentMoveOutRequest.id == request_id)
        .with_for_update()
    )
    if request is None:
        raise HTTPException(404, "Move-out request not found")
    require_property_manager(db, actor, request.property_id, "residents.manage")
    if request.status != "pending":
        raise HTTPException(409, "This move-out request has already been reviewed")

    stay = db.scalar(
        select(m.ResidentStay)
        .where(
            m.ResidentStay.id == request.resident_stay_id,
            m.ResidentStay.property_id == request.property_id,
            m.ResidentStay.resident_id == request.resident_id,
        )
        .with_for_update()
    )
    if stay is None or stay.status != "active":
        raise HTTPException(409, "The resident stay is no longer active")
    now = datetime.now(timezone.utc)
    old_request = {"status": request.status, "owner_note": request.owner_note}
    old_stay = {
        "status": stay.status,
        "notice_date": stay.notice_date.isoformat() if stay.notice_date else None,
        "move_out_date": stay.move_out_date.isoformat() if stay.move_out_date else None,
    }

    request.status = "approved" if body.decision == "approve" else "rejected"
    request.owner_note = body.owner_note.strip()
    request.reviewed_by = actor_user_id(actor)
    request.reviewed_at = now
    if body.decision == "approve":
        if request.requested_move_out_date < date.today():
            raise HTTPException(409, "The requested move-out date has passed; ask the resident to submit a new request")
        stay.status = "notice_given"
        stay.notice_date = date.today()
        stay.move_out_date = request.requested_move_out_date
        add_notification(
            db,
            request.resident_id,
            "Move-out request approved",
            f"Your move-out request for {request.requested_move_out_date.isoformat()} was approved. The stay remains active until move-out is completed.",
        )
        audit(
            db, actor, "resident_stay", stay.id, "move_out_request_approved",
            old=old_stay,
            new={
                "status": stay.status,
                "notice_date": stay.notice_date.isoformat(),
                "move_out_date": stay.move_out_date.isoformat(),
            },
        )
    else:
        add_notification(
            db,
            request.resident_id,
            "Move-out request needs follow-up",
            "Your owner reviewed the move-out request. Open My Stay to read their note and contact them to agree next steps.",
        )
    audit(
        db,
        actor,
        "resident_move_out_request",
        request.id,
        "reviewed",
        old=old_request,
        new={
            "status": request.status,
            "owner_note": request.owner_note,
            "reviewed_by": request.reviewed_by,
        },
    )
    db.commit()
    db.refresh(request)
    prop = db.get(m.Property, request.property_id)
    resident = db.get(m.User, request.resident_id)
    unit = db.get(m.PropertyUnit, stay.unit_id)
    return move_out_request_payload(
        request,
        property_name=prop.name if prop else "",
        resident_name=resident.display_name if resident else "",
        resident_email=resident.email if resident else "",
        room_code=unit.room_code if unit else "",
        unit_code=unit.unit_code if unit else "",
        joining_date=stay.joining_date,
    )


@app.get("/properties/{property_id}/amenities")
def list_property_amenities(property_id: int, db: Session = Depends(get_db)):
    prop = property_or_404(db, property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    rows = list(db.scalars(select(m.PropertyAmenity).where(m.PropertyAmenity.property_id == property_id)))
    out = []
    for row in rows:
        amenity = db.get(m.Amenity, row.amenity_id)
        if amenity:
            out.append({"code": amenity.code, "label": amenity.label, "category": amenity.category, "scope": row.scope, "unit_id": row.unit_id,
                        "availability": row.availability, "pricing": row.pricing, "extra_charge": row.extra_charge, "notes": row.notes})
    return out

@app.put("/owner/properties/{property_id}/amenities")
def replace_property_amenities(property_id: int, body: PropertyAmenityBatch, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, property_id, "amenities.manage")
    # Replace the selected catalogue scopes in one transaction. Existing references are kept only when explicitly resubmitted.
    db.query(m.PropertyAmenity).filter(m.PropertyAmenity.property_id == property_id).delete(synchronize_session=False)
    for item in body.amenities:
        if item.unit_id is not None:
            unit = db.get(m.PropertyUnit, item.unit_id)
            if not unit or unit.property_id != property_id: raise HTTPException(422, "Amenity unit must belong to this property")
        amenity = db.scalar(select(m.Amenity).where(m.Amenity.code == item.code))
        if not amenity:
            amenity = m.Amenity(code=item.code, label=item.label, category=item.category)
            db.add(amenity); db.flush()
        elif amenity.label != item.label or amenity.category != item.category:
            # Preserve catalogue identity; use authoritative catalogue metadata once a code exists.
            pass
        db.add(m.PropertyAmenity(property_id=property_id, amenity_id=amenity.id, scope=item.scope, unit_id=item.unit_id,
                                 availability=item.availability, pricing=item.pricing, extra_charge=item.extra_charge, notes=item.notes))
    audit(db, actor, "property_amenities", property_id, "replaced", new={"count": len(body.amenities)})
    try: db.commit()
    except IntegrityError as exc:
        db.rollback(); raise HTTPException(409, "Duplicate amenity scope. Keep one row per amenity, scope and unit.") from exc
    return {"property_id": property_id, "count": len(body.amenities)}

@app.post("/enquiries", status_code=201)
def create_enquiry(body: EnquiryCreate, actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    prop = property_or_404(db, body.property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    item = m.Enquiry(property_id=prop.id, customer_id=actor_user_id(actor), **body.model_dump(exclude={"property_id"}))
    db.add(item); db.flush(); add_notification(db, prop.owner_id, "New property enquiry", f"A prospective resident enquired about {prop.name}."); audit(db, actor, "enquiry", item.id, "created", new={"property_id": prop.id}); db.commit(); db.refresh(item)
    return {"id": item.id, "property_id": item.property_id, "status": item.status, "created_at": item.created_at.isoformat()}

@app.get("/enquiries")
def list_enquiries(property_id: int | None = None, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    stmt = select(m.Enquiry)
    if actor["role"] == "customer":
        stmt = stmt.where(m.Enquiry.customer_id == actor_user_id(actor))
    elif actor["role"] != "admin":
        scoped_ids = visible_property_ids(db, actor, "enquiries") or []
        stmt = stmt.where(m.Enquiry.property_id.in_(scoped_ids))
    if property_id: stmt = stmt.where(m.Enquiry.property_id == property_id)
    rows = list(db.scalars(stmt.order_by(m.Enquiry.created_at.desc()).limit(300)))
    property_ids = {e.property_id for e in rows}
    property_names = {p.id: p.name for p in db.scalars(select(m.Property).where(m.Property.id.in_(property_ids)))} if property_ids else {}
    return [{"id": e.id, "property_id": e.property_id, "property_name": property_names.get(e.property_id, f"Property #{e.property_id}"),
             "customer_id": e.customer_id, "message": e.message, "status": e.status,
             "move_in_date": e.move_in_date.isoformat() if e.move_in_date else None, "preferred_room_type": e.preferred_room_type,
             "owner_response": e.owner_response, "created_at": e.created_at.isoformat()} for e in rows]

@app.patch("/owner/enquiries/{enquiry_id}")
def update_enquiry(enquiry_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.Enquiry, enquiry_id)
    if not item: raise HTTPException(404, "Enquiry not found")
    require_property_manager(db, actor, item.property_id, "enquiries.manage")
    allowed_status = {"new", "contacted", "visit_scheduled", "converted", "closed"}
    status_value = body.get("status", item.status)
    if status_value not in allowed_status: raise HTTPException(422, "Invalid enquiry status")
    allowed_transitions = {
        "new": {"contacted", "visit_scheduled", "closed"},
        "contacted": {"visit_scheduled", "converted", "closed"},
        "visit_scheduled": {"contacted", "converted", "closed"},
        "converted": {"closed"},
        "closed": set(),
    }
    if status_value != item.status and status_value not in allowed_transitions.get(item.status, set()):
        raise HTTPException(409, f"Cannot move enquiry from {item.status} to {status_value}")
    old = {"status": item.status}; item.status = status_value; item.owner_response = str(body.get("owner_response", item.owner_response))[:4000]
    audit(db, actor, "enquiry", item.id, "updated", old=old, new={"status": item.status}); db.commit(); return {"id": item.id, "status": item.status, "owner_response": item.owner_response}

@app.post("/owner/enquiries/{enquiry_id}/status")
def update_enquiry_status_action(enquiry_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    """POST-compatible alias for clients whose HTTP stack does not support PATCH."""
    return update_enquiry(enquiry_id, body, actor, db)

@app.post("/visits", status_code=201)
def create_visit(body: VisitCreate, actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    prop = property_or_404(db, body.property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    preferred_at = body.preferred_at.replace(tzinfo=timezone.utc) if body.preferred_at.tzinfo is None else body.preferred_at.astimezone(timezone.utc)
    if preferred_at < datetime.now(timezone.utc) - timedelta(minutes=1): raise HTTPException(422, "Visit time must be in the future")
    if body.enquiry_id:
        enquiry = db.get(m.Enquiry, body.enquiry_id)
        if not enquiry or enquiry.customer_id != actor_user_id(actor) or enquiry.property_id != prop.id: raise HTTPException(404, "Matching enquiry not found")
    item = m.Visit(property_id=prop.id, customer_id=actor_user_id(actor), enquiry_id=body.enquiry_id, preferred_at=preferred_at)
    db.add(item); db.flush(); add_notification(db, prop.owner_id, "New visit request", f"A prospective resident requested a visit for {prop.name}."); audit(db, actor, "visit", item.id, "created", new={"property_id": prop.id}); db.commit(); db.refresh(item)
    return {"id": item.id, "status": item.status, "preferred_at": item.preferred_at.isoformat()}

@app.get("/visits")
def list_visits(actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    stmt = select(m.Visit)
    if actor["role"] == "customer":
        stmt = stmt.where(m.Visit.customer_id == actor_user_id(actor))
    elif actor["role"] != "admin":
        scoped_ids = visible_property_ids(db, actor, "visits") or []
        stmt = stmt.where(m.Visit.property_id.in_(scoped_ids))
    rows = list(db.scalars(stmt.order_by(m.Visit.preferred_at).limit(300)))
    property_ids = {v.property_id for v in rows}
    property_names = {p.id: p.name for p in db.scalars(select(m.Property).where(m.Property.id.in_(property_ids)))} if property_ids else {}
    return [{"id": v.id, "property_id": v.property_id, "property_name": property_names.get(v.property_id, f"Property #{v.property_id}"),
             "customer_id": v.customer_id, "preferred_at": v.preferred_at.isoformat(), "status": v.status, "outcome": v.outcome} for v in rows]

@app.post("/owner/visits/{visit_id}/status")
def update_visit_status_action(visit_id: int, body: VisitStatus, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.Visit, visit_id)
    if not item: raise HTTPException(404, "Visit not found")
    require_property_manager(db, actor, item.property_id, "visits.manage")
    allowed_transitions = {
        "requested": {"confirmed", "cancelled"},
        "confirmed": {"completed", "cancelled", "no_show"},
        "completed": set(),
        "cancelled": set(),
        "no_show": set(),
    }
    if body.status != item.status and body.status not in allowed_transitions.get(item.status, set()):
        raise HTTPException(409, f"Cannot move visit from {item.status} to {body.status}")
    old = {"status": item.status, "outcome": item.outcome}
    item.status = body.status
    item.outcome = body.outcome
    audit(db, actor, "visit", item.id, "status_changed", old=old, new={"status": item.status, "outcome": item.outcome})
    db.commit()
    db.refresh(item)
    return {"id": item.id, "status": item.status, "outcome": item.outcome, "preferred_at": item.preferred_at.isoformat()}

@app.post("/reservations", status_code=201)
def create_reservation(body: ReservationCreate, actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    prop = property_or_404(db, body.property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    unit = None
    if body.unit_id is not None:
        unit = db.scalar(select(m.PropertyUnit).where(m.PropertyUnit.id == body.unit_id).with_for_update())
        if not unit or unit.property_id != prop.id: raise HTTPException(404, "Unit not found for this property")
        if unit.status != "available": raise HTTPException(409, f"Unit is currently {unit.status}")
        expire_pending_reservations_for_unit(db, unit.id, actor)
        now = datetime.now(timezone.utc)
        active_pending = and_(m.Reservation.status == "pending", or_(m.Reservation.expires_at.is_(None), m.Reservation.expires_at > now))
        active_status = m.Reservation.status.in_(["confirmed", "checked_in"])
        conflict = db.scalar(select(m.Reservation.id).where(
            m.Reservation.unit_id == unit.id,
            or_(active_status, active_pending)
        ))
        if conflict: raise HTTPException(409, "Unit already has an active reservation")
    rent = unit.monthly_rent if unit and unit.monthly_rent else prop.rent_from
    deposit = unit.deposit if unit and unit.deposit else prop.deposit_from
    item = m.Reservation(property_id=prop.id, unit_id=body.unit_id, customer_id=actor_user_id(actor), preferred_room_type=body.preferred_room_type,
                         move_in_date=body.move_in_date, rent_snapshot=rent, deposit_snapshot=deposit, terms_snapshot=body.terms_snapshot,
                         expires_at=datetime.now(timezone.utc) + timedelta(hours=24))
    db.add(item)
    try:
        db.flush(); add_notification(db, prop.owner_id, "Reservation request", f"A customer requested a reservation for {prop.name}."); audit(db, actor, "reservation", item.id, "requested", new={"property_id": prop.id, "unit_id": body.unit_id, "expires_at": item.expires_at.isoformat()}); db.commit(); db.refresh(item)
    except IntegrityError as exc:
        db.rollback(); raise HTTPException(409, "Another request reserved this unit. Refresh availability and choose another unit.") from exc
    return {"id": item.id, "status": item.status, "expires_at": item.expires_at.isoformat() if item.expires_at else None,
            "rent_snapshot": item.rent_snapshot, "deposit_snapshot": item.deposit_snapshot}

@app.get("/reservations")
def list_reservations(property_id: int | None = None, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    stmt = select(m.Reservation)
    if actor["role"] == "customer":
        stmt = stmt.where(m.Reservation.customer_id == actor_user_id(actor))
    elif actor["role"] != "admin":
        scoped_ids = visible_property_ids(db, actor, "reservations") or []
        stmt = stmt.where(m.Reservation.property_id.in_(scoped_ids))
    if property_id: stmt = stmt.where(m.Reservation.property_id == property_id)
    rows = list(db.scalars(stmt.order_by(m.Reservation.created_at.desc()).limit(300)))
    property_ids = {r.property_id for r in rows}
    property_names = {p.id: p.name for p in db.scalars(select(m.Property).where(m.Property.id.in_(property_ids)))} if property_ids else {}
    return [{"id": r.id, "property_id": r.property_id, "property_name": property_names.get(r.property_id, f"Property #{r.property_id}"),
             "unit_id": r.unit_id, "customer_id": r.customer_id, "preferred_room_type": r.preferred_room_type,
             "move_in_date": r.move_in_date.isoformat(), "status": r.status,
             "expires_at": r.expires_at.isoformat() if r.expires_at else None, "rent_snapshot": r.rent_snapshot, "deposit_snapshot": r.deposit_snapshot} for r in rows]

@app.patch("/owner/reservations/{reservation_id}")
def update_reservation(reservation_id: int, body: ReservationStatus, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.Reservation, reservation_id)
    if not item: raise HTTPException(404, "Reservation not found")
    require_property_manager(db, actor, item.property_id, "reservations.manage")
    if item.status == "pending" and item.expires_at is not None:
        expiry = item.expires_at if item.expires_at.tzinfo is not None else item.expires_at.replace(tzinfo=timezone.utc)
        if expiry <= datetime.now(timezone.utc):
            old = {"status": item.status}
            item.status = "expired"
            audit(db, actor, "reservation", item.id, "expired_due_to_timeout", old=old, new={"status": "expired"})
            db.commit()
            raise HTTPException(409, "Reservation request has expired; refresh availability before confirming.")
    allowed_transitions = {
        "pending": {"confirmed", "cancelled", "expired"},
        "confirmed": {"checked_in", "cancelled", "expired"},
        "checked_in": set(),
        "cancelled": set(),
        "expired": set(),
    }
    if body.status != item.status and body.status not in allowed_transitions.get(item.status, set()):
        raise HTTPException(409, f"Cannot move reservation from {item.status} to {body.status}")
    old = {"status": item.status, "expires_at": item.expires_at.isoformat() if item.expires_at else None}
    item.status = body.status
    # A confirmed or closed request is no longer a 24-hour pending hold.
    item.expires_at = body.expires_at
    item.cancellation_reason = body.cancellation_reason
    if item.unit_id:
        unit = db.get(m.PropertyUnit, item.unit_id)
        if unit:
            if body.status == "confirmed": unit.status = "reserved"
            elif body.status == "checked_in": unit.status = "occupied"
            elif body.status in {"cancelled", "expired"} and unit.status == "reserved": unit.status = "available"
    audit(db, actor, "reservation", item.id, "status_changed", old=old, new={"status": item.status})
    db.commit(); db.refresh(item); return {"id": item.id, "status": item.status}

@app.post("/owner/reservations/{reservation_id}/status")
def update_reservation_status_action(reservation_id: int, body: ReservationStatus, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    """POST-compatible alias for Android clients that cannot issue HTTP PATCH."""
    return update_reservation(reservation_id, body, actor, db)

@app.post("/owner/stays", status_code=201)
def create_stay(body: StayCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "residents.manage")
    resident = db.get(m.User, body.resident_id)
    credential = db.get(m.UserCredential, body.resident_id)
    if not resident or resident.role != "customer" or not resident.is_active or not credential or credential.email_verified_at is None:
        raise HTTPException(422, "Choose an active customer account with a verified email before assigning a resident stay")
    unit = db.scalar(select(m.PropertyUnit).where(m.PropertyUnit.id == body.unit_id).with_for_update())
    if not unit or unit.property_id != body.property_id: raise HTTPException(404, "Unit not found")
    if unit.status not in {"available", "reserved"}: raise HTTPException(409, f"Unit cannot be assigned while {unit.status}")
    reservation = db.get(m.Reservation, body.reservation_id) if body.reservation_id else None
    if reservation and (reservation.property_id != body.property_id or reservation.unit_id not in {None, body.unit_id} or reservation.customer_id != body.resident_id or reservation.status not in {"confirmed", "pending"}):
        raise HTTPException(409, "Reservation is not eligible for this resident and unit assignment")
    active = db.scalar(select(m.ResidentStay.id).where(m.ResidentStay.unit_id == body.unit_id, m.ResidentStay.status.in_(["active", "notice_given"])))
    if active: raise HTTPException(409, "Unit already has an active resident stay")
    stay = m.ResidentStay(**body.model_dump(), status="active")
    db.add(stay); unit.status = "occupied"
    if reservation: reservation.status = "checked_in"; reservation.unit_id = unit.id
    db.flush()
    if body.deposit_held > 0:
        deposit_entry = m.LedgerEntry(
            property_id=stay.property_id, resident_stay_id=stay.id, category="deposit",
            amount=body.deposit_held, payment_method="manual", payment_reference="",
            provider_status="manual_record",
            description="Security deposit recorded at move-in; manually recorded, not provider verified.",
            status="held", created_by=actor_user_id(actor)
        )
        db.add(deposit_entry); db.flush()
        audit(db, actor, "ledger_entry", deposit_entry.id, "initial_deposit_recorded",
              new={"resident_stay_id": stay.id, "amount": body.deposit_held, "provider_status": "manual_record"})
    audit(db, actor, "resident_stay", stay.id, "created", new={"unit_id": unit.id, "resident_id": stay.resident_id})
    db.commit(); db.refresh(stay)
    return {"id": stay.id, "status": stay.status, "unit_id": stay.unit_id, "joining_date": stay.joining_date.isoformat()}

@app.patch("/owner/stays/{stay_id}")
def update_stay(stay_id: int, body: StayPatch, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    stay = db.get(m.ResidentStay, stay_id)
    if not stay: raise HTTPException(404, "Stay not found")
    require_property_manager(db, actor, stay.property_id, "residents.manage")

    old_status = stay.status
    if old_status in {"completed", "cancelled"} and body.status != old_status:
        raise HTTPException(409, f"A {old_status} stay is terminal and cannot be reactivated")

    next_notice_date = body.notice_date if body.notice_date is not None else stay.notice_date
    next_move_out_date = body.move_out_date if body.move_out_date is not None else stay.move_out_date
    if body.status == "notice_given" and next_notice_date is None:
        raise HTTPException(422, "Notice date is required when giving notice")
    if body.status == "completed" and body.move_out_date is None:
        raise HTTPException(422, "Move-out date is required to complete a stay")
    if next_move_out_date is not None and next_move_out_date < stay.joining_date:
        raise HTTPException(422, "Move-out date cannot be before the joining date")
    if next_notice_date is not None and next_move_out_date is not None and next_notice_date > next_move_out_date:
        raise HTTPException(422, "Notice date cannot be after the move-out date")

    releasing_unit = body.status in {"completed", "cancelled"} and old_status not in {"completed", "cancelled"}
    unit = None
    if releasing_unit:
        unit = db.scalar(select(m.PropertyUnit).where(m.PropertyUnit.id == stay.unit_id).with_for_update())
        other_active_stay = db.scalar(select(m.ResidentStay.id).where(
            m.ResidentStay.unit_id == stay.unit_id,
            m.ResidentStay.id != stay.id,
            m.ResidentStay.status.in_(["active", "notice_given"])
        ))
        if other_active_stay:
            raise HTTPException(409, "Unit still has another active resident stay and cannot be released")

    old = {"status": stay.status, "notice_date": stay.notice_date.isoformat() if stay.notice_date else None,
           "move_out_date": stay.move_out_date.isoformat() if stay.move_out_date else None}
    stay.status = body.status
    if body.notice_date is not None:
        stay.notice_date = body.notice_date
    if body.move_out_date is not None:
        stay.move_out_date = body.move_out_date
    if unit is not None:
        unit.status = "available"
    new = {"status": stay.status, "notice_date": stay.notice_date.isoformat() if stay.notice_date else None,
           "move_out_date": stay.move_out_date.isoformat() if stay.move_out_date else None}
    audit(db, actor, "resident_stay", stay.id, "status_changed", old=old, new=new)
    db.commit(); db.refresh(stay)
    return {"id": stay.id, "status": stay.status,
            "notice_date": stay.notice_date.isoformat() if stay.notice_date else None,
            "move_out_date": stay.move_out_date.isoformat() if stay.move_out_date else None}

@app.post("/owner/ledger", status_code=201)
def create_ledger_entry(body: LedgerCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "finance.manage")
    if body.resident_stay_id:
        stay = db.get(m.ResidentStay, body.resident_stay_id)
        if not stay or stay.property_id != body.property_id: raise HTTPException(422, "Stay does not belong to this property")
    if body.paid_at is not None and body.category != "payment":
        raise HTTPException(422, "A payment date can only be attached to a payment ledger entry")
    if body.provider_status != "manual_record":
        raise HTTPException(422, "Payment-provider confirmation is unavailable. Record this entry manually until a provider is integrated.")
    entry = m.LedgerEntry(property_id=body.property_id, created_by=actor_user_id(actor), **body.model_dump(exclude={"property_id"}))
    db.add(entry); db.flush(); audit(db, actor, "ledger_entry", entry.id, "created", new={"category": entry.category, "amount": entry.amount, "status": entry.status}); db.commit(); db.refresh(entry)
    return {"id": entry.id, "category": entry.category, "amount": entry.amount, "status": entry.status, "provider_status": entry.provider_status,
            "paid_at": entry.paid_at.isoformat() if entry.paid_at else None}

def ledger_entry_payload(entry: m.LedgerEntry, property_name: str, resident_id: str = "", unit_code: str = "") -> dict:
    return {
        "id": entry.id, "property_id": entry.property_id, "property_name": property_name,
        "resident_stay_id": entry.resident_stay_id, "resident_id": resident_id, "unit_code": unit_code,
        "category": entry.category, "amount": entry.amount, "currency": entry.currency,
        "billing_period": entry.billing_period, "due_date": entry.due_date.isoformat() if entry.due_date else None,
        "paid_at": entry.paid_at.isoformat() if entry.paid_at else None,
        "payment_method": entry.payment_method, "payment_reference": entry.payment_reference,
        "provider_status": entry.provider_status, "status": entry.status, "description": entry.description,
        "created_at": entry.created_at.isoformat()
    }

@app.get("/owner/ledger")
def list_ledger(property_id: int | None = None, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    if property_id is not None:
        require_property_manager(db, actor, property_id, "finance.view")
        stmt = select(m.LedgerEntry).where(m.LedgerEntry.property_id == property_id)
    elif actor["role"] == "admin":
        stmt = select(m.LedgerEntry)
    else:
        scoped_ids = visible_property_ids(db, actor, "finance") or []
        stmt = select(m.LedgerEntry).where(m.LedgerEntry.property_id.in_(scoped_ids))
    rows = list(db.scalars(stmt.order_by(m.LedgerEntry.created_at.desc()).limit(1000)))
    property_ids = {entry.property_id for entry in rows}
    property_names = {
        prop.id: prop.name
        for prop in db.scalars(select(m.Property).where(m.Property.id.in_(property_ids)))
    } if property_ids else {}
    stay_ids = {entry.resident_stay_id for entry in rows if entry.resident_stay_id is not None}
    stays = {stay.id: stay for stay in db.scalars(select(m.ResidentStay).where(m.ResidentStay.id.in_(stay_ids)))} if stay_ids else {}
    unit_ids = {stay.unit_id for stay in stays.values()}
    units = {unit.id: unit for unit in db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.id.in_(unit_ids)))} if unit_ids else {}
    return [
        ledger_entry_payload(
            entry, property_names.get(entry.property_id, f"Property #{entry.property_id}"),
            stays[entry.resident_stay_id].resident_id if entry.resident_stay_id in stays else "",
            units[stays[entry.resident_stay_id].unit_id].unit_code if entry.resident_stay_id in stays and stays[entry.resident_stay_id].unit_id in units else ""
        )
        for entry in rows
    ]

@app.get("/ledger/me")
def list_my_ledger(actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    stay_ids = list(db.scalars(select(m.ResidentStay.id).where(m.ResidentStay.resident_id == actor_user_id(actor))))
    if not stay_ids:
        return []
    rows = list(db.scalars(
        select(m.LedgerEntry)
        .where(m.LedgerEntry.resident_stay_id.in_(stay_ids))
        .order_by(m.LedgerEntry.created_at.desc())
        .limit(1000)
    ))
    property_ids = {entry.property_id for entry in rows}
    property_names = {
        prop.id: prop.name
        for prop in db.scalars(select(m.Property).where(m.Property.id.in_(property_ids)))
    } if property_ids else {}
    stays = {stay.id: stay for stay in db.scalars(select(m.ResidentStay).where(m.ResidentStay.id.in_(stay_ids)))} if stay_ids else {}
    unit_ids = {stay.unit_id for stay in stays.values()}
    units = {unit.id: unit for unit in db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.id.in_(unit_ids)))} if unit_ids else {}
    return [
        ledger_entry_payload(
            entry, property_names.get(entry.property_id, f"Property #{entry.property_id}"),
            stays[entry.resident_stay_id].resident_id if entry.resident_stay_id in stays else "",
            units[stays[entry.resident_stay_id].unit_id].unit_code if entry.resident_stay_id in stays and stays[entry.resident_stay_id].unit_id in units else ""
        )
        for entry in rows
    ]

@app.post("/issues", status_code=201)
def create_issue(body: IssueCreate, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    prop = property_or_404(db, body.property_id)
    if actor["role"] == "customer":
        active_stay = db.scalar(select(m.ResidentStay.id).where(m.ResidentStay.property_id == prop.id, m.ResidentStay.resident_id == actor_user_id(actor), m.ResidentStay.status.in_(["active", "notice_given"])))
        if not active_stay: raise HTTPException(403, "A resident stay is required to submit a property issue")
    elif actor["role"] != "admin" and not can_manage_property(db, actor, prop, "maintenance.manage"):
        raise HTTPException(403, "No maintenance access for this property")
    issue = m.MaintenanceIssue(property_id=prop.id, reported_by=actor_user_id(actor), **body.model_dump(exclude={"property_id"}))
    if issue.unit_id:
        unit = db.get(m.PropertyUnit, issue.unit_id)
        if not unit or unit.property_id != prop.id: raise HTTPException(422, "Unit does not belong to property")
        if issue.priority == "urgent": unit.status = "maintenance"
    db.add(issue); db.flush()
    add_notification(db, prop.owner_id, "Maintenance issue reported", f"{issue.title} · priority {issue.priority}.")
    audit(db, actor, "maintenance_issue", issue.id, "created", new={"status": issue.status, "priority": issue.priority}); db.commit(); db.refresh(issue)
    return {"id": issue.id, "status": issue.status, "priority": issue.priority}

@app.get("/issues")
def list_issues(property_id: int | None = None, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    stmt = select(m.MaintenanceIssue)
    if actor["role"] in {"owner", "manager", "staff"}:
        property_ids = visible_property_ids(db, actor, "issues") or []
        stmt = stmt.where(m.MaintenanceIssue.property_id.in_(property_ids))
    elif actor["role"] == "customer": stmt = stmt.where(m.MaintenanceIssue.reported_by == actor_user_id(actor))
    if property_id: stmt = stmt.where(m.MaintenanceIssue.property_id == property_id)
    return [{"id": i.id, "property_id": i.property_id, "unit_id": i.unit_id, "title": i.title, "description": i.description, "priority": i.priority,
             "status": i.status, "assigned_staff_id": i.assigned_staff_id, "resolution_note": i.resolution_note, "created_at": i.created_at.isoformat()} for i in db.scalars(stmt.order_by(m.MaintenanceIssue.created_at.desc()).limit(300))]

@app.patch("/owner/issues/{issue_id}")
def update_issue(issue_id: int, body: IssuePatch, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    issue = db.get(m.MaintenanceIssue, issue_id)
    if not issue: raise HTTPException(404, "Issue not found")
    require_property_manager(db, actor, issue.property_id, "maintenance.manage")
    old = {"status": issue.status}; issue.status = body.status
    if body.assigned_staff_id is not None: issue.assigned_staff_id = body.assigned_staff_id
    if body.resolution_note is not None: issue.resolution_note = body.resolution_note
    if body.status == "resolved" and issue.unit_id:
        open_issues = db.scalar(select(func.count(m.MaintenanceIssue.id)).where(m.MaintenanceIssue.unit_id == issue.unit_id, m.MaintenanceIssue.id != issue.id, m.MaintenanceIssue.status.in_(["open", "in_progress", "reopened", "escalated"]))) or 0
        if open_issues == 0:
            unit = db.get(m.PropertyUnit, issue.unit_id)
            if unit and unit.status == "maintenance": unit.status = "available"
    audit(db, actor, "maintenance_issue", issue.id, "status_changed", old=old, new={"status": issue.status}); db.commit(); return {"id": issue.id, "status": issue.status}

@app.post("/owner/notices", status_code=201)
def create_notice(body: NoticeCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "notices.manage")
    item = m.Notice(property_id=body.property_id, created_by=actor_user_id(actor), **body.model_dump(exclude={"property_id"}))
    if item.status == "published": item.published_at = datetime.now(timezone.utc)
    db.add(item); db.flush(); audit(db, actor, "notice", item.id, "created", new={"status": item.status}); db.commit(); db.refresh(item)
    return {"id": item.id, "status": item.status, "published_at": item.published_at.isoformat() if item.published_at else None}

@app.get("/owner/notices")
def list_notices(property_id: int | None = None, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    stmt = select(m.Notice)
    if actor["role"] == "customer":
        stmt = stmt.where(m.Notice.status == "published")
        owned_stays = select(m.ResidentStay.property_id).where(m.ResidentStay.resident_id == actor_user_id(actor), m.ResidentStay.status.in_(["active", "notice_given"]))
        stmt = stmt.where(m.Notice.property_id.in_(owned_stays))
    elif actor["role"] != "admin":
        stmt = stmt.where(m.Notice.property_id.in_(select(m.Property.id).where(m.Property.owner_id == actor_user_id(actor))))
    if property_id is not None: stmt = stmt.where(m.Notice.property_id == property_id)
    rows = list(db.scalars(stmt.order_by(m.Notice.created_at.desc()).limit(300)))
    return [{"id": n.id, "property_id": n.property_id, "title": n.title, "body": n.body, "audience": n.audience, "status": n.status,
             "published_at": n.published_at.isoformat() if n.published_at else None, "created_at": n.created_at.isoformat()} for n in rows]

@app.patch("/owner/notices/{notice_id}")
def patch_notice(notice_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.Notice, notice_id)
    if not item: raise HTTPException(404, "Notice not found")
    require_property_manager(db, actor, item.property_id, "notices.manage")
    if "title" in body: item.title = str(body["title"])[:180]
    if "body" in body: item.body = str(body["body"])[:10000]
    if "status" in body:
        new_status = body["status"]
        if new_status not in {"draft", "published", "archived"}: raise HTTPException(422, "Invalid notice status")
        item.status = new_status
        item.published_at = datetime.now(timezone.utc) if new_status == "published" else item.published_at
    audit(db, actor, "notice", item.id, "updated", new={"status": item.status})
    db.commit(); db.refresh(item)
    return {"id": item.id, "status": item.status, "published_at": item.published_at.isoformat() if item.published_at else None}

@app.get("/notifications")
def list_notifications(limit: int = Query(default=100, ge=1, le=500), actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    rows = list(db.scalars(select(m.Notification).where(m.Notification.recipient_id == actor_user_id(actor)).order_by(m.Notification.created_at.desc()).limit(limit)))
    return [{"id": n.id, "title": n.title, "body": n.body, "channel": n.channel, "status": n.status, "created_at": n.created_at.isoformat()} for n in rows]

@app.patch("/notifications/{notification_id}/read")
def mark_notification_read(notification_id: int, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    item = db.get(m.Notification, notification_id)
    if not item or item.recipient_id != actor_user_id(actor): raise HTTPException(404, "Notification not found")
    item.status = "read"; db.commit(); return {"id": item.id, "status": item.status}

@app.get("/owner/staff")
def list_staff(property_id: int | None = None, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    stmt = select(m.StaffAssignment)
    if actor["role"] != "admin": stmt = stmt.where(m.StaffAssignment.owner_id == actor_user_id(actor))
    if property_id is not None:
        require_property_manager(db, actor, property_id, "staff.manage")
        stmt = stmt.where(m.StaffAssignment.property_id == property_id)
    rows = list(db.scalars(stmt.order_by(m.StaffAssignment.created_at.desc()).limit(500)))
    return [{"id": x.id, "owner_id": x.owner_id, "staff_user_id": x.staff_user_id, "property_id": x.property_id, "role": x.role,
             "permissions": x.permissions or [], "status": x.status, "created_at": x.created_at.isoformat()} for x in rows]

@app.post("/owner/staff", status_code=201)
def create_staff(body: StaffCreate, actor: dict = Depends(require_roles("owner", "manager", "admin")), db: Session = Depends(get_db)):
    prop = require_property_manager(db, actor, body.property_id, "staff.manage")
    if body.staff_user_id == prop.owner_id: raise HTTPException(422, "The property owner cannot be assigned as their own staff account")
    assignment = m.StaffAssignment(owner_id=prop.owner_id, **body.model_dump())
    db.add(assignment)
    try:
        db.flush(); audit(db, actor, "staff_assignment", assignment.id, "created", new={"property_id": prop.id, "role": assignment.role}); db.commit(); db.refresh(assignment)
    except IntegrityError as exc:
        db.rollback(); raise HTTPException(409, "That user already has a staff assignment for this property") from exc
    return {"id": assignment.id, "property_id": assignment.property_id, "staff_user_id": assignment.staff_user_id, "role": assignment.role, "permissions": assignment.permissions, "status": assignment.status}

@app.patch("/owner/staff/{assignment_id}")
def patch_staff(assignment_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.StaffAssignment, assignment_id)
    if not item: raise HTTPException(404, "Staff assignment not found")
    require_property_manager(db, actor, item.property_id, "staff.manage")
    if "role" in body:
        if body["role"] not in {"manager", "caretaker", "maintenance", "finance", "front_desk"}: raise HTTPException(422, "Unsupported staff role")
        item.role = body["role"]
    if "permissions" in body:
        if not isinstance(body["permissions"], list) or any(not isinstance(x, str) for x in body["permissions"]): raise HTTPException(422, "Permissions must be a list of strings")
        item.permissions = list(dict.fromkeys(body["permissions"]))
    if "status" in body:
        if body["status"] not in {"invited", "active", "suspended", "revoked"}: raise HTTPException(422, "Unsupported staff status")
        item.status = body["status"]
    audit(db, actor, "staff_assignment", item.id, "updated", new={"role": item.role, "status": item.status, "permissions": item.permissions})
    db.commit(); db.refresh(item); return {"id": item.id, "role": item.role, "status": item.status, "permissions": item.permissions}

@app.get("/owner/meal-plans")
def list_meal_plans(property_id: int, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    prop = property_or_404(db, property_id)
    if actor["role"] not in {"admin", "customer"} and not can_manage_property(db, actor, prop, "food.manage"): raise HTTPException(403, "No access to meal plans")
    if actor["role"] == "customer":
        if not db.scalar(select(m.ResidentStay.id).where(m.ResidentStay.property_id == property_id, m.ResidentStay.resident_id == actor_user_id(actor), m.ResidentStay.status.in_(["active", "notice_given"]))):
            raise HTTPException(403, "Meal plans are only shared with current residents")
    rows = list(db.scalars(select(m.MealPlan).where(m.MealPlan.property_id == property_id).order_by(m.MealPlan.day_of_week, m.MealPlan.meal_type)))
    return [{"id": x.id, "property_id": x.property_id, "day_of_week": x.day_of_week, "meal_type": x.meal_type, "menu": x.menu, "dietary_options": x.dietary_options, "charge_per_month": x.charge_per_month, "status": x.status} for x in rows]

@app.post("/owner/meal-plans", status_code=201)
def create_meal_plan(body: MealPlanCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "food.manage")
    item = m.MealPlan(**body.model_dump()); db.add(item); db.flush(); audit(db, actor, "meal_plan", item.id, "created", new={"day": item.day_of_week, "type": item.meal_type}); db.commit(); db.refresh(item)
    return {"id": item.id, "status": item.status}

@app.patch("/owner/meal-plans/{plan_id}")
def patch_meal_plan(plan_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.MealPlan, plan_id)
    if not item: raise HTTPException(404, "Meal plan not found")
    require_property_manager(db, actor, item.property_id, "food.manage")
    allowed = {"day_of_week", "meal_type", "menu", "dietary_options", "charge_per_month", "status"}
    for k, v in body.items():
        if k in allowed: setattr(item, k, v)
    audit(db, actor, "meal_plan", item.id, "updated", new={"status": item.status}); db.commit(); return {"id": item.id, "status": item.status}

@app.post("/owner/housekeeping", status_code=201)
def create_housekeeping(body: HousekeepingCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "housekeeping.manage")
    if body.unit_id is not None:
        unit = db.get(m.PropertyUnit, body.unit_id)
        if not unit or unit.property_id != body.property_id: raise HTTPException(422, "Unit must belong to the selected property")
    item = m.HousekeepingTask(**body.model_dump()); db.add(item); db.flush(); audit(db, actor, "housekeeping_task", item.id, "created", new={"status": item.status}); db.commit(); db.refresh(item)
    return {"id": item.id, "status": item.status}

@app.get("/owner/housekeeping")
def list_housekeeping(property_id: int | None = None, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    stmt = select(m.HousekeepingTask)
    if actor["role"] != "admin": stmt = stmt.where(m.HousekeepingTask.property_id.in_(select(m.Property.id).where(m.Property.owner_id == actor_user_id(actor))))
    if property_id is not None:
        require_property_manager(db, actor, property_id, "housekeeping.manage")
        stmt = stmt.where(m.HousekeepingTask.property_id == property_id)
    rows = list(db.scalars(stmt.order_by(m.HousekeepingTask.due_at).limit(500)))
    return [{"id": x.id, "property_id": x.property_id, "unit_id": x.unit_id, "task_type": x.task_type, "description": x.description, "due_at": x.due_at.isoformat() if x.due_at else None, "assigned_staff_id": x.assigned_staff_id, "status": x.status} for x in rows]

@app.patch("/owner/housekeeping/{task_id}")
def patch_housekeeping(task_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.HousekeepingTask, task_id)
    if not item: raise HTTPException(404, "Housekeeping task not found")
    require_property_manager(db, actor, item.property_id, "housekeeping.manage")
    if "status" in body and body["status"] not in {"scheduled", "in_progress", "completed", "cancelled", "overdue"}: raise HTTPException(422, "Invalid task status")
    for k in ("status", "description", "assigned_staff_id", "task_type"):
        if k in body: setattr(item, k, str(body[k])[:3000] if k == "description" else body[k])
    audit(db, actor, "housekeeping_task", item.id, "updated", new={"status": item.status}); db.commit(); return {"id": item.id, "status": item.status}

@app.post("/owner/visitors", status_code=201)
def create_visitor(body: VisitorCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "visitors.manage")
    if body.resident_stay_id:
        stay = db.get(m.ResidentStay, body.resident_stay_id)
        if not stay or stay.property_id != body.property_id: raise HTTPException(422, "Stay must belong to the selected property")
    item = m.VisitorEntry(property_id=body.property_id, **body.model_dump(exclude={"property_id"}))
    db.add(item); db.flush(); audit(db, actor, "visitor_entry", item.id, "created", new={"property_id": item.property_id}); db.commit(); db.refresh(item)
    return {"id": item.id, "status": item.status}

@app.get("/owner/visitors")
def list_visitors(property_id: int | None = None, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    stmt = select(m.VisitorEntry)
    if actor["role"] != "admin": stmt = stmt.where(m.VisitorEntry.property_id.in_(select(m.Property.id).where(m.Property.owner_id == actor_user_id(actor))))
    if property_id is not None:
        require_property_manager(db, actor, property_id, "visitors.manage")
        stmt = stmt.where(m.VisitorEntry.property_id == property_id)
    rows = list(db.scalars(stmt.order_by(m.VisitorEntry.expected_at.desc()).limit(500)))
    return [{"id": x.id, "property_id": x.property_id, "resident_stay_id": x.resident_stay_id, "visitor_name": x.visitor_name, "purpose": x.purpose, "expected_at": x.expected_at.isoformat() if x.expected_at else None, "checked_in_at": x.checked_in_at.isoformat() if x.checked_in_at else None, "checked_out_at": x.checked_out_at.isoformat() if x.checked_out_at else None, "status": x.status} for x in rows]

@app.patch("/owner/visitors/{visitor_id}")
def patch_visitor(visitor_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.VisitorEntry, visitor_id)
    if not item: raise HTTPException(404, "Visitor entry not found")
    require_property_manager(db, actor, item.property_id, "visitors.manage")
    now = datetime.now(timezone.utc)
    action = body.get("action")
    if action == "check_in": item.checked_in_at = now; item.status = "checked_in"
    elif action == "check_out": item.checked_out_at = now; item.status = "checked_out"
    elif action == "cancel": item.status = "cancelled"
    else: raise HTTPException(422, "Action must be check_in, check_out or cancel")
    audit(db, actor, "visitor_entry", item.id, action); db.commit(); return {"id": item.id, "status": item.status}

@app.get("/properties/{property_id}/reviews")
def public_reviews(property_id: int, db: Session = Depends(get_db)):
    prop = property_or_404(db, property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    rows = list(db.scalars(select(m.Review).where(m.Review.property_id == property_id, m.Review.moderation_status == "approved").order_by(m.Review.created_at.desc()).limit(200)))
    return [{"id": x.id, "rating": x.rating, "body": x.body, "owner_response": x.owner_response, "created_at": x.created_at.isoformat()} for x in rows]

@app.post("/reviews", status_code=201)
def create_review(body: ReviewCreate, actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    stay = db.get(m.ResidentStay, body.resident_stay_id)
    if not stay or stay.resident_id != actor_user_id(actor) or stay.status != "completed": raise HTTPException(403, "Only a resident with a completed stay may review")
    existing = db.scalar(select(m.Review.id).where(m.Review.resident_stay_id == stay.id))
    if existing: raise HTTPException(409, "A review already exists for this stay")
    review = m.Review(property_id=stay.property_id, resident_stay_id=stay.id, reviewer_id=actor_user_id(actor), rating=body.rating, body=body.body)
    db.add(review); db.flush(); audit(db, actor, "review", review.id, "created", new={"property_id": stay.property_id}); db.commit(); return {"id": review.id, "moderation_status": review.moderation_status}

@app.patch("/owner/reviews/{review_id}")
def respond_to_review(review_id: int, body: dict, actor: dict = Depends(require_roles("owner", "manager", "admin")), db: Session = Depends(get_db)):
    item = db.get(m.Review, review_id)
    if not item: raise HTTPException(404, "Review not found")
    require_property_manager(db, actor, item.property_id, "reviews.manage")
    item.owner_response = str(body.get("owner_response", ""))[:5000]
    audit(db, actor, "review", item.id, "owner_response_added"); db.commit(); return {"id": item.id, "owner_response": item.owner_response}

@app.patch("/admin/reviews/{review_id}/moderation")
def moderate_review(review_id: int, body: dict, actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    item = db.get(m.Review, review_id)
    if not item: raise HTTPException(404, "Review not found")
    decision = body.get("decision")
    if decision not in {"approved", "rejected", "pending"}: raise HTTPException(422, "Invalid moderation decision")
    old = {"moderation_status": item.moderation_status}; item.moderation_status = decision
    audit(db, actor, "review", item.id, "moderated", old=old, new={"moderation_status": decision, "note": str(body.get("note", ""))[:2000]})
    db.commit(); return {"id": item.id, "moderation_status": item.moderation_status}

@app.get("/amenities/catalog")
def amenity_catalog():
    return {"categories": [
        {"name": "Room", "items": ["Bed and mattress", "Wardrobe", "Study desk", "AC", "Fan", "Attached bathroom", "Balcony", "Refrigerator"]},
        {"name": "Food & kitchen", "items": ["Breakfast", "Lunch", "Dinner", "Kitchen access", "Drinking water", "Microwave", "Vegetarian meals", "Non-vegetarian meals"]},
        {"name": "Utilities", "items": ["Wi-Fi", "Hot water", "Power backup", "Water storage", "Electricity included"]},
        {"name": "Cleaning & convenience", "items": ["Housekeeping", "Laundry", "Linen changes", "Waste collection", "Lift", "Accessibility"]},
        {"name": "Safety & building", "items": ["CCTV in common areas", "Controlled entry", "Security guard", "Fire equipment", "First aid", "Parking", "Common lounge"]}
    ], "note": "Catalogue is a starter list; property-specific configuration is a follow-on UI/API integration."}

@app.get("/properties/{property_id}/photos")
def public_property_photos(property_id: int, db: Session = Depends(get_db)):
    prop = property_or_404(db, property_id)
    if prop.status != "published": raise HTTPException(404, "Property not found")
    rows = list(db.scalars(select(m.ListingPhoto).where(m.ListingPhoto.property_id == property_id).order_by(m.ListingPhoto.sort_order, m.ListingPhoto.id)))
    return [{"id": x.id, "storage_key": x.storage_key, "caption": x.caption, "sort_order": x.sort_order} for x in rows]

@app.post("/owner/properties/{property_id}/photos", status_code=201)
def add_property_photo(property_id: int, body: PhotoMetadataCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, property_id, "listing.media.manage")
    # storage_key must come from a server-generated upload/session; this API records metadata only.
    if body.storage_key.startswith(("http://", "https://", "file://", "content://")):
        raise HTTPException(422, "Use a server-issued object-storage key, not a public URL or device path")
    photo = m.ListingPhoto(property_id=property_id, **body.model_dump())
    db.add(photo); db.flush(); audit(db, actor, "listing_photo", photo.id, "metadata_added"); db.commit(); db.refresh(photo)
    return {"id": photo.id, "storage_key": photo.storage_key, "caption": photo.caption, "sort_order": photo.sort_order}

@app.delete("/owner/properties/{property_id}/photos/{photo_id}")
def delete_property_photo(property_id: int, photo_id: int, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, property_id, "listing.media.manage")
    photo = db.get(m.ListingPhoto, photo_id)
    if not photo or photo.property_id != property_id: raise HTTPException(404, "Photo metadata not found")
    audit(db, actor, "listing_photo", photo.id, "metadata_removed"); db.delete(photo); db.commit(); return {"deleted": True}

@app.post("/reports", status_code=201)
def create_platform_report(body: PlatformReportCreate, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    if body.property_id is not None:
        property_or_404(db, body.property_id)
    elif body.target_type == "property":
        try: property_or_404(db, int(body.target_id))
        except (ValueError, TypeError): raise HTTPException(422, "Provide a valid property ID for a listing report")
    report = m.PlatformReport(reporter_id=actor_user_id(actor), **body.model_dump())
    db.add(report); db.flush(); audit(db, actor, "platform_report", report.id, "submitted", new={"target_type": report.target_type, "target_id": report.target_id}); db.commit(); db.refresh(report)
    return {"id": report.id, "status": report.status, "created_at": report.created_at.isoformat()}

@app.get("/admin/moderation-reports")
def list_platform_reports(status_filter: str | None = Query(default=None, alias="status"), actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    stmt = select(m.PlatformReport)
    if status_filter: stmt = stmt.where(m.PlatformReport.status == status_filter)
    rows = list(db.scalars(stmt.order_by(m.PlatformReport.created_at.desc()).limit(1000)))
    return [{"id": x.id, "reporter_id": x.reporter_id, "target_type": x.target_type, "target_id": x.target_id, "property_id": x.property_id,
             "category": x.category, "details": x.details, "status": x.status, "moderator_note": x.moderator_note, "created_at": x.created_at.isoformat()} for x in rows]

@app.patch("/admin/moderation-reports/{report_id}")
def decide_platform_report(report_id: int, body: PlatformReportDecision, actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    item = db.get(m.PlatformReport, report_id)
    if not item: raise HTTPException(404, "Report not found")
    old = {"status": item.status}; item.status = body.status; item.moderator_note = body.moderator_note
    audit(db, actor, "platform_report", item.id, "moderation_decision", old=old, new={"status": item.status})
    db.commit(); return {"id": item.id, "status": item.status}

@app.post("/privacy/requests", status_code=201)
def create_privacy_request(body: PrivacyRequestCreate, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    item = m.PrivacyRequest(subject_id=actor_user_id(actor), request_type=body.request_type, details=body.details)
    db.add(item); db.flush(); audit(db, actor, "privacy_request", item.id, "submitted", new={"request_type": item.request_type}); db.commit(); db.refresh(item)
    return {"id": item.id, "request_type": item.request_type, "status": item.status, "requested_at": item.requested_at.isoformat(),
            "note": "This records a request; export/deletion execution and legally required retention handling require an operational workflow."}

@app.get("/privacy/requests/me")
def list_my_privacy_requests(actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    rows = list(db.scalars(select(m.PrivacyRequest).where(m.PrivacyRequest.subject_id == actor_user_id(actor)).order_by(m.PrivacyRequest.requested_at.desc())))
    return [{"id": x.id, "request_type": x.request_type, "details": x.details, "status": x.status, "admin_note": x.admin_note, "requested_at": x.requested_at.isoformat(), "completed_at": x.completed_at.isoformat() if x.completed_at else None} for x in rows]

@app.get("/admin/privacy-requests")
def list_privacy_requests(status_filter: str | None = Query(default=None, alias="status"), actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    stmt = select(m.PrivacyRequest)
    if status_filter: stmt = stmt.where(m.PrivacyRequest.status == status_filter)
    rows = list(db.scalars(stmt.order_by(m.PrivacyRequest.requested_at).limit(1000)))
    return [{"id": x.id, "subject_id": x.subject_id, "request_type": x.request_type, "details": x.details, "status": x.status, "admin_note": x.admin_note, "requested_at": x.requested_at.isoformat()} for x in rows]

@app.patch("/admin/privacy-requests/{request_id}")
def decide_privacy_request(request_id: int, body: PrivacyRequestDecision, actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    item = db.get(m.PrivacyRequest, request_id)
    if not item: raise HTTPException(404, "Privacy request not found")
    old = {"status": item.status}; item.status = body.status; item.admin_note = body.admin_note
    if body.status == "completed": item.completed_at = datetime.now(timezone.utc)
    audit(db, actor, "privacy_request", item.id, "status_changed", old=old, new={"status": item.status})
    db.commit(); return {"id": item.id, "status": item.status, "completed_at": item.completed_at.isoformat() if item.completed_at else None}

@app.post("/owner/stays/{stay_id}/documents", status_code=201)
def add_document_metadata(stay_id: int, body: DocumentMetadataCreate, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    stay = db.get(m.ResidentStay, stay_id)
    if not stay: raise HTTPException(404, "Resident stay not found")
    require_property_manager(db, actor, stay.property_id, "documents.manage")
    if body.storage_key.startswith(("http://", "https://", "file://", "content://")):
        raise HTTPException(422, "Use a server-issued secure-storage key, not a public URL or device path")
    doc = m.ResidentDocument(property_id=stay.property_id, resident_stay_id=stay.id, uploaded_by=actor_user_id(actor), **body.model_dump())
    db.add(doc); db.flush(); audit(db, actor, "resident_document", doc.id, "metadata_added", new={"stay_id": stay.id}); db.commit(); db.refresh(doc)
    return {"id": doc.id, "category": doc.category, "filename": doc.filename, "status": doc.status}

@app.get("/stays/{stay_id}/documents")
def list_stay_documents(stay_id: int, actor: dict = Depends(get_actor), db: Session = Depends(get_db)):
    stay = db.get(m.ResidentStay, stay_id)
    if not stay: raise HTTPException(404, "Resident stay not found")
    if actor["role"] == "customer":
        if stay.resident_id != actor_user_id(actor): raise HTTPException(403, "This stay is not accessible to your account")
    elif not can_manage_property(db, actor, property_or_404(db, stay.property_id), "documents.view") and actor["role"] != "admin":
        raise HTTPException(403, "No document access for this property")
    rows = list(db.scalars(select(m.ResidentDocument).where(m.ResidentDocument.resident_stay_id == stay_id).order_by(m.ResidentDocument.created_at.desc())))
    return [{"id": x.id, "category": x.category, "filename": x.filename, "status": x.status, "created_at": x.created_at.isoformat()} for x in rows]

@app.post("/owner/billing/generate")
def generate_rent_charges(body: BillingRunRequest, actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    require_property_manager(db, actor, body.property_id, "finance.manage")
    year, month = map(int, body.billing_period.split("-"))
    days_in_month = __import__("calendar").monthrange(year, month)[1]
    period_start = date(year, month, 1); period_end = date(year, month, days_in_month)
    stays = list(db.scalars(select(m.ResidentStay).where(m.ResidentStay.property_id == body.property_id, m.ResidentStay.joining_date <= period_end,
                                                           m.ResidentStay.status.in_(["active", "notice_given", "completed"]))))
    created = []; skipped = 0
    for stay in stays:
        occupied_start = max(stay.joining_date, period_start)
        occupied_end = min(stay.move_out_date or period_end, period_end)
        if occupied_end < occupied_start: continue
        duplicate = db.scalar(select(m.LedgerEntry.id).where(m.LedgerEntry.resident_stay_id == stay.id, m.LedgerEntry.category == "rent_charge", m.LedgerEntry.billing_period == body.billing_period))
        if duplicate: skipped += 1; continue
        days = (occupied_end - occupied_start).days + 1
        denominator = 30 if body.pro_rata_policy == "fixed_30" else days_in_month
        amount = round(stay.agreed_monthly_rent * min(days, denominator) / denominator)
        detail = f"Rent for {body.billing_period} ({days}/{denominator} days; policy={body.pro_rata_policy})"
        entry = m.LedgerEntry(property_id=body.property_id, resident_stay_id=stay.id, category="rent_charge", amount=amount,
                              billing_period=body.billing_period, due_date=body.due_date, payment_method="manual", payment_reference="",
                              provider_status="manual_record", description=detail, status="open", created_by=actor_user_id(actor))
        db.add(entry); db.flush(); created.append({"ledger_entry_id": entry.id, "resident_stay_id": stay.id, "amount": amount, "days": days})
    audit(db, actor, "billing_run", body.property_id, "rent_charges_generated", new={"period": body.billing_period, "created": len(created), "skipped": skipped, "policy": body.pro_rata_policy})
    db.commit()
    return {"property_id": body.property_id, "billing_period": body.billing_period, "pro_rata_policy": body.pro_rata_policy, "created_count": len(created), "skipped_existing": skipped, "entries": created,
            "note": "Review agreement terms and local rules before using automated charges. This endpoint does not collect payments or add late fees."}

@app.post("/saved-searches", status_code=201)
def create_saved_search(body: SavedSearchCreate, actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    item = m.SavedSearch(customer_id=actor_user_id(actor), **body.model_dump()); db.add(item); db.commit(); db.refresh(item)
    return {"id": item.id, "name": item.name, "criteria": item.criteria, "alerts_enabled": item.alerts_enabled}

@app.get("/saved-searches")
def list_saved_searches(actor: dict = Depends(require_roles("customer")), db: Session = Depends(get_db)):
    return [{"id": s.id, "name": s.name, "criteria": s.criteria, "alerts_enabled": s.alerts_enabled, "created_at": s.created_at.isoformat()}
            for s in db.scalars(select(m.SavedSearch).where(m.SavedSearch.customer_id == actor_user_id(actor)).order_by(m.SavedSearch.created_at.desc()))]

@app.get("/owner/dashboard")
def owner_dashboard(actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")), db: Session = Depends(get_db)):
    prop_ids = list(db.scalars(select(m.Property.id))) if actor["role"] == "admin" else (visible_property_ids(db, actor, "dashboard") or [])
    if not prop_ids: return {"property_count": 0, "units": {"total": 0, "available": 0, "reserved": 0, "occupied": 0, "maintenance": 0}, "enquiries_new": 0, "reservations_pending": 0, "issues_open": 0, "rent_due": 0}
    units = list(db.scalars(select(m.PropertyUnit).where(m.PropertyUnit.property_id.in_(prop_ids))))
    return {"property_count": len(prop_ids), "units": {"total": len(units), "available": sum(x.status == "available" for x in units), "reserved": sum(x.status == "reserved" for x in units), "occupied": sum(x.status == "occupied" for x in units), "maintenance": sum(x.status == "maintenance" for x in units)},
            "enquiries_new": db.scalar(select(func.count(m.Enquiry.id)).where(m.Enquiry.property_id.in_(prop_ids), m.Enquiry.status == "new")) or 0,
            "reservations_pending": db.scalar(select(func.count(m.Reservation.id)).where(m.Reservation.property_id.in_(prop_ids), m.Reservation.status == "pending")) or 0,
            "issues_open": db.scalar(select(func.count(m.MaintenanceIssue.id)).where(m.MaintenanceIssue.property_id.in_(prop_ids), m.MaintenanceIssue.status.in_(["open", "reopened", "escalated"]))) or 0,
            "rent_due": db.scalar(select(func.coalesce(func.sum(m.LedgerEntry.amount), 0)).where(m.LedgerEntry.property_id.in_(prop_ids), m.LedgerEntry.category == "rent_charge", m.LedgerEntry.status.in_(["open", "overdue", "part_paid"]))) or 0}

@app.get("/admin/listing-review")
def admin_listing_review(actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    rows = list(db.scalars(select(m.Property).where(m.Property.status.in_(["pending_review", "draft", "paused", "rejected"])).order_by(m.Property.updated_at.desc()).limit(500)))
    return [property_dict(p, db) for p in rows]

@app.post("/admin/properties/{property_id}/decision")
def admin_property_decision(property_id: int, body: dict, actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    prop = property_or_404(db, property_id)
    decision = body.get("decision")
    if decision not in {"approve", "request_changes", "reject", "pause"}: raise HTTPException(422, "Decision must be approve, request_changes, reject or pause")
    old = {"status": prop.status, "verification_status": prop.verification_status}
    mapping = {"approve": ("published", "verified"), "request_changes": ("draft", "changes_requested"), "reject": ("rejected", "rejected"), "pause": ("paused", prop.verification_status)}
    prop.status, prop.verification_status = mapping[decision]
    audit(db, actor, "property", prop.id, f"admin_{decision}", old=old, new={"status": prop.status, "verification_status": prop.verification_status, "note": str(body.get("note", ""))[:2000]})
    db.commit(); db.refresh(prop); return property_dict(prop, db)

@app.get("/admin/audit-log")
def admin_audit_log(limit: int = Query(default=100, ge=1, le=1000), actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    rows = list(db.scalars(select(m.AuditLog).order_by(m.AuditLog.created_at.desc()).limit(limit)))
    return [{"id": x.id, "actor_id": x.actor_id, "actor_role": x.actor_role, "entity_type": x.entity_type, "entity_id": x.entity_id,
             "action": x.action, "old_value": x.old_value, "new_value": x.new_value, "created_at": x.created_at.isoformat()} for x in rows]

@app.get("/admin/reports")
def admin_reports(actor: dict = Depends(require_roles("admin")), db: Session = Depends(get_db)):
    return {"users": db.scalar(select(func.count(m.User.id))) or 0, "properties": db.scalar(select(func.count(m.Property.id))) or 0,
            "published_properties": db.scalar(select(func.count(m.Property.id)).where(m.Property.status == "published")) or 0,
            "pending_review": db.scalar(select(func.count(m.Property.id)).where(m.Property.status == "pending_review")) or 0,
            "reservations": db.scalar(select(func.count(m.Reservation.id))) or 0,
            "open_issues": db.scalar(select(func.count(m.MaintenanceIssue.id)).where(m.MaintenanceIssue.status.in_(["open", "reopened", "escalated"]))) or 0}

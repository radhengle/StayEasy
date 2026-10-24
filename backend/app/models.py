from datetime import datetime, timezone
from sqlalchemy import Boolean, Date, DateTime, Float, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint, text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .database import Base

def utcnow():
    return datetime.now(timezone.utc)

class User(Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    phone: Mapped[str] = mapped_column(String(40), default="")
    display_name: Mapped[str] = mapped_column(String(180), default="")
    role: Mapped[str] = mapped_column(String(24), default="customer", index=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class TenantProfile(Base):
    """Tenant-owned optional profile fields; property-specific copies remain attached to an invitation."""
    __tablename__ = "tenant_profiles"
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    emergency_contact_name: Mapped[str] = mapped_column(String(180), default="")
    emergency_contact_phone: Mapped[str] = mapped_column(String(40), default="")
    accommodation_preferences: Mapped[dict] = mapped_column(JSON, default=dict)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)

class Property(Base):
    __tablename__ = "properties"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    owner_id: Mapped[str] = mapped_column(String(36), index=True)
    name: Mapped[str] = mapped_column(String(180))
    description: Mapped[str] = mapped_column(Text, default="")
    address: Mapped[str] = mapped_column(String(300), default="")
    locality: Mapped[str] = mapped_column(String(120), index=True)
    city: Mapped[str] = mapped_column(String(100), index=True)
    pin_code: Mapped[str] = mapped_column(String(10), default="", index=True)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    nearby_places: Mapped[list] = mapped_column(JSON, default=list)
    eligibility: Mapped[str] = mapped_column(String(30), default="Mixed")
    couples_welcome: Mapped[bool] = mapped_column(Boolean, default=False)
    furnishing: Mapped[str] = mapped_column(String(30), default="Furnished")
    bathroom_policy: Mapped[str] = mapped_column(String(40), default="Shared")
    food_policy: Mapped[str] = mapped_column(String(40), default="Not offered")
    visitor_policy: Mapped[str] = mapped_column(String(120), default="Ask owner")
    curfew_policy: Mapped[str] = mapped_column(String(120), default="Ask owner")
    cooking_allowed: Mapped[bool] = mapped_column(Boolean, default=False)
    minimum_stay_months: Mapped[int] = mapped_column(Integer, default=1)
    notice_period_days: Mapped[int] = mapped_column(Integer, default=30)
    rent_from: Mapped[int] = mapped_column(Integer, default=0)
    deposit_from: Mapped[int] = mapped_column(Integer, default=0)
    food_monthly: Mapped[int] = mapped_column(Integer, default=0)
    utilities_estimate: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String(24), default="draft", index=True)  # draft, pending_review, published, paused, rejected
    verification_status: Mapped[str] = mapped_column(String(24), default="unverified", index=True)
    availability_confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)
    units: Mapped[list["PropertyUnit"]] = relationship(back_populates="property", cascade="all, delete-orphan")

class Amenity(Base):
    __tablename__ = "amenities"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    code: Mapped[str] = mapped_column(String(70), unique=True, index=True)
    label: Mapped[str] = mapped_column(String(120))
    category: Mapped[str] = mapped_column(String(80), default="general")
    scope_allowed: Mapped[list] = mapped_column(JSON, default=lambda: ["property", "room", "unit"])

class PropertyAmenity(Base):
    __tablename__ = "property_amenities"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id", ondelete="CASCADE"), index=True)
    amenity_id: Mapped[int] = mapped_column(ForeignKey("amenities.id"), index=True)
    scope: Mapped[str] = mapped_column(String(20), default="property")
    unit_id: Mapped[int | None] = mapped_column(ForeignKey("property_units.id", ondelete="CASCADE"), nullable=True)
    availability: Mapped[str] = mapped_column(String(24), default="available")
    pricing: Mapped[str] = mapped_column(String(24), default="included")
    extra_charge: Mapped[int] = mapped_column(Integer, default=0)
    notes: Mapped[str] = mapped_column(Text, default="")
    __table_args__ = (UniqueConstraint("property_id", "amenity_id", "scope", "unit_id", name="uq_property_amenity_scope"),)

class ListingPhoto(Base):
    __tablename__ = "listing_photos"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id", ondelete="CASCADE"), index=True)
    storage_key: Mapped[str] = mapped_column(String(500))
    caption: Mapped[str] = mapped_column(String(200), default="")
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    moderation_status: Mapped[str] = mapped_column(String(24), default="pending")

class PropertyUnit(Base):
    __tablename__ = "property_units"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id", ondelete="CASCADE"), index=True)
    floor_label: Mapped[str] = mapped_column(String(50), default="Ground")
    room_code: Mapped[str] = mapped_column(String(70), index=True)
    unit_code: Mapped[str] = mapped_column(String(90), index=True)
    rentable_type: Mapped[str] = mapped_column(String(24), default="bed")  # bed or room
    sharing_capacity: Mapped[int] = mapped_column(Integer, default=1)
    eligibility_override: Mapped[str] = mapped_column(String(30), default="")
    couples_override: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    monthly_rent: Mapped[int] = mapped_column(Integer, default=0)
    deposit: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String(24), default="available", index=True)  # available/reserved/occupied/maintenance/inactive
    expected_vacancy: Mapped[Date | None] = mapped_column(Date, nullable=True)
    last_availability_confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    property: Mapped[Property] = relationship(back_populates="units")

Index("uq_property_unit_code", PropertyUnit.property_id, PropertyUnit.unit_code, unique=True)

class Enquiry(Base):
    __tablename__ = "enquiries"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    customer_id: Mapped[str] = mapped_column(String(36), index=True)
    move_in_date: Mapped[Date | None] = mapped_column(Date, nullable=True)
    preferred_room_type: Mapped[str] = mapped_column(String(50), default="")
    message: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(24), default="new", index=True)
    owner_response: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)

class Visit(Base):
    __tablename__ = "visits"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    enquiry_id: Mapped[int | None] = mapped_column(ForeignKey("enquiries.id"), nullable=True, index=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    customer_id: Mapped[str] = mapped_column(String(36), index=True)
    preferred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(24), default="requested", index=True)
    outcome: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class Reservation(Base):
    __tablename__ = "reservations"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    unit_id: Mapped[int | None] = mapped_column(ForeignKey("property_units.id"), nullable=True, index=True)
    customer_id: Mapped[str] = mapped_column(String(36), index=True)
    preferred_room_type: Mapped[str] = mapped_column(String(50), default="")
    move_in_date: Mapped[Date] = mapped_column(Date)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    status: Mapped[str] = mapped_column(String(24), default="pending", index=True)
    rent_snapshot: Mapped[int] = mapped_column(Integer, default=0)
    deposit_snapshot: Mapped[int] = mapped_column(Integer, default=0)
    terms_snapshot: Mapped[dict] = mapped_column(JSON, default=dict)
    cancellation_reason: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

Index("uq_active_reservation_per_unit", Reservation.unit_id, unique=True,
      postgresql_where=text("unit_id IS NOT NULL AND status IN ('pending','confirmed','checked_in')"),
      sqlite_where=text("unit_id IS NOT NULL AND status IN ('pending','confirmed','checked_in')"))

class PropertyInvitation(Base):
    __tablename__ = "property_invitations"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id", ondelete="CASCADE"), index=True)
    unit_id: Mapped[int] = mapped_column(ForeignKey("property_units.id"), index=True)
    enquiry_id: Mapped[int | None] = mapped_column(ForeignKey("enquiries.id"), nullable=True, index=True)
    invited_by: Mapped[str] = mapped_column(String(36), index=True)
    recipient_user_id: Mapped[str | None] = mapped_column(String(36), nullable=True, index=True)
    recipient_email: Mapped[str] = mapped_column(String(255), index=True)
    recipient_name: Mapped[str] = mapped_column(String(180), default="")
    recipient_phone: Mapped[str] = mapped_column(String(40), default="")
    token_hash: Mapped[str] = mapped_column(String(64), unique=True)
    move_in_date: Mapped[Date] = mapped_column(Date)
    rent_snapshot: Mapped[int] = mapped_column(Integer, default=0)
    deposit_snapshot: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String(24), default="pending", index=True)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    accepted_by: Mapped[str | None] = mapped_column(String(36), nullable=True, index=True)
    accepted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    reservation_id: Mapped[int | None] = mapped_column(ForeignKey("reservations.id"), nullable=True, index=True)
    reassignment_reason: Mapped[str] = mapped_column(Text, default="")
    proposed_unit_id: Mapped[int | None] = mapped_column(ForeignKey("property_units.id"), nullable=True, index=True)
    proposed_rent_snapshot: Mapped[int | None] = mapped_column(Integer, nullable=True)
    proposed_deposit_snapshot: Mapped[int | None] = mapped_column(Integer, nullable=True)
    reassignment_proposed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    required_document_categories: Mapped[list] = mapped_column(JSON, default=list)
    document_request_reason: Mapped[str] = mapped_column(Text, default="")
    registration_profile: Mapped[dict] = mapped_column(JSON, default=dict)
    registration_submitted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


Index("ix_property_invitations_pending_expiry", PropertyInvitation.status, PropertyInvitation.expires_at)


class InvitationDocument(Base):
    """Metadata for encrypted tenant documents. Plaintext document bytes never live in the database."""
    __tablename__ = "invitation_documents"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    invitation_id: Mapped[int] = mapped_column(ForeignKey("property_invitations.id", ondelete="CASCADE"), index=True)
    uploaded_by: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    category: Mapped[str] = mapped_column(String(40), index=True)
    purpose: Mapped[str] = mapped_column(Text)
    storage_key: Mapped[str] = mapped_column(String(80), unique=True)
    original_filename: Mapped[str] = mapped_column(String(200), default="")
    media_type: Mapped[str] = mapped_column(String(80))
    size_bytes: Mapped[int] = mapped_column(Integer)
    consent_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(24), default="submitted", index=True)
    reviewed_by: Mapped[str | None] = mapped_column(String(36), nullable=True)
    review_note: Mapped[str] = mapped_column(Text, default="")
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    retention_until: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


Index("ix_invitation_documents_invitation_status", InvitationDocument.invitation_id, InvitationDocument.status)



class ResidentStay(Base):
    __tablename__ = "resident_stays"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    unit_id: Mapped[int] = mapped_column(ForeignKey("property_units.id"), index=True)
    resident_id: Mapped[str] = mapped_column(String(36), index=True)
    reservation_id: Mapped[int | None] = mapped_column(ForeignKey("reservations.id"), nullable=True)
    joining_date: Mapped[Date] = mapped_column(Date)
    notice_date: Mapped[Date | None] = mapped_column(Date, nullable=True)
    move_out_date: Mapped[Date | None] = mapped_column(Date, nullable=True)
    agreed_monthly_rent: Mapped[int] = mapped_column(Integer)
    deposit_held: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String(24), default="active", index=True)
    agreement_reference: Mapped[str] = mapped_column(String(300), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class ResidentMoveOutRequest(Base):
    __tablename__ = "resident_move_out_requests"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    resident_stay_id: Mapped[int] = mapped_column(ForeignKey("resident_stays.id", ondelete="CASCADE"), index=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id", ondelete="CASCADE"), index=True)
    resident_id: Mapped[str] = mapped_column(String(36), index=True)
    requested_move_out_date: Mapped[Date] = mapped_column(Date)
    reason: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(24), default="pending", index=True)  # pending/approved/rejected/cancelled
    owner_note: Mapped[str] = mapped_column(String(1000), default="")
    reviewed_by: Mapped[str] = mapped_column(String(36), default="")
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)


Index(
    "uq_pending_moveout_request_per_stay",
    ResidentMoveOutRequest.resident_stay_id,
    unique=True,
    postgresql_where=text("status = 'pending'"),
    sqlite_where=text("status = 'pending'"),
)


class LedgerEntry(Base):
    __tablename__ = "ledger_entries"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    resident_stay_id: Mapped[int | None] = mapped_column(ForeignKey("resident_stays.id"), nullable=True, index=True)
    reservation_id: Mapped[int | None] = mapped_column(ForeignKey("reservations.id"), nullable=True, index=True)
    category: Mapped[str] = mapped_column(String(30), index=True)  # rent_charge/payment/deposit/refund/deduction/expense/food/utility
    amount: Mapped[int] = mapped_column(Integer)  # Backward-compatible rounded rupee display amount
    amount_paise: Mapped[int | None] = mapped_column(Integer, nullable=True)  # Exact minor-unit amount where available
    currency: Mapped[str] = mapped_column(String(3), default="INR")
    billing_period: Mapped[str] = mapped_column(String(30), default="")
    due_date: Mapped[Date | None] = mapped_column(Date, nullable=True)
    paid_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    payment_method: Mapped[str] = mapped_column(String(30), default="manual")
    payment_reference: Mapped[str] = mapped_column(String(180), default="")
    provider_status: Mapped[str] = mapped_column(String(24), default="manual_record")
    description: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(24), default="open", index=True)
    created_by: Mapped[str] = mapped_column(String(36), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class MaintenanceIssue(Base):
    __tablename__ = "maintenance_issues"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    unit_id: Mapped[int | None] = mapped_column(ForeignKey("property_units.id"), nullable=True, index=True)
    reported_by: Mapped[str] = mapped_column(String(36), index=True)
    category: Mapped[str] = mapped_column(String(70), default="other")
    title: Mapped[str] = mapped_column(String(180))
    description: Mapped[str] = mapped_column(Text)
    priority: Mapped[str] = mapped_column(String(20), default="normal", index=True)
    status: Mapped[str] = mapped_column(String(24), default="open", index=True)
    assigned_staff_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    target_resolution_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    resolution_note: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)

class Notice(Base):
    __tablename__ = "notices"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    title: Mapped[str] = mapped_column(String(180))
    body: Mapped[str] = mapped_column(Text)
    audience: Mapped[str] = mapped_column(String(50), default="residents")
    status: Mapped[str] = mapped_column(String(24), default="draft", index=True)
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_by: Mapped[str] = mapped_column(String(36))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class StaffAssignment(Base):
    __tablename__ = "staff_assignments"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    owner_id: Mapped[str] = mapped_column(String(36), index=True)
    staff_user_id: Mapped[str] = mapped_column(String(36), index=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    role: Mapped[str] = mapped_column(String(30), default="caretaker")
    permissions: Mapped[list] = mapped_column(JSON, default=list)
    status: Mapped[str] = mapped_column(String(24), default="invited")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    __table_args__ = (UniqueConstraint("staff_user_id", "property_id", name="uq_staff_property"),)

class SavedSearch(Base):
    __tablename__ = "saved_searches"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    customer_id: Mapped[str] = mapped_column(String(36), index=True)
    name: Mapped[str] = mapped_column(String(150))
    criteria: Mapped[dict] = mapped_column(JSON, default=dict)
    alerts_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class Review(Base):
    __tablename__ = "reviews"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    resident_stay_id: Mapped[int] = mapped_column(ForeignKey("resident_stays.id"), unique=True)
    reviewer_id: Mapped[str] = mapped_column(String(36))
    rating: Mapped[int] = mapped_column(Integer)
    body: Mapped[str] = mapped_column(Text, default="")
    owner_response: Mapped[str] = mapped_column(Text, default="")
    moderation_status: Mapped[str] = mapped_column(String(24), default="pending", index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class Notification(Base):
    __tablename__ = "notifications"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    recipient_id: Mapped[str] = mapped_column(String(36), index=True)
    title: Mapped[str] = mapped_column(String(180))
    body: Mapped[str] = mapped_column(Text)
    channel: Mapped[str] = mapped_column(String(24), default="in_app")
    status: Mapped[str] = mapped_column(String(24), default="queued", index=True)
    provider_reference: Mapped[str] = mapped_column(String(180), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class MealPlan(Base):
    __tablename__ = "meal_plans"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    day_of_week: Mapped[int] = mapped_column(Integer)
    meal_type: Mapped[str] = mapped_column(String(30))
    menu: Mapped[str] = mapped_column(Text, default="")
    dietary_options: Mapped[list] = mapped_column(JSON, default=list)
    charge_per_month: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String(24), default="available")

class HousekeepingTask(Base):
    __tablename__ = "housekeeping_tasks"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    unit_id: Mapped[int | None] = mapped_column(ForeignKey("property_units.id"), nullable=True)
    task_type: Mapped[str] = mapped_column(String(70))
    description: Mapped[str] = mapped_column(Text, default="")
    due_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    assigned_staff_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    status: Mapped[str] = mapped_column(String(24), default="scheduled", index=True)

class VisitorEntry(Base):
    __tablename__ = "visitor_entries"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    resident_stay_id: Mapped[int | None] = mapped_column(ForeignKey("resident_stays.id"), nullable=True)
    visitor_name: Mapped[str] = mapped_column(String(150))
    purpose: Mapped[str] = mapped_column(String(180), default="")
    expected_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    checked_in_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    checked_out_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    status: Mapped[str] = mapped_column(String(24), default="expected")

class AuditLog(Base):
    __tablename__ = "audit_logs"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    actor_id: Mapped[str] = mapped_column(String(36), index=True)
    actor_role: Mapped[str] = mapped_column(String(24))
    entity_type: Mapped[str] = mapped_column(String(70), index=True)
    entity_id: Mapped[str] = mapped_column(String(70))
    action: Mapped[str] = mapped_column(String(70))
    old_value: Mapped[dict] = mapped_column(JSON, default=dict)
    new_value: Mapped[dict] = mapped_column(JSON, default=dict)
    request_id: Mapped[str] = mapped_column(String(70), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

class PrivacyRequest(Base):
    __tablename__ = "privacy_requests"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    subject_id: Mapped[str] = mapped_column(String(36), index=True)
    request_type: Mapped[str] = mapped_column(String(30), index=True)  # export/correct/delete
    details: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(24), default="submitted", index=True)
    admin_note: Mapped[str] = mapped_column(Text, default="")
    requested_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

class PlatformReport(Base):
    __tablename__ = "platform_reports"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    reporter_id: Mapped[str] = mapped_column(String(36), index=True)
    target_type: Mapped[str] = mapped_column(String(30), index=True)  # property/review/other
    target_id: Mapped[str] = mapped_column(String(70), default="")
    property_id: Mapped[int | None] = mapped_column(ForeignKey("properties.id"), nullable=True, index=True)
    category: Mapped[str] = mapped_column(String(70), default="other")
    details: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(24), default="open", index=True)
    moderator_note: Mapped[str] = mapped_column(Text, default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)

class ResidentDocument(Base):
    __tablename__ = "resident_documents"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    property_id: Mapped[int] = mapped_column(ForeignKey("properties.id"), index=True)
    resident_stay_id: Mapped[int] = mapped_column(ForeignKey("resident_stays.id"), index=True)
    uploaded_by: Mapped[str] = mapped_column(String(36), index=True)
    category: Mapped[str] = mapped_column(String(50), default="other")
    filename: Mapped[str] = mapped_column(String(255), default="document")
    storage_key: Mapped[str] = mapped_column(String(600))
    status: Mapped[str] = mapped_column(String(24), default="pending", index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

Index("ix_platform_report_open", PlatformReport.status, PlatformReport.created_at)
Index("ix_privacy_request_open", PrivacyRequest.status, PrivacyRequest.requested_at)


class UserCredential(Base):
    """Private authentication state separated from the public user/profile record."""
    __tablename__ = "user_credentials"
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    requested_workspace: Mapped[str] = mapped_column(String(16), default="tenant")
    owner_access_status: Mapped[str] = mapped_column(
        String(24), default="not_requested", server_default=text("'not_requested'"), nullable=False
    )
    owner_access_requested_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    owner_access_reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    owner_access_review_note: Mapped[str] = mapped_column(String(500), default="", server_default=text("''"), nullable=False)
    email_verified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    verification_code_hash: Mapped[str | None] = mapped_column(String(64), nullable=True)
    verification_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    reset_code_hash: Mapped[str | None] = mapped_column(String(64), nullable=True)
    reset_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    auth_version: Mapped[int] = mapped_column(Integer, default=0)
    failed_login_attempts: Mapped[int] = mapped_column(Integer, default=0)
    verification_attempts: Mapped[int] = mapped_column(Integer, default=0)
    reset_attempts: Mapped[int] = mapped_column(Integer, default=0)
    locked_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

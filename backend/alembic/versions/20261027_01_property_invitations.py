"""Add secure property-specific tenant invitations."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261027_01"
down_revision = "20261026_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)

    if not inspector.has_table("property_invitations"):
        op.create_table(
            "property_invitations",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("property_id", sa.Integer(), sa.ForeignKey("properties.id", ondelete="CASCADE"), nullable=False),
            sa.Column("unit_id", sa.Integer(), sa.ForeignKey("property_units.id"), nullable=False),
            sa.Column("enquiry_id", sa.Integer(), sa.ForeignKey("enquiries.id"), nullable=True),
            sa.Column("invited_by", sa.String(length=36), nullable=False),
            sa.Column("recipient_user_id", sa.String(length=36), nullable=True),
            sa.Column("recipient_email", sa.String(length=255), nullable=False),
            sa.Column("recipient_name", sa.String(length=180), nullable=False, server_default=sa.text("''")),
            sa.Column("recipient_phone", sa.String(length=40), nullable=False, server_default=sa.text("''")),
            sa.Column("token_hash", sa.String(length=64), nullable=False),
            sa.Column("move_in_date", sa.Date(), nullable=False),
            sa.Column("rent_snapshot", sa.Integer(), nullable=False, server_default=sa.text("0")),
            sa.Column("deposit_snapshot", sa.Integer(), nullable=False, server_default=sa.text("0")),
            sa.Column("status", sa.String(length=24), nullable=False, server_default=sa.text("'pending'")),
            sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("accepted_by", sa.String(length=36), nullable=True),
            sa.Column("accepted_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("reservation_id", sa.Integer(), sa.ForeignKey("reservations.id"), nullable=True),
            sa.Column("reassignment_reason", sa.Text(), nullable=False, server_default=sa.text("''")),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.UniqueConstraint("token_hash", name="uq_property_invitations_token_hash"),
        )

    existing_indexes = {index["name"] for index in inspect(bind).get_indexes("property_invitations")}
    for name, columns in (
        ("ix_property_invitations_property_id", ["property_id"]),
        ("ix_property_invitations_unit_id", ["unit_id"]),
        ("ix_property_invitations_enquiry_id", ["enquiry_id"]),
        ("ix_property_invitations_recipient_user_id", ["recipient_user_id"]),
        ("ix_property_invitations_recipient_email", ["recipient_email"]),
        ("ix_property_invitations_status", ["status"]),
        ("ix_property_invitations_expires_at", ["expires_at"]),
        ("ix_property_invitations_pending_expiry", ["status", "expires_at"]),
    ):
        if name not in existing_indexes:
            op.create_index(name, "property_invitations", columns, unique=False)


def downgrade() -> None:
    raise RuntimeError(
        "Tenant invitation and acceptance history is retained for auditability; restore a backup instead of dropping it."
    )

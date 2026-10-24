"""Add audited resident move-out requests and owner responses."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261026_01"
down_revision = "20261024_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if not inspector.has_table("resident_move_out_requests"):
        op.create_table(
            "resident_move_out_requests",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("resident_stay_id", sa.Integer(), sa.ForeignKey("resident_stays.id", ondelete="CASCADE"), nullable=False),
            sa.Column("property_id", sa.Integer(), sa.ForeignKey("properties.id", ondelete="CASCADE"), nullable=False),
            sa.Column("resident_id", sa.String(length=36), nullable=False),
            sa.Column("requested_move_out_date", sa.Date(), nullable=False),
            sa.Column("reason", sa.Text(), nullable=False, server_default=sa.text("''")),
            sa.Column("status", sa.String(length=24), nullable=False, server_default=sa.text("'pending'")),
            sa.Column("owner_note", sa.String(length=1000), nullable=False, server_default=sa.text("''")),
            sa.Column("reviewed_by", sa.String(length=36), nullable=False, server_default=sa.text("''")),
            sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        )

    existing_indexes = {index["name"] for index in inspect(bind).get_indexes("resident_move_out_requests")}
    for name, columns in (
        ("ix_resident_move_out_requests_resident_stay_id", ["resident_stay_id"]),
        ("ix_resident_move_out_requests_property_id", ["property_id"]),
        ("ix_resident_move_out_requests_resident_id", ["resident_id"]),
        ("ix_resident_move_out_requests_status", ["status"]),
    ):
        if name not in existing_indexes:
            op.create_index(name, "resident_move_out_requests", columns, unique=False)

    if "uq_pending_moveout_request_per_stay" not in existing_indexes:
        op.create_index(
            "uq_pending_moveout_request_per_stay",
            "resident_move_out_requests",
            ["resident_stay_id"],
            unique=True,
            postgresql_where=sa.text("status = 'pending'"),
            sqlite_where=sa.text("status = 'pending'"),
        )


def downgrade() -> None:
    raise RuntimeError(
        "Resident move-out request history is retained for auditability; restore a backup instead of dropping it."
    )

"""Track property-feed sync health without storing credentials or payloads."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261101_01"
down_revision = "20261031_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if not inspector.has_table("integration_sync_state"):
        op.create_table(
            "integration_sync_state",
            sa.Column("integration_key", sa.String(length=80), primary_key=True),
            sa.Column("status", sa.String(length=24), nullable=False, server_default="never_run"),
            sa.Column("last_attempt_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("last_success_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("last_error_category", sa.String(length=80), nullable=False, server_default=""),
            sa.Column("received_count", sa.Integer(), nullable=False, server_default="0"),
            sa.Column("created_count", sa.Integer(), nullable=False, server_default="0"),
            sa.Column("updated_count", sa.Integer(), nullable=False, server_default="0"),
            sa.Column("skipped_count", sa.Integer(), nullable=False, server_default="0"),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        )
        op.create_index("ix_integration_sync_state_status", "integration_sync_state", ["status"])


def downgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if inspector.has_table("integration_sync_state"):
        indexes = {index["name"] for index in inspector.get_indexes("integration_sync_state")}
        if "ix_integration_sync_state_status" in indexes:
            op.drop_index("ix_integration_sync_state_status", table_name="integration_sync_state")
        op.drop_table("integration_sync_state")

"""Track owner workspace requests and administrator review outcomes."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect, text

revision = "20261025_01"
down_revision = "20261024_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    existing = {column["name"] for column in inspect(bind).get_columns("user_credentials")}

    # The historical bootstrap migration builds from current model metadata in dev.
    # Check first so a fresh database and an existing installation both upgrade safely.
    if "owner_access_status" not in existing:
        op.add_column(
            "user_credentials",
            sa.Column(
                "owner_access_status",
                sa.String(length=24),
                nullable=False,
                server_default=sa.text("'not_requested'"),
            ),
        )
    if "owner_access_requested_at" not in existing:
        op.add_column(
            "user_credentials",
            sa.Column("owner_access_requested_at", sa.DateTime(timezone=True), nullable=True),
        )
    if "owner_access_reviewed_at" not in existing:
        op.add_column(
            "user_credentials",
            sa.Column("owner_access_reviewed_at", sa.DateTime(timezone=True), nullable=True),
        )
    if "owner_access_review_note" not in existing:
        op.add_column(
            "user_credentials",
            sa.Column(
                "owner_access_review_note",
                sa.String(length=500),
                nullable=False,
                server_default=sa.text("''"),
            ),
        )

    bind.execute(text("""
        UPDATE user_credentials
        SET owner_access_status = CASE
            WHEN requested_workspace = 'owner'
                 AND user_id IN (SELECT id FROM users WHERE role = 'owner')
                THEN 'approved'
            WHEN requested_workspace = 'owner'
                 AND owner_access_status = 'not_requested'
                THEN 'pending_approval'
            ELSE owner_access_status
        END
    """))
    bind.execute(text("""
        UPDATE user_credentials
        SET owner_access_requested_at = created_at
        WHERE requested_workspace = 'owner' AND owner_access_requested_at IS NULL
    """))


def downgrade() -> None:
    raise RuntimeError(
        "Owner access review history is retained for auditability; restore a backup instead of dropping it."
    )

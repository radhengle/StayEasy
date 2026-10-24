"""Add precise minor-unit and reservation linkage to rent ledger."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261028_01"
down_revision = "20261027_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if not inspector.has_table("ledger_entries"):
        return

    columns = {column["name"] for column in inspect(bind).get_columns("ledger_entries")}
    if "amount_paise" not in columns:
        op.add_column("ledger_entries", sa.Column("amount_paise", sa.Integer(), nullable=True))
    if "reservation_id" not in columns:
        op.add_column(
            "ledger_entries",
            sa.Column("reservation_id", sa.Integer(), sa.ForeignKey("reservations.id"), nullable=True),
        )

    indexes = {index["name"] for index in inspect(bind).get_indexes("ledger_entries")}
    if "ix_ledger_entries_reservation_id" not in indexes:
        op.create_index("ix_ledger_entries_reservation_id", "ledger_entries", ["reservation_id"], unique=False)


def downgrade() -> None:
    raise RuntimeError(
        "Exact rent amounts and reservation-linked accounting history are retained for auditability; restore a backup instead of dropping these fields."
    )

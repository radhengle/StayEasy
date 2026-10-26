"""Persist Route transfer reversal references beside each refund attempt."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261103_01"
down_revision = "20261102_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if not inspector.has_table("payment_refunds"):
        return

    columns = {column["name"] for column in inspect(bind).get_columns("payment_refunds")}
    if "provider_transfer_reversal_id" not in columns:
        op.add_column(
            "payment_refunds",
            sa.Column("provider_transfer_reversal_id", sa.String(length=100), nullable=True),
        )
    if "transfer_reversal_amount_paise" not in columns:
        op.add_column(
            "payment_refunds",
            sa.Column("transfer_reversal_amount_paise", sa.Integer(), nullable=True),
        )
    if "transfer_reversal_status" not in columns:
        op.add_column(
            "payment_refunds",
            sa.Column("transfer_reversal_status", sa.String(length=24), nullable=False, server_default="not_required"),
        )

    indexes = {index["name"] for index in inspect(bind).get_indexes("payment_refunds")}
    if "uq_payment_refunds_provider_transfer_reversal_id" not in indexes:
        op.create_index(
            "uq_payment_refunds_provider_transfer_reversal_id",
            "payment_refunds",
            ["provider_transfer_reversal_id"],
            unique=True,
        )


def downgrade() -> None:
    raise RuntimeError(
        "Payment refund and transfer reversal references are financial audit history; "
        "use the documented retention and recovery process rather than dropping them."
    )

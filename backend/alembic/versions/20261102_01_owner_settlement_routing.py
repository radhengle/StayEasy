"""Track approved Razorpay Route accounts and per-payment owner transfers."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261102_01"
down_revision = "20261101_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)

    if not inspector.has_table("owner_settlement_accounts"):
        op.create_table(
            "owner_settlement_accounts",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("owner_id", sa.String(length=36), sa.ForeignKey("users.id"), nullable=False),
            sa.Column("linked_account_id", sa.String(length=100), nullable=False),
            sa.Column("status", sa.String(length=24), nullable=False, server_default="pending"),
            sa.Column("linked_by", sa.String(length=36), nullable=False, server_default=""),
            sa.Column("linked_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.UniqueConstraint("owner_id", name="uq_owner_settlement_accounts_owner_id"),
            sa.UniqueConstraint("linked_account_id", name="uq_owner_settlement_accounts_linked_account_id"),
        )
    account_indexes = {index["name"] for index in inspect(bind).get_indexes("owner_settlement_accounts")}
    for name, columns in (
        ("ix_owner_settlement_accounts_status", ["status"]),
    ):
        if name not in account_indexes:
            op.create_index(name, "owner_settlement_accounts", columns, unique=False)

    if not inspect(bind).has_table("payment_transfers"):
        op.create_table(
            "payment_transfers",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("payment_attempt_id", sa.Integer(), sa.ForeignKey("payment_attempts.id"), nullable=False),
            sa.Column("owner_id", sa.String(length=36), sa.ForeignKey("users.id"), nullable=False),
            sa.Column("linked_account_id", sa.String(length=100), nullable=False),
            sa.Column("provider_order_id", sa.String(length=100), nullable=False),
            sa.Column("provider_transfer_id", sa.String(length=100), nullable=True),
            sa.Column("amount_paise", sa.Integer(), nullable=False),
            sa.Column("currency", sa.String(length=3), nullable=False, server_default="INR"),
            sa.Column("status", sa.String(length=30), nullable=False, server_default="creating"),
            sa.Column("settlement_status", sa.String(length=24), nullable=False, server_default="pending"),
            sa.Column("failure_code", sa.String(length=80), nullable=False, server_default=""),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("processed_at", sa.DateTime(timezone=True), nullable=True),
            sa.UniqueConstraint("payment_attempt_id", name="uq_payment_transfers_payment_attempt_id"),
            sa.UniqueConstraint("provider_transfer_id", name="uq_payment_transfers_provider_transfer_id"),
        )
    transfer_indexes = {index["name"] for index in inspect(bind).get_indexes("payment_transfers")}
    for name, columns in (
        ("ix_payment_transfers_owner_id", ["owner_id"]),
        ("ix_payment_transfers_provider_order_id", ["provider_order_id"]),
        ("ix_payment_transfers_status", ["status"]),
    ):
        if name not in transfer_indexes:
            op.create_index(name, "payment_transfers", columns, unique=False)


def downgrade() -> None:
    raise RuntimeError(
        "Payment transfer and owner settlement records are financial audit history; "
        "use the documented retention and recovery process rather than dropping them."
    )

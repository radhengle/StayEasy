"""Add online payment attempts, refund tracking, webhook deduplication and ledger links."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261030_01"
down_revision = "20261029_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)

    if not inspector.has_table("payment_attempts"):
        op.create_table(
            "payment_attempts",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("charge_entry_id", sa.Integer(), sa.ForeignKey("ledger_entries.id"), nullable=False),
            sa.Column("customer_id", sa.String(length=36), sa.ForeignKey("users.id"), nullable=False),
            sa.Column("provider", sa.String(length=24), nullable=False, server_default=sa.text("'razorpay'")),
            sa.Column("idempotency_key", sa.String(length=128), nullable=False),
            sa.Column("amount_paise", sa.Integer(), nullable=False),
            sa.Column("currency", sa.String(length=3), nullable=False, server_default=sa.text("'INR'")),
            sa.Column("status", sa.String(length=30), nullable=False, server_default=sa.text("'creating'")),
            sa.Column("provider_order_id", sa.String(length=100), nullable=True),
            sa.Column("provider_payment_id", sa.String(length=100), nullable=True),
            sa.Column("payment_method", sa.String(length=32), nullable=False, server_default=sa.text("''")),
            sa.Column("failure_code", sa.String(length=80), nullable=False, server_default=sa.text("''")),
            sa.Column("captured_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.UniqueConstraint("customer_id", "idempotency_key", name="uq_payment_attempts_customer_idempotency"),
            sa.UniqueConstraint("provider_order_id", name="uq_payment_attempts_provider_order_id"),
            sa.UniqueConstraint("provider_payment_id", name="uq_payment_attempts_provider_payment_id"),
        )
    inspector = inspect(bind)
    attempt_indexes = {index["name"] for index in inspector.get_indexes("payment_attempts")}
    for name, columns in (
        ("ix_payment_attempts_charge_entry_id", ["charge_entry_id"]),
        ("ix_payment_attempts_customer_id", ["customer_id"]),
        ("ix_payment_attempts_status", ["status"]),
    ):
        if name not in attempt_indexes:
            op.create_index(name, "payment_attempts", columns, unique=False)
    attempt_indexes = {index["name"] for index in inspect(bind).get_indexes("payment_attempts")}
    if "uq_payment_attempts_one_active_per_charge" not in attempt_indexes:
        op.create_index(
            "uq_payment_attempts_one_active_per_charge",
            "payment_attempts",
            ["charge_entry_id"],
            unique=True,
            postgresql_where=sa.text("status IN ('creating', 'created', 'authorized', 'unknown')"),
            sqlite_where=sa.text("status IN ('creating', 'created', 'authorized', 'unknown')"),
        )

    if not inspect(bind).has_table("payment_refunds"):
        op.create_table(
            "payment_refunds",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("payment_attempt_id", sa.Integer(), sa.ForeignKey("payment_attempts.id"), nullable=False),
            sa.Column("provider_refund_id", sa.String(length=100), nullable=True),
            sa.Column("amount_paise", sa.Integer(), nullable=False),
            sa.Column("idempotency_key", sa.String(length=128), nullable=False),
            sa.Column("reason", sa.String(length=500), nullable=False, server_default=sa.text("''")),
            sa.Column("status", sa.String(length=24), nullable=False, server_default=sa.text("'requested'")),
            sa.Column("requested_by", sa.String(length=36), sa.ForeignKey("users.id"), nullable=False),
            sa.Column("failure_code", sa.String(length=80), nullable=False, server_default=sa.text("''")),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("processed_at", sa.DateTime(timezone=True), nullable=True),
            sa.UniqueConstraint("provider_refund_id", name="uq_payment_refunds_provider_refund_id"),
            sa.UniqueConstraint("requested_by", "idempotency_key", name="uq_payment_refunds_requester_idempotency"),
        )
    refund_indexes = {index["name"] for index in inspect(bind).get_indexes("payment_refunds")}
    for name, columns in (
        ("ix_payment_refunds_payment_attempt_id", ["payment_attempt_id"]),
        ("ix_payment_refunds_status", ["status"]),
        ("ix_payment_refunds_requested_by", ["requested_by"]),
        ("ix_payment_refunds_attempt_status", ["payment_attempt_id", "status"]),
    ):
        if name not in refund_indexes:
            op.create_index(name, "payment_refunds", columns, unique=False)

    if not inspect(bind).has_table("payment_webhook_events"):
        op.create_table(
            "payment_webhook_events",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("event_id", sa.String(length=180), nullable=False),
            sa.Column("event_type", sa.String(length=80), nullable=False),
            sa.Column("payload_sha256", sa.String(length=64), nullable=False),
            sa.Column("processing_status", sa.String(length=24), nullable=False, server_default=sa.text("'received'")),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
            sa.Column("processed_at", sa.DateTime(timezone=True), nullable=True),
            sa.UniqueConstraint("event_id", name="uq_payment_webhook_events_event_id"),
        )
    webhook_indexes = {index["name"] for index in inspect(bind).get_indexes("payment_webhook_events")}
    for name, columns in (
        ("ix_payment_webhook_events_event_type", ["event_type"]),
        ("ix_payment_webhook_events_processing_status", ["processing_status"]),
    ):
        if name not in webhook_indexes:
            op.create_index(name, "payment_webhook_events", columns, unique=False)

    if inspect(bind).has_table("ledger_entries"):
        columns = {column["name"] for column in inspect(bind).get_columns("ledger_entries")}
        if "related_charge_id" not in columns:
            op.add_column(
                "ledger_entries",
                sa.Column("related_charge_id", sa.Integer(), sa.ForeignKey("ledger_entries.id"), nullable=True),
            )
        if "payment_attempt_id" not in columns:
            op.add_column(
                "ledger_entries",
                sa.Column("payment_attempt_id", sa.Integer(), sa.ForeignKey("payment_attempts.id", name="fk_ledger_payment_attempt_id"), nullable=True),
            )
        ledger_indexes = {index["name"] for index in inspect(bind).get_indexes("ledger_entries")}
        if "ix_ledger_entries_related_charge_id" not in ledger_indexes:
            op.create_index("ix_ledger_entries_related_charge_id", "ledger_entries", ["related_charge_id"], unique=False)
        if "uq_ledger_entries_payment_attempt_id" not in ledger_indexes:
            op.create_index("uq_ledger_entries_payment_attempt_id", "ledger_entries", ["payment_attempt_id"], unique=True)


def downgrade() -> None:
    raise RuntimeError(
        "Payment and refund records are financial audit history; use the documented data retention and recovery process rather than dropping them."
    )

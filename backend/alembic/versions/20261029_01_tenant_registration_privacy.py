"""Persist optional tenant profile fields and scoped encrypted-document metadata."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261029_01"
down_revision = "20261028_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)

    if not inspector.has_table("tenant_profiles"):
        op.create_table(
            "tenant_profiles",
            sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
            sa.Column("emergency_contact_name", sa.String(length=180), nullable=False, server_default=sa.text("''")),
            sa.Column("emergency_contact_phone", sa.String(length=40), nullable=False, server_default=sa.text("''")),
            sa.Column("accommodation_preferences", sa.JSON(), nullable=False, server_default=sa.text("'{}'")),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        )

    if inspector.has_table("property_invitations"):
        columns = {column["name"] for column in inspect(bind).get_columns("property_invitations")}
        additions = (
            ("required_document_categories", sa.JSON(), sa.text("'[]'")),
            ("document_request_reason", sa.Text(), sa.text("''")),
            ("registration_profile", sa.JSON(), sa.text("'{}'")),
        )
        for name, column_type, default in additions:
            if name not in columns:
                op.add_column(
                    "property_invitations",
                    sa.Column(name, column_type, nullable=False, server_default=default),
                )
        columns = {column["name"] for column in inspect(bind).get_columns("property_invitations")}
        if "registration_submitted_at" not in columns:
            op.add_column(
                "property_invitations",
                sa.Column("registration_submitted_at", sa.DateTime(timezone=True), nullable=True),
            )

    if not inspect(bind).has_table("invitation_documents"):
        op.create_table(
            "invitation_documents",
            sa.Column("id", sa.Integer(), primary_key=True),
            sa.Column("invitation_id", sa.Integer(), sa.ForeignKey("property_invitations.id", ondelete="CASCADE"), nullable=False),
            sa.Column("uploaded_by", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
            sa.Column("category", sa.String(length=40), nullable=False),
            sa.Column("purpose", sa.Text(), nullable=False),
            sa.Column("storage_key", sa.String(length=80), nullable=False, unique=True),
            sa.Column("original_filename", sa.String(length=200), nullable=False, server_default=sa.text("''")),
            sa.Column("media_type", sa.String(length=80), nullable=False),
            sa.Column("size_bytes", sa.Integer(), nullable=False),
            sa.Column("consent_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("status", sa.String(length=24), nullable=False, server_default=sa.text("'submitted'")),
            sa.Column("reviewed_by", sa.String(length=36), nullable=True),
            sa.Column("review_note", sa.Text(), nullable=False, server_default=sa.text("''")),
            sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("retention_until", sa.DateTime(timezone=True), nullable=False),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        )

    indexes = {index["name"] for index in inspect(bind).get_indexes("invitation_documents")}
    for name, columns in (
        ("ix_invitation_documents_invitation_id", ["invitation_id"]),
        ("ix_invitation_documents_uploaded_by", ["uploaded_by"]),
        ("ix_invitation_documents_category", ["category"]),
        ("ix_invitation_documents_status", ["status"]),
        ("ix_invitation_documents_retention_until", ["retention_until"]),
        ("ix_invitation_documents_invitation_status", ["invitation_id", "status"]),
    ):
        if name not in indexes:
            op.create_index(name, "invitation_documents", columns, unique=False)


def downgrade() -> None:
    raise RuntimeError(
        "Tenant profile and document review records may contain regulated personal data; do not drop them implicitly. Use the documented retention/deletion workflow."
    )

"""Track source identity for repeatable external property-feed ingestion."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import inspect

revision = "20261031_01"
down_revision = "20261030_01"
branch_labels = None
depends_on = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if not inspector.has_table("properties"):
        return
    columns = {column["name"] for column in inspector.get_columns("properties")}
    if "data_source" not in columns:
        op.add_column("properties", sa.Column("data_source", sa.String(length=40), nullable=False, server_default="owner"))
    if "external_source_key" not in columns:
        op.add_column("properties", sa.Column("external_source_key", sa.String(length=160), nullable=True))
    if "external_updated_at" not in columns:
        op.add_column("properties", sa.Column("external_updated_at", sa.DateTime(timezone=True), nullable=True))
    indexes = {index["name"] for index in inspector.get_indexes("properties")}
    if "uq_properties_external_source_key" not in indexes:
        op.create_index("uq_properties_external_source_key", "properties", ["external_source_key"], unique=True)


def downgrade() -> None:
    bind = op.get_bind()
    inspector = inspect(bind)
    if not inspector.has_table("properties"):
        return
    indexes = {index["name"] for index in inspector.get_indexes("properties")}
    if "uq_properties_external_source_key" in indexes:
        op.drop_index("uq_properties_external_source_key", table_name="properties")
    columns = {column["name"] for column in inspector.get_columns("properties")}
    for column in ("external_updated_at", "external_source_key", "data_source"):
        if column in columns:
            op.drop_column("properties", column)

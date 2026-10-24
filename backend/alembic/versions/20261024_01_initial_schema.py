"""Bootstrap the current StayEasy schema for new or existing dev databases.

This bootstrap uses the model metadata so a clean database can be initialized and an
existing schema receives missing tables. Before public production deployment, freeze
and review this baseline as explicit table/column operations and verify existing DB
schema drift; future migrations must be immutable, explicit revisions.
"""
from pathlib import Path
import sys

from alembic import op

ROOT = Path(__file__).resolve().parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from backend.app.database import Base
from backend.app import models as _models  # noqa: F401

revision = "20261024_01"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    Base.metadata.create_all(bind=op.get_bind())


def downgrade() -> None:
    # Intentionally non-destructive: dropping the baseline would delete application data.
    raise RuntimeError("The StayEasy schema baseline is not destructively reversible; restore a backup instead.")

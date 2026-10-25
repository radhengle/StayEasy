"""Purge tenant invitation documents whose configured retention period has ended.

Run after applying database migrations:
    python -m backend.scripts.purge_expired_invitation_documents --dry-run
    python -m backend.scripts.purge_expired_invitation_documents

DOCUMENT_STORAGE_ROOT must point at the same private directory used by the API.
This job never decrypts or prints document content. It removes expired encrypted
blobs, deletes their database metadata, and writes a minimal audit record.
"""
import argparse
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from backend.app import models as m
from backend.app.database import SessionLocal


_STORAGE_KEY = re.compile(r"^[0-9a-f]{32}$")


def _configured_storage_root(root: str | Path | None = None) -> Path:
    """Resolve a configured existing storage directory; fail closed on misconfiguration."""
    import os

    configured_root = str(root) if root is not None else os.getenv("DOCUMENT_STORAGE_ROOT", "")
    if not configured_root.strip():
        raise ValueError("DOCUMENT_STORAGE_ROOT must point to the configured private document directory.")
    candidate = Path(configured_root).expanduser()
    try:
        resolved = candidate.resolve(strict=True)
    except OSError as exc:
        raise ValueError("DOCUMENT_STORAGE_ROOT does not exist or cannot be resolved.") from exc
    if not resolved.is_dir():
        raise ValueError("DOCUMENT_STORAGE_ROOT must be an existing directory.")
    return resolved


def _blob_path(root: Path, storage_key: str) -> Path:
    """Accept only server-generated storage keys and reject path/symlink traversal."""
    if not _STORAGE_KEY.fullmatch(storage_key or ""):
        raise ValueError("Stored document reference is invalid.")
    candidate = root / f"{storage_key}.enc"
    # Resolve an existing path to detect a storage-key symlink pointing outside the root.
    try:
        resolved = candidate.resolve(strict=False)
    except OSError as exc:
        raise ValueError("Stored document path cannot be resolved.") from exc
    if resolved.parent != root:
        raise ValueError("Stored document path resolves outside the configured root.")
    return candidate


def purge_expired_invitation_documents(
    db: Session,
    storage_root: str | Path | None = None,
    *,
    now: datetime | None = None,
    dry_run: bool = False,
    batch_size: int = 500,
) -> dict[str, Any]:
    """Delete expired encrypted blobs and their metadata without leaking document details.

    If removing a blob fails, the database record is retained so a later run can retry.
    If the blob is already missing, metadata can still be removed because expired
    downloads are denied by the API and the content is no longer present.
    """
    if not 1 <= batch_size <= 5000:
        raise ValueError("batch_size must be between 1 and 5000.")
    root = _configured_storage_root(storage_root)
    cutoff = now or datetime.now(timezone.utc)
    if cutoff.tzinfo is None:
        cutoff = cutoff.replace(tzinfo=timezone.utc)
    cutoff = cutoff.astimezone(timezone.utc)

    stats: dict[str, Any] = {
        "dry_run": dry_run,
        "expired_found": 0,
        "purged": 0,
        "content_deleted": 0,
        "content_missing": 0,
        "failed": 0,
        "errors": [],
    }

    if dry_run:
        stats["expired_found"] = len(list(db.scalars(
            select(m.InvitationDocument.id).where(m.InvitationDocument.retention_until <= cutoff)
        )))
        return stats

    blocked_ids: set[int] = set()
    while True:
        statement = (
            select(m.InvitationDocument)
            .where(m.InvitationDocument.retention_until <= cutoff)
            .order_by(m.InvitationDocument.retention_until, m.InvitationDocument.id)
            .limit(batch_size)
            .with_for_update(skip_locked=True)
        )
        if blocked_ids:
            statement = statement.where(m.InvitationDocument.id.not_in(blocked_ids))
        batch = list(db.scalars(statement))
        if not batch:
            break

        stats["expired_found"] += len(batch)
        removed_this_batch = 0
        for document in batch:
            try:
                blob = _blob_path(root, document.storage_key)
                existed = blob.exists()
                if existed:
                    blob.unlink()
                # Build the audit record with only the minimal retention proof. Do not
                # copy filename, purpose, email, document category or plaintext content.
                db.add(m.AuditLog(
                    actor_id="system-retention-purge",
                    actor_role="system",
                    entity_type="invitation_document",
                    entity_id=str(document.id),
                    action="retention_purge",
                    old_value={
                        "status": document.status,
                        "size_bytes": document.size_bytes,
                        "retention_until": document.retention_until.isoformat(),
                    },
                    new_value={
                        "content_deleted": existed,
                        "content_missing": not existed,
                        "policy": "retention_expired",
                    },
                ))
                db.delete(document)
                stats["purged"] += 1
                stats["content_deleted"] += int(existed)
                stats["content_missing"] += int(not existed)
                removed_this_batch += 1
            except (OSError, ValueError) as exc:
                blocked_ids.add(document.id)
                stats["failed"] += 1
                # Error output is deliberately restricted to a record ID and a generic
                # reason; never include storage paths, user identifiers or file details.
                stats["errors"].append({
                    "document_id": document.id,
                    "reason": "storage_path_invalid" if isinstance(exc, ValueError) else "blob_delete_failed",
                })

        try:
            db.commit()
        except Exception:
            db.rollback()
            # If the filesystem deletion succeeded but this commit failed, the expired
            # record remains. A future run sees a missing blob and safely removes metadata.
            raise

        if removed_this_batch == 0 and len(blocked_ids) == stats["failed"]:
            # All records in the batch were unprocessable. They are excluded from the
            # next query, so continue to the other expired records if any remain.
            continue

    return stats


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Remove expired tenant invitation document blobs and metadata."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Count eligible records without deleting any files or database rows.",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=500,
        help="Maximum number of records locked and processed per database transaction (1–5000).",
    )
    args = parser.parse_args()

    try:
        with SessionLocal() as db:
            result = purge_expired_invitation_documents(
                db,
                dry_run=args.dry_run,
                batch_size=args.batch_size,
            )
        mode = "Dry run" if args.dry_run else "Retention purge"
        print(
            f"{mode}: expired={result['expired_found']}, purged={result['purged']}, "
            f"content_deleted={result['content_deleted']}, "
            f"content_missing={result['content_missing']}, failed={result['failed']}."
        )
        if result["errors"]:
            for error in result["errors"]:
                print(f"Document record {error['document_id']}: {error['reason']}.")
        return 1 if result["failed"] else 0
    except (ValueError, OSError) as exc:
        parser.error(str(exc))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())

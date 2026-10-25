"""Purge tenant invitation documents whose configured retention period has ended.

Run after applying database migrations:
    python -m backend.scripts.purge_expired_invitation_documents --dry-run
    python -m backend.scripts.purge_expired_invitation_documents

DOCUMENT_STORAGE_ROOT must point at the same private directory used by the API.
This job never decrypts or prints document content. It removes expired encrypted
blobs, deletes their database metadata, and sweeps old unreferenced generated blobs
after a grace period. It writes minimal audit records without document details.
"""
import argparse
import hashlib
import os
import re
import stat
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
        raise ValueError("Stored document path resolves outside the confidef _eligible_orphan_blobs(
    root: Path,
    referenced_keys: set[str],
    cutoff: datetime,
    grace_hours: int,
) -> list[Path]:
    """Return only old, regular generated blobs that have no metadata reference."""
    oldest_mtime = cutoff.timestamp() - grace_hours * 60 * 60
    candidates: list[Path] = []
    for path in root.iterdir():
        if path.suffix != ".enc" or not _STORAGE_KEY.fullmatch(path.stem):
            continue
        if path.stem in referenced_keys:
            continue
        try:
            entry = path.lstat()
        except FileNotFoundError:
            continue
        except OSError:
            # A candidate that cannot be inspected should not be deleted.
            continue
        # Never follow or remove symlinks as part of orphan cleanup. Also ignore
        # anything that is not a regular encrypted-blob file.
        if stat.S_ISLNK(entry.st_mode) or not stat.S_ISREG(entry.st_mode):
            continue
        if entry.st_mtime <= oldest_mtime:
            candidates.append(path)
    return candidates


def purge_expired_invitation_documents(
    db: Session,
    storage_root: str | Path | None = None,
    *,
    now: datetime | None = None,
    dry_run: bool = False,
    batch_size: int = 500,
    orphan_grace_hours: int = 24,
) -> dict[str, Any]:
    """Delete expired encrypted blobs, expired metadata and old unreferenced blobs.

    The orphan grace period protects a file being written before its metadata
    transaction commits. If removing a referenced blob fails, the database record
    is retained so a later run can retry. If a referenced blob is already missing,
    metadata can still be removed because expired downloads are denied by the API.
    """
    if not 1 <= batch_size <= 5000:
        raise ValueError("batch_size must be between 1 and 5000.")
    if not 1 <= orphan_grace_hours <= 168:
        raise ValueError("orphan_grace_hours must be between 1 and 168.")
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
        "orphan_found": 0,
        "orphan_deleted": 0,
        "failed": 0,
        "errors": [],
    }

    if dry_run:
        stats["expired_found"] = len(list(db.scalars(
            select(m.InvitationDocument.id).where(m.InvitationDocument.retention_until <= cutoff)
        )))
        referenced_keys = set(db.scalars(select(m.InvitationDocument.storage_key)))
        stats["orphan_found"] = len(_eligible_orphan_blobs(
            root, referenced_keys, cutoff, orphan_grace_hours
        ))
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

    # The pre-registration delete endpoint, older deployments, or interrupted uploads
    # can leave encrypted files without metadata. Delete only generated .enc filenames
    # older than the grace period; never follow symlinks or touch arbitrary files.
    referenced_keys = set(db.scalars(select(m.InvitationDocument.storage_key)))
    orphan_candidates = _eligible_orphan_blobs(root, referenced_keys, cutoff, orphan_grace_hours)
    stats["orphan_found"] = len(orphan_candidates)
    for blob in orphan_candidates:
        try:
            # The key is randomly generated. Hash it for a stable audit reference without
            # exposing the storage key or actual path in the audit row.
            key_hash = hashlib.sha256(blob.stem.encode("ascii")).hexdigest()[:32]
            blob.unlink()
            db.add(m.AuditLog(
                actor_id="system-retention-purge",
                actor_role="system",
                entity_type="invitation_document_blob",
                entity_id=key_hash,
                action="orphan_blob_purge",
                old_value={},
                new_value={
                    "content_deleted": True,
                    "policy": "orphan_grace_period",
                    "grace_hours": orphan_grace_hours,
                },
            ))
            stats["orphan_deleted"] += 1
        except OSError:
            stats["failed"] += 1
            stats["errors"].append({"reason": "orphan_blob_delete_failed"})

    try:
        db.commit()
    except Exception:
        db.rollback()
        raise
    return stats
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
    parser.add_argument(
        "--orphan-grace-hours",
        type=int,
        default=int(os.getenv("DOCUMENT_ORPHAN_GRACE_HOURS", "24")),
        help="Minimum age for deleting unreferenced encrypted blobs (1–168 hours; defaults to 24).",
    )
    args = parser.parse_args()

    try:
        with SessionLocal() as db:
            result = purge_expired_invitation_documents(
                db,
                dry_run=args.dry_run,
                batch_size=args.batch_size,
                orphan_grace_hours=args.orphan_grace_hours,
            )
        mode = "Dry run" if args.dry_run else "Retention purge"
        print(
            f"{mode}: expired={result['expired_found']}, purged={result['purged']}, "
            f"content_deleted={result['content_deleted']}, content_missing={result['content_missing']}, "
            f"orphan_found={result['orphan_found']}, orphan_deleted={result['orphan_deleted']}, "
            f"failed={result['failed']}."
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

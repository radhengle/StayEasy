"""Admin-only review workflow for Owner workspace access requests."""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from . import models as m
from .database import get_db
from .security import require_roles

router = APIRouter(prefix="/admin", tags=["owner access"])


class OwnerAccessDecision(BaseModel):
    decision: str = Field(pattern="^(approve|reject)$")
    note: str = Field(default="", max_length=500)


@router.get("/owner-requests")
def list_owner_access_requests(
    status: str = Query(default="pending_approval", pattern="^(pending_approval|approved|rejected|all)$"),
    actor: dict = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    """List owner access requests; only administrators can see applicant identity and review notes."""
    stmt = (
        select(m.User, m.UserCredential)
        .join(m.UserCredential, m.UserCredential.user_id == m.User.id)
        .where(m.UserCredential.requested_workspace == "owner")
    )
    if status != "all":
        stmt = stmt.where(m.UserCredential.owner_access_status == status)
    rows = db.execute(
        stmt.order_by(m.UserCredential.owner_access_requested_at, m.User.created_at)
    ).all()
    return [{
        "id": user.id,
        "display_name": user.display_name,
        "email": user.email,
        "email_verified": credential.email_verified_at is not None,
        "account_active": user.is_active,
        "role": user.role,
        "requested_workspace": credential.requested_workspace,
        "owner_access_status": "active" if user.role == "owner" else credential.owner_access_status,
        "requested_at": credential.owner_access_requested_at.isoformat() if credential.owner_access_requested_at else None,
        "reviewed_at": credential.owner_access_reviewed_at.isoformat() if credential.owner_access_reviewed_at else None,
        "review_note": credential.owner_access_review_note,
    } for user, credential in rows]


@router.post("/owner-requests/{user_id}/decision")
def decide_owner_access_request(
    user_id: str,
    body: OwnerAccessDecision,
    actor: dict = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    user = db.get(m.User, user_id)
    credential = db.get(m.UserCredential, user_id)
    if user is None or credential is None or credential.requested_workspace != "owner":
        raise HTTPException(status_code=404, detail="Owner access request not found")
    if credential.owner_access_status != "pending_approval" or user.role != "customer":
        raise HTTPException(status_code=409, detail="This owner access request is no longer pending")
    if not user.is_active:
        raise HTTPException(status_code=409, detail="Suspended accounts cannot be approved")
    if body.decision == "approve" and credential.email_verified_at is None:
        raise HTTPException(status_code=422, detail="Verify the applicant's email before approving Owner access")

    old = {
        "role": user.role,
        "owner_access_status": credential.owner_access_status,
        "review_note": credential.owner_access_review_note,
    }
    now = datetime.now(timezone.utc)
    if body.decision == "approve":
        user.role = "owner"
        credential.owner_access_status = "approved"
        # Invalidate any customer token. The newly approved owner must sign in again
        # to receive a token carrying the server-authorized Owner role.
        credential.auth_version += 1
    else:
        credential.owner_access_status = "rejected"
    credential.owner_access_reviewed_at = now
    credential.owner_access_review_note = body.note.strip()[:500]

    db.add(m.AuditLog(
        actor_id=str(actor["subject"]),
        actor_role="admin",
        entity_type="user",
        entity_id=user.id,
        action="owner_access_decision",
        old_value=old,
        new_value={
            "role": user.role,
            "owner_access_status": credential.owner_access_status,
            "review_note": credential.owner_access_review_note,
        },
    ))
    db.commit()
    db.refresh(user)
    db.refresh(credential)
    return {
        "id": user.id,
        "display_name": user.display_name,
        "email": user.email,
        "role": user.role,
        "owner_access_status": "active" if user.role == "owner" else credential.owner_access_status,
        "reviewed_at": credential.owner_access_reviewed_at.isoformat() if credential.owner_access_reviewed_at else None,
        "review_note": credential.owner_access_review_note,
    }

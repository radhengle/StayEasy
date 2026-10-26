"""Secure Razorpay payments for rent charges.

All amounts are integer paise. The client never chooses an amount or establishes a
paid status. Only a verified checkout signature plus a server-side captured-payment
lookup, or a signed webhook, can post a payment to the ledger.
"""
import base64
import hashlib
import hmac
import json
import os
from datetime import datetime, timezone
from decimal import Decimal, ROUND_HALF_UP
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

from fastapi import APIRouter, Depends, Header, HTTPException, Request as FastAPIRequest
from pydantic import BaseModel, Field
from sqlalchemy import and_, or_, select, func
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from . import models as m
from .database import get_db
from .security import require_roles

router = APIRouter(tags=["payments"])
ACTIVE_ORDER_STATUSES = ("creating", "created", "authorized", "unknown")
PAID_LEDGER_STATUSES = ("paid", "settled", "closed", "recorded")
BILLABLE_CATEGORIES = ("rent_charge", "deposit", "food", "utility", "late_fee")
RAZORPAY_API = "https://api.razorpay.com/v1"


class GatewayRequestError(RuntimeError):
    def __init__(self, message: str, ambiguous: bool = False):
        super().__init__(message)
        self.ambiguous = ambiguous


class VerifyPaymentRequest(BaseModel):
    payment_attempt_id: int = Field(gt=0)
    razorpay_payment_id: str = Field(min_length=4, max_length=100, pattern=r"^[A-Za-z0-9_-]+$")
    razorpay_signature: str = Field(min_length=64, max_length=64, pattern=r"^[A-Fa-f0-9]{64}$")


class RefundRequest(BaseModel):
    amount_paise: int | None = Field(default=None, gt=0)
    reason: str = Field(default="", max_length=500)

class OwnerSettlementAccountLinkRequest(BaseModel):
    linked_account_id: str = Field(min_length=8, max_length=100, pattern=r"^acc_[A-Za-z0-9]+$")
    status: str = Field(default="active", pattern=r"^(pending|active|suspended)$")


def _settlement_mode() -> str:
    app_env = os.getenv("APP_ENV", "development").strip().lower()
    raw = os.getenv("PAYMENT_SETTLEMENT_MODE", "").strip().lower()
    if raw:
        mode = raw
    else:
        # Production defaults to owner routing so rent cannot accidentally remain
        # in StayEasy's primary merchant balance without an explicit decision.
        mode = "owner_route" if app_env in {"staging", "production"} else "platform"
    if mode not in {"platform", "owner_route"}:
        raise HTTPException(503, "Payment settlement mode is invalid. Payments are disabled until it is corrected.")
    platform_approved = os.getenv("ALLOW_PLATFORM_SETTLEMENT_IN_PRODUCTION", "").strip().lower() in {"1", "true", "yes", "on"}
    if app_env in {"staging", "production"} and mode == "platform" and not platform_approved:
        raise HTTPException(503, "Platform-only settlement is not explicitly approved in this environment. Configure owner_route or obtain the required settlement approval.")
    return mode


def _razorpay_route_enabled() -> bool:
    return os.getenv("RAZORPAY_ROUTE_ENABLED", "").strip().lower() in {"1", "true", "yes", "on"}


def _credentials() -> tuple[str, str]:
    key_id = os.getenv("RAZORPAY_KEY_ID", "").strip()
    key_secret = os.getenv("RAZORPAY_KEY_SECRET", "").strip()
    if not key_id or not key_secret:
        raise HTTPException(503, "Online payments are not configured. No money has been collected.")
    return key_id, key_secret


def _gateway_request(method: str, path: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
    key_id, key_secret = _credentials()
    body = json.dumps(payload, separators=(",", ":"), ensure_ascii=False).encode("utf-8") if payload is not None else None
    auth = base64.b64encode(f"{key_id}:{key_secret}".encode("utf-8")).decode("ascii")
    req = Request(
        f"{RAZORPAY_API}{path}",
        data=body,
        method=method,
        headers={"Authorization": f"Basic {auth}", "Content-Type": "application/json", "Accept": "application/json"},
    )
    try:
        with urlopen(req, timeout=12) as response:
            raw = response.read()
    except HTTPError as exc:
        # A 4xx is a definite rejection; 5xx may have happened after the provider
        # accepted the request, so callers must preserve an unknown/reconciliation state.
        raise GatewayRequestError("Payment provider rejected the request." if exc.code < 500
                                  else "Payment provider response is indeterminate.", ambiguous=exc.code >= 500) from exc
    except (URLError, TimeoutError, OSError) as exc:
        raise GatewayRequestError("Payment provider response is indeterminate.", ambiguous=True) from exc
    try:
        parsed = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise GatewayRequestError("Payment provider returned an invalid response.", ambiguous=True) from exc
    if not isinstance(parsed, dict):
        raise GatewayRequestError("Payment provider returned an invalid response.", ambiguous=True)
    return parsed


def create_razorpay_order(amount_paise: int, receipt: str, notes: dict[str, str]) -> dict[str, Any]:
    return _gateway_request("POST", "/orders", {
        "amount": amount_paise,
        "currency": "INR",
        "receipt": receipt[:40],
        "partial_payment": False,
        "notes": notes,
    })


def create_razorpay_order_with_transfer(
    amount_paise: int,
    receipt: str,
    notes: dict[str, str],
    transfer: dict[str, Any],
) -> dict[str, Any]:
    """Create an INR order with one predeclared Route transfer to the verified owner account."""
    return _gateway_request("POST", "/orders", {
        "amount": amount_paise,
        "currency": "INR",
        "receipt": receipt[:40],
        "partial_payment": False,
        "notes": notes,
        "transfers": [transfer],
    })


def fetch_razorpay_order(order_id: str) -> dict[str, Any]:
    return _gateway_request("GET", f"/orders/{quote(order_id, safe='')}")


def fetch_razorpay_transfers_for_settlement(settlement_id: str) -> dict[str, Any]:
    """Fetch the canonical Route transfer collection for an owner settlement webhook."""
    return _gateway_request("GET", f"/transfers?recipient_settlement_id={quote(settlement_id, safe='')}")


def fetch_razorpay_orders_by_receipt(receipt: str) -> dict[str, Any]:
    """Find an order after POST /orders timed out before its ID was persisted."""
    return _gateway_request("GET", f"/orders?receipt={quote(receipt, safe='')}&count=100")


def fetch_razorpay_payment(payment_id: str) -> dict[str, Any]:
    return _gateway_request("GET", f"/payments/{payment_id}")


def fetch_razorpay_order_payments(order_id: str) -> dict[str, Any]:
    """Recover payment attempts after the Android process dies before saving its callback."""
    return _gateway_request("GET", f"/orders/{order_id}/payments")


def create_razorpay_refund(
    payment_id: str,
    amount_paise: int,
    notes: dict[str, str],
    reverse_all: bool = False,
) -> dict[str, Any]:
    payload: dict[str, Any] = {"amount": amount_paise, "notes": notes}
    if reverse_all:
        # Route supports automatic linked-account transfer reversal before the
        # refund for a payment sent to a single linked account.
        payload["reverse_all"] = True
    return _gateway_request("POST", f"/payments/{payment_id}/refund", payload)


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


def _amount_paise(entry: m.LedgerEntry) -> int:
    return int(entry.amount_paise if entry.amount_paise is not None else entry.amount * 100)


def _paid_for_charge(db: Session, charge: m.LedgerEntry) -> int:
    # New gateway payments are linked directly to the charge. A legacy/manual
    # payment is matched only when it has a billable period, the same resident or
    # reservation, and a paid status; this prevents unrelated ledger payments from
    # being silently netted against a new invoice.
    conditions = [m.LedgerEntry.related_charge_id == charge.id]
    if charge.billing_period:
        legacy_scope = [
            m.LedgerEntry.related_charge_id.is_(None),
            m.LedgerEntry.property_id == charge.property_id,
            m.LedgerEntry.billing_period == charge.billing_period,
        ]
        if charge.resident_stay_id is not None:
            legacy_scope.append(m.LedgerEntry.resident_stay_id == charge.resident_stay_id)
        elif charge.reservation_id is not None:
            legacy_scope.append(m.LedgerEntry.reservation_id == charge.reservation_id)
        else:
            legacy_scope.append(m.LedgerEntry.id == -1)
        conditions.append(and_(*legacy_scope))
    rows = db.scalars(select(m.LedgerEntry).where(
        m.LedgerEntry.category == "payment",
        m.LedgerEntry.status.in_(PAID_LEDGER_STATUSES),
        or_(*conditions),
    ))
    received = sum(_amount_paise(row) for row in rows)
    refunds = db.scalars(select(m.LedgerEntry).where(
        m.LedgerEntry.category == "refund",
        m.LedgerEntry.status.in_(PAID_LEDGER_STATUSES),
        m.LedgerEntry.related_charge_id == charge.id,
    ))
    refunded = sum(_amount_paise(row) for row in refunds)
    return max(0, received - refunded)


def _amount_due(db: Session, charge: m.LedgerEntry) -> int:
    return max(0, _amount_paise(charge) - _paid_for_charge(db, charge))


def _customer_owns_charge(db: Session, customer_id: str, charge: m.LedgerEntry) -> bool:
    linked = False
    if charge.reservation_id is not None:
        reservation = db.get(m.Reservation, charge.reservation_id)
        if not reservation or reservation.customer_id != customer_id:
            return False
        if reservation.status in {"cancelled", "expired"}:
            return False
        linked = True
    if charge.resident_stay_id is not None:
        stay = db.get(m.ResidentStay, charge.resident_stay_id)
        if not stay or stay.resident_id != customer_id or stay.status not in {"active", "notice_given"}:
            return False
        linked = True
    return linked


def _require_finance_manager(db: Session, actor: dict[str, Any], property_id: int) -> m.Property:
    prop = db.get(m.Property, property_id)
    if prop is None:
        raise HTTPException(404, "Property not found")
    if actor["role"] == "admin":
        return prop
    if actor["role"] == "owner" and prop.owner_id == str(actor["subject"]):
        return prop
    assignment = db.scalar(select(m.StaffAssignment).where(
        m.StaffAssignment.staff_user_id == str(actor["subject"]),
        m.StaffAssignment.property_id == property_id,
        m.StaffAssignment.status == "active",
    ))
    if not assignment or not {"finance.manage", "*"}.intersection(set(assignment.permissions or [])):
        raise HTTPException(403, "You do not have finance-management access for this property")
    return prop


def _owner_route_account_for_charge(db: Session, charge: m.LedgerEntry) -> m.OwnerSettlementAccount | None:
    if _settlement_mode() != "owner_route":
        return None
    if not _razorpay_route_enabled():
        raise HTTPException(503, "Owner settlement routing is not enabled with Razorpay. Online rent payments are paused.")
    prop = db.get(m.Property, charge.property_id)
    if prop is None:
        raise HTTPException(404, "Property for this charge was not found.")
    account = db.scalar(select(m.OwnerSettlementAccount).where(
        m.OwnerSettlementAccount.owner_id == prop.owner_id,
        m.OwnerSettlementAccount.status == "active",
    ))
    if account is None:
        raise HTTPException(503, "This property's owner has not completed approved payment settlement setup. No payment order was created.")
    return account


def _assert_existing_settlement_mode(db: Session, attempt: m.PaymentAttempt) -> None:
    mode = _settlement_mode()
    recorded_mode = str(attempt.settlement_mode or "platform")
    if recorded_mode != mode:
        raise HTTPException(409, "Settlement configuration changed after this order was created. Contact support; do not start another payment.")
    transfer = db.scalar(select(m.PaymentTransfer).where(m.PaymentTransfer.payment_attempt_id == attempt.id))
    if recorded_mode == "owner_route":
        if not _razorpay_route_enabled() or transfer is None:
            raise HTTPException(409, "Owner settlement setup is missing or disabled for this existing order. Finance reconciliation is required.")
        if transfer.status not in {"created", "processed", "pending"}:
            raise HTTPException(409, "Owner settlement is not confirmed for this order. Finance reconciliation is required.")
        account = db.scalar(select(m.OwnerSettlementAccount).where(m.OwnerSettlementAccount.owner_id == transfer.owner_id))
        if account is None or account.status != "active" or account.linked_account_id != transfer.linked_account_id:
            raise HTTPException(409, "Owner settlement destination changed after this order was created. Finance reconciliation is required.")
    elif transfer is not None:
        raise HTTPException(409, "This order was created with owner settlement routing. Restore its original settlement configuration before retrying.")


def _record_route_transfer_from_order(
    db: Session,
    attempt: m.PaymentAttempt,
    order: dict[str, Any],
) -> bool:
    transfer = db.scalar(select(m.PaymentTransfer).where(m.PaymentTransfer.payment_attempt_id == attempt.id))
    if transfer is None:
        return True
    provider_order_id = str(order.get("id") or "")
    items = order.get("transfers")
    if not provider_order_id or not isinstance(items, list) or len(items) != 1:
        return False
    item = items[0]
    if not isinstance(item, dict):
        return False
    transfer_id = str(item.get("id") or "")
    recipient = str(item.get("recipient") or item.get("account") or "")
    transfer_status = str(item.get("transfer_status") or item.get("status") or "").lower()
    if (
        not transfer_id.startswith("trf_")
        or recipient != transfer.linked_account_id
        or item.get("amount") != transfer.amount_paise
        or str(item.get("currency") or "") != transfer.currency
        or transfer_status != "created"
    ):
        return False
    transfer.provider_order_id = provider_order_id
    transfer.provider_transfer_id = transfer_id
    recipient_settlement_id = str(item.get("recipient_settlement_id") or "")
    transfer.recipient_settlement_id = recipient_settlement_id or None
    transfer.status = transfer_status
    settlement_status = str(item.get("settlement_status") or "pending").lower()
    transfer.settlement_status = settlement_status if settlement_status in {"pending", "on_hold", "settled"} else "pending"
    transfer.failure_code = ""
    transfer.updated_at = _utcnow()
    return True


def _attempt_payload(attempt: m.PaymentAttempt) -> dict[str, Any]:
    return {
        "id": attempt.id,
        "charge_id": attempt.charge_entry_id,
        "amount_paise": attempt.amount_paise,
        "currency": attempt.currency,
        "provider": attempt.provider,
        "settlement_mode": attempt.settlement_mode or "platform",
        "status": attempt.status,
        "provider_order_id": attempt.provider_order_id,
        "provider_payment_id": attempt.provider_payment_id,
        "payment_method": attempt.payment_method,
        "failure_code": attempt.failure_code,
        "captured_at": attempt.captured_at.isoformat() if attempt.captured_at else None,
        "created_at": attempt.created_at.isoformat() if attempt.created_at else None,
    }


def _checkout_payload(attempt: m.PaymentAttempt) -> dict[str, Any]:
    key_id, _ = _credentials()
    if not attempt.provider_order_id:
        raise HTTPException(409, "The payment order is not ready yet. Retry the same request shortly.")
    return {
        **_attempt_payload(attempt),
        "gateway": "razorpay",
        "key_id": key_id,
        "checkout_order_id": attempt.provider_order_id,
        "checkout_amount": attempt.amount_paise,
        "checkout_currency": attempt.currency,
    }


@router.get("/payments/owner/settlement-status")
def owner_settlement_status(
    actor: dict = Depends(require_roles("owner")),
    db: Session = Depends(get_db),
):
    account = db.scalar(select(m.OwnerSettlementAccount).where(m.OwnerSettlementAccount.owner_id == str(actor["subject"])))
    return {
        "mode": _settlement_mode(),
        "route_enabled": _razorpay_route_enabled(),
        "account_status": account.status if account else "not_configured",
        "linked_account_configured": bool(account and account.status == "active"),
        "note": "Route accounts must be onboarded and KYC-approved in Razorpay before an administrator links the account here.",
    }


@router.get("/payments/admin/owner-settlement-accounts")
def list_owner_settlement_accounts(
    actor: dict = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    rows = list(db.scalars(select(m.OwnerSettlementAccount).order_by(m.OwnerSettlementAccount.owner_id.asc())))
    return {"accounts": [{
        "owner_id": row.owner_id,
        "linked_account_id": row.linked_account_id,
        "status": row.status,
        "linked_by": row.linked_by,
        "linked_at": row.linked_at.isoformat() if row.linked_at else None,
    } for row in rows]}


@router.put("/payments/admin/owner-settlement-accounts/{owner_id}")
def link_owner_settlement_account(
    owner_id: str,
    body: OwnerSettlementAccountLinkRequest,
    actor: dict = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    owner = db.get(m.User, owner_id)
    if owner is None or owner.role != "owner" or not owner.is_active:
        raise HTTPException(404, "Active approved owner account not found.")
    if body.status == "active" and not _razorpay_route_enabled():
        raise HTTPException(503, "Enable Razorpay Route only after the provider has approved the Route feature and linked account.")
    existing_for_provider = db.scalar(select(m.OwnerSettlementAccount).where(
        m.OwnerSettlementAccount.linked_account_id == body.linked_account_id,
        m.OwnerSettlementAccount.owner_id != owner_id,
    ))
    if existing_for_provider:
        raise HTTPException(409, "This Razorpay linked account is already assigned to another owner.")
    row = db.scalar(select(m.OwnerSettlementAccount).where(m.OwnerSettlementAccount.owner_id == owner_id).with_for_update())
    old_values = {
        "linked_account_id": row.linked_account_id,
        "status": row.status,
    } if row is not None else {}
    action = "owner_settlement_account_updated" if row is not None else "owner_settlement_account_linked"
    if row is None:
        row = m.OwnerSettlementAccount(
            owner_id=owner_id,
            linked_account_id=body.linked_account_id,
            status=body.status,
            linked_by=str(actor["subject"]),
            linked_at=_utcnow() if body.status == "active" else None,
        )
        db.add(row)
    else:
        if row.linked_account_id != body.linked_account_id:
            active_transfer = db.scalar(select(m.PaymentTransfer).where(
                m.PaymentTransfer.owner_id == owner_id,
                m.PaymentTransfer.status.in_(("creating", "created", "pending", "unknown")),
            ))
            if active_transfer:
                raise HTTPException(409, "Owner has unsettled transfer records. Reconcile them before changing the linked account.")
        row.linked_account_id = body.linked_account_id
        row.status = body.status
        row.linked_by = str(actor["subject"])
        row.linked_at = _utcnow() if body.status == "active" else None
        row.updated_at = _utcnow()
    db.add(m.AuditLog(
        actor_id=str(actor["subject"]),
        actor_role=str(actor["role"]),
        entity_type="owner_settlement_account",
        entity_id=owner_id,
        action=action,
        old_value=old_values,
        new_value={
            "linked_account_id": row.linked_account_id,
            "status": row.status,
        },
    ))
    try:
        db.commit()
        db.refresh(row)
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(409, "Owner settlement account conflicts with an existing provider account link.") from exc
    return {
        "owner_id": row.owner_id,
        "linked_account_id": row.linked_account_id,
        "status": row.status,
        "linked_at": row.linked_at.isoformat() if row.linked_at else None,
    }


def _find_attempt_for_customer(db: Session, attempt_id: int, customer_id: str) -> m.PaymentAttempt:
    attempt = db.get(m.PaymentAttempt, attempt_id)
    if not attempt or attempt.customer_id != customer_id:
        raise HTTPException(404, "Payment attempt not found")
    return attempt


def _recover_indeterminate_order_creation(db: Session, attempt: m.PaymentAttempt) -> bool:
    """Resolve a timed-out order creation by its stable, unique Razorpay receipt.

    Returns True only when a matching, still-created order is safe to launch in
    Checkout. If the order exists in another state, persist its ID and require
    payment-status reconciliation instead of silently creating a second order.
    """
    receipt = f"se-{attempt.id}"
    try:
        collection = fetch_razorpay_orders_by_receipt(receipt)
    except GatewayRequestError as exc:
        attempt.status = "unknown"
        attempt.failure_code = "provider_order_lookup_unavailable"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(
            503,
            "Razorpay order reconciliation is temporarily unavailable. Keep this payment attempt and retry status checking; do not start another payment.",
        ) from exc

    items = collection.get("items")
    if not isinstance(items, list):
        attempt.status = "unknown"
        attempt.failure_code = "provider_order_lookup_invalid_response"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(503, "Razorpay returned an invalid order lookup response. Do not start another payment.")

    matching = [
        item for item in items
        if isinstance(item, dict) and str(item.get("receipt") or "") == receipt
    ]
    if len(matching) > 1:
        attempt.status = "unknown"
        attempt.failure_code = "multiple_provider_orders_for_receipt"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(409, "Multiple Razorpay orders share this receipt. Finance reconciliation is required.")

    if not matching:
        # Absence from one lookup is not sufficient to prove the original create
        # request failed; preserve the attempt and block any new order.
        return False

    order = matching[0]
    provider_order_id = str(order.get("id") or "")
    route_transfer = db.scalar(select(m.PaymentTransfer).where(m.PaymentTransfer.payment_attempt_id == attempt.id))
    if route_transfer is not None:
        try:
            # The collection lookup may omit nested transfer data; fetch the canonical order
            # before relaunching Checkout so the owner destination is independently validated.
            order = fetch_razorpay_order(provider_order_id) if provider_order_id else {}
        except GatewayRequestError as exc:
            attempt.status = "unknown"
            attempt.failure_code = "provider_route_order_lookup_unavailable"
            route_transfer.status = "unknown"
            route_transfer.failure_code = "provider_route_order_lookup_unavailable"
            attempt.updated_at = _utcnow()
            route_transfer.updated_at = _utcnow()
            db.commit()
            raise HTTPException(503, "Owner settlement details are not yet verifiable. Do not start another payment.") from exc

    if (not provider_order_id or str(order.get("id") or provider_order_id) != provider_order_id
            or (order.get("receipt") is not None and str(order.get("receipt")) != receipt)
            or order.get("amount") != attempt.amount_paise
            or str(order.get("currency") or "") != attempt.currency):
        attempt.status = "unknown"
        attempt.failure_code = "provider_order_amount_or_currency_mismatch"
        if route_transfer is not None:
            route_transfer.status = "unknown"
            route_transfer.failure_code = "provider_route_order_mismatch"
            route_transfer.updated_at = _utcnow()
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(409, "The located Razorpay order does not match the stored amount/currency. Finance reconciliation is required.")

    if route_transfer is not None and not _record_route_transfer_from_order(db, attempt, order):
        attempt.provider_order_id = provider_order_id
        attempt.status = "unknown"
        attempt.failure_code = "route_transfer_missing_or_mismatched"
        route_transfer.provider_order_id = provider_order_id
        route_transfer.status = "unknown"
        route_transfer.failure_code = "route_transfer_missing_or_mismatched"
        route_transfer.updated_at = _utcnow()
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(409, "The owner transfer on this Razorpay order could not be verified. Finance reconciliation is required; Checkout was not relaunched.")

    attempt.provider_order_id = provider_order_id
    if str(order.get("status") or "").lower() == "created":
        attempt.status = "created"
        attempt.failure_code = ""
        checkout_ready = True
    else:
        attempt.status = "unknown"
        attempt.failure_code = "provider_order_state_requires_reconciliation"
        checkout_ready = False
    attempt.updated_at = _utcnow()
    db.commit()
    db.refresh(attempt)
    return checkout_ready


def _set_charge_paid_if_settled(db: Session, charge: m.LedgerEntry) -> None:
    if _amount_due(db, charge) == 0 and charge.status in {"open", "pending", "partial"}:
        charge.status = "paid"


def _settle_captured_payment(
    db: Session,
    attempt: m.PaymentAttempt,
    provider_payment: dict[str, Any],
) -> m.PaymentAttempt:
    provider_order_id = str(provider_payment.get("order_id") or "")
    provider_payment_id = str(provider_payment.get("id") or "")
    amount = provider_payment.get("amount")
    currency = str(provider_payment.get("currency") or "")
    status_value = str(provider_payment.get("status") or "").lower()
    is_captured = status_value == "captured" and provider_payment.get("captured") is not False
    if not attempt.provider_order_id or provider_order_id != attempt.provider_order_id:
        raise HTTPException(409, "The payment does not belong to this StayEasy order.")
    if not provider_payment_id or amount != attempt.amount_paise or currency != attempt.currency:
        raise HTTPException(409, "The payment amount or currency does not match the server-created order.")
    if not is_captured:
        raise HTTPException(409, "The provider has not confirmed a captured payment yet.")
    existing = db.scalar(select(m.PaymentAttempt).where(
        m.PaymentAttempt.provider_payment_id == provider_payment_id
    ))
    if existing is not None and existing.id != attempt.id:
        raise HTTPException(409, "This provider payment is already linked to another StayEasy payment.")
    if attempt.status in {"captured", "captured_unapplied", "refunded"}:
        if attempt.provider_payment_id != provider_payment_id:
            raise HTTPException(409, "This payment order has already been settled with a different provider payment.")
        return attempt

    charge = db.scalar(select(m.LedgerEntry).where(
        m.LedgerEntry.id == attempt.charge_entry_id
    ).with_for_update())
    if not charge:
        raise HTTPException(409, "The linked rent charge no longer exists.")
    payment_entry = db.scalar(select(m.LedgerEntry).where(m.LedgerEntry.payment_attempt_id == attempt.id))
    if payment_entry is None:
        remaining = _amount_due(db, charge)
        rounded_rupees = int((Decimal(attempt.amount_paise) / Decimal(100)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))
        # A second open order might have captured after another device settled this
        # charge. Preserve the actual gateway movement and flag it for reconciliation.
        unapplied = attempt.amount_paise > remaining
        payment_entry = m.LedgerEntry(
            property_id=charge.property_id,
            resident_stay_id=charge.resident_stay_id,
            reservation_id=charge.reservation_id,
            related_charge_id=charge.id,
            payment_attempt_id=attempt.id,
            category="payment",
            amount=rounded_rupees,
            amount_paise=attempt.amount_paise,
            currency=attempt.currency,
            billing_period=charge.billing_period,
            due_date=charge.due_date,
            paid_at=_utcnow(),
            payment_method="online",
            payment_reference=provider_payment_id,
            provider_status="provider_confirmed",
            description=f"Verified online payment for charge #{charge.id}.",
            status="paid",
            created_by=attempt.customer_id,
        )
        db.add(payment_entry)
        short_paid = attempt.amount_paise < remaining
        if unapplied:
            attempt.status = "captured_unapplied"
            attempt.failure_code = "overpayment_reconciliation_required"
            charge.status = "paid"
        else:
            attempt.status = "captured"
            attempt.failure_code = "charge_balance_changed_after_order" if short_paid else ""
            charge.status = "partial" if short_paid else "paid"
        # If the balance changed after order creation, an underpaid charge remains partial.
    attempt.provider_payment_id = provider_payment_id
    attempt.payment_method = str(provider_payment.get("method") or "online")[:32]
    attempt.captured_at = _utcnow()
    attempt.updated_at = _utcnow()
    db.flush()
    return attempt


def _set_payment_failed(db: Session, attempt: m.PaymentAttempt, provider_payment: dict[str, Any]) -> None:
    if attempt.status in {"captured", "captured_unapplied", "refunded"}:
        return
    attempt.provider_payment_id = str(provider_payment.get("id") or "") or None
    err = provider_payment.get("error_code") or provider_payment.get("error_reason") or "payment_failed"
    attempt.failure_code = str(err)[:80]
    attempt.status = "failed"
    attempt.updated_at = _utcnow()


def _refund_payload(refund: m.PaymentRefund) -> dict[str, Any]:
    return {
        "id": refund.id,
        "payment_attempt_id": refund.payment_attempt_id,
        "amount_paise": refund.amount_paise,
        "status": refund.status,
        "provider_refund_id": refund.provider_refund_id,
        "reason": refund.reason,
        "failure_code": refund.failure_code,
        "created_at": refund.created_at.isoformat() if refund.created_at else None,
        "processed_at": refund.processed_at.isoformat() if refund.processed_at else None,
    }


def _refresh_refund_status(db: Session, refund: m.PaymentRefund, provider_status: str) -> None:
    if provider_status == "processed":
        refund.status = "processed"
        refund.processed_at = _utcnow()
        db.flush()
        attempt = db.get(m.PaymentAttempt, refund.payment_attempt_id)
        if attempt:
            total_refunded = db.scalar(select(func.coalesce(func.sum(m.PaymentRefund.amount_paise), 0)).where(
                m.PaymentRefund.payment_attempt_id == attempt.id,
                m.PaymentRefund.status == "processed",
            )) or 0
            # Include this refund in the sum before commit because autoflush is on.
            if total_refunded >= attempt.amount_paise:
                attempt.status = "refunded"
            elif attempt.status == "refunded":
                attempt.status = "captured"

            charge = db.get(m.LedgerEntry, attempt.charge_entry_id)
            if charge and refund.provider_refund_id:
                reversal = db.scalar(select(m.LedgerEntry).where(
                    m.LedgerEntry.category == "refund",
                    m.LedgerEntry.payment_reference == refund.provider_refund_id,
                ))
                if reversal is None:
                    refund_rupees = int((Decimal(refund.amount_paise) / Decimal(100)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))
                    db.add(m.LedgerEntry(
                        property_id=charge.property_id,
                        resident_stay_id=charge.resident_stay_id,
                        reservation_id=charge.reservation_id,
                        related_charge_id=charge.id,
                        category="refund",
                        amount=refund_rupees,
                        amount_paise=refund.amount_paise,
                        currency=attempt.currency,
                        billing_period=charge.billing_period,
                        due_date=charge.due_date,
                        paid_at=_utcnow(),
                        payment_method="online_refund",
                        payment_reference=refund.provider_refund_id,
                        provider_status="provider_confirmed",
                        description=f"Verified online refund for charge #{charge.id}.",
                        status="paid",
                        created_by=refund.requested_by,
                    ))
                    db.flush()
                net_paid = _paid_for_charge(db, charge)
                charge.status = "paid" if _amount_due(db, charge) == 0 else ("partial" if net_paid > 0 else "open")
    elif provider_status == "failed":
        refund.status = "failed"
        refund.failure_code = "provider_refund_failed"
    else:
        refund.status = "pending"


@router.get("/payments/charges")
def list_my_payable_charges(
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    subject = str(actor["subject"])
    charges = list(db.scalars(select(m.LedgerEntry).where(
        m.LedgerEntry.category.in_(BILLABLE_CATEGORIES),
        m.LedgerEntry.status.in_(("open", "pending", "partial")),
    ).order_by(m.LedgerEntry.due_date.asc(), m.LedgerEntry.id.asc()).limit(500)))
    results = []
    for charge in charges:
        if not _customer_owns_charge(db, subject, charge):
            continue
        due = _amount_due(db, charge)
        if due <= 0:
            continue
        prop = db.get(m.Property, charge.property_id)
        results.append({
            "id": charge.id,
            "property_id": charge.property_id,
            "property_name": prop.name if prop else f"Property #{charge.property_id}",
            "category": charge.category,
            "billing_period": charge.billing_period,
            "due_date": charge.due_date.isoformat() if charge.due_date else None,
            "amount_paise": _amount_paise(charge),
            "amount_due_paise": due,
            "currency": charge.currency or "INR",
            "status": charge.status,
        })
    return {"charges": results}


@router.post("/payments/orders", status_code=201)
def create_payment_order(
    body: dict[str, int],
    idempotency_key: str = Header(alias="Idempotency-Key", min_length=8, max_length=128),
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    charge_id = body.get("charge_id")
    if not isinstance(charge_id, int) or isinstance(charge_id, bool) or charge_id <= 0:
        raise HTTPException(422, "Provide a positive charge_id.")
    subject = str(actor["subject"])
    # Fail closed before creating any local attempt if the provider is not configured.
    _credentials()

    existing = db.scalar(select(m.PaymentAttempt).where(
        m.PaymentAttempt.customer_id == subject,
        m.PaymentAttempt.idempotency_key == idempotency_key,
    ))
    if existing:
        if existing.charge_entry_id != charge_id:
            raise HTTPException(409, "This idempotency key was already used for a different charge.")
        if existing.status in {"created", "authorized"}:
            _assert_existing_settlement_mode(db, existing)
            return _checkout_payload(existing)
        if existing.status == "unknown":
            if not existing.provider_order_id:
                if _recover_indeterminate_order_creation(db, existing):
                    return _checkout_payload(existing)
                if existing.provider_order_id:
                    raise HTTPException(409, "The existing Razorpay order was found but is not safe to relaunch. Check payment status; do not start another payment.")
                raise HTTPException(409, "Razorpay has not yet returned a matching order for this attempt. Keep using the same request key and do not create another payment.")
            raise HTTPException(409, "Payment status needs provider reconciliation. Check payment history; do not start another payment.")
        if existing.status == "creating":
            raise HTTPException(409, "Payment order creation is still in progress. Retry this same request shortly; do not start another payment.")
        raise HTTPException(409, "This request already has a terminal result. Use a new idempotency key to retry.")

    charge = db.scalar(select(m.LedgerEntry).where(m.LedgerEntry.id == charge_id).with_for_update())
    if not charge or charge.category not in BILLABLE_CATEGORIES:
        raise HTTPException(404, "Payable charge not found.")
    if not _customer_owns_charge(db, subject, charge):
        raise HTTPException(404, "Payable charge not found.")
    if charge.status not in {"open", "pending", "partial"}:
        raise HTTPException(409, "This charge is not open for online payment.")
    due = _amount_due(db, charge)
    if due < 100:
        raise HTTPException(409, "The remaining charge is below Razorpay's INR 1.00 minimum or has already been settled.")
    active = db.scalar(select(m.PaymentAttempt).where(
        m.PaymentAttempt.charge_entry_id == charge.id,
        m.PaymentAttempt.status.in_(ACTIVE_ORDER_STATUSES),
    ).with_for_update())
    if active:
        # A client can lose its in-memory idempotency key after an app restart.
        # Recover an ambiguous order by its stable provider receipt before deciding
        # whether a new request can safely launch Checkout.
        if active.status == "unknown" and not active.provider_order_id:
            if _recover_indeterminate_order_creation(db, active):
                return _checkout_payload(active)
            if active.provider_order_id:
                raise HTTPException(409, "The existing Razorpay order was found but is not safe to relaunch. Check payment status; do not start another payment.")
            raise HTTPException(409, "Razorpay has not yet returned a matching order for this attempt. Keep using this charge and do not create another payment.")
        # A client may time out after the order was successfully created and retry
        # with a fresh idempotency key. Reuse the still-open, provider-backed order
        # rather than forcing a second order or a dead-end 409.
        if active.status == "created" and active.provider_order_id:
            _assert_existing_settlement_mode(db, active)
            return _checkout_payload(active)
        if active.status == "authorized":
            raise HTTPException(409, "A payment is authorized and awaiting capture. Check payment history; do not start another payment.")
        if active.status == "unknown":
            raise HTTPException(409, "Payment status needs provider reconciliation. Do not start another payment.")
        raise HTTPException(409, "A payment order is still being created. Retry this request shortly; do not start another payment.")

    settlement_mode = _settlement_mode()
    settlement_account = _owner_route_account_for_charge(db, charge)
    attempt = m.PaymentAttempt(
        charge_entry_id=charge.id, customer_id=subject, provider="razorpay",
        settlement_mode=settlement_mode,
        idempotency_key=idempotency_key, amount_paise=due, currency="INR",
        status="creating",
    )
    db.add(attempt)
    try:
        db.commit()
        db.refresh(attempt)
    except IntegrityError as exc:
        db.rollback()
        duplicate = db.scalar(select(m.PaymentAttempt).where(
            m.PaymentAttempt.customer_id == subject,
            m.PaymentAttempt.idempotency_key == idempotency_key,
        ))
        if duplicate and duplicate.charge_entry_id == charge_id and duplicate.status in {"created", "authorized"}:
            return _checkout_payload(duplicate)
        raise HTTPException(409, "A payment session is already being created for this charge. Retry the same request.") from exc

    route_transfer_record = None
    if settlement_account is not None:
        route_transfer_record = m.PaymentTransfer(
            payment_attempt_id=attempt.id,
            owner_id=settlement_account.owner_id,
            linked_account_id=settlement_account.linked_account_id,
            provider_order_id="",
            amount_paise=due,
            currency="INR",
            status="creating",
            settlement_status="pending",
        )
        db.add(route_transfer_record)
        db.commit()
        db.refresh(route_transfer_record)

    try:
        order_notes = {
            "stayeasy_payment_attempt_id": str(attempt.id),
            "stayeasy_charge_id": str(charge.id),
            "stayeasy_property_id": str(charge.property_id),
        }
        if route_transfer_record is not None:
            transfer_spec = {
                "account": route_transfer_record.linked_account_id,
                "amount": due,
                "currency": "INR",
                "notes": {"stayeasy_payment_attempt_id": str(attempt.id),
                          "stayeasy_property_id": str(charge.property_id)},
            }
            gateway_order = create_razorpay_order_with_transfer(due, f"se-{attempt.id}", order_notes, transfer_spec)
        else:
            gateway_order = create_razorpay_order(due, f"se-{attempt.id}", order_notes)
        provider_order_id = str(gateway_order.get("id") or "")
        if (not provider_order_id or gateway_order.get("amount") != due
                or str(gateway_order.get("currency") or "") != "INR"):
            raise GatewayRequestError("The payment provider did not confirm the expected order amount.", ambiguous=True)
        if route_transfer_record is not None and not _record_route_transfer_from_order(db, attempt, gateway_order):
            attempt.provider_order_id = provider_order_id
            attempt.status = "unknown"
            attempt.failure_code = "route_transfer_missing_or_mismatched"
            route_transfer_record.provider_order_id = provider_order_id
            route_transfer_record.status = "unknown"
            route_transfer_record.failure_code = "route_transfer_missing_or_mismatched"
            route_transfer_record.updated_at = _utcnow()
            attempt.updated_at = _utcnow()
            db.commit()
            raise HTTPException(409, "The provider did not confirm the configured owner transfer. Payment order is blocked from Checkout pending reconciliation.")
        attempt.provider_order_id = provider_order_id
        attempt.status = "created"
        attempt.updated_at = _utcnow()
        if route_transfer_record is not None:
            route_transfer_record.provider_order_id = provider_order_id
            route_transfer_record.updated_at = _utcnow()
        db.commit()
        db.refresh(attempt)
    except GatewayRequestError as exc:
        db.rollback()
        failed_attempt = db.get(m.PaymentAttempt, attempt.id)
        if failed_attempt:
            failed_attempt.status = "unknown" if exc.ambiguous else "failed"
            failed_attempt.failure_code = "gateway_order_indeterminate" if exc.ambiguous else "gateway_order_rejected"
            failed_attempt.updated_at = _utcnow()
            transfer = db.scalar(select(m.PaymentTransfer).where(m.PaymentTransfer.payment_attempt_id == failed_attempt.id))
            if transfer is not None:
                transfer.status = "unknown" if exc.ambiguous else "failed"
                transfer.failure_code = "gateway_order_indeterminate" if exc.ambiguous else "gateway_order_rejected"
                transfer.updated_at = _utcnow()
            db.commit()
        if exc.ambiguous:
            raise HTTPException(503, "The provider's order result is indeterminate. Do not create a new payment request; support must reconcile it.") from exc
        raise HTTPException(502, "The payment provider did not create the order. No payment was collected.") from exc
    except Exception:
        db.rollback()
        failed_attempt = db.get(m.PaymentAttempt, attempt.id)
        if failed_attempt and failed_attempt.status == "creating":
            failed_attempt.status = "unknown"
            failed_attempt.failure_code = "order_response_persistence_indeterminate"
            failed_attempt.updated_at = _utcnow()
            transfer = db.scalar(select(m.PaymentTransfer).where(m.PaymentTransfer.payment_attempt_id == failed_attempt.id))
            if transfer is not None:
                transfer.status = "unknown"
                transfer.failure_code = "order_response_persistence_indeterminate"
                transfer.updated_at = _utcnow()
            db.commit()
        raise
    return _checkout_payload(attempt)


@router.post("/payments/verify")
def verify_checkout_payment(
    body: VerifyPaymentRequest,
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    attempt = _find_attempt_for_customer(db, body.payment_attempt_id, str(actor["subject"]))
    if not attempt.provider_order_id:
        raise HTTPException(409, "The payment order is not ready for verification.")
    _, key_secret = _credentials()
    expected = hmac.new(
        key_secret.encode("utf-8"),
        f"{attempt.provider_order_id}|{body.razorpay_payment_id}".encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    if not hmac.compare_digest(expected, body.razorpay_signature.lower()):
        raise HTTPException(400, "Payment signature verification failed.")

    try:
        provider_payment = fetch_razorpay_payment(body.razorpay_payment_id)
    except GatewayRequestError as exc:
        # The callback signature is valid. Persist the provider ID for a later
        # server-to-server reconciliation, but do not post a payment yet.
        attempt.provider_payment_id = body.razorpay_payment_id
        attempt.status = "unknown"
        attempt.failure_code = "provider_verification_unavailable"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(503, "We could not confirm the payment status with the provider. Use Check status; do not pay again.") from exc
    provider_payment["id"] = provider_payment.get("id") or body.razorpay_payment_id
    if str(provider_payment.get("order_id") or "") != attempt.provider_order_id:
        raise HTTPException(409, "The provider payment does not belong to this StayEasy order.")
    status_value = str(provider_payment.get("status") or "").lower()
    if status_value == "refunded":
        attempt.status = "unknown"
        attempt.failure_code = "provider_reports_refunded_requires_reconciliation"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(409, "Razorpay reports this payment as refunded. Finance reconciliation is required; it will not be marked paid again.")
    if status_value == "captured" and provider_payment.get("captured") is not False:
        _settle_captured_payment(db, attempt, provider_payment)
        db.commit()
        db.refresh(attempt)
        return {"payment": _attempt_payload(attempt), "ledger_posted": True}
    if status_value in {"authorized", "created", "pending"}:
        attempt.status = "authorized" if status_value == "authorized" else "created"
        attempt.provider_payment_id = body.razorpay_payment_id
        attempt.payment_method = str(provider_payment.get("method") or "")[:32]
        attempt.updated_at = _utcnow()
        db.commit()
        return {"payment": _attempt_payload(attempt), "ledger_posted": False, "message": "Payment is not captured yet."}
    _set_payment_failed(db, attempt, provider_payment)
    db.commit()
    raise HTTPException(409, "The provider reports that this payment was not captured.")


@router.get("/payments/mine")
def list_my_payments(
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    attempts = list(db.scalars(select(m.PaymentAttempt).where(
        m.PaymentAttempt.customer_id == str(actor["subject"])
    ).order_by(m.PaymentAttempt.created_at.desc()).limit(200)))
    out = []
    for attempt in attempts:
        charge = db.get(m.LedgerEntry, attempt.charge_entry_id)
        prop = db.get(m.Property, charge.property_id) if charge else None
        out.append({**_attempt_payload(attempt), "property_name": prop.name if prop else "Property",
                    "refunds": [_refund_payload(refund) for refund in db.scalars(select(m.PaymentRefund).where(
                        m.PaymentRefund.payment_attempt_id == attempt.id
                    ).order_by(m.PaymentRefund.created_at.desc()))]})
    return {"payments": out}


@router.get("/payments/{payment_attempt_id}")
def get_my_payment(
    payment_attempt_id: int,
    actor: dict = Depends(require_roles("customer")),
    db: Session = Depends(get_db),
):
    attempt = _find_attempt_for_customer(db, payment_attempt_id, str(actor["subject"]))
    return _attempt_payload(attempt)


@router.post("/payments/{payment_attempt_id}/refund", status_code=202)
def request_payment_refund(
    payment_attempt_id: int,
    body: RefundRequest,
    idempotency_key: str = Header(alias="Idempotency-Key", min_length=8, max_length=128),
    actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")),
    db: Session = Depends(get_db),
):
    attempt = db.scalar(select(m.PaymentAttempt).where(
        m.PaymentAttempt.id == payment_attempt_id
    ).with_for_update())
    if not attempt:
        raise HTTPException(404, "Payment not found")
    charge = db.get(m.LedgerEntry, attempt.charge_entry_id)
    if not charge:
        raise HTTPException(409, "The charge for this payment no longer exists.")
    _require_finance_manager(db, actor, charge.property_id)

    existing = db.scalar(select(m.PaymentRefund).where(
        m.PaymentRefund.requested_by == str(actor["subject"]),
        m.PaymentRefund.idempotency_key == idempotency_key,
    ))
    if existing:
        if existing.payment_attempt_id != attempt.id or existing.amount_paise != (body.amount_paise or existing.amount_paise):
            raise HTTPException(409, "This idempotency key was already used for a different refund.")
        return _refund_payload(existing)
    if attempt.status not in {"captured", "captured_unapplied", "refunded"} or not attempt.provider_payment_id:
        raise HTTPException(409, "Only a provider-confirmed captured payment can be refunded.")
    if attempt.status == "refunded":
        raise HTTPException(409, "This payment has already been fully refunded.")

    reserved = db.scalar(select(func.coalesce(func.sum(m.PaymentRefund.amount_paise), 0)).where(
        m.PaymentRefund.payment_attempt_id == attempt.id,
        m.PaymentRefund.status.in_(("requested", "pending", "unknown", "processed")),
    )) or 0
    available = max(0, attempt.amount_paise - int(reserved))
    amount = body.amount_paise if body.amount_paise is not None else available
    if amount < 100 or amount > available:
        raise HTTPException(422, "Refund must be at least INR 1.00 and no greater than the unrefunded captured amount.")

    owner_transfer = db.scalar(select(m.PaymentTransfer).where(
        m.PaymentTransfer.payment_attempt_id == attempt.id
    ))
    if owner_transfer is not None and (reserved > 0 or amount != attempt.amount_paise):
        # reverse_all reverses the Route transfer(s) in full, not a proportionally
        # calculated partial share. Never partially refund a routed payment and
        # accidentally pull the owner's entire rent transfer back.
        raise HTTPException(
            409,
            "Partial or subsequent refunds for owner-routed payments are blocked until amount-specific Route transfer reversal is available. This request did not reach Razorpay.",
        )

    refund = m.PaymentRefund(
        payment_attempt_id=attempt.id,
        amount_paise=amount,
        idempotency_key=idempotency_key,
        reason=body.reason.strip(),
        status="requested",
        requested_by=str(actor["subject"]),
    )
    db.add(refund)
    try:
        db.commit()
        db.refresh(refund)
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(409, "A refund with this idempotency key or amount is already being processed.") from exc

    try:
        refund_notes = {
            "stayeasy_payment_attempt_id": str(attempt.id),
            "stayeasy_refund_id": str(refund.id),
        }
        if owner_transfer is not None:
            # This payment was routed to a single owner linked account. Have Razorpay
            # reverse the linked-account transfer before returning money to the tenant.
            gateway_refund = create_razorpay_refund(
                attempt.provider_payment_id, amount, refund_notes, reverse_all=True
            )
        else:
            gateway_refund = create_razorpay_refund(attempt.provider_payment_id, amount, refund_notes)
        provider_refund_id = str(gateway_refund.get("id") or "")
        if not provider_refund_id:
            raise GatewayRequestError("The provider did not confirm a refund reference.", ambiguous=True)
        refund.provider_refund_id = provider_refund_id
        provider_status = str(gateway_refund.get("status") or "pending").lower()
        _refresh_refund_status(db, refund, provider_status)
        db.commit()
        db.refresh(refund)
    except GatewayRequestError as exc:
        db.rollback()
        current = db.get(m.PaymentRefund, refund.id)
        if current:
            current.status = "unknown" if exc.ambiguous else "failed"
            current.failure_code = "gateway_refund_indeterminate" if exc.ambiguous else "gateway_refund_rejected"
            db.commit()
        if exc.ambiguous:
            raise HTTPException(503, "The refund result is indeterminate. Do not submit it again with a new key; reconcile with the provider.") from exc
        raise HTTPException(502, "The provider rejected this refund request.") from exc
    return _refund_payload(refund)


@router.post("/payments/{payment_attempt_id}/refresh")
def refresh_payment_status(
    payment_attempt_id: int,
    actor: dict = Depends(require_roles("customer", "owner", "manager", "staff", "admin")),
    db: Session = Depends(get_db),
):
    """Reconcile an existing order with Razorpay; this never initiates a new charge."""
    attempt = db.scalar(select(m.PaymentAttempt).where(
        m.PaymentAttempt.id == payment_attempt_id
    ).with_for_update())
    if attempt is None:
        raise HTTPException(404, "Payment attempt not found.")
    charge = db.get(m.LedgerEntry, attempt.charge_entry_id)
    if actor["role"] == "customer":
        if attempt.customer_id != str(actor["subject"]):
            raise HTTPException(404, "Payment attempt not found.")
    else:
        if charge is None:
            raise HTTPException(409, "The charge linked to this payment no longer exists.")
        _require_finance_manager(db, actor, charge.property_id)

    if attempt.status in {"captured", "captured_unapplied", "refunded"}:
        return {"payment": _attempt_payload(attempt), "ledger_posted": True}
    if not attempt.provider_order_id:
        raise HTTPException(409, "Provider order reference is missing. Finance must reconcile this indeterminate order before retrying.")

    try:
        if attempt.provider_payment_id:
            candidates = [fetch_razorpay_payment(attempt.provider_payment_id)]
        else:
            collection = fetch_razorpay_order_payments(attempt.provider_order_id)
            candidates = [item for item in collection.get("items", []) if isinstance(item, dict)]
    except GatewayRequestError as exc:
        attempt.status = "unknown"
        attempt.failure_code = "provider_status_unavailable"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(503, "Razorpay status is temporarily unavailable. Do not start another payment; retry Check status.") from exc

    for candidate in candidates:
        if str(candidate.get("order_id") or "") != attempt.provider_order_id:
            continue
        if candidate.get("amount") != attempt.amount_paise or str(candidate.get("currency") or "") != attempt.currency:
            attempt.status = "unknown"
            attempt.failure_code = "provider_amount_mismatch"
            attempt.updated_at = _utcnow()
            db.commit()
            raise HTTPException(409, "The provider payment does not match the stored order amount/currency. Finance review is required.")

    refunded = [item for item in candidates if str(item.get("order_id") or "") == attempt.provider_order_id and str(item.get("status") or "").lower() == "refunded"]
    if refunded:
        attempt.status = "unknown"
        attempt.failure_code = "provider_reports_refunded_requires_reconciliation"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(409, "Razorpay reports a refunded payment. Finance reconciliation is required.")

    captured = [
        item for item in candidates
        if str(item.get("order_id") or "") == attempt.provider_order_id
        and str(item.get("status") or "").lower() == "captured"
        and item.get("captured") is not False
    ]
    if len(captured) > 1:
        attempt.status = "unknown"
        attempt.failure_code = "multiple_captures_requires_reconciliation"
        attempt.updated_at = _utcnow()
        db.commit()
        raise HTTPException(409, "Multiple captured payments were returned for one order. Finance reconciliation is required; do not pay again.")

    if captured:
        _settle_captured_payment(db, attempt, captured[0])
        db.commit()
        db.refresh(attempt)
        return {"payment": _attempt_payload(attempt), "ledger_posted": True}

    selected = next((
        item for desired in ("authorized", "created", "pending", "failed")
        for item in candidates
        if str(item.get("order_id") or "") == attempt.provider_order_id
        and str(item.get("status") or "").lower() == desired
    ), None)
    if selected is None:
        if attempt.status != "unknown":
            attempt.status = "created"
            attempt.failure_code = ""
        attempt.updated_at = _utcnow()
        db.commit()
        return {
            "payment": _attempt_payload(attempt),
            "ledger_posted": False,
            "message": "Razorpay has no payment attempt to confirm yet. The existing order is still available; do not create a duplicate charge.",
        }

    selected_id = str(selected.get("id") or "")
    if not selected_id:
        raise HTTPException(502, "Razorpay returned a payment without an ID; status was not changed.")
    state = str(selected.get("status") or "").lower()
    if state in {"authorized", "created", "pending"}:
        attempt.provider_payment_id = selected_id
        attempt.status = "authorized" if state == "authorized" else "created"
        attempt.payment_method = str(selected.get("method") or "")[:32]
        attempt.failure_code = ""
        attempt.updated_at = _utcnow()
    else:
        _set_payment_failed(db, attempt, selected)
    db.commit()
    db.refresh(attempt)
    return {
        "payment": _attempt_payload(attempt),
        "ledger_posted": False,
        "message": "Razorpay status was refreshed. The payment is not captured yet." if attempt.status != "failed" else "Razorpay reports that this payment attempt failed. Review the charge before retrying.",
    }


@router.get("/payments/owner/summary")
def list_property_payments(
    property_id: int | None = None,
    actor: dict = Depends(require_roles("owner", "manager", "staff", "admin")),
    db: Session = Depends(get_db),
):
    if actor["role"] == "admin":
        properties = None
        can_refund_properties = None
    elif property_id is not None:
        _require_finance_manager(db, actor, property_id)
        properties = {property_id}
        can_refund_properties = {property_id}
    elif actor["role"] == "owner":
        properties = set(db.scalars(select(m.Property.id).where(m.Property.owner_id == str(actor["subject"]))))
        can_refund_properties = properties
    else:
        assignments = list(db.scalars(select(m.StaffAssignment).where(
            m.StaffAssignment.staff_user_id == str(actor["subject"]),
            m.StaffAssignment.status == "active",
        )))
        properties = {a.property_id for a in assignments if {"finance.view", "finance.manage", "*"}.intersection(a.permissions or [])}
        can_refund_properties = {a.property_id for a in assignments if {"finance.manage", "*"}.intersection(a.permissions or [])}
    attempts = list(db.scalars(select(m.PaymentAttempt).order_by(m.PaymentAttempt.created_at.desc()).limit(1000)))
    result = []
    for attempt in attempts:
        charge = db.get(m.LedgerEntry, attempt.charge_entry_id)
        if not charge or (properties is not None and charge.property_id not in properties):
            continue
        prop = db.get(m.Property, charge.property_id)
        transfer = db.scalar(select(m.PaymentTransfer).where(m.PaymentTransfer.payment_attempt_id == attempt.id))
        recorded_settlement_mode = str(attempt.settlement_mode or "platform")
        if recorded_settlement_mode == "owner_route":
            settlement = {
                "mode": "owner_route",
                "status": transfer.status if transfer else "missing_transfer_record",
                "settlement_status": transfer.settlement_status if transfer else "unknown",
                "amount_paise": transfer.amount_paise if transfer else 0,
                "failure_code": transfer.failure_code if transfer else "missing_transfer_record",
            }
        else:
            settlement = {
                "mode": "platform",
                "status": "not_routed",
                "settlement_status": "not_applicable",
                "amount_paise": 0,
                "failure_code": "",
            }
        result.append({**_attempt_payload(attempt), "property_id": charge.property_id,
                       "property_name": prop.name if prop else f"Property #{charge.property_id}",
                       "charge_status": charge.status,
                       "owner_settlement": settlement,
                       "can_refund": can_refund_properties is None or charge.property_id in can_refund_properties,
                       "refunds": [_refund_payload(refund) for refund in db.scalars(select(m.PaymentRefund).where(
                           m.PaymentRefund.payment_attempt_id == attempt.id
                       ).order_by(m.PaymentRefund.created_at.desc()))]})
    return {"payments": result}


def _process_refund_webhook(db: Session, event_type: str, payload: dict[str, Any]) -> bool:
    refund_entity = payload.get("refund", {}).get("entity", {})
    if not isinstance(refund_entity, dict):
        return False
    provider_refund_id = str(refund_entity.get("id") or "")
    provider_payment_id = str(refund_entity.get("payment_id") or "")
    amount = refund_entity.get("amount")
    refund = db.scalar(select(m.PaymentRefund).where(m.PaymentRefund.provider_refund_id == provider_refund_id)) if provider_refund_id else None
    if refund is None and provider_payment_id and isinstance(amount, int):
        attempt = db.scalar(select(m.PaymentAttempt).where(m.PaymentAttempt.provider_payment_id == provider_payment_id))
        if attempt:
            candidates = list(db.scalars(select(m.PaymentRefund).where(
                m.PaymentRefund.payment_attempt_id == attempt.id,
                m.PaymentRefund.amount_paise == amount,
                m.PaymentRefund.status.in_(("requested", "pending", "unknown")),
            ).order_by(m.PaymentRefund.created_at.asc())))
            if len(candidates) == 1:
                refund = candidates[0]
                refund.provider_refund_id = provider_refund_id or None
    if refund is None:
        return False
    expected_payment_id = db.scalar(select(m.PaymentAttempt.provider_payment_id).where(m.PaymentAttempt.id == refund.payment_attempt_id))
    if not isinstance(amount, int) or amount != refund.amount_paise:
        raise HTTPException(409, "Refund webhook amount does not match the recorded refund request.")
    if provider_payment_id and expected_payment_id and provider_payment_id != expected_payment_id:
        raise HTTPException(409, "Refund webhook payment ID does not match the recorded payment.")
    _refresh_refund_status(db, refund, "processed" if event_type == "refund.processed" else "failed")
    return True


@router.post("/payments/webhooks/razorpay")
async def razorpay_webhook(
    request: FastAPIRequest,
    db: Session = Depends(get_db),
):
    webhook_secret = os.getenv("RAZORPAY_WEBHOOK_SECRET", "").strip()
    if not webhook_secret:
        raise HTTPException(503, "Payment webhook verification is not configured.")
    raw_body = await request.body()
    supplied = request.headers.get("X-Razorpay-Signature", "").strip()
    expected = hmac.new(webhook_secret.encode("utf-8"), raw_body, hashlib.sha256).hexdigest()
    if not supplied or not hmac.compare_digest(expected, supplied.lower()):
        raise HTTPException(401, "Invalid payment webhook signature.")
    try:
        body = json.loads(raw_body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise HTTPException(400, "Invalid webhook JSON.") from exc
    if not isinstance(body, dict):
        raise HTTPException(400, "Invalid webhook JSON.")
    event_type = str(body.get("event") or "unknown")[:80]
    digest = hashlib.sha256(raw_body).hexdigest()
    event_id = request.headers.get("X-Razorpay-Event-Id", "").strip()[:180] or digest

    event = db.scalar(select(m.PaymentWebhookEvent).where(m.PaymentWebhookEvent.event_id == event_id))
    if event and event.payload_sha256 != digest:
        raise HTTPException(409, "Webhook event identifier was reused with a different payload.")
    if event and event.processing_status in {"processed", "ignored"}:
        return {"accepted": True, "duplicate": True}
    if event is None:
        event = m.PaymentWebhookEvent(event_id=event_id, event_type=event_type,
                                      payload_sha256=digest, processing_status="received")
        db.add(event)
        try:
            db.commit()
            db.refresh(event)
        except IntegrityError:
            db.rollback()
            duplicate = db.scalar(select(m.PaymentWebhookEvent).where(m.PaymentWebhookEvent.event_id == event_id))
            if duplicate and duplicate.payload_sha256 == digest and duplicate.processing_status in {"processed", "ignored"}:
                return {"accepted": True, "duplicate": True}
            raise HTTPException(409, "Webhook is already being processed. Retry shortly.")

    try:
        if event_type.startswith("payment."):
            payment_entity = body.get("payload", {}).get("payment", {}).get("entity", {})
            if not isinstance(payment_entity, dict):
                raise HTTPException(400, "Payment webhook entity is missing.")
            order_id = str(payment_entity.get("order_id") or "")
            attempt = db.scalar(select(m.PaymentAttempt).where(m.PaymentAttempt.provider_order_id == order_id).with_for_update()) if order_id else None
            if attempt is None:
                event.processing_status = "ignored"
            elif event_type == "payment.captured":
                _settle_captured_payment(db, attempt, payment_entity)
                event.processing_status = "processed"
            elif event_type == "payment.failed":
                _set_payment_failed(db, attempt, payment_entity)
                event.processing_status = "processed"
            elif event_type == "payment.authorized":
                if attempt.status not in {"captured", "captured_unapplied", "refunded"}:
                    attempt.status = "authorized"
                    attempt.provider_payment_id = str(payment_entity.get("id") or "") or None
                    attempt.payment_method = str(payment_entity.get("method") or "")[:32]
                    attempt.updated_at = _utcnow()
                event.processing_status = "processed"
            else:
                event.processing_status = "ignored"
        elif event_type in {"refund.processed", "refund.failed"}:
            body_payload = body.get("payload", {})
            matched = _process_refund_webhook(db, event_type, body_payload if isinstance(body_payload, dict) else {})
            event.processing_status = "processed" if matched else "ignored"
        elif event_type == "settlement.processed":
            settlement_entity = body.get("payload", {}).get("settlement", {}).get("entity", {})
            if not isinstance(settlement_entity, dict):
                raise HTTPException(400, "Route settlement webhook entity is missing.")
            settlement_id = str(settlement_entity.get("id") or "")
            if not settlement_id.startswith("setl_"):
                raise HTTPException(400, "Route settlement webhook has no valid settlement ID.")
            settlement_amount = settlement_entity.get("amount")
            if not isinstance(settlement_amount, int) or settlement_amount < 0:
                raise HTTPException(400, "Route settlement amount is invalid.")
            route_transfers = list(db.scalars(select(m.PaymentTransfer).where(
                m.PaymentTransfer.recipient_settlement_id == settlement_id
            ).with_for_update()))
            if not route_transfers:
                # Settlement webhooks can race the local transfer-to-settlement linkage.
                # Ask Razorpay for the authoritative transfer collection rather than
                # permanently ignoring the event or guessing from its amount alone.
                try:
                    provider_collection = fetch_razorpay_transfers_for_settlement(settlement_id)
                except (GatewayRequestError, HTTPException) as exc:
                    raise HTTPException(503, "Razorpay settlement transfer lookup failed; retry reconciliation.") from exc
                provider_items = provider_collection.get("items")
                if not isinstance(provider_items, list):
                    raise HTTPException(503, "Razorpay returned no usable transfer collection for this settlement.")
                for item in provider_items:
                    if not isinstance(item, dict):
                        continue
                    provider_transfer_id = str(item.get("id") or "")
                    source_id = str(item.get("source") or "")
                    notes = item.get("notes") if isinstance(item.get("notes"), dict) else {}
                    attempt_text = str(notes.get("stayeasy_payment_attempt_id") or "")
                    attempt = db.get(m.PaymentAttempt, int(attempt_text)) if attempt_text.isdigit() else None
                    if attempt is None and source_id.startswith("order_"):
                        attempt = db.scalar(select(m.PaymentAttempt).where(
                            m.PaymentAttempt.provider_order_id == source_id
                        ))
                    route_transfer = db.scalar(select(m.PaymentTransfer).where(
                        m.PaymentTransfer.provider_transfer_id == provider_transfer_id
                    ).with_for_update()) if provider_transfer_id else None
                    if route_transfer is None and attempt is not None:
                        route_transfer = db.scalar(select(m.PaymentTransfer).where(
                            m.PaymentTransfer.payment_attempt_id == attempt.id
                        ).with_for_update())
                    if route_transfer is None:
                        continue
                    recipient = str(item.get("recipient") or "")
                    transfer_amount = item.get("amount")
                    transfer_currency = str(item.get("currency") or "")
                    provider_status = str(item.get("transfer_status") or item.get("status") or "").lower()
                    if (not provider_transfer_id.startswith("trf_")
                            or recipient != route_transfer.linked_account_id
                            or transfer_amount != route_transfer.amount_paise
                            or transfer_currency != route_transfer.currency
                            or provider_status not in {"created", "pending", "processed", "failed", "reversed", "partially_reversed"}):
                        raise HTTPException(409, "Razorpay settlement transfer does not match the stored owner, amount, currency or status.")
                    route_transfer.provider_transfer_id = provider_transfer_id
                    if source_id.startswith("order_"):
                        route_transfer.provider_order_id = source_id
                    route_transfer.recipient_settlement_id = settlement_id
                    route_transfer.status = provider_status
                    route_transfer.settlement_status = "settled"
                    route_transfer.failure_code = "provider_transfer_failed" if provider_status == "failed" else ""
                    route_transfer.updated_at = _utcnow()
                    route_transfers.append(route_transfer)
                if not route_transfers:
                    raise HTTPException(503, "Razorpay settlement has no matching StayEasy owner transfer yet; retry reconciliation.")
            for route_transfer in route_transfers:
                route_transfer.recipient_settlement_id = settlement_id
                route_transfer.settlement_status = "settled"
                route_transfer.updated_at = _utcnow()
            event.processing_status = "processed"
        elif event_type.startswith("transfer."):
            transfer_entity = body.get("payload", {}).get("transfer", {}).get("entity", {})
            if not isinstance(transfer_entity, dict):
                raise HTTPException(400, "Owner transfer webhook entity is missing.")
            provider_transfer_id = str(transfer_entity.get("id") or "")
            source_id = str(transfer_entity.get("source") or "")
            notes = transfer_entity.get("notes") if isinstance(transfer_entity.get("notes"), dict) else {}
            attempt_text = str(notes.get("stayeasy_payment_attempt_id") or "")
            attempt = None
            if attempt_text.isdigit():
                attempt = db.get(m.PaymentAttempt, int(attempt_text))
            if attempt is None and source_id:
                attempt = db.scalar(select(m.PaymentAttempt).where(
                    m.PaymentAttempt.provider_order_id == source_id
                ))
            route_transfer = db.scalar(select(m.PaymentTransfer).where(
                m.PaymentTransfer.provider_transfer_id == provider_transfer_id
            )) if provider_transfer_id else None
            if route_transfer is None and attempt is not None:
                route_transfer = db.scalar(select(m.PaymentTransfer).where(
                    m.PaymentTransfer.payment_attempt_id == attempt.id
                ).with_for_update())
            if route_transfer is None:
                event.processing_status = "ignored"
            else:
                recipient = str(transfer_entity.get("recipient") or "")
                transfer_amount = transfer_entity.get("amount")
                transfer_currency = str(transfer_entity.get("currency") or "")
                if (not provider_transfer_id.startswith("trf_")
                        or recipient != route_transfer.linked_account_id
                        or transfer_amount != route_transfer.amount_paise
                        or transfer_currency != route_transfer.currency):
                    raise HTTPException(409, "Owner transfer webhook does not match the stored recipient, amount and currency.")
                expected_statuses = {
                    "transfer.created": {"created"},
                    "transfer.processed": {"processed"},
                    "transfer.failed": {"failed"},
                    "transfer.reversed": {"reversed", "partially_reversed"},
                }.get(event_type, set())
                status_from_provider = str(transfer_entity.get("transfer_status") or transfer_entity.get("status") or "").lower()
                if not expected_statuses or status_from_provider not in expected_statuses:
                    event.processing_status = "ignored"
                else:
                    route_transfer.provider_transfer_id = provider_transfer_id
                    if source_id.startswith("order_"):
                        route_transfer.provider_order_id = source_id
                    recipient_settlement_id = str(transfer_entity.get("recipient_settlement_id") or "")
                    route_transfer.recipient_settlement_id = recipient_settlement_id or route_transfer.recipient_settlement_id
                    route_transfer.status = status_from_provider
                    settlement_status = str(transfer_entity.get("settlement_status") or "").lower()
                    if settlement_status in {"pending", "on_hold", "settled"}:
                        route_transfer.settlement_status = settlement_status
                    route_transfer.failure_code = "provider_transfer_failed" if status_from_provider == "failed" else ""
                    if status_from_provider == "processed":
                        route_transfer.processed_at = _utcnow()
                        # Do not imply bank settlement unless Razorpay explicitly reports it.
                        if settlement_status not in {"settled", "on_hold"}:
                            route_transfer.settlement_status = "pending"
                    route_transfer.updated_at = _utcnow()
                    event.processing_status = "processed"
        else:
            event.processing_status = "ignored"
        event.processed_at = _utcnow()
        db.commit()
    except HTTPException:
        db.rollback()
        retry_event = db.scalar(select(m.PaymentWebhookEvent).where(m.PaymentWebhookEvent.event_id == event_id))
        if retry_event:
            retry_event.processing_status = "failed"
            db.commit()
        raise
    except Exception as exc:
        db.rollback()
        retry_event = db.scalar(select(m.PaymentWebhookEvent).where(m.PaymentWebhookEvent.event_id == event_id))
        if retry_event:
            retry_event.processing_status = "failed"
            db.commit()
        raise HTTPException(503, "Webhook could not be reconciled; the provider may retry it.") from exc
    return {"accepted": True, "duplicate": False, "status": event.processing_status}

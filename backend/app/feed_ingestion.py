"""Polling importer for a configured property-partner JSON feed.

The app does not scrape marketplaces or invent listings. Configure PROPERTY_FEED_URL
with a feed you are authorised to use. The feed must return {"items": [...]} or a JSON
array. Provider records remain labelled unverified and include a stable source key.
"""
from __future__ import annotations

import json
import logging
import os
import time
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from sqlalchemy import select

from .database import Base, SessionLocal, engine
from . import models as m

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
log = logging.getLogger("stayeasy.feed")
MAX_FEED_BYTES = 2 * 1024 * 1024


def _read_feed(url: str) -> list[dict]:
    if not url:
        raise RuntimeError("PROPERTY_FEED_URL is empty. Configure an authorised provider JSON feed.")
    if not url.lower().startswith(("https://", "http://")):
        raise ValueError("PROPERTY_FEED_URL must be an HTTP(S) URL.")
    if url.lower().startswith("http://") and os.getenv("APP_ENV", "development").lower() != "development":
        raise ValueError("Non-HTTPS property feeds are allowed only in development.")
    headers = {"Accept": "application/json", "User-Agent": "StayEasy-feed-sync/1.0"}
    token = os.getenv("PROPERTY_FEED_TOKEN", "").strip()
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = Request(url, headers=headers, method="GET")
    with urlopen(request, timeout=15) as response:
        body = response.read(MAX_FEED_BYTES + 1)
    if len(body) > MAX_FEED_BYTES:
        raise ValueError("Property feed exceeded the 2 MiB limit.")
    parsed = json.loads(body.decode("utf-8"))
    items = parsed.get("items", []) if isinstance(parsed, dict) else parsed
    if not isinstance(items, list):
        raise ValueError("Feed must be a JSON array or an object containing an items array.")
    if len(items) > 1000:
        raise ValueError("Feed exceeds the 1,000 listing batch limit.")
    return [item for item in items if isinstance(item, dict)]


def sync_once() -> dict[str, int]:
    url = os.getenv("PROPERTY_FEED_URL", "").strip()
    source = os.getenv("PROPERTY_FEED_SOURCE", "partner-feed").strip()[:39] or "partner-feed"
    items = _read_feed(url)
    Base.metadata.create_all(bind=engine)
    created = updated = skipped = 0
    db = SessionLocal()
    try:
        for item in items:
            external_id = str(item.get("external_id", "")).strip()[:120]
            name = str(item.get("name", "")).strip()[:180]
            locality = str(item.get("locality", "")).strip()[:120]
            city = str(item.get("city", "")).strip()[:100]
            try:
                rent = int(item.get("rent_from", 0))
                deposit = int(item.get("deposit_from", 0))
                food_monthly = int(item.get("food_monthly", 0) or 0)
                utilities_estimate = int(item.get("utilities_estimate", 0) or 0)
            except (TypeError, ValueError):
                skipped += 1
                continue
            if not external_id or len(name) < 2 or not locality or not city or rent < 0 or deposit < 0:
                skipped += 1
                continue
            source_key = f"{source}:{external_id}"
            prop = db.scalar(select(m.Property).where(m.Property.external_source_key == source_key))
            fields = {
                "owner_id": str(item.get("owner_id") or "external-feed-owner")[:36],
                "name": name,
                "description": ("EXTERNAL FEED — " + str(item.get("description", "")).strip())[:5000],
                "address": str(item.get("address", "")).strip()[:300],
                "locality": locality,
                "city": city,
                "pin_code": str(item.get("pin_code", "")).strip()[:10],
                "latitude": item.get("latitude") if isinstance(item.get("latitude"), (int, float)) else None,
                "longitude": item.get("longitude") if isinstance(item.get("longitude"), (int, float)) else None,
                "eligibility": item.get("eligibility", "Mixed") if item.get("eligibility") in {"Boys only", "Girls only", "Mixed", "All genders"} else "Mixed",
                "couples_welcome": bool(item.get("couples_welcome", False)),
                "furnishing": str(item.get("furnishing", "Furnished"))[:30],
                "bathroom_policy": str(item.get("bathroom_policy", "Shared"))[:40],
                "food_policy": str(item.get("food_policy", "Not offered"))[:40],
                "rent_from": rent,
                "deposit_from": deposit,
                "food_monthly": max(0, food_monthly),
                "utilities_estimate": max(0, utilities_estimate),
                "status": "published",
                "verification_status": "unverified",
                "data_source": source,
                "external_source_key": source_key,
                "external_updated_at": datetime.now(timezone.utc),
            }
            if prop is None:
                prop = m.Property(**fields)
                db.add(prop)
                created += 1
            else:
                for key, value in fields.items():
                    setattr(prop, key, value)
                prop.updated_at = datetime.now(timezone.utc)
                updated += 1
        db.commit()
        return {"received": len(items), "created": created, "updated": updated, "skipped": skipped}
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def main() -> None:
    interval = max(60, int(os.getenv("PROPERTY_FEED_POLL_SECONDS", "300")))
    while True:
        try:
            result = sync_once()
            log.info("Property feed sync completed: %s", result)
        except (HTTPError, URLError, TimeoutError, ValueError, RuntimeError) as exc:
            log.error("Property feed sync failed (no provider payload logged): %s", exc)
        except Exception:
            log.exception("Unexpected feed sync failure")
        time.sleep(interval)


if __name__ == "__main__":
    main()

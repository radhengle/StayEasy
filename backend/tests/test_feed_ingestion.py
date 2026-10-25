from __future__ import annotations

import json

import pytest
from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from backend.app import feed_ingestion, models
from backend.app.database import Base


class FakeResponse:
    def __init__(self, body: bytes):
        self.body = body

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self, limit: int = -1) -> bytes:
        return self.body[:limit]


def _use_isolated_database(monkeypatch):
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(bind=engine)
    monkeypatch.setattr(feed_ingestion, "engine", engine)
    monkeypatch.setattr(feed_ingestion, "SessionLocal", sessionmaker(bind=engine, expire_on_commit=False))
    return engine


def test_feed_requires_configured_url():
    with pytest.raises(RuntimeError, match="PROPERTY_FEED_URL"):
        feed_ingestion._read_feed("")


def test_feed_parses_items_and_adds_bearer_header(monkeypatch):
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.setenv("PROPERTY_FEED_TOKEN", "feed-test-token")
    payload = {"items": [{"external_id": "pune-1", "name": "Listing"}]}
    requested = {}

    def fake_urlopen(request, timeout):
        requested["url"] = request.full_url
        requested["authorization"] = request.get_header("Authorization")
        requested["timeout"] = timeout
        return FakeResponse(json.dumps(payload).encode())

    monkeypatch.setattr(feed_ingestion, "urlopen", fake_urlopen)
    assert feed_ingestion._read_feed("https://feed.example.test/listings") == payload["items"]
    assert requested["authorization"] == "Bearer feed-test-token"
    assert requested["timeout"] == 15


def test_feed_accepts_top_level_json_array(monkeypatch):
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.delenv("PROPERTY_FEED_TOKEN", raising=False)
    payload = [{"external_id": "pune-2"}]
    monkeypatch.setattr(feed_ingestion, "urlopen", lambda *_args, **_kwargs: FakeResponse(json.dumps(payload).encode()))
    assert feed_ingestion._read_feed("https://feed.example.test/listings") == payload


def test_http_feed_blocked_outside_development(monkeypatch):
    monkeypatch.setenv("APP_ENV", "production")
    with pytest.raises(ValueError, match="HTTPS"):
        feed_ingestion._read_feed("http://feed.example.test/listings")


def test_feed_rejects_oversized_response(monkeypatch):
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.delenv("PROPERTY_FEED_TOKEN", raising=False)
    monkeypatch.setattr(feed_ingestion, "urlopen", lambda *_args, **_kwargs: FakeResponse(b" " * (feed_ingestion.MAX_FEED_BYTES + 1)))
    with pytest.raises(ValueError, match="2 MiB"):
        feed_ingestion._read_feed("https://feed.example.test/listings")


def test_sync_once_creates_published_unverified_provider_listing(monkeypatch):
    engine = _use_isolated_database(monkeypatch)
    monkeypatch.setenv("PROPERTY_FEED_URL", "https://feed.example.test/listings")
    monkeypatch.setenv("PROPERTY_FEED_SOURCE", "authorised-partner")
    monkeypatch.setattr(feed_ingestion, "_read_feed", lambda _url: [{
        "external_id": "partner-100",
        "name": "Partner PG",
        "locality": "Kothrud",
        "city": "Pune",
        "rent_from": 8500,
        "deposit_from": 5000,
        "food_monthly": 1800,
        "latitude": 18.5074,
        "longitude": 73.8077,
    }])

    try:
        result = feed_ingestion.sync_once()
        assert result == {"received": 1, "created": 1, "updated": 0, "skipped": 0}
        with feed_ingestion.SessionLocal() as db:
            prop = db.scalar(select(models.Property).where(models.Property.external_source_key == "authorised-partner:partner-100"))
            assert prop is not None
            assert prop.name == "Partner PG"
            assert prop.rent_from == 8500
            assert prop.data_source == "authorised-partner"
            assert prop.status == "published"
            assert prop.verification_status == "unverified"
            assert prop.external_updated_at is not None
    finally:
        engine.dispose()


def test_sync_once_upserts_existing_provider_listing_and_skips_invalid_rows(monkeypatch):
    engine = _use_isolated_database(monkeypatch)
    monkeypatch.setenv("PROPERTY_FEED_URL", "https://feed.example.test/listings")
    monkeypatch.setenv("PROPERTY_FEED_SOURCE", "authorised-partner")
    payload = [{
        "external_id": "partner-200",
        "name": "First Name",
        "locality": "Baner",
        "city": "Pune",
        "rent_from": 9000,
    }, {
        "external_id": "invalid-no-rent",
        "name": "Invalid listing",
        "locality": "Baner",
        "city": "Pune",
        "rent_from": "not-a-number",
    }]
    monkeypatch.setattr(feed_ingestion, "_read_feed", lambda _url: payload)

    try:
        first = feed_ingestion.sync_once()
        assert first == {"received": 2, "created": 1, "updated": 0, "skipped": 1}
        payload[0]["name"] = "Updated Name"
        payload[0]["rent_from"] = 9700
        second = feed_ingestion.sync_once()
        assert second == {"received": 2, "created": 0, "updated": 1, "skipped": 1}

        with feed_ingestion.SessionLocal() as db:
            rows = db.scalars(select(models.Property).where(models.Property.data_source == "authorised-partner")).all()
            assert len(rows) == 1
            assert rows[0].name == "Updated Name"
            assert rows[0].rent_from == 9700
    finally:
        engine.dispose()

def test_sync_once_persists_successful_feed_health_and_batch_counts(monkeypatch):
    engine = _use_isolated_database(monkeypatch)
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.setenv("PROPERTY_FEED_URL", "https://feed.example.test/listings")
    monkeypatch.setenv("PROPERTY_FEED_SOURCE", "status-test")
    payload = [{
        "external_id": "health-1",
        "name": "Feed health listing",
        "locality": "Baner",
        "city": "Pune",
        "rent_from": 11000,
    }]
    monkeypatch.setattr(feed_ingestion, "_read_feed", lambda _url: payload)
    try:
        result = feed_ingestion.sync_once()
        assert result == {"received": 1, "created": 1, "updated": 0, "skipped": 0}
        with feed_ingestion.SessionLocal() as db:
            state = db.get(models.IntegrationSyncState, "property_feed")
            assert state is not None
            assert state.status == "success"
            assert state.last_attempt_at is not None
            assert state.last_success_at is not None
            assert state.last_error_category == ""
            assert (state.received_count, state.created_count, state.updated_count, state.skipped_count) == (1, 1, 0, 0)
    finally:
        engine.dispose()


def test_sync_once_persists_safe_failure_category_without_provider_payload(monkeypatch):
    engine = _use_isolated_database(monkeypatch)
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.setenv("PROPERTY_FEED_URL", "https://feed.example.test/private?token=never-store")
    monkeypatch.setenv("PROPERTY_FEED_TOKEN", "never-store-this-token")

    def fail_read(_url):
        raise ValueError("malformed provider payload with private details")

    monkeypatch.setattr(feed_ingestion, "_read_feed", fail_read)
    try:
        with pytest.raises(ValueError):
            feed_ingestion.sync_once()
        with feed_ingestion.SessionLocal() as db:
            state = db.get(models.IntegrationSyncState, "property_feed")
            assert state is not None
            assert state.status == "error"
            assert state.last_attempt_at is not None
            assert state.last_error_category == "invalid_feed"
            assert "never-store" not in state.last_error_category
            assert "private details" not in state.last_error_category
    finally:
        engine.dispose()


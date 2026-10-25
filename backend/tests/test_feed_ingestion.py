from __future__ import annotations

import json

import pytest

from backend.app import feed_ingestion


class FakeResponse:
    def __init__(self, body: bytes):
        self.body = body

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self, limit: int = -1) -> bytes:
        return self.body[:limit]


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

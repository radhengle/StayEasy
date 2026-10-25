#!/usr/bin/env python3
"""Approve one verified Owner request in a local StayEasy development stack.

This intentionally uses the development-only /auth/dev-token endpoint. It refuses to
run unless .env says APP_ENV=development. Never use this helper against staging/production.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]


def load_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if path.exists():
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def api_json(base_url: str, path: str, *, method: str = "GET",
             payload: dict | None = None, token: str = "") -> object:
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = Request(base_url.rstrip("/") + path, data=body, headers=headers, method=method)
    try:
        with urlopen(req, timeout=15) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")[:600]
        raise RuntimeError(f"StayEasy API returned HTTP {exc.code}: {detail}") from exc
    except (URLError, TimeoutError, OSError) as exc:
        raise RuntimeError(f"Cannot reach StayEasy API at {base_url}: {exc}") from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("email", help="Email address used to register the Owner account")
    parser.add_argument("--base-url", default="http://127.0.0.1:8000", help="Local API URL")
    args = parser.parse_args()

    env = load_env(ROOT / ".env")
    if env.get("APP_ENV", "development").lower() != "development":
        print("Refusing to run: .env APP_ENV must be development.", file=sys.stderr)
        return 2
    password = env.get("DEV_TOKEN_PASSWORD", "")
    if not password:
        print("DEV_TOKEN_PASSWORD is missing from the local .env.", file=sys.stderr)
        return 2

    email = args.email.strip().lower()
    if not email:
        print("Provide the Owner account email.", file=sys.stderr)
        return 2
    try:
        token_result = api_json(args.base_url, "/auth/dev-token", method="POST", payload={
            "subject": "stayeasy-local-owner-approval",
            "role": "admin",
            "dev_password": password,
        })
        token = token_result.get("access_token", "") if isinstance(token_result, dict) else ""
        if not token:
            raise RuntimeError("The local dev-token endpoint returned no access token.")
        rows = api_json(args.base_url, "/admin/owner-requests?status=pending_approval", token=token)
        if not isinstance(rows, list):
            raise RuntimeError("The Owner requests endpoint returned an unexpected response.")
        matches = [row for row in rows if str(row.get("email", "")).strip().lower() == email]
        if not matches:
            print(f"No pending Owner request found for {email}. Verify registration and check the email inbox first.", file=sys.stderr)
            return 1
        applicant = matches[0]
        if applicant.get("email_verified") is not True:
            print("The account email is not verified yet. Verify the code from Mailpit before approval.", file=sys.stderr)
            return 1
        result = api_json(
            args.base_url,
            "/admin/owner-requests/" + quote(str(applicant["id"]), safe="") + "/decision",
            method="POST",
            payload={"decision": "approve", "note": "Approved for local end-to-end testing"},
            token=token,
        )
        print(json.dumps({
            "email": email,
            "approved": True,
            "role": result.get("role") if isinstance(result, dict) else "owner",
            "next_step": "In StayEasy, sign out and sign back in to receive a token with the approved Owner role.",
        }, indent=2))
        return 0
    except RuntimeError as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

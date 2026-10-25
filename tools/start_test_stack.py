#!/usr/bin/env python3
"""Create a private local .env and run the StayEasy API + PostgreSQL test stack."""
from __future__ import annotations
import argparse, secrets, subprocess, sys, time
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env"

def ensure_env() -> dict[str, str]:
    values: dict[str, str] = {}
    if ENV_FILE.exists():
        for raw in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    defaults = {
        "POSTGRES_DB":"stayeasy", "POSTGRES_USER":"stayeasy",
        "POSTGRES_PASSWORD":secrets.token_urlsafe(28), "APP_ENV":"development",
        "JWT_SECRET":secrets.token_urlsafe(48), "JWT_ISSUER":"stayeasy-api",
        "ACCESS_TOKEN_MINUTES":"120", "DEV_TOKEN_PASSWORD":secrets.token_urlsafe(20),
        "API_PORT":"8000", "API_BIND_HOST":"127.0.0.1", "SEED_DEMO_DATA":"true",
        "CORS_ORIGINS":"http://localhost:8080,http://127.0.0.1:8080",
        "SMTP_HOST":"mailpit", "SMTP_PORT":"1025", "SMTP_STARTTLS":"false",
        "SMTP_USERNAME":"", "SMTP_PASSWORD":"",
        "SMTP_FROM_EMAIL":"no-reply@stayeasy.local",
        "RAZORPAY_KEY_ID":"", "RAZORPAY_KEY_SECRET":"", "RAZORPAY_WEBHOOK_SECRET":"",
        "PROPERTY_FEED_URL":"", "PROPERTY_FEED_SOURCE":"partner-feed",
        "PROPERTY_FEED_TOKEN":"", "PROPERTY_FEED_POLL_SECONDS":"300"
    }
    changed = False
    stock_values = {
        "POSTGRES_PASSWORD": {"stayeasy-local-dev-change-me", "REPLACE_WITH_LONG_HEX_PASSWORD"},
        "JWT_SECRET": {"local-development-secret-change-this-to-a-random-value-over-32-chars", "REPLACE_WITH_AT_LEAST_32_RANDOM_CHARACTERS"},
        "DEV_TOKEN_PASSWORD": {"stayeasy-local-only-change-me", "SET_A_LOCAL_ONLY_PASSWORD"},
    }
    for key, value in defaults.items():
        current = values.get(key, "")
        if not current or current in stock_values.get(key, set()):
            values[key] = value
            changed = True
    if changed or not ENV_FILE.exists():
        ENV_FILE.write_text("# Generated local-only StayEasy secrets. Never commit this file.\n" +
                            "".join(f"{k}={v}\n" for k, v in values.items()), encoding="utf-8")
    return values

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--with-feed", action="store_true", help="start the external feed worker as well")
    args = parser.parse_args()
    env = ensure_env()
    if args.with_feed and not env.get("PROPERTY_FEED_URL"):
        print("Set an authorised PROPERTY_FEED_URL in .env before using --with-feed.", file=sys.stderr)
        return 2
    # Mailpit captures verification/reset emails locally, making real account
    # registration testable without exposing codes through API responses.
    command = ["docker", "compose", "--profile", "devmail"]
    if args.with_feed:
        command += ["--profile", "ingestion"]
    command += ["up", "--build", "-d"]
    try:
        subprocess.run(command, cwd=ROOT, check=True)
    except FileNotFoundError:
        print("Docker Desktop / Docker Compose is unavailable. Start Docker Desktop and retry.", file=sys.stderr)
        return 2
    except subprocess.CalledProcessError as exc:
        return exc.returncode or 1
    port = env.get("API_PORT", "8000")
    health_url = f"http://127.0.0.1:{port}/health"
    print("Checking local API health...")
    for _ in range(45):
        try:
            with urlopen(health_url, timeout=2) as response:
                if response.status == 200:
                    print("API: " + health_url)
                    print("API docs: http://127.0.0.1:" + port + "/docs")
                    print("Android emulator URL: http://10.0.2.2:" + port)
                    if env.get("SMTP_HOST") == "mailpit":
                        print("Local verification email inbox: http://127.0.0.1:8025")
                    print("Demo property/stay/rent rows are test fixtures, not real rental inventory.")
                    print("Development-token password (enter in API connection screen):")
                    print(env["DEV_TOKEN_PASSWORD"])
                    if not (env.get("RAZORPAY_KEY_ID") and env.get("RAZORPAY_KEY_SECRET")):
                        print("Payment checkout needs Razorpay TEST-mode keys in .env; payment history and charge APIs are available now.")
                    return 0
        except (URLError, TimeoutError, OSError):
            pass
        time.sleep(2)
    print("API failed to become healthy. Inspect: docker compose logs --tail=150 api", file=sys.stderr)
    return 1

if __name__ == "__main__":
    raise SystemExit(main())

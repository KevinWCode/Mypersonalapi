"""One-time Google sign-in for the calendar, run by you on the PC:

    python -m app.google_auth

It opens Google in your browser, you allow read-only calendar access, and the refresh token is saved next to the
database (data/google_token.json). After that the collector reads the calendar by itself. Plain code, no AI.
"""
import base64
import hashlib
import json
import secrets
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urlparse

import httpx

from app import config

AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_URL = "https://oauth2.googleapis.com/token"
# Read-only: the server can see events but never create, change or delete them.
SCOPE = "https://www.googleapis.com/auth/calendar.readonly"


def token_path() -> Path:
    return Path(config.GOOGLE_TOKEN_FILE)


def configured() -> bool:
    return bool(config.GOOGLE_CLIENT_ID and config.GOOGLE_CLIENT_SECRET and token_path().is_file())


def access_token(client: httpx.Client) -> str:
    """Swaps the saved refresh token for a short-lived access token."""
    refresh = json.loads(token_path().read_text())["refresh_token"]
    resp = client.post(TOKEN_URL, data={
        "client_id": config.GOOGLE_CLIENT_ID, "client_secret": config.GOOGLE_CLIENT_SECRET,
        "refresh_token": refresh, "grant_type": "refresh_token",
    })
    resp.raise_for_status()
    return resp.json()["access_token"]


def sign_in() -> None:
    if not (config.GOOGLE_CLIENT_ID and config.GOOGLE_CLIENT_SECRET):
        raise SystemExit("Put GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env first (see docs/google-calendar.md).")
    verifier = secrets.token_urlsafe(64)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    state = secrets.token_urlsafe(16)
    result: dict = {}

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            q = parse_qs(urlparse(self.path).query)
            ok = q.get("state", [""])[0] == state and "code" in q
            if ok:
                result["code"] = q["code"][0]
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.end_headers()
            self.wfile.write(("Signed in. You can close this tab." if ok else "Sign-in didn't work. Try again.").encode())

        def log_message(self, *args):
            pass

    # Google sends you back to this PC only (127.0.0.1), on a free port.
    server = HTTPServer(("127.0.0.1", 0), Handler)
    redirect = f"http://127.0.0.1:{server.server_port}"
    url = AUTH_URL + "?" + urlencode({
        "client_id": config.GOOGLE_CLIENT_ID, "redirect_uri": redirect, "response_type": "code", "scope": SCOPE,
        "access_type": "offline", "prompt": "consent", "state": state,
        "code_challenge": challenge, "code_challenge_method": "S256",
    })
    print("Opening Google sign-in. If no browser opens, paste this address into one:\n" + url)
    webbrowser.open(url)
    while "code" not in result:
        server.handle_request()
    resp = httpx.post(TOKEN_URL, data={
        "client_id": config.GOOGLE_CLIENT_ID, "client_secret": config.GOOGLE_CLIENT_SECRET, "code": result["code"],
        "code_verifier": verifier, "grant_type": "authorization_code", "redirect_uri": redirect,
    }, timeout=30)
    resp.raise_for_status()
    refresh = resp.json().get("refresh_token")
    if not refresh:
        raise SystemExit("Google didn't send a refresh token. Remove the app's access in your Google account and try again.")
    token_path().parent.mkdir(parents=True, exist_ok=True)
    token_path().write_text(json.dumps({"refresh_token": refresh}))
    print(f"Saved to {token_path()}. The calendar will be read on the next collect.")


if __name__ == "__main__":
    sign_in()

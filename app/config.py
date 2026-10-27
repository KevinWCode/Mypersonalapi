import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def load_env_file(path: Path) -> None:
    """Reads KEY=value lines from .env when running without Docker (Docker passes them in itself).
    Values already set in the environment win."""
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


load_env_file(ROOT / ".env")

DB_PATH = os.environ.get("DB_PATH", "data/personal.db")
GITHUB_USER = os.environ.get("GITHUB_USER", "")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")
INGEST_TOKEN = os.environ.get("INGEST_TOKEN", "")
COLLECT_EVERY_MINUTES = int(os.environ.get("COLLECT_EVERY_MINUTES", "60"))
# Meal files (JSON) saved here are added to food totals. Next to the database by default: data/inbox on the PC.
FOOD_INBOX = os.environ.get("FOOD_INBOX", str(Path(DB_PATH).parent / "inbox"))
ALPACA_KEY_ID = os.environ.get("ALPACA_KEY_ID", "")
ALPACA_SECRET_KEY = os.environ.get("ALPACA_SECRET_KEY", "")
# More paper accounts (one per trading bot): ALPACA_KEY_ID_2 / ALPACA_SECRET_KEY_2, _3 and so on.
# They're added together and shown as one account.
ALPACA_EXTRA_ACCOUNTS = [
    (os.environ.get(f"ALPACA_KEY_ID_{n}", ""), os.environ.get(f"ALPACA_SECRET_KEY_{n}", "")) for n in range(2, 10)
]
# Paper trading by default; set to https://api.alpaca.markets for the live account.
ALPACA_BASE_URL = os.environ.get("ALPACA_BASE_URL", "https://paper-api.alpaca.markets")

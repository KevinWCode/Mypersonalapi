import os

DB_PATH = os.environ.get("DB_PATH", "data/personal.db")
GITHUB_USER = os.environ.get("GITHUB_USER", "")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")
INGEST_TOKEN = os.environ.get("INGEST_TOKEN", "")
COLLECT_EVERY_MINUTES = int(os.environ.get("COLLECT_EVERY_MINUTES", "60"))
ALPACA_KEY_ID = os.environ.get("ALPACA_KEY_ID", "")
ALPACA_SECRET_KEY = os.environ.get("ALPACA_SECRET_KEY", "")
# Paper trading by default; set to https://api.alpaca.markets for the live account.
ALPACA_BASE_URL = os.environ.get("ALPACA_BASE_URL", "https://paper-api.alpaca.markets")

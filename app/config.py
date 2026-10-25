import os

DB_PATH = os.environ.get("DB_PATH", "data/personal.db")
GITHUB_USER = os.environ.get("GITHUB_USER", "")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")
INGEST_TOKEN = os.environ.get("INGEST_TOKEN", "")
COLLECT_EVERY_MINUTES = int(os.environ.get("COLLECT_EVERY_MINUTES", "60"))

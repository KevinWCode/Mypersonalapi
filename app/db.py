"""One table holds every stat: one row per (date, metric, source)."""
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

from app import config

SCHEMA = """
CREATE TABLE IF NOT EXISTS observations (
    date       TEXT NOT NULL,  -- YYYY-MM-DD, local day the stat belongs to
    metric     TEXT NOT NULL,  -- e.g. github.commits, screen.app.com.instagram.android
    value      REAL NOT NULL,
    unit       TEXT NOT NULL,  -- count, minutes, ...
    source     TEXT NOT NULL,  -- github, phone, manual, ...
    updated_at TEXT NOT NULL,
    PRIMARY KEY (date, metric, source)
);

-- Filled stock orders from Alpaca, for "last bought / last sold".
CREATE TABLE IF NOT EXISTS trades (
    id        TEXT PRIMARY KEY,  -- Alpaca order id
    filled_at TEXT NOT NULL,     -- ISO time, UTC
    symbol    TEXT NOT NULL,
    side      TEXT NOT NULL,     -- buy or sell
    qty       REAL NOT NULL,
    price     REAL NOT NULL
);

-- Current Alpaca positions, replaced on every collect.
CREATE TABLE IF NOT EXISTS positions (
    symbol        TEXT PRIMARY KEY,
    qty           REAL NOT NULL,
    market_value  REAL NOT NULL,
    unrealized_pl REAL NOT NULL,
    updated_at    TEXT NOT NULL
);
"""


def connect(path: str | None = None) -> sqlite3.Connection:
    path = path or config.DB_PATH
    if path != ":memory:":
        Path(path).parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    conn.executescript(SCHEMA)
    return conn


def upsert(conn: sqlite3.Connection, rows: list[dict]) -> int:
    """Insert or replace observations. Re-running a collector for the same day overwrites, never duplicates."""
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    conn.executemany(
        """INSERT INTO observations (date, metric, value, unit, source, updated_at)
           VALUES (:date, :metric, :value, :unit, :source, :updated_at)
           ON CONFLICT (date, metric, source)
           DO UPDATE SET value = excluded.value, unit = excluded.unit, updated_at = excluded.updated_at""",
        [{**r, "updated_at": now} for r in rows],
    )
    conn.commit()
    return len(rows)


def fetch_range(conn: sqlite3.Connection, start: str, end: str) -> list[sqlite3.Row]:
    return conn.execute(
        "SELECT date, metric, value, unit, source FROM observations WHERE date BETWEEN ? AND ? ORDER BY date, metric",
        (start, end),
    ).fetchall()

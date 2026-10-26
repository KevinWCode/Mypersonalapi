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

-- Meals from the food inbox folder or pasted on the dashboard. Daily food.* totals are recomputed from these.
CREATE TABLE IF NOT EXISTS meals (
    id          TEXT PRIMARY KEY,
    date        TEXT NOT NULL,
    meal        TEXT NOT NULL,  -- breakfast, lunch, dinner or snack
    kcal        REAL NOT NULL,
    protein_g   REAL NOT NULL,
    carbs_g     REAL NOT NULL,
    fat_g       REAL NOT NULL,
    description TEXT NOT NULL,
    origin      TEXT NOT NULL,  -- paste, or inbox:<file name>
    added_at    TEXT NOT NULL
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
    # FastAPI may open, use and close one request's connection on different worker threads. Each connection still
    # serves only one request at a time, so turning off sqlite's same-thread check is safe.
    conn = sqlite3.connect(path, check_same_thread=False)
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


def add(conn: sqlite3.Connection, day: str, entries: list[dict], source: str) -> dict[str, float]:
    """Add to (mode "add") or replace (mode "set") one day's values, never going below 0. Returns the new values."""
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    for e in entries:
        # excluded.value is already clamped to 0, so an add uses the raw amount (which is negative for an undo).
        new_value = "MAX(0, observations.value + ?)" if e["mode"] == "add" else "MAX(0, ?)"
        conn.execute(
            f"""INSERT INTO observations (date, metric, value, unit, source, updated_at)
                VALUES (?, ?, MAX(0, ?), ?, ?, ?)
                ON CONFLICT (date, metric, source)
                DO UPDATE SET value = {new_value}, unit = excluded.unit, updated_at = excluded.updated_at""",
            (day, e["metric"], e["value"], e["unit"], source, now, e["value"]),
        )
    conn.commit()
    metrics = sorted({e["metric"] for e in entries})
    rows = conn.execute(
        f"SELECT metric, value FROM observations WHERE date = ? AND source = ? AND metric IN ({','.join('?' * len(metrics))})",
        (day, source, *metrics),
    ).fetchall()
    return {metric: value for metric, value in rows}


def fetch_range(conn: sqlite3.Connection, start: str, end: str) -> list[sqlite3.Row]:
    return conn.execute(
        "SELECT date, metric, value, unit, source FROM observations WHERE date BETWEEN ? AND ? ORDER BY date, metric",
        (start, end),
    ).fetchall()


def last_updates(conn: sqlite3.Connection) -> dict[str, str]:
    """Most recent write time (UTC) per source."""
    rows = conn.execute("SELECT source, MAX(updated_at) FROM observations GROUP BY source").fetchall()
    return {source: at for source, at in rows}

"""Reads the Alpaca account: daily equity and P/L, fills, and open positions. Read-only: GET requests only."""
import logging
import sqlite3
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

import httpx

from app import config, db

MARKET_TZ = ZoneInfo("America/New_York")
log = logging.getLogger("collect.alpaca")


def _client() -> httpx.Client:
    return httpx.Client(
        base_url=config.ALPACA_BASE_URL,
        timeout=30,
        headers={"APCA-API-KEY-ID": config.ALPACA_KEY_ID, "APCA-API-SECRET-KEY": config.ALPACA_SECRET_KEY},
    )


def daily_pl(history: dict) -> list[dict]:
    """Daily equity and that day's P/L from a 1D portfolio history.

    Alpaca's daily profit_loss is cumulative from the start of the window, so a day's P/L is the change from the
    day before. The first day has no previous day and only gets its equity.
    """
    rows = []
    prev_pl = None
    for ts, equity, pl in zip(history["timestamp"], history["equity"], history["profit_loss"]):
        if equity is None or equity == 0:
            prev_pl = None
            continue
        day = datetime.fromtimestamp(ts, MARKET_TZ).date().isoformat()
        rows.append({"date": day, "metric": "trading.equity", "value": equity, "unit": "usd", "source": "alpaca"})
        if prev_pl is not None and pl is not None:
            rows.append({"date": day, "metric": "trading.pl_day", "value": pl - prev_pl, "unit": "usd", "source": "alpaca"})
        prev_pl = pl
    return rows


def collect(conn: sqlite3.Connection, client: httpx.Client | None = None) -> int:
    if not (config.ALPACA_KEY_ID and config.ALPACA_SECRET_KEY):
        log.info("alpaca: not configured, skipping")
        return 0
    client = client or _client()

    history = client.get("/v2/account/portfolio/history", params={"period": "1M", "timeframe": "1D"})
    history.raise_for_status()
    n = db.upsert(conn, daily_pl(history.json()))

    # Fill activities rather than orders: a busy order list is mostly cancelled or expired orders, which pushed
    # real buys out of the most recent 100.
    fills = client.get("/v2/account/activities/FILL", params={"direction": "desc", "page_size": 100})
    fills.raise_for_status()
    filled = fills.json()
    conn.executemany(
        "INSERT OR REPLACE INTO trades (id, filled_at, symbol, side, qty, price) VALUES (?, ?, ?, ?, ?, ?)",
        [(f["id"], f["transaction_time"], f["symbol"], f["side"], float(f["qty"]), float(f["price"])) for f in filled],
    )

    positions = client.get("/v2/positions")
    positions.raise_for_status()
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    conn.execute("DELETE FROM positions")
    conn.executemany(
        "INSERT INTO positions (symbol, qty, market_value, unrealized_pl, updated_at) VALUES (?, ?, ?, ?, ?)",
        [(p["symbol"], float(p["qty"]), float(p["market_value"]), float(p["unrealized_pl"]), now) for p in positions.json()],
    )
    conn.commit()
    return n + len(filled) + len(positions.json())


def summary(conn: sqlite3.Connection, today: str) -> dict:
    """What the dashboard shows: rolling P/L over 7 and 30 days, last buy and sell, and the biggest positions."""
    def pl_since(days: int) -> float:
        row = conn.execute(
            "SELECT COALESCE(SUM(value), 0) FROM observations WHERE metric = 'trading.pl_day' "
            "AND date > date(?, ?) AND date <= ?",
            (today, f"-{days} days", today),
        ).fetchone()
        return round(row[0], 2)

    def last(side: str) -> dict | None:
        row = conn.execute(
            # LIKE so short sales (sell_short) count as sells.
            "SELECT symbol, qty, price, filled_at FROM trades WHERE side LIKE ? ORDER BY filled_at DESC LIMIT 1",
            (f"{side}%",),
        ).fetchone()
        return dict(row) if row else None

    equity = conn.execute(
        "SELECT value FROM observations WHERE metric = 'trading.equity' ORDER BY date DESC LIMIT 1"
    ).fetchone()
    top = conn.execute(
        "SELECT symbol, qty, market_value, unrealized_pl FROM positions ORDER BY market_value DESC LIMIT 5"
    ).fetchall()
    return {
        "equity": equity[0] if equity else None,
        "pl_7d": pl_since(7),
        "pl_30d": pl_since(30),
        "last_buy": last("buy"),
        "last_sell": last("sell"),
        "top_positions": [dict(r) for r in top],
    }

"""Reads the Alpaca accounts: daily equity and P/L, fills, and open positions. Read-only: GET requests only.

Several paper accounts (one per trading bot) are stored per account and added together, so they show as one."""
import logging
import sqlite3
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

import httpx

from app import config, db

MARKET_TZ = ZoneInfo("America/New_York")
log = logging.getLogger("collect.alpaca")


def accounts() -> list[tuple[str, str, str]]:
    """(source, key id, secret) for each configured account. Each account's rows get their own source
    (alpaca1, alpaca2, ...) so one account's day doesn't overwrite another's."""
    pairs = [(config.ALPACA_KEY_ID, config.ALPACA_SECRET_KEY), *config.ALPACA_EXTRA_ACCOUNTS]
    return [(f"alpaca{i}", key, secret) for i, (key, secret) in enumerate(pairs, start=1) if key and secret]


def _client(key: str, secret: str) -> httpx.Client:
    return httpx.Client(
        base_url=config.ALPACA_BASE_URL,
        timeout=30,
        headers={"APCA-API-KEY-ID": key, "APCA-API-SECRET-KEY": secret},
    )


def daily_pl(history: dict, source: str = "alpaca1") -> list[dict]:
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
        rows.append({"date": day, "metric": "trading.equity", "value": equity, "unit": "usd", "source": source})
        if prev_pl is not None and pl is not None:
            rows.append({"date": day, "metric": "trading.pl_day", "value": pl - prev_pl, "unit": "usd", "source": source})
        prev_pl = pl
    return rows


def collect(conn: sqlite3.Connection, clients: dict[str, httpx.Client] | None = None) -> int:
    """Collects every account. clients maps source to client and is only passed by tests."""
    if clients is None:
        clients = {source: _client(key, secret) for source, key, secret in accounts()}
    if not clients:
        log.info("alpaca: not configured, skipping")
        return 0
    # Positions are a snapshot, so they're replaced for all accounts together.
    conn.execute("DELETE FROM positions")
    n = sum(_collect_account(conn, source, client) for source, client in clients.items())
    conn.commit()
    return n


def _collect_account(conn: sqlite3.Connection, source: str, client: httpx.Client) -> int:
    history = client.get("/v2/account/portfolio/history", params={"period": "1M", "timeframe": "1D"})
    history.raise_for_status()
    n = db.upsert(conn, daily_pl(history.json(), source))

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
    conn.executemany(
        # Two bots holding the same stock add up to one position.
        "INSERT INTO positions (symbol, qty, market_value, unrealized_pl, updated_at) VALUES (?, ?, ?, ?, ?) "
        "ON CONFLICT (symbol) DO UPDATE SET qty = qty + excluded.qty, market_value = market_value + excluded.market_value, "
        "unrealized_pl = unrealized_pl + excluded.unrealized_pl, updated_at = excluded.updated_at",
        [(p["symbol"], float(p["qty"]), float(p["market_value"]), float(p["unrealized_pl"]), now) for p in positions.json()],
    )
    return n + len(filled) + len(positions.json())


def summary(conn: sqlite3.Connection, today: str) -> dict:
    """What the dashboard shows: rolling P/L over 7 and 30 days, last buy and sell, recent fills and the biggest
    positions."""
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

    # All accounts added together on the latest day.
    equity = conn.execute(
        "SELECT SUM(value) FROM observations WHERE metric = 'trading.equity' "
        "AND date = (SELECT MAX(date) FROM observations WHERE metric = 'trading.equity')"
    ).fetchone()
    recent = conn.execute(
        "SELECT symbol, side, qty, price, filled_at FROM trades ORDER BY filled_at DESC LIMIT 5"
    ).fetchall()
    top = conn.execute(
        "SELECT symbol, qty, market_value, unrealized_pl FROM positions ORDER BY market_value DESC LIMIT 5"
    ).fetchall()
    return {
        "equity": equity[0] if equity and equity[0] is not None else None,
        "pl_7d": pl_since(7),
        "pl_30d": pl_since(30),
        "last_buy": last("buy"),
        "last_sell": last("sell"),
        "recent_trades": [dict(r) for r in recent],
        "top_positions": [dict(r) for r in top],
    }

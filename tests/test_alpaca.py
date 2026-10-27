from datetime import datetime
from zoneinfo import ZoneInfo

import httpx
from fastapi.testclient import TestClient

from app import config, db
from app.collectors import alpaca
from app.main import app

NY = ZoneInfo("America/New_York")


def ts(day: str) -> int:
    return int(datetime.fromisoformat(day).replace(tzinfo=NY).timestamp())


HISTORY = {
    "timestamp": [ts("2026-10-20"), ts("2026-10-21"), ts("2026-10-22"), ts("2026-10-23")],
    "equity": [1000.0, 1010.0, 1005.0, 1030.0],
    "profit_loss": [0.0, 10.0, 5.0, 30.0],
    "profit_loss_pct": [0, 0.01, 0.005, 0.03],
    "base_value": 1000.0,
}
FILLS = [
    {"id": "f4", "activity_type": "FILL", "type": "fill", "symbol": "NVDA", "side": "sell_short", "qty": "1", "price": "141", "transaction_time": "2026-10-23T16:00:00Z"},
    {"id": "f3", "activity_type": "FILL", "type": "fill", "symbol": "GMEX", "side": "sell", "qty": "2", "price": "2.84", "transaction_time": "2026-10-23T15:00:00Z"},
    {"id": "f2", "activity_type": "FILL", "type": "partial_fill", "symbol": "AAPL", "side": "buy", "qty": "1", "price": "230", "transaction_time": "2026-10-22T15:00:00Z"},
    {"id": "f1", "activity_type": "FILL", "type": "fill", "symbol": "MSFT", "side": "buy", "qty": "1", "price": "410", "transaction_time": "2026-10-21T15:00:00Z"},
]
POSITIONS = [
    {"symbol": "AAPL", "qty": "1", "market_value": "232", "unrealized_pl": "2"},
    {"symbol": "MSFT", "qty": "1", "market_value": "415", "unrealized_pl": "5"},
]


def fake_alpaca(history=HISTORY, fills=FILLS, positions=POSITIONS):
    def handler(request: httpx.Request):
        assert request.method == "GET"
        body = {
            "/v2/account/portfolio/history": history,
            "/v2/account/activities/FILL": fills,
            "/v2/positions": positions,
        }[request.url.path]
        return httpx.Response(200, json=body)
    return httpx.Client(base_url="https://paper-api.alpaca.markets", transport=httpx.MockTransport(handler))


def test_client_sends_keys():
    client = alpaca._client("key", "secret")
    assert client.headers["APCA-API-KEY-ID"] == "key"
    assert client.headers["APCA-API-SECRET-KEY"] == "secret"


def test_accounts_from_numbered_keys(monkeypatch):
    monkeypatch.setattr(config, "ALPACA_EXTRA_ACCOUNTS", [("key2", "secret2"), ("", ""), ("key4", "secret4")])
    assert alpaca.accounts() == [("alpaca1", "key", "secret"), ("alpaca2", "key2", "secret2"), ("alpaca4", "key4", "secret4")]


def test_daily_pl_is_change_from_previous_day():
    rows = alpaca.daily_pl(HISTORY)
    pl = {r["date"]: r["value"] for r in rows if r["metric"] == "trading.pl_day"}
    assert pl == {"2026-10-21": 10.0, "2026-10-22": -5.0, "2026-10-23": 25.0}


def test_collect_and_summary():
    conn = db.connect()
    alpaca.collect(conn, clients={"alpaca1": fake_alpaca()})
    s = alpaca.summary(conn, "2026-10-23")
    assert s["equity"] == 1030.0
    assert s["pl_30d"] == 30.0
    assert s["pl_7d"] == 30.0
    assert s["last_buy"]["symbol"] == "AAPL"
    assert s["last_sell"]["symbol"] == "NVDA"  # a short sale counts as a sell
    assert s["last_buy"]["price"] == 230.0
    assert [p["symbol"] for p in s["top_positions"]] == ["MSFT", "AAPL"]
    assert [t["symbol"] for t in s["recent_trades"]] == ["NVDA", "GMEX", "AAPL", "MSFT"]

    # A week later the oldest P/L days fall out of the 7-day window.
    assert alpaca.summary(conn, "2026-10-29")["pl_7d"] == 25.0


def test_several_accounts_show_as_one():
    conn = db.connect()
    other_history = {**HISTORY, "equity": [500.0, 505.0, 500.0, 510.0], "profit_loss": [0.0, 5.0, 0.0, 10.0]}
    other_fills = [{"id": "g1", "activity_type": "FILL", "type": "fill", "symbol": "AAPL", "side": "buy", "qty": "2",
                    "price": "231", "transaction_time": "2026-10-23T17:00:00Z"}]
    other_positions = [{"symbol": "AAPL", "qty": "2", "market_value": "464", "unrealized_pl": "2"}]
    alpaca.collect(conn, clients={
        "alpaca1": fake_alpaca(),
        "alpaca2": fake_alpaca(other_history, other_fills, other_positions),
    })
    s = alpaca.summary(conn, "2026-10-23")
    assert s["equity"] == 1540.0
    assert s["pl_30d"] == 40.0
    assert s["last_buy"]["symbol"] == "AAPL" and s["last_buy"]["qty"] == 2
    assert s["recent_trades"][0]["symbol"] == "AAPL"
    assert s["top_positions"][0] == {"symbol": "AAPL", "qty": 3.0, "market_value": 696.0, "unrealized_pl": 4.0}
    # The dashboard's daily P/L adds the accounts up too.
    day = TestClient(app).get("/days", params={"start": "2026-10-23", "end": "2026-10-23"}).json()["days"][0]
    assert day["metrics"]["trading.pl_day"] == 35.0


def test_skips_when_not_configured(monkeypatch):
    monkeypatch.setattr(config, "ALPACA_KEY_ID", "")
    monkeypatch.setattr(config, "ALPACA_EXTRA_ACCOUNTS", [])
    assert alpaca.collect(db.connect()) == 0


def test_trading_endpoint_works_with_no_data():
    body = TestClient(app).get("/trading").json()
    assert body["last_buy"] is None and body["pl_30d"] == 0

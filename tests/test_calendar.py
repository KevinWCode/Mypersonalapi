from datetime import date
from zoneinfo import ZoneInfo

import httpx
from fastapi.testclient import TestClient

from app import config, db
from app.collectors import calendar
from app.main import app

client = TestClient(app)
AUTH = {"Authorization": "Bearer secret"}
LONDON = ZoneInfo("Europe/London")

ICS = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//test//EN
BEGIN:VEVENT
UID:study-1
DTSTART:20261108T090000Z
DTEND:20261108T100000Z
SUMMARY:Study block
RRULE:FREQ=DAILY;COUNT=3
END:VEVENT
BEGIN:VEVENT
UID:cook-1
DTSTART;TZID=Europe/London:20261108T180000
DTEND;TZID=Europe/London:20261108T190000
SUMMARY:Cook dinner
END:VEVENT
BEGIN:VEVENT
UID:holiday
DTSTART;VALUE=DATE:20261108
SUMMARY:Bank holiday
END:VEVENT
BEGIN:VEVENT
UID:old
DTSTART:20260101T090000Z
DTEND:20260101T100000Z
SUMMARY:Long ago
END:VEVENT
END:VCALENDAR
"""


def test_parse_expands_repeats_skips_all_day_and_old():
    events = calendar.parse(ICS, date(2026, 11, 8), LONDON)
    assert [(e["date"], e["title"], e["kind"]) for e in events] == [
        ("2026-11-08", "Study block", "study"), ("2026-11-08", "Cook dinner", "cook"),
        ("2026-11-09", "Study block", "study"), ("2026-11-10", "Study block", "study"),
    ]
    assert len({e["id"] for e in events}) == 4  # each repeat gets its own id


def test_collect_then_tick_earns_calendar_xp(monkeypatch):
    monkeypatch.setattr(config, "GOOGLE_CALENDAR_ICS_URL", "https://calendar.example/secret.ics")
    fake = httpx.Client(transport=httpx.MockTransport(lambda r: httpx.Response(200, text=ICS)))
    conn = db.connect()
    assert calendar.collect(conn, client=fake, today=date(2026, 11, 8)) == 4

    events = client.get("/calendar", params={"date": "2026-11-08"}).json()["events"]
    assert [e["title"] for e in events] == ["Study block", "Cook dinner"] and not any(e["done"] for e in events)

    def line():
        g = client.get("/game", params={"date": "2026-11-08"}).json()
        return next(x for x in g["today"]["metrics"] if x["key"] == "calendar")

    assert line()["xp"] == 0 and line()["detail"] == "0 of 2 block(s) kept"
    r = client.post("/log", headers=AUTH, json={"date": "2026-11-08", "entries": [
        {"metric": events[0]["done_metric"], "value": 1, "unit": "count", "mode": "set"}]})
    assert r.status_code == 200
    assert client.get("/calendar", params={"date": "2026-11-08"}).json()["events"][0]["done"] is True
    assert line()["xp"] == 50 and line()["detail"] == "1 of 2 block(s) kept"


def test_not_configured_skips():
    assert calendar.collect(db.connect()) == 0


def test_kinds():
    assert calendar.kind_of("Gym: kettlebell") == "workout"
    assert calendar.kind_of("Meal prep") == "cook"
    assert calendar.kind_of("Dentist") == "other"

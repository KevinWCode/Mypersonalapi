"""Reads Google Calendar through its secret iCal address (Settings > your calendar > "Secret address in iCal
format"). Read-only, no Google sign-in. Timed events from 3 days ago to 7 days ahead are stored; each one can be
ticked done on the dashboard, and kept blocks earn calendar XP."""
import hashlib
import logging
import sqlite3
from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

import httpx
import icalendar
import recurring_ical_events

from app import config

log = logging.getLogger("collect.calendar")

# A block's kind comes from words in its title. Anything else is "other".
KINDS = {
    "study": ("study", "revise", "revision", "learn", "course", "read"),
    "cook": ("cook", "meal prep", "dinner", "lunch prep"),
    "workout": ("workout", "gym", "kettlebell", "run", "sprint", "training", "football", "catch"),
}
DAYS_BACK, DAYS_AHEAD = 3, 7


def kind_of(title: str) -> str:
    t = title.lower()
    return next((k for k, words in KINDS.items() if any(w in t for w in words)), "other")


def done_metric(event_id: str) -> str:
    return f"cal.{event_id}.done"


def parse(ics: str, today: date, tz: ZoneInfo) -> list[dict]:
    cal = icalendar.Calendar.from_ical(ics)
    start = datetime.combine(today - timedelta(days=DAYS_BACK), datetime.min.time(), tz)
    end = datetime.combine(today + timedelta(days=DAYS_AHEAD + 1), datetime.min.time(), tz)
    events = []
    for ev in recurring_ical_events.of(cal).between(start, end):
        s, e = ev.get("DTSTART").dt, ev.get("DTEND").dt if ev.get("DTEND") else None
        if not isinstance(s, datetime):
            continue  # all-day events aren't blocks
        s = s.astimezone(tz) if s.tzinfo else s.replace(tzinfo=tz)
        e = (e.astimezone(tz) if e.tzinfo else e.replace(tzinfo=tz)) if isinstance(e, datetime) else s
        title = str(ev.get("SUMMARY") or "Untitled")
        # One id per occurrence, so each repeat of a weekly block is ticked on its own.
        event_id = hashlib.sha1(f"{ev.get('UID')}|{s.isoformat()}".encode()).hexdigest()[:12]
        events.append({"id": event_id, "date": s.date().isoformat(), "start": s.isoformat(timespec="minutes"),
                       "end": e.isoformat(timespec="minutes"), "title": title, "kind": kind_of(title)})
    return sorted(events, key=lambda x: x["start"])


def collect(conn: sqlite3.Connection, client: httpx.Client | None = None, today: date | None = None) -> int:
    if not config.GOOGLE_CALENDAR_ICS_URL:
        log.info("calendar: not configured, skipping")
        return 0
    tz = ZoneInfo(config.CALENDAR_TZ)
    today = today or datetime.now(tz).date()
    client = client or httpx.Client(timeout=30, follow_redirects=True)
    resp = client.get(config.GOOGLE_CALENDAR_ICS_URL)
    resp.raise_for_status()
    events = parse(resp.text, today, tz)
    first = (today - timedelta(days=DAYS_BACK)).isoformat()
    last = (today + timedelta(days=DAYS_AHEAD)).isoformat()
    # The window is replaced as a whole, so moved or deleted events disappear. Ticks stay, keyed by event id.
    conn.execute("DELETE FROM calendar_events WHERE date BETWEEN ? AND ?", (first, last))
    conn.executemany("INSERT OR REPLACE INTO calendar_events (id, date, start, end, title, kind) "
                     "VALUES (:id, :date, :start, :end, :title, :kind)", events)
    conn.commit()
    return len(events)


def for_day(conn: sqlite3.Connection, day: str) -> list[dict]:
    rows = conn.execute("SELECT id, start, end, title, kind FROM calendar_events WHERE date = ? ORDER BY start",
                        (day,)).fetchall()
    done = {r[0] for r in conn.execute(
        "SELECT metric FROM observations WHERE date = ? AND metric LIKE 'cal.%.done' AND value > 0", (day,))}
    return [{**dict(r), "done": done_metric(r["id"]) in done, "done_metric": done_metric(r["id"])} for r in rows]


def counts_by_day(conn: sqlite3.Connection, start: str, end: str) -> dict[str, int]:
    return dict(conn.execute("SELECT date, COUNT(*) FROM calendar_events WHERE date BETWEEN ? AND ? GROUP BY date",
                             (start, end)).fetchall())

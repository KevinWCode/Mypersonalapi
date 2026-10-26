"""Today's headline numbers in one small response, for the phone's home screen widget."""
from datetime import date, timedelta

from app import db
from app.metrics import build_days


def _social(metrics: dict) -> float | None:
    # A day only counts once the phone has sent its screen time, so a missing day isn't read as 0 minutes.
    if "screen.total_minutes" not in metrics:
        return None
    return metrics.get("screen.social_media_minutes", 0)


def _avg(values: list[float]) -> float | None:
    return sum(values) / len(values) if values else None


def build(conn, day: date) -> dict:
    start = day - timedelta(days=13)
    by_date = {d["date"]: d["metrics"] for d in build_days(db.fetch_range(conn, start.isoformat(), day.isoformat()))}

    def week(offset: int) -> list[float]:
        days = ((day - timedelta(days=i)).isoformat() for i in range(offset, offset + 7))
        return [v for v in (_social(by_date.get(d, {})) for d in days) if v is not None]

    # Same sum as the dashboard headline: last week's pace over the days measured this week, minus this week.
    this_week, last_week = week(0), week(7)
    reclaimed = _avg(last_week) * len(this_week) - sum(this_week) if this_week and last_week else None
    today = by_date.get(day.isoformat(), {})
    return {
        "date": day.isoformat(),
        "social": {
            "reclaimed_minutes": reclaimed,
            "week_minutes": sum(this_week) if this_week else None,
            "week_per_day": _avg(this_week),
            "last_week_per_day": _avg(last_week),
        },
        "kcal": today.get("food.kcal"),
        "protein_g": today.get("food.protein_g"),
        # Sleep is filed under the day you wake up, so today's is last night's.
        "sleep_minutes": today.get("sleep.total_minutes"),
        "steps": today.get("health.steps"),
    }

"""Turns raw observations into the per-day numbers dashboards read, including category totals."""
from collections import defaultdict

# Android package names counted as social media. Add apps here to include them in the total.
SOCIAL_MEDIA_APPS = {
    "com.zhiliaoapp.musically": "tiktok",
    "com.ss.android.ugc.trill": "tiktok",  # TikTok's package name in some regions
    "com.instagram.android": "instagram",
}

APP_PREFIX = "screen.app."


def build_days(rows) -> list[dict]:
    days: dict[str, dict[str, float]] = defaultdict(dict)
    for r in rows:
        metric, value = r["metric"], r["value"]
        day = days[r["date"]]
        if metric.startswith(APP_PREFIX):
            package = metric[len(APP_PREFIX):]
            name = SOCIAL_MEDIA_APPS.get(package)
            if name is None:
                continue
            key = f"screen.{name}_minutes"
            day[key] = day.get(key, 0) + value
            day["screen.social_media_minutes"] = day.get("screen.social_media_minutes", 0) + value
        else:
            day[metric] = day.get(metric, 0) + value
    return [{"date": d, "metrics": days[d]} for d in sorted(days)]

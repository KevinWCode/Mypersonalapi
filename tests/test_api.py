from concurrent.futures import ThreadPoolExecutor

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
AUTH = {"Authorization": "Bearer secret"}


def post(observations, headers=AUTH):
    return client.post("/ingest", json={"source": "phone", "observations": observations}, headers=headers)


def test_ingest_rejects_bad_token():
    assert post([], headers={"Authorization": "Bearer nope"}).status_code == 401


def test_social_media_total_adds_tiktok_and_instagram():
    post([
        {"date": "2026-10-23", "metric": "screen.app.com.zhiliaoapp.musically", "value": 30, "unit": "minutes"},
        {"date": "2026-10-23", "metric": "screen.app.com.instagram.android", "value": 15, "unit": "minutes"},
        {"date": "2026-10-23", "metric": "screen.app.com.android.chrome", "value": 50, "unit": "minutes"},
    ])
    day = client.get("/days", params={"start": "2026-10-23", "end": "2026-10-23"}).json()["days"][0]
    assert day["metrics"] == {
        "screen.tiktok_minutes": 30,
        "screen.instagram_minutes": 15,
        "screen.social_media_minutes": 45,
        "screen.total_minutes": 95,
    }


def test_reupload_overwrites_instead_of_doubling():
    obs = [{"date": "2026-10-22", "metric": "screen.app.com.instagram.android", "value": 10, "unit": "minutes"}]
    post(obs)
    obs[0]["value"] = 12
    post(obs)
    day = client.get("/days", params={"start": "2026-10-22", "end": "2026-10-22"}).json()["days"][0]
    assert day["metrics"]["screen.social_media_minutes"] == 12


def test_days_rejects_reversed_range():
    assert client.get("/days", params={"start": "2026-10-23", "end": "2026-10-01"}).status_code == 400


def test_status_shows_last_sync_per_source():
    assert client.get("/status").json() == {"last_update": {}}
    post([{"date": "2026-10-23", "metric": "health.steps", "value": 9000, "unit": "count"}])
    assert set(client.get("/status").json()["last_update"]) == {"phone"}


def test_dashboard_page_is_served():
    page = client.get("/dashboard")
    assert page.status_code == 200
    assert page.headers["content-type"].startswith("text/html")
    assert "Freedom gained" in page.text
    assert client.get("/", follow_redirects=False).headers["location"] == "/dashboard"


def test_parallel_requests_like_the_dashboard_makes():
    with ThreadPoolExecutor(6) as pool:
        codes = list(pool.map(lambda path: client.get(path).status_code, ["/days", "/trading", "/status"] * 10))
    assert set(codes) == {200}


def log(entries, day="2026-10-26", headers=AUTH):
    return client.post("/log", json={"date": day, "entries": entries}, headers=headers)


def test_log_adds_up_taps():
    log([{"metric": "water.ml", "value": 250, "unit": "ml"}])
    log([{"metric": "water.ml", "value": 250, "unit": "ml"}])
    body = log([{"metric": "study.minutes", "value": 30, "unit": "minutes"}, {"metric": "food.protein_g", "value": 25, "unit": "g"}]).json()
    assert body["values"] == {"study.minutes": 30, "food.protein_g": 25}
    day = client.get("/days", params={"start": "2026-10-26", "end": "2026-10-26"}).json()["days"][0]["metrics"]
    assert day["water.ml"] == 500


def test_log_undo_and_set_never_go_below_zero():
    log([{"metric": "coffee.cups", "value": 1, "unit": "cups"}])
    assert log([{"metric": "coffee.cups", "value": -3, "unit": "cups"}]).json()["values"] == {"coffee.cups": 0}
    assert log([{"metric": "kb.swing.done", "value": 1, "unit": "count", "mode": "set"}]).json()["values"] == {"kb.swing.done": 1}
    assert log([{"metric": "kb.swing.done", "value": 0, "unit": "count", "mode": "set"}]).json()["values"] == {"kb.swing.done": 0}


def test_log_needs_the_token_and_only_takes_dashboard_metrics():
    assert log([], headers={"Authorization": "Bearer nope"}).status_code == 401
    assert log([]).json() == {"values": {}}
    assert log([{"metric": "health.steps", "value": 99999, "unit": "count"}]).status_code == 422
    assert log([{"metric": "food.kcal", "value": 10, "unit": "kcal"}]).status_code == 422  # meals go through /meals


def test_summary_for_the_widget():
    obs = []
    for i in range(14):
        d = f"2026-10-{26 - i:02d}"
        # 60 min of TikTok a day last week, 40 this week; one day this week the phone didn't sync.
        if i != 3:
            obs.append({"date": d, "metric": "screen.app.com.zhiliaoapp.musically", "value": 40 if i < 7 else 60, "unit": "minutes"})
    obs += [
        {"date": "2026-10-26", "metric": "sleep.total_minutes", "value": 432, "unit": "minutes"},
        {"date": "2026-10-26", "metric": "health.steps", "value": 2100, "unit": "count"},
        {"date": "2026-10-26", "metric": "food.protein_g", "value": 25, "unit": "g"},
    ]
    post(obs)
    client.post("/meals", json={"date": "2026-10-26", "meals": [{"meal": "breakfast", "kcal": 260, "protein_g": 9}]}, headers=AUTH)

    s = client.get("/summary", params={"date": "2026-10-26"}).json()
    assert s["social"] == {"reclaimed_minutes": 120, "week_minutes": 240, "week_per_day": 40, "last_week_per_day": 60}
    # Protein adds the meal to the +25 g Quick log tap, like the dashboard.
    assert (s["kcal"], s["protein_g"], s["sleep_minutes"], s["steps"]) == (260, 34, 432, 2100)

    empty = client.get("/summary", params={"date": "2026-01-01"}).json()
    assert empty["social"]["reclaimed_minutes"] is None and empty["kcal"] is None and empty["steps"] is None

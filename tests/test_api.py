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

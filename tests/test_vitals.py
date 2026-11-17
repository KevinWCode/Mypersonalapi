from datetime import date, datetime

from fastapi.testclient import TestClient

from app import game, perks, vitals
from app.main import app

client = TestClient(app)
AUTH = {"Authorization": "Bearer secret"}


def log(day, metric, value=1, unit="count"):
    r = client.post("/log", json={"date": day, "entries": [{"metric": metric, "value": value, "unit": unit}]}, headers=AUTH)
    assert r.status_code == 200


def bars(m, yt=0):
    return {b["key"]: b for b in vitals.run([(date(2026, 10, 23), m, yt)])}


def test_empty_day_just_ticks_down():
    b = bars({})
    assert {k: v["value"] for k, v in b.items()} == {"hp": 40, "stamina": 45, "mana": 50, "focus": 50}


def test_great_day_refills_hp():
    m = {"sleep.total_minutes": 480, "food.kcal": 2100, "food.protein_g": 160, "water.ml": 3000, "health.steps": 11000}
    b = bars(m)
    assert b["hp"]["gained_today"] == 50 and b["hp"]["value"] == 90  # 70 + 50 - 30


def test_poor_sleep_drains_stamina_mana_and_focus():
    b = bars({"sleep.total_minutes": 300})
    labels = {k: [e["label"] for e in v["today"]] for k, v in b.items()}
    assert "Poor sleep (under 6 h)" in labels["stamina"] and "Poor sleep (under 6 h)" in labels["mana"]
    assert "Poor sleep (under 6 h)" in labels["focus"]
    # 300/450 of the 35 sleep refill, then -15, then the 25 tick.
    assert b["stamina"]["value"] == round(70 + 35 * 300 / 450 - 15 - 25)


def test_journal_and_social_media_move_mana_and_focus():
    good = bars({"journal.entries": 1, "screen.total_minutes": 100, "screen.social_media_minutes": 10})
    bad = bars({"screen.total_minutes": 200, "screen.social_media_minutes": 90}, yt=120)
    assert good["mana"]["gained_today"] == 15 and good["focus"]["gained_today"] == 18
    assert bad["focus"]["gained_today"] == -25


def test_bars_stay_between_0_and_100_and_carry_over():
    full = {"sleep.total_minutes": 480, "food.kcal": 2000, "food.protein_g": 150, "water.ml": 3000, "health.steps": 10000}
    days = [(date(2026, 10, 23 + i), full, 0) for i in range(5)]
    assert {b["key"]: b["value"] for b in vitals.run(days)}["hp"] == 100
    empty = [(date(2026, 10, 1 + i), {}, 0) for i in range(10)]
    out = {b["key"]: b for b in vitals.run(empty)}
    assert out["hp"]["value"] == 0 and out["hp"]["low"]


def test_today_drains_through_the_day():
    b = {x["key"]: x for x in vitals.run([(date(2026, 10, 23), {}, 0)], today_elapsed=0.5)}
    assert b["hp"]["value"] == 55 and b["hp"]["end_of_day"] == 40


def test_today_elapsed():
    now = datetime(2026, 11, 17, 12, 0)
    assert game.today_elapsed(date(2026, 11, 16), now) == 1.0
    assert game.today_elapsed(date(2026, 11, 17), now) == 0.5
    assert game.today_elapsed(date(2026, 11, 18), now) == 0.0


def test_journal_button_scores_xp_and_bars():
    d = "2026-10-24"
    log(d, "journal.entries")
    g = client.get("/game", params={"date": d}).json()
    journal = next(line for line in g["today"]["metrics"] if line["key"] == "journal")
    assert journal["xp"] == 75 and journal["max"] == 100
    log(d, "journal.entries")
    g = client.get("/game", params={"date": d}).json()
    assert next(line for line in g["today"]["metrics"] if line["key"] == "journal")["xp"] == 100
    assert [b["key"] for b in g["vitals"]] == ["hp", "stamina", "mana", "focus"]


def test_journal_counts_for_wis_and_per():
    assert "journal" in perks.GROUPS["wis"][2] and "journal" in perks.GROUPS["per"][2]


def test_full_night_and_low_social_keep_focus_level():
    b = bars({"sleep.total_minutes": 450, "screen.total_minutes": 100, "screen.social_media_minutes": 20})
    assert b["focus"]["gained_today"] == 18 and b["mana"]["gained_today"] == 10

from fastapi.testclient import TestClient

from app import game
from app.main import app

client = TestClient(app)
AUTH = {"Authorization": "Bearer secret"}


def post(observations, source="phone"):
    r = client.post("/ingest", json={"source": source, "observations": observations}, headers=AUTH)
    assert r.status_code == 200


def obs(day, metric, value, unit="count"):
    return {"date": day, "metric": metric, "value": value, "unit": unit}


def test_level_curve_matches_the_spec():
    assert game.level_cost(1) == 500
    assert game.level_cost(27) == 2060
    assert game.level_cost(28) == 2300
    assert game.level_cost(56) == 3800
    assert game.level_cost(84) == 5300
    assert game.level_cost(109) == 6550
    assert game.XP_TO_MAX == 397_210
    assert game.level_for(0)["level"] == 1
    assert game.level_for(499)["level"] == 1
    assert game.level_for(500) == {"level": 2, "tier": "Initiate", "level_xp": 0, "level_cost": 560, "xp_to_next": 560}
    assert game.level_for(game.XP_TO_MAX)["level"] == 110
    assert game.level_for(10**9)["level"] == 110


def test_daily_max():
    assert game.DAILY_MAX == 1150


def test_full_day_scores_every_line():
    d = "2026-10-24"
    post([
        obs(d, "sleep.total_minutes", 480, "minutes"), obs(d, "sleep.deep_minutes", 90, "minutes"),
        obs(d, "sleep.rem_minutes", 100, "minutes"), obs(d, "health.resting_heart_rate", 55, "bpm"),
        obs(d, "health.steps", 12500), obs(d, "workout.sessions", 1), obs(d, "workout.minutes", 45, "minutes"),
        obs(d, "kb.swing.sets", 3), obs(d, "football.max_catch_yards", 15, "yards"),
        obs(d, "screen.app.com.instagram.android", 20, "minutes"),
        obs(d, "screen.app.com.google.android.youtube", 30, "minutes"),
    ])
    post([obs(d, "github.commits", 6)], source="github")
    g = client.get("/game", params={"date": d}).json()
    xp = {line["key"]: line["xp"] for line in g["today"]["metrics"]}
    assert xp == {"sleep": 125, "heart": 40, "steps": 100, "workout": 115, "kettlebell": 75, "catch": 50,
                  "calendar": 0, "github": 100, "email": 0, "journal": 0, "youtube": 50, "social": 100, "stocks": 0}
    # 755 from the stats, plus 10% for waking Well-Rested after 8 h.
    assert g["today"]["buffs"] == [{"name": "Well-Rested", "detail": "7.5 h+ sleep: +10% XP today", "xp": 76}]
    assert g["today"]["xp"] == 831
    # Day 1 (Oct 23) had no data, so the total is just this day.
    assert g["total_xp"] == 831 and g["level"] == 2 and g["day_number"] == 2


def test_social_media_penalty_never_takes_xp_below_zero():
    d = "2026-10-25"
    post([obs(d, "screen.app.com.zhiliaoapp.musically", 90, "minutes"),
          obs(d, "screen.app.com.google.android.youtube", 90, "minutes")])
    g = client.get("/game", params={"date": d}).json()
    social = next(line for line in g["today"]["metrics"] if line["key"] == "social")
    assert social["xp"] == -50
    assert sum(line["xp"] for line in g["today"]["metrics"]) == -50
    assert g["today"]["xp"] == 0  # the day can't go below 0
    assert g["level"] == 1


def test_social_between_30_and_60_scores_zero():
    d = "2026-10-25"
    post([obs(d, "screen.app.com.instagram.android", 45, "minutes")])
    g = client.get("/game", params={"date": d}).json()
    assert next(line for line in g["today"]["metrics"] if line["key"] == "social")["xp"] == 0


def test_missing_phone_day_is_not_scored_as_zero_screen_time():
    g = client.get("/game", params={"date": "2026-10-26"}).json()
    lines = {line["key"]: line for line in g["today"]["metrics"]}
    assert lines["social"]["xp"] == 0 and lines["social"]["detail"] == "Phone not synced"
    assert g["total_xp"] == 0


def test_resting_hr_compares_with_earlier_days():
    post([obs(f"2026-10-{n}", "health.resting_heart_rate", 55, "bpm") for n in (23, 24, 25)])
    post([obs("2026-10-26", "health.resting_heart_rate", 62, "bpm")])
    g = client.get("/game", params={"date": "2026-10-26"}).json()
    assert next(line for line in g["today"]["metrics"] if line["key"] == "heart")["xp"] == 0


def test_stocks_flat_or_up_and_new_capital():
    post([obs("2026-10-27", "trading.equity", 1500, "usd")], source="alpaca1")
    post([obs("2026-10-28", "trading.equity", 2010, "usd"), obs("2026-10-28", "trading.pl_day", 10, "usd")],
         source="alpaca1")
    g = client.get("/game", params={"date": "2026-10-28"}).json()
    stocks = next(line for line in g["today"]["metrics"] if line["key"] == "stocks")
    assert stocks["xp"] == 75 and "capital added" in stocks["detail"]


def test_before_game_start_is_level_one():
    g = client.get("/game", params={"date": "2026-10-01"}).json()
    assert g["level"] == 1 and g["total_xp"] == 0 and g["history"] == []


def test_each_stat_has_its_own_level():
    # Max sleep every day levels Sleep at the character's pace: 125/1150 of the character's curve.
    sheet = game.stat_sheet("sleep", "Sleep", "Rest", 0, 125)
    assert sheet["level"] == 1 and sheet["rank"] == "Novice" and sheet["score"] == 8 and sheet["modifier"] == -1
    assert sheet["level_cost"] == round(500 * 125 / 1150)
    top = game.stat_sheet("sleep", "Sleep", "Rest", game.XP_TO_MAX * 125 / 1150, 125)
    assert top["level"] == 110 and top["rank"] == "Master" and top["score"] == 20 and top["modifier"] == 5


def test_stats_in_game_response():
    d = "2026-10-24"
    post([obs(d, "sleep.total_minutes", 480, "minutes"), obs(d, "screen.app.com.zhiliaoapp.musically", 90, "minutes")])
    stats = {s["key"]: s for s in client.get("/game", params={"date": d}).json()["stats"]}
    assert len(stats) == 13
    assert stats["sleep"]["xp"] == 75 and stats["sleep"]["level"] == 2  # Sleep's level 1 costs 54 XP
    assert stats["social"]["xp"] == 0  # a penalty can't take a stat below 0


def test_short_sleep_gets_no_buff():
    d = "2026-10-24"
    post([obs(d, "sleep.total_minutes", 400, "minutes"), obs(d, "health.steps", 5000)])
    g = client.get("/game", params={"date": d}).json()
    assert g["today"]["buffs"] == [] and g["today"]["xp"] == 50


def sheet(key, level, xp=0):
    return {"key": key, "level": level, "xp": xp}


def all_stats(**levels):
    keys = [k for _, _, keys in game.perks.GROUPS.values() for k in keys]
    return [sheet(k, levels.get(k, 1)) for k in keys]


def test_class_from_two_strongest_groups():
    assert game.character_class(all_stats())["name"] == "Adventurer"
    assert game.character_class(all_stats(kettlebell=6, sleep=5))["name"] == "Ironclad"
    assert game.character_class(all_stats(github=9, social=8))["name"] == "Runesmith"
    # One group at double the next is a pure class.
    c = game.character_class(all_stats(kettlebell=10, steps=5))
    assert c == {"name": "Juggernaut", "groups": ["str"], "detail": "Pure STR"}


def test_group_tiers_titles_and_tokens():
    groups = {g["key"]: g for g in game.group_sheets(all_stats(kettlebell=30, sleep=9), {("str", 1): "2026-11-01"})}
    s = groups["str"]
    assert s["level"] == 30 and s["tier"] == 2 and s["title"] == "Lord of the Kettlebells"
    assert s["next_tier_level"] == 56
    assert [p["earned"] for p in s["perks"]] == [True, True, False, False]
    assert s["perks"][0]["claimed_on"] == "2026-11-01" and s["perks"][1]["claimed_on"] is None
    assert groups["con"]["tier"] == 0 and groups["con"]["title"] is None


def log(day, metric):
    r = client.post("/log", json={"date": day, "entries": [{"metric": metric, "value": 1, "unit": "count", "mode": "set"}]},
                    headers=AUTH)
    assert r.status_code == 200


def test_unearned_claim_is_ignored():
    log("2026-10-24", "perk.wis.1.claimed")
    g = client.get("/game", params={"date": "2026-10-24"}).json()
    wis = next(x for x in g["groups"] if x["key"] == "wis")
    assert wis["perks"][0]["claimed_on"] is None and g["today"]["buffs"] == []


def test_media_pass_waives_screen_penalties(monkeypatch):
    # Pretend Focus is already tier 1 by lowering its unlock level.
    monkeypatch.setitem(game.perks.TIERS, 1, ("Novice", 1))
    d = "2026-10-24"
    post([obs(d, "screen.app.com.zhiliaoapp.musically", 120, "minutes"),
          obs(d, "screen.app.com.google.android.youtube", 200, "minutes")])
    log(d, "perk.wis.1.claimed")
    g = client.get("/game", params={"date": d}).json()
    xp = {line["key"]: line["xp"] for line in g["today"]["metrics"]}
    assert xp["social"] == 0 and xp["youtube"] == 50
    assert g["today"]["buffs"][0]["name"] == "Unrestricted Media Pass"


def test_snooze_pass_gives_well_rested(monkeypatch):
    monkeypatch.setitem(game.perks.TIERS, 1, ("Novice", 1))
    d = "2026-10-24"
    post([obs(d, "health.steps", 10000)])
    log(d, "perk.con.1.claimed")
    g = client.get("/game", params={"date": d}).json()
    assert g["today"]["xp"] == 110
    assert g["today"]["buffs"] == [{"name": "Well-Rested", "xp": 10, "detail": "Snooze Button Pass: +10% XP today"}]


def test_perk_claims_only_accept_real_tokens():
    r = client.post("/log", json={"date": "2026-10-24", "entries": [
        {"metric": "perk.xyz.9.claimed", "value": 1, "unit": "count", "mode": "set"}]}, headers=AUTH)
    assert r.status_code == 422


def test_class_in_game_response():
    g = client.get("/game", params={"date": "2026-10-24"}).json()
    assert g["class"]["name"] == "Adventurer"

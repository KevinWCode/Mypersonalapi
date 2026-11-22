"""Progression simulator: runs five made-up player profiles through the current XP rules for 365 days.

Stage 1 of the rework brief: check the level curve, attribute balance and classes before changing them.
Uses app/game.py (kept in parity with the phone's Game.kt). Nothing is read from or written to any database.

    python -m tools.simulate_progression            # table for 30/90/180/365 days
"""
import random
from collections import defaultdict

from app import game, perks

DAYS = 365
CHECKPOINTS = [30, 90, 180, 365]


def chance(p):
    return random.random() < p


# Each profile returns one day's metrics. Keys match what the phone app records.
def fitness(d):
    m = {"sleep.total_minutes": random.gauss(460, 30), "health.steps": random.gauss(11000, 2500),
         "screen.communication_minutes": random.gauss(15, 10), "screen.total_minutes": 200, "screen.social_media_minutes": random.gauss(45, 15),
         "screen.youtube_minutes": 40, "health.resting_heart_rate": 55}
    m["sleep.deep_minutes"] = m["sleep.rem_minutes"] = m["sleep.total_minutes"] * 0.18
    if chance(5 / 7): m.update({"workout.sessions": 1, "workout.minutes": 50})
    if chance(3 / 7): m["kb.swing.sets"] = 5
    if chance(2 / 7): m["football.max_catch_yards"] = 18
    if chance(1 / 7): m["reading.minutes"] = 20
    return m


def study(d):
    m = {"sleep.total_minutes": random.gauss(420, 40), "health.steps": random.gauss(5000, 1500),
         "screen.communication_minutes": random.gauss(10, 10), "screen.total_minutes": 240, "screen.social_media_minutes": random.gauss(35, 15),
         "screen.youtube_minutes": 50, "study.minutes": random.gauss(120, 30), "reading.minutes": 30,
         "health.resting_heart_rate": 64}
    m["sleep.deep_minutes"] = m["sleep.rem_minutes"] = m["sleep.total_minutes"] * 0.16
    if chance(0.6): m["journal.entries"] = 1
    if chance(1 / 7): m.update({"workout.sessions": 1, "workout.minutes": 30})
    return m


def average(d):
    m = {"sleep.total_minutes": random.gauss(400, 45), "health.steps": random.gauss(6500, 2000),
         "screen.communication_minutes": random.gauss(25, 10), "screen.total_minutes": 300, "screen.social_media_minutes": random.gauss(80, 30),
         "screen.youtube_minutes": 70, "health.resting_heart_rate": 66}
    m["sleep.deep_minutes"] = m["sleep.rem_minutes"] = m["sleep.total_minutes"] * 0.15
    if chance(2 / 7): m.update({"workout.sessions": 1, "workout.minutes": 35})
    if chance(0.2): m["journal.entries"] = 1
    if chance(0.3): m["reading.minutes"] = 20
    return m


def disciplined(d):
    m = {"sleep.total_minutes": random.gauss(480, 20), "health.steps": random.gauss(10500, 2000),
         "screen.communication_minutes": random.gauss(30, 10), "screen.total_minutes": 120, "screen.social_media_minutes": random.gauss(15, 8),
         "screen.youtube_minutes": 20, "study.minutes": 60, "reading.minutes": 30,
         "journal.entries": 1, "github.commits": random.choice([0, 1, 2, 4]),
         "health.resting_heart_rate": 54, "trading.pl_day": random.gauss(5, 20)}
    m["sleep.deep_minutes"] = m["sleep.rem_minutes"] = m["sleep.total_minutes"] * 0.19
    if chance(5 / 7): m.update({"workout.sessions": 1, "workout.minutes": 45})
    if chance(3 / 7): m["kb.swing.sets"] = 5
    if chance(2 / 7): m["football.max_catch_yards"] = 20
    return m


def low(d):
    m = {"sleep.total_minutes": random.gauss(380, 60), "health.steps": random.gauss(3500, 1500),
         "screen.communication_minutes": random.gauss(35, 10), "screen.total_minutes": 420, "screen.social_media_minutes": random.gauss(150, 40),
         "screen.youtube_minutes": 120, "health.resting_heart_rate": 72}
    m["sleep.deep_minutes"] = m["sleep.rem_minutes"] = m["sleep.total_minutes"] * 0.14
    if chance(0.1): m["reading.minutes"] = 15
    return m


PROFILES = {"Fitness-focused": fitness, "Study-focused": study, "Busy / average": average,
            "Highly disciplined": disciplined, "Low activity": low}


def run(profile):
    random.seed(1)
    stat_xp, total, out = defaultdict(float), 0, {}
    for d in range(1, DAYS + 1):
        m = {k: max(0, v) for k, v in profile(d).items()}
        lines = game.score_day(m, {"youtube": m.get("screen.youtube_minutes", 0)})
        base = max(0, sum(l["xp"] for l in lines))
        total += base + (round(base * 0.1) if m.get("sleep.total_minutes", 0) >= 450 else 0)
        for l in lines:
            stat_xp[l["key"]] = max(0, stat_xp[l["key"]] + l["xp"])
        if d in CHECKPOINTS:
            sheets = game._sheets(stat_xp)
            power = game.group_power(sheets)
            out[d] = {"level": game.level_for(total)["level"], "xp": total,
                      "attrs": {perks.GROUPS[g][1]: p[0] for g, p in power.items()},
                      "class": game.character_class(sheets)["name"], "avg": round(total / d)}
    return out


if __name__ == "__main__":
    for name, fn in PROFILES.items():
        res = run(fn)
        print(f"\n{name}")
        for d, r in res.items():
            attrs = " ".join(f"{k}{v}" for k, v in r["attrs"].items())
            print(f"  day {d:>3}: level {r['level']:>3}  avg {r['avg']:>4} XP/day  {attrs}  class {r['class']}")

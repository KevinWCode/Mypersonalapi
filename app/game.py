"""The RPG layer: turns each day's stats into XP, and total XP into a level from 1 to 110.

Plain rules, no AI. Every day from GAME_START is scored again from the stored stats on each request, so a late
phone sync or a rule change simply re-scores the past. A day never scores below 0, so a level is never lost.
"""
import os
from collections import defaultdict
from datetime import date, timedelta

from app import db, perks
from app.collectors import calendar
from app.metrics import APP_PREFIX, build_days

# Day 1 of the game: the first day with phone data.
GAME_START = date.fromisoformat(os.environ.get("GAME_START") or "2026-10-23")
MAX_LEVEL = 110

# (name, first level, last level, cost of the first level, extra cost per level). A level's cost is the XP needed
# to go from it to the next one, so the last cost paid is level 109's.
QUARTERS = [
    ("Initiate", 1, 27, 500, 60),
    ("Challenger", 28, 55, 2300, 50),
    ("Veteran", 56, 83, 3800, 50),
    ("Master", 84, 109, 5300, 50),
]

YOUTUBE_APP = "com.google.android.youtube"


def level_cost(level: int) -> int:
    for _, first, last, start, step in QUARTERS:
        if first <= level <= last:
            return start + (level - first) * step
    raise ValueError(f"no level {level}")


def tier(level: int) -> str:
    for name, first, last, *_ in QUARTERS:
        if level <= last:
            return name
    return QUARTERS[-1][0]


XP_TO_MAX = sum(level_cost(lv) for lv in range(1, MAX_LEVEL))


def level_for(total_xp: float, scale: float = 1) -> dict:
    """scale shrinks the curve for a single stat (its daily max / the whole day's max), so maxing a stat every day
    levels it at the same pace as the character."""
    level, left = 1, total_xp
    while level < MAX_LEVEL and left >= level_cost(level) * scale - 1e-6:  # tolerance for the scaled float sums
        left -= level_cost(level) * scale
        level += 1
    cost = round(level_cost(level) * scale) if level < MAX_LEVEL else None
    return {
        "level": level,
        "tier": tier(level),
        "level_xp": round(left) if cost else 0,
        "level_cost": cost,
        "xp_to_next": round(cost - left) if cost else 0,
    }


# Each stat's rank by its level, in the same four bands as the character's tiers.
STAT_RANKS = [(27, "Novice"), (55, "Adept"), (83, "Expert"), (MAX_LEVEL, "Master")]


def stat_sheet(key: str, label: str, stat: str, xp: float, max_xp: int) -> dict:
    """One stat's own level, plus a tabletop-style ability score: 8 at level 1 up to 20 at level 110, and its
    modifier, (score - 10) // 2, so -1 to +5."""
    lv = level_for(xp, max_xp / DAILY_MAX)
    score = 8 + lv["level"] * 12 // MAX_LEVEL
    return {
        "key": key, "label": label, "stat": stat, "xp": round(xp),
        "level": lv["level"], "rank": next(name for top, name in STAT_RANKS if lv["level"] <= top),
        "score": score, "modifier": (score - 10) // 2,
        "level_xp": lv["level_xp"], "level_cost": lv["level_cost"], "xp_to_next": lv["xp_to_next"],
    }


# Well-Rested: waking after 7.5 h asleep adds 10% to that day's XP. Sleep is filed under the day you wake up.
WELL_RESTED_MINUTES = 450
WELL_RESTED_BONUS = 0.10

def group_power(stats: list[dict]) -> dict:
    """Each group's (highest stat level, that stat's XP) - its level, with XP to break ties."""
    by_key = {s["key"]: s for s in stats}
    return {g: max((by_key[k]["level"], by_key[k]["xp"]) for k in keys) for g, (_, _, keys) in perks.GROUPS.items()}


def character_class(stats: list[dict]) -> dict:
    power = group_power(stats)
    first, second = sorted(power, key=lambda g: power[g], reverse=True)[:2]
    ability = {g: perks.GROUPS[g][1] for g in power}
    if power[first][0] < 2:
        return {"name": "Adventurer", "groups": [], "detail": "Level a stat to 2 to find your class"}
    if power[first][0] >= 2 * power[second][0]:
        return {"name": perks.PURE_CLASSES[first], "groups": [first], "detail": f"Pure {ability[first]}"}
    return {"name": perks.DUAL_CLASSES[frozenset((first, second))], "groups": [first, second],
            "detail": f"{ability[first]} + {ability[second]}"}


def group_sheets(stats: list[dict], claims: dict) -> list[dict]:
    """Each group's level, tier, title and perk tokens, earned or not, and when each was claimed."""
    power = group_power(stats)
    out = []
    for g, (name, ability, keys) in perks.GROUPS.items():
        level = power[g][0]
        tier = perks.tier_for(level)
        next_tier = next((t for t in perks.TIERS if t > tier), None)
        out.append({
            "key": g, "name": name, "ability": ability, "stats": keys, "level": level, "tier": tier,
            "title": perks.PERKS[(g, tier)][0] if tier else None,
            "next_tier_level": perks.TIERS[next_tier][1] if next_tier else None,
            "perks": [{
                "tier": t, "rank": rank, "unlock_level": unlock, "title": perks.PERKS[(g, t)][0],
                "token": perks.PERKS[(g, t)][1], "detail": perks.PERKS[(g, t)][2], "earned": level >= unlock,
                "claimed_on": claims.get((g, t)), "claim_metric": perks.claim_metric(g, t),
            } for t, (rank, unlock) in perks.TIERS.items()],
        })
    return out


def _sheets(stat_xp: dict) -> list[dict]:
    return [stat_sheet(line["key"], line["label"], line["stat"], stat_xp.get(line["key"], 0), line["max"])
            for line in score_day({}, {})]


def _avg(values: list[float]) -> float | None:
    return sum(values) / len(values) if values else None


def _line(key, label, stat, xp, max_xp, detail):
    return {"key": key, "label": label, "stat": stat, "xp": round(xp), "max": max_xp, "detail": detail}


def score_day(m: dict, ctx: dict) -> list[dict]:
    """One line per metric. m is the day's metrics from build_days; ctx holds baselines from earlier days."""
    lines = []

    # Sleep: 7.5 h asleep, plus deep and REM together making up 35% or more of it.
    sleep = m.get("sleep.total_minutes")
    xp, detail = 0, "No sleep synced"
    if sleep:
        deep_rem = m.get("sleep.deep_minutes", 0) + m.get("sleep.rem_minutes", 0)
        xp = (75 if sleep >= 450 else 0) + (50 if deep_rem >= 0.35 * sleep else 0)
        detail = f"{sleep / 60:.1f} h, deep + REM {deep_rem / sleep:.0%}"
    lines.append(_line("sleep", "Sleep", "Rest", xp, 125, detail))

    # Heart: resting HR no more than 2 bpm over its 30-day average, and HRV above its average.
    rhr, hrv = m.get("health.resting_heart_rate"), m.get("health.hrv_rmssd")
    xp, parts = 0, []
    if rhr:
        base = ctx.get("rhr_base")
        if base is None or rhr <= base + 2:
            xp += 40
        parts.append(f"resting {rhr:.0f} bpm" + (f" (avg {base:.0f})" if base else ""))
    if hrv:
        base = ctx.get("hrv_base")
        if base is None or hrv > base:
            xp += 35
        parts.append(f"HRV {hrv:.0f} ms")
    else:
        parts.append("no HRV from Samsung")
    lines.append(_line("heart", "Heart rate & HRV", "HP", xp, 75, ", ".join(parts)))

    steps = m.get("health.steps", 0)
    lines.append(_line("steps", "Steps", "Stamina", min(int(steps // 1000) * 10, 100), 100, f"{steps:,.0f} steps"))

    # Workout: a session, plus 30+ minutes standing in for the heart rate / calorie target until workouts send those.
    # Sprint pace needs GPS workouts, which aren't sent yet.
    sessions, minutes = m.get("workout.sessions", 0), m.get("workout.minutes", 0)
    xp = (75 if sessions else 0) + (40 if sessions and minutes >= 30 else 0)
    lines.append(_line("workout", "Workout", "Agility", xp, 150,
                       f"{sessions:.0f} session(s), {minutes:.0f} min" if sessions else "No workout"))

    kb = any(v > 0 for k, v in m.items() if k.startswith("kb.") and (k.endswith(".sets") or k.endswith(".done")))
    lines.append(_line("kettlebell", "Kettlebell", "STR", 75 if kb else 0, 75, "Session logged" if kb else "No session"))

    catch = m.get("football.max_catch_yards")
    lines.append(_line("catch", "Catch drill", "DEX", 50 if catch else 0, 50,
                       f"{catch:.0f} yd" if catch else "No drill"))

    # Calendar: blocks ticked done that day (on the dashboard), 50 XP each, up to 3.
    blocks = sum(1 for k, v in m.items() if k.startswith("cal.") and k.endswith(".done") and v > 0)
    planned = ctx.get("blocks_planned", 0)
    if ctx.get("calendar_connected") or blocks:
        detail = f"{blocks:.0f} of {planned} block(s) kept" if planned else f"{blocks:.0f} block(s) kept"
    else:
        detail = "Calendar not connected yet"
    lines.append(_line("calendar", "Calendar blocks", "Quests", 50 * min(blocks, 3), 150, detail))

    commits = m.get("github.commits", 0)
    lines.append(_line("github", "GitHub commits", "Craft", 25 * min(int(commits), 4), 100, f"{commits:.0f} commit(s)"))

    unread = m.get("email.unread")
    lines.append(_line("email", "Inbox", "Guild", 50 if unread is not None and unread <= 10 else 0, 50,
                       f"{unread:.0f} unread" if unread is not None else "Gmail not connected yet"))

    journal = m.get("journal.entries")
    lines.append(_line("journal", "Journal", "Logbook", 50 if journal else 0, 50,
                       "Entry written" if journal else "No journal source yet"))

    # Screen time only counts once the phone has synced that day, so a missing day isn't read as 0 minutes.
    synced = "screen.total_minutes" in m
    yt = ctx.get("youtube", 0)
    lines.append(_line("youtube", "YouTube", "Lore", 50 if synced and yt <= 60 else 0, 50,
                       f"{yt:.0f} min (limit 60)" if synced else "Phone not synced"))

    social = m.get("screen.social_media_minutes", 0)
    xp = 0
    if synced:
        xp = 100 if social < 30 else (-50 if social > 60 else 0)
    lines.append(_line("social", "Social media", "Sanity", xp, 100,
                       f"{social:.0f} min (under 30 earns XP, over 60 costs 50)" if synced else "Phone not synced"))

    # Stocks: a day that ends flat or up, plus money added (equity rose by more than the day's P/L).
    pl = m.get("trading.pl_day")
    xp, detail = 0, "Market closed or no data"
    if pl is not None:
        xp = 50 if pl >= 0 else 0
        detail = f"Day P/L ${pl:,.2f}"
        prev, equity = ctx.get("prev_equity"), m.get("trading.equity")
        if prev is not None and equity is not None and equity - prev - pl > 1:
            xp += 25
            detail += ", capital added"
    lines.append(_line("stocks", "Stocks", "Treasury", xp, 75, detail))
    return lines


DAILY_MAX = sum(line["max"] for line in score_day({}, {}))


def build(conn, day: date) -> dict:
    lookback = GAME_START - timedelta(days=30)
    rows = db.fetch_range(conn, lookback.isoformat(), day.isoformat())
    by_date = {d["date"]: d["metrics"] for d in build_days(rows)}
    planned = calendar.counts_by_day(conn, lookback.isoformat(), day.isoformat())
    youtube: dict[str, float] = defaultdict(float)
    for r in rows:
        if r["metric"] == APP_PREFIX + YOUTUBE_APP:
            youtube[r["date"]] += r["value"]

    # When each perk token was first logged as claimed. A claim only counts once the token was earned.
    logged: dict[tuple[str, int], str] = {}
    for g, t in perks.PERKS:
        logged[(g, t)] = next((dk for dk in sorted(by_date) if by_date[dk].get(perks.claim_metric(g, t), 0) > 0), None)
    claims: dict[tuple[str, int], str] = {}

    history, stat_xp = [], defaultdict(float)
    rhr_seen, hrv_seen = [], []
    prev_equity = None
    today_lines: list[dict] = []
    today_buffs: list[dict] = []
    d = lookback
    while d <= day:
        key = d.isoformat()
        m = by_date.get(key, {})
        if d >= GAME_START:
            power = group_power(_sheets(stat_xp))
            for (g, t), on in logged.items():
                if on == key and (g, t) not in claims and power[g][0] >= perks.TIERS[t][1]:
                    claims[(g, t)] = key
            yesterday = (d - timedelta(days=1)).isoformat()
            active = {gt for gt, on in claims.items() if on == key}
            buffs = []

            ctx = {"rhr_base": _avg(rhr_seen[-30:]), "hrv_base": _avg(hrv_seen[-30:]),
                   "prev_equity": prev_equity, "youtube": youtube.get(key, 0),
                   "blocks_planned": planned.get(key, 0), "calendar_connected": bool(planned)}
            lines = score_day(m, ctx)
            by_key = {line["key"]: line for line in lines}
            if ("wis", 1) in active:
                by_key["social"]["xp"] = max(0, by_key["social"]["xp"])
                by_key["youtube"]["xp"] = by_key["youtube"]["max"]
                buffs.append({"name": "Unrestricted Media Pass", "detail": "Social media and YouTube can't cost XP today", "xp": 0})
            if ("per", 2) in active:
                cal = by_key["calendar"]
                cal["xp"] = min(cal["max"], cal["xp"] + 50)
                buffs.append({"name": "Chef Exemption Pass", "detail": "One extra calendar block counted", "xp": 0})

            base = max(0, sum(line["xp"] for line in lines))
            rested = m.get("sleep.total_minutes", 0) >= WELL_RESTED_MINUTES
            pass_used = next((perks.PERKS[gt][1] for gt in (("con", 1), ("con", 2)) if gt in active), None)
            # The buff adds to the character's XP only; each stat keeps the XP it earned itself.
            bonus = round(base * WELL_RESTED_BONUS) if rested or pass_used else 0
            if bonus:
                buffs.append({"name": "Well-Rested", "xp": bonus,
                              "detail": f"{pass_used}: +10% XP today" if pass_used and not rested else "7.5 h+ sleep: +10% XP today"})
            xp = base + bonus
            if ("wis", 4) in active or claims.get(("wis", 4)) == yesterday:
                floor = round(_avg([h["xp"] for h in history[-14:]]) or 0)
                if floor > xp:
                    buffs.append({"name": "Digital Detox Retreat", "detail": "Scored at your 14-day average", "xp": floor - xp})
                    xp = floor
            history.append({"date": key, "xp": xp, "well_rested": bool(bonus)})
            for line in lines:
                # A stat's XP never drops below 0, so a bad social media day can't undo levels.
                stat_xp[line["key"]] = max(0, stat_xp[line["key"]] + line["xp"])
            if d == day:
                today_lines, today_buffs = lines, buffs
        if "health.resting_heart_rate" in m:
            rhr_seen.append(m["health.resting_heart_rate"])
        if "health.hrv_rmssd" in m:
            hrv_seen.append(m["health.hrv_rmssd"])
        if "trading.equity" in m:
            prev_equity = m["trading.equity"]
        d += timedelta(days=1)

    total = sum(h["xp"] for h in history)
    stats = _sheets(stat_xp)
    groups = group_sheets(stats, claims)
    top = max(groups, key=lambda g: (g["tier"], g["level"]))
    recent = _avg([h["xp"] for h in history[-14:]])
    remaining = max(0, XP_TO_MAX - total)
    days_left = round(remaining / recent) if recent and remaining else (0 if not remaining else None)
    return {
        "date": day.isoformat(),
        **level_for(total),
        "max_level": MAX_LEVEL,
        "total_xp": total,
        "xp_to_max": XP_TO_MAX,
        "day_number": len(history),
        "class": character_class(stats),
        "title": top["title"],
        "today": {
            "xp": history[-1]["xp"] if history else 0,
            "max": DAILY_MAX,
            "buffs": today_buffs,
            "metrics": today_lines,
        },
        "avg_daily_xp_14d": round(recent) if recent is not None else None,
        "days_to_max_level": days_left,
        "max_level_date": (day + timedelta(days=days_left)).isoformat() if days_left is not None else None,
        "stats": stats,
        "groups": groups,
        "history": history[-30:],
    }

"""Four vital bars, each 0 to 100: HP, Stamina, Mana and Focus.

Every bar ticks down a fixed amount over each day, spread evenly across the hours. The day's stats top it back up
(sleep, food, water, study, a journal entry...) or drain it further (poor sleep, too much social media). Like XP,
the bars are worked out again from the stored stats on every request, starting at START_VALUE on GAME_START.

Plain rules, no AI. To tune the game, change the numbers in BARS and the RULES below.
"""
from datetime import date

START_VALUE = 70
TOP = 100
LOW = 25  # at or below this a bar shows as low

# key -> (name, what it stands for, points it loses over a full day)
BARS = {
    "hp": ("HP", "Body health: food, water, sleep and movement", 30),
    "stamina": ("Stamina", "Physical energy: mostly sleep, plus food and coffee", 25),
    "mana": ("Mana", "Mental energy: sleep, journalling, reading and deep sleep", 20),
    "focus": ("Focus", "Attention: sleep, study, journalling, kept blocks, low social media", 20),
}

POOR_SLEEP_MINUTES = 360  # under 6 h asleep
FULL_SLEEP_MINUTES = 450  # 7.5 h, the same line as Well-Rested


def _part(x: float, full: float) -> float:
    """How much of a target was reached, 0 to 1."""
    return max(0.0, min(1.0, x / full)) if full else 0.0


def effects(m: dict, youtube_minutes: float = 0) -> dict[str, list[dict]]:
    """Each bar's gains (+) and drains (-) from one day's stats, not counting the daily tick."""
    out: dict[str, list[dict]] = {k: [] for k in BARS}

    def add(bar, label, points):
        points = round(points, 1)
        if points:
            out[bar].append({"label": label, "points": points})

    sleep = m.get("sleep.total_minutes")
    kcal = m.get("food.kcal", 0)
    journal = m.get("journal.entries", 0)
    cups = min(m.get("coffee.cups", 0), 2)
    synced = "screen.total_minutes" in m
    social = m.get("screen.social_media_minutes", 0)
    blocks = sum(1 for k, v in m.items() if k.startswith("cal.") and k.endswith(".done") and v > 0)
    poor_sleep = sleep is not None and sleep < POOR_SLEEP_MINUTES

    # HP: up to +50 on a day with a full night, 2,000 kcal, 150 g protein, 3 L water and 10k steps.
    if sleep:
        add("hp", "Sleep", 20 * _part(sleep, FULL_SLEEP_MINUTES))
    add("hp", "Food", 12 * _part(kcal, 2000))
    add("hp", "Protein", 8 * _part(m.get("food.protein_g", 0), 150))
    add("hp", "Water", 6 * _part(m.get("water.ml", 0), 3000))
    add("hp", "Steps", 4 * _part(m.get("health.steps", 0), 10000))

    # Stamina: mostly sleep. Under 6 h also costs 15 on top of the smaller refill.
    if sleep:
        add("stamina", "Sleep", 35 * _part(sleep, FULL_SLEEP_MINUTES))
    if poor_sleep:
        add("stamina", "Poor sleep (under 6 h)", -15)
    add("stamina", "Food", 10 * _part(kcal, 2000))
    add("stamina", "Coffee", 4 * cups)

    # Mana: a night's sleep keeps it about level; journal, reading and plenty of deep + REM sleep raise it.
    if sleep:
        add("mana", "Sleep", 10 * _part(sleep, FULL_SLEEP_MINUTES))
    add("mana", "Journal entry", 15 if journal >= 1 else 0)
    add("mana", "Second journal entry", 5 if journal >= 2 else 0)
    add("mana", "Reading", 10 * min(m.get("reading.minutes", 0) / 60, 2))
    if sleep:
        deep_rem = m.get("sleep.deep_minutes", 0) + m.get("sleep.rem_minutes", 0)
        add("mana", "Deep + REM sleep", 10 if deep_rem >= 0.35 * sleep else 0)
    if poor_sleep:
        add("mana", "Poor sleep (under 6 h)", -10)

    # Focus: sleep, study, journal, kept calendar blocks and low screen time. Heavy social media or YouTube drains it.
    if sleep:
        add("focus", "Sleep", 8 * _part(sleep, FULL_SLEEP_MINUTES))
    add("focus", "Study", 8 * min(m.get("study.minutes", 0) / 30, 3))
    add("focus", "Journal entry", 8 if journal >= 1 else 0)
    add("focus", "Calendar blocks kept", 4 * min(blocks, 3))
    add("focus", "Coffee", 3 * cups)
    if synced:
        if social < 30:
            add("focus", "Social media under 30 min", 10)
        elif social > 60:
            add("focus", "Social media over 60 min", -15)
        if youtube_minutes > 60:
            add("focus", "YouTube over 60 min", -10)
    if poor_sleep:
        add("focus", "Poor sleep (under 6 h)", -10)
    return out


def _clamp(v: float) -> float:
    return max(0.0, min(float(TOP), v))


def run(days: list[tuple[date, dict, float]], today_elapsed: float = 1.0) -> list[dict]:
    """days: (date, metrics, YouTube minutes) from GAME_START to the day asked for, in order.

    Past days are applied in full. The last day only ticks down by the share of it that has passed
    (today_elapsed, 0 to 1), so the bar drains through the day."""
    values = {k: float(START_VALUE) for k in BARS}
    last = {}
    for i, (d, m, yt) in enumerate(days):
        fx = effects(m, yt)
        share = today_elapsed if i == len(days) - 1 else 1.0
        start = dict(values)
        for k, (_, _, drain) in BARS.items():
            gained = sum(e["points"] for e in fx[k])
            values[k] = _clamp(start[k] + gained - drain * share)
            last[k] = {"start": start[k], "gained": gained, "effects": fx[k]}
    out = []
    for k, (name, about, drain) in BARS.items():
        info = last.get(k, {"start": float(START_VALUE), "gained": 0, "effects": []})
        value = values[k]
        out.append({
            "key": k, "name": name, "about": about, "value": round(value), "max": TOP,
            "low": value <= LOW, "drain_per_day": drain,
            "start_of_day": round(info["start"]), "gained_today": round(info["gained"], 1),
            # Where the bar ends tonight if nothing else is logged.
            "end_of_day": round(_clamp(info["start"] + info["gained"] - drain)),
            "today": info["effects"],
        })
    return out

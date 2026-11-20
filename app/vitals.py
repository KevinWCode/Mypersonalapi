"""Four vital bars, each 0 to 100: HP, Stamina, Mana and Focus.

A night's sleep resets HP, Stamina and Focus:
- HP: 7 h = 100, 8 h = a 110 bonus. It slowly regenerates through the day up to 100; coffee and social media knock
  it down.
- Stamina: 10 per hour asleep. It drains 60 over the waking day (07:00-23:00); a real workout (kettlebells, or a 30+ min
  watch workout that isn't a walk) costs 33 and adds 10 to the next day's reset; each 250 ml of water adds 2.
- Focus: 12 per hour asleep. It drains 60 over the waking day; water (2 per 250 ml), a workout, study,
  journalling, kept calendar blocks and a day under 25 min of social media (+15 the next day) top it up;
  social media costs 1 per 2 min.
Social media costs 1 HP and 1 Mana per 5 min; YouTube counts the same at a third of the strength.
Sugar costs 1 HP per gram and gives a sugar rush of 1 Stamina per 2 g that lasts that day only: it isn't carried into
the next day.
Mana isn't reset: it drains 60 over the waking day and has four refills of up to 25 each: sleep (15 at 6 h, 20 at 7 h, 25 at
8 h), one journal entry, reading (12.5 per 30 min) and a night with 35%+ deep + REM sleep.

Like XP, the bars are worked out again from the stored stats on every request, starting at START_VALUE on GAME_START.

Plain rules, no AI. To tune the game, change the numbers in BARS and the RULES below.
"""
from datetime import date

START_VALUE = 70
TOP = 100
LOW = 25  # at or below this a bar shows as low

# key -> (name, what it stands for, points it changes by over a full day: minus drains, plus regenerates)
BARS = {
    "hp": ("HP", "Reset by sleep, regenerates slowly, lowered by coffee, sugar, social media and YouTube", 12),
    "stamina": ("Stamina", "Reset by sleep (10 an hour), drains 60 over the waking day, water, workouts and a sugar rush move it", -60),
    "mana": ("Mana", "Four refills of up to 25: sleep, one journal entry, reading, deep + REM sleep; social media drains it", -60),
    "focus": ("Focus", "Reset by sleep (12 an hour), drains 60 over the waking day, water, workouts and study top it up", -60),
}

# Stamina, Mana and Focus drain over the 16 waking hours, 07:00 to 23:00 (3.75 an hour). HP regenerates over all 24.
AWAKE_FROM_HOUR = 7
AWAKE_HOURS = 16
WATER_PER_250_ML = 2  # Stamina and Focus, per 250 ml logged


def awake_share(day_share: float) -> float:
    """How much of the 16 waking hours has passed, from how much of the whole day has passed."""
    return max(0.0, min(1.0, (day_share * 24 - AWAKE_FROM_HOUR) / AWAKE_HOURS))

STAMINA_PER_SLEEP_HOUR = 10
FOCUS_PER_SLEEP_HOUR = 12
WORKOUT_STAMINA = -33
WORKOUT_NEXT_DAY_STAMINA = 10
WORKOUT_FOCUS = 15

# HP sleep reset: 7 h asleep = 100, rising to 110 at 8 h. Less sleep resets it lower, in proportion (6 h = 86).
HP_FULL_MINUTES = 420
HP_BONUS_MINUTES = 480
HP_BONUS_TOP = 110
HP_COFFEE = -5  # per cup
# Social media: -1 HP and -1 Mana per full 5 minutes, -1 Focus per full 2 minutes, no daily cap. YouTube counts the
# same way at a third of the strength (per 15 and 6 minutes). Under 25 minutes of social media gives +15 Focus the
# next day.
SOCIAL_HP_MANA_MINUTES = 5
SOCIAL_FOCUS_MINUTES = 2
YOUTUBE_FACTOR = 3
CALM_SOCIAL_MINUTES = 25
CALM_NEXT_DAY_FOCUS = 15
# Sugar: -1 HP per full gram, and a temporary +1 Stamina per full 2 g (the rest of that day; gone by the next).
SUGAR_HP_PER_G = 1
SUGAR_STAMINA_EVERY_G = 2


def screen_drain(minutes: float, every: float) -> int:
    """-1 for every full `every` minutes."""
    return -int(minutes // every)


def hp_after_sleep(minutes: float) -> float:
    if minutes <= HP_FULL_MINUTES:
        return TOP * minutes / HP_FULL_MINUTES
    extra = min(minutes, HP_BONUS_MINUTES) - HP_FULL_MINUTES
    return TOP + (HP_BONUS_TOP - TOP) * extra / (HP_BONUS_MINUTES - HP_FULL_MINUTES)



MANA_PART = 25  # each of Mana's four refills tops out at 25


def mana_from_sleep(minutes: float) -> float:
    """15 at 6 h, 20 at 7 h, 25 at 8 h and over; less sleep in proportion (3 h = 7.5)."""
    hours = minutes / 60
    if hours <= 6:
        return 15 * hours / 6
    return min(MANA_PART, 15 + 5 * (hours - 6))


def worked_out(m: dict) -> bool:
    """A real workout: a kettlebell session logged on the dashboard or phone, or a watch workout of 30+ minutes
    that isn't a walk (the phone app sends those as workout.counted_sessions). Shorter or walking sessions don't
    count."""
    return m.get("workout.counted_sessions", 0) > 0 or any(
        v > 0 for k, v in m.items() if k.startswith("kb.") and (k.endswith(".sets") or k.endswith(".done")))


def after_sleep(minutes: float) -> dict[str, float]:
    """What a night's sleep resets each bar to."""
    hours = minutes / 60
    return {"hp": hp_after_sleep(minutes), "stamina": min(TOP, STAMINA_PER_SLEEP_HOUR * hours),
            "focus": min(TOP, FOCUS_PER_SLEEP_HOUR * hours)}


def effects(m: dict, youtube_minutes: float = 0, workout_yesterday: bool = False,
            calm_yesterday: bool = False) -> dict[str, list[dict]]:
    """Each bar's gains (+) and drains (-) from one day's stats, not counting the daily tick."""
    out: dict[str, list[dict]] = {k: [] for k in BARS}

    def add(bar, label, points, temporary=False):
        points = round(points, 1)
        if points:
            out[bar].append({"label": label, "points": points, **({"temporary": True} if temporary else {})})

    sleep = m.get("sleep.total_minutes")
    journal = m.get("journal.entries", 0)
    cups = min(m.get("coffee.cups", 0), 2)
    synced = "screen.total_minutes" in m
    social = m.get("screen.social_media_minutes", 0)
    blocks = sum(1 for k, v in m.items() if k.startswith("cal.") and k.endswith(".done") and v > 0)

    # HP: sleep resets it (in run below); coffee, social media and YouTube knock it down.
    add("hp", "Coffee", HP_COFFEE * m.get("coffee.cups", 0))
    if synced:
        for bar in ("hp", "mana", "focus"):
            every = SOCIAL_FOCUS_MINUTES if bar == "focus" else SOCIAL_HP_MANA_MINUTES
            add(bar, f"Social media ({social:.0f} min)", screen_drain(social, every))
    for bar in ("hp", "mana", "focus"):
        every = (SOCIAL_FOCUS_MINUTES if bar == "focus" else SOCIAL_HP_MANA_MINUTES) * YOUTUBE_FACTOR
        add(bar, f"YouTube ({youtube_minutes:.0f} min)", screen_drain(youtube_minutes, every))

    # Stamina: sleep resets it (in run below); a workout uses 33 and gives 10 back the next day.
    workout = worked_out(m)
    if workout_yesterday:
        add("stamina", "Yesterday's workout", WORKOUT_NEXT_DAY_STAMINA)
    if workout:
        add("stamina", "Workout", WORKOUT_STAMINA)
    # Sugar: costs HP for every gram; the Stamina rush only counts for that day (see run).
    sugar = m.get("food.sugar_g", 0)
    add("hp", f"Sugar ({sugar:.0f} g)", -SUGAR_HP_PER_G * int(sugar))
    add("stamina", f"Sugar rush ({sugar:.0f} g, today only)", int(sugar // SUGAR_STAMINA_EVERY_G), temporary=True)
    water = WATER_PER_250_ML * int(m.get("water.ml", 0) // 250)
    add("stamina", "Water", water)
    add("focus", "Water", water)

    # Mana: four refills of up to 25 each - sleep, one journal entry, reading and plenty of deep + REM sleep.
    if sleep:
        add("mana", "Sleep", mana_from_sleep(sleep))
        deep_rem = m.get("sleep.deep_minutes", 0) + m.get("sleep.rem_minutes", 0)
        add("mana", "Deep + REM sleep", MANA_PART if deep_rem >= 0.35 * sleep else 0)
    add("mana", "Journal entry", MANA_PART if journal >= 1 else 0)
    add("mana", "Reading", min(MANA_PART, 12.5 * m.get("reading.minutes", 0) / 30))

    # Focus: sleep resets it (in run below). A workout, study, journal, kept calendar blocks and low screen time top
    # it up; heavy social media or YouTube drains it.
    if workout:
        add("focus", "Workout", WORKOUT_FOCUS)
    add("focus", "Study", 8 * min(m.get("study.minutes", 0) / 30, 3))
    add("focus", "Journal entry", 8 if journal >= 1 else 0)
    add("focus", "Calendar blocks kept", 4 * min(blocks, 3))
    add("focus", "Coffee", 3 * cups)
    if calm_yesterday:
        add("focus", f"Social media under {CALM_SOCIAL_MINUTES} min yesterday", CALM_NEXT_DAY_FOCUS)
    return out


def _clamp(v: float, top: float = TOP) -> float:
    return max(0.0, min(float(top), v))


def apply_tick(value: float, tick: float) -> float:
    """A drain takes the bar down; regeneration lifts it, but only up to 100, so a sleep bonus above 100 stays
    and isn't added to."""
    if tick <= 0:
        return value + tick
    return max(value, min(TOP, value + tick))


def end_value(key: str, start: float, gained: float, tick: float) -> float:
    top = HP_BONUS_TOP if key == "hp" else TOP
    return _clamp(apply_tick(_clamp(start + gained, top), tick), top)


def run(days: list[tuple[date, dict, float]], today_elapsed: float = 1.0) -> list[dict]:
    """days: (date, metrics, YouTube minutes) from GAME_START to the day asked for, in order.

    Past days are applied in full. The last day only ticks by the share of it that has passed (today_elapsed,
    0 to 1), so the bars move through the day."""
    values = {k: float(START_VALUE) for k in BARS}
    last = {}
    yesterday: dict = {}
    for i, (d, m, yt) in enumerate(days):
        calm = "screen.total_minutes" in yesterday and yesterday.get("screen.social_media_minutes", 0) < CALM_SOCIAL_MINUTES
        fx = effects(m, yt, worked_out(yesterday), calm)
        yesterday = m
        share = today_elapsed if i == len(days) - 1 else 1.0
        start = dict(values)
        sleep = m.get("sleep.total_minutes")
        if sleep:
            for k, v in after_sleep(sleep).items():
                start[k] = v
                fx[k].insert(0, {"label": f"Slept {sleep / 60:.1f} h: reset to {v:.0f}", "points": 0})
        is_today = i == len(days) - 1
        for k, (_, _, tick) in BARS.items():
            gained = sum(e["points"] for e in fx[k])
            # A temporary boost (the sugar rush) shows on its own day but has worn off by the next.
            lasting = gained - sum(e["points"] for e in fx[k] if e.get("temporary"))
            values[k] = end_value(k, start[k], gained if is_today else lasting,
                                  tick * (share if tick > 0 else awake_share(share)))
            last[k] = {"start": start[k], "gained": gained, "effects": fx[k]}
    out = []
    for k, (name, about, tick) in BARS.items():
        info = last.get(k, {"start": float(START_VALUE), "gained": 0, "effects": []})
        value = values[k]
        out.append({
            "key": k, "name": name, "about": about, "value": round(value), "max": TOP,
            "low": value <= LOW, "tick_per_day": tick,
            # A drain runs over the waking hours only; regeneration runs all day.
            "tick_from_hour": AWAKE_FROM_HOUR if tick < 0 else 0, "tick_hours": AWAKE_HOURS if tick < 0 else 24,
            "top": HP_BONUS_TOP if k == "hp" else TOP,
            "start_of_day": round(info["start"], 1), "gained_today": round(info["gained"], 1),
            # Where the bar ends tonight if nothing else is logged.
            "end_of_day": round(end_value(k, info["start"], info["gained"], tick)),
            "today": info["effects"],
        })
    return out

"""Writes the phone app's GameParityTest.kt: a random fixture of raw rows scored by the server's game.py.

Re-run after changing app/game.py, app/vitals.py or app/perks.py, then port the change to the Kotlin files until
the Android unit tests pass:
    python tools/make_game_parity.py android/app/src/test/java/com/kevinwcode/personalapi/GameParityTest.kt
"""
import random, sys
from datetime import date, timedelta
sys.path.insert(0, '.')
from app import game

random.seed(7)
START = game.GAME_START
rows = []
def add(d, metric, value):
    rows.append({"date": d.isoformat(), "metric": metric, "value": float(value)})

d = START - timedelta(days=6)
end = START + timedelta(days=45)
while d <= end:
    r = random.random
    if r() < 0.9:
        add(d, "sleep.total_minutes", random.choice([300, 360, 405, 430, 450, 470, 495, 520]))
        add(d, "sleep.deep_minutes", random.randint(40, 110)); add(d, "sleep.rem_minutes", random.randint(50, 120))
    if r() < 0.85: add(d, "health.resting_heart_rate", random.randint(52, 66))
    if r() < 0.3: add(d, "health.hrv_rmssd", random.randint(30, 60))
    if r() < 0.9: add(d, "health.steps", random.randint(1500, 16000))
    if r() < 0.5:
        add(d, "workout.sessions", random.randint(1, 2)); add(d, "workout.minutes", random.randint(10, 70))
        if r() < 0.6: add(d, "workout.counted_sessions", 1)
    if r() < 0.4: add(d, "kb.swing.sets", random.randint(1, 5))
    if r() < 0.2: add(d, "kb.clean.done", 1)
    if r() < 0.25: add(d, "football.max_catch_yards", random.randint(10, 22))
    for i in range(random.randint(0, 4)):
        if r() < 0.5: add(d, f"cal.abc{i}def.done", 1)
    if r() < 0.5: add(d, "github.commits", random.randint(1, 6))
    if r() < 0.3: add(d, "email.unread", random.randint(0, 25))
    if r() < 0.6: add(d, "journal.entries", random.randint(1, 2))
    if r() < 0.6: add(d, "water.ml", random.choice([250, 500, 750, 1500]))
    if r() < 0.6: add(d, "coffee.cups", random.randint(1, 3))
    if r() < 0.4: add(d, "reading.minutes", random.choice([15, 30, 45, 90]))
    if r() < 0.4: add(d, "study.minutes", random.choice([20, 60, 120]))
    if r() < 0.92:
        add(d, "screen.app.com.android.chrome", random.randint(5, 120))
        if r() < 0.8: add(d, "screen.app.com.zhiliaoapp.musically", random.choice([4, 12, 22, 35, 50, 75]))
        if r() < 0.5: add(d, "screen.app.com.instagram.android", random.choice([3, 9, 18, 27]))
        if r() < 0.3: add(d, "screen.app.com.reddit.frontpage", random.randint(2, 30))
        if r() < 0.7: add(d, "screen.app.com.google.android.youtube", random.choice([10, 25, 45, 61, 90]))
    if d.weekday() < 5 and r() < 0.9:
        add(d, "trading.pl_day", round(random.uniform(-40, 40), 2))
        add(d, "trading.equity", round(1500 + random.uniform(-100, 300), 2))
    d += timedelta(days=1)
# Perk claims (only count once earned).
for day_off, metric in [(20, "perk.wis.1.claimed"), (25, "perk.con.1.claimed"), (30, "perk.per.2.claimed"),
                        (33, "perk.wis.4.claimed"), (40, "perk.str.1.claimed")]:
    add(START + timedelta(days=day_off), metric, 1)

game.db.fetch_range = lambda conn, a, b: [r for r in rows if a <= r["date"] <= b]
game.calendar.counts_by_day = lambda conn, a, b: {}

def lst(items): return 'listOf(' + ', '.join(items) + ')' if items else 'emptyList<String>()'
def kt(s): return '"' + str(s).replace('"', '\\"').replace('$', '\\$') + '"'
cases = []
for off, share in [(10, 1.0), (20, 1.0), (25, 1.0), (30, 1.0), (33, 1.0), (34, 1.0), (31, 1.0), (45, 0.6), (45, 1.0), (-2, 1.0)]:
    day = START + timedelta(days=off)
    game.today_elapsed = (lambda s: (lambda dd, now=None: s))(share)
    g = game.build(None, day)
    cases.append((day, share, g))

out = []
out.append('''package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The phone's game must score exactly like the server's app/game.py and app/vitals.py.
 * Generated: a random 52-day fixture of raw rows, with the expected numbers from the Python code.
 */
class GameParityTest {
    private val rows: List<Triple<String, String, Double>> = listOf(''')
for r in rows:
    out.append(f'        Triple({kt(r["date"])}, {kt(r["metric"])}, {r["value"]!r}),')
out.append('''    )

    private val days: Map<LocalDate, Map<String, Double>> = rows.groupBy { LocalDate.parse(it.first) }
        .mapValues { (_, rs) -> DayMetrics.build(rs.map { it.second to it.third }) }

    private fun at(day: String, share: Double): Game.State {
        val d = LocalDate.parse(day)
        val seconds = Math.round(share * 86400).toLong()
        // share 1.0 is the end of the day, which todayElapsed reads as a past day.
        val now = if (share >= 1.0) d.plusDays(1).atStartOfDay() else d.atStartOfDay().plusSeconds(seconds)
        return Game.build(days, d, now = now)
    }
''')
for i, (day, share, g) in enumerate(cases):
    out.append(f'''    @Test
    fun case{i}() {{
        val g = at({kt(day.isoformat())}, {share!r})
        assertEquals({g["total_xp"]}, g.totalXp)
        assertEquals({g["level"]}, g.level.level)
        assertEquals({g["level_xp"]}, g.level.levelXp)
        assertEquals({g["day_number"]}, g.dayNumber)
        assertEquals({g["today"]["xp"]}, g.todayXp)
        assertEquals({kt(g["class"]["name"])}, g.characterClass.name)
        assertEquals({kt(g["title"]) if g["title"] else "null"}, g.title)
        assertEquals({g["avg_daily_xp_14d"] if g["avg_daily_xp_14d"] is not None else "null"}, g.avgDailyXp14d)
        assertEquals({g["days_to_max_level"] if g["days_to_max_level"] is not None else "null"}, g.daysToMaxLevel)
        assertEquals({lst([str(h["xp"]) for h in g["history"]])}, g.history.map {{ it.xp }})
        assertEquals({lst([kt(f'{m["key"]}={m["xp"]}') for m in g["today"]["metrics"]])}, g.lines.map {{ "${{it.key}}=${{it.xp}}" }})
        assertEquals({lst([kt(f'{b["name"]}={b["xp"]}') for b in g["today"]["buffs"]])}, g.buffs.map {{ "${{it.name}}=${{it.xp}}" }})
        assertEquals({lst([kt(f'{s["key"]}={s["xp"]}/{s["level"]}') for s in g["stats"]])}, g.stats.map {{ "${{it.key}}=${{it.xp}}/${{it.level}}" }})
        assertEquals({lst([kt(f'{x["key"]}={x["level"]}/{x["tier"]}') for x in g["groups"]])}, g.groups.map {{ "${{it.key}}=${{it.level}}/${{it.tier}}" }})
        assertEquals({lst([kt(f'{x["key"]}={x["value"]}/{x["end_of_day"]}') for x in g["vitals"]])}, g.vitals.map {{ "${{it.key}}=${{it.value}}/${{it.endOfDay}}" }})
        assertEquals({lst([kt(f'{x["key"]}={x["days"]}') for x in g["streaks"]])}, g.streaks.map {{ "${{it.key}}=${{it.days}}" }})
    }}
''')
out.append('''    @Test
    fun levelCurveMatchesTheServer() {
        assertEquals(%d, Game.XP_TO_MAX)
        assertEquals(%d, Game.DAILY_MAX)
    }
}''' % (game.XP_TO_MAX, game.DAILY_MAX))
open(sys.argv[1], 'w').write("\n".join(out) + "\n")
for day, share, g in cases:
    print(day, share, g["level"], g["total_xp"], g["class"]["name"], g["title"], [b["name"] for b in g["today"]["buffs"]], [(x["key"], x["tier"]) for x in g["groups"]])

package com.kevinwcode.personalapi

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/**
 * The RPG layer, worked out on the phone: each day's stats become XP, and total XP a level from 1 to 110.
 * A port of the server's app/game.py with the same numbers, so the phone and the PC agree. Every day from
 * [DEFAULT_GAME_START] is scored again from the stored stats each time, so a late sync or a rule change re-scores the
 * past, and a day never scores below 0.
 */
object Game {
    val DEFAULT_GAME_START: LocalDate = LocalDate.of(2026, 10, 23)
    const val MAX_LEVEL = 110

    data class Quarter(val name: String, val first: Int, val last: Int, val start: Int, val step: Int)

    val QUARTERS = listOf(
        Quarter("Initiate", 1, 27, 500, 60),
        Quarter("Challenger", 28, 55, 2300, 50),
        Quarter("Veteran", 56, 83, 3800, 50),
        Quarter("Master", 84, 109, 5300, 50),
    )

    fun levelCost(level: Int): Int {
        val q = QUARTERS.first { level in it.first..it.last }
        return q.start + (level - q.first) * q.step
    }

    fun tier(level: Int): String = QUARTERS.firstOrNull { level <= it.last }?.name ?: QUARTERS.last().name

    val XP_TO_MAX: Int = (1 until MAX_LEVEL).sumOf { levelCost(it) }

    data class LevelInfo(val level: Int, val tier: String, val levelXp: Int, val levelCost: Int?, val xpToNext: Int)

    /** Python's round(): halves go to the even number, so the phone rounds exactly like the server. */
    fun pyRound(x: Double): Int = Math.rint(x).toInt()

    /** scale shrinks the curve for a single stat (its daily max / the whole day's max). */
    fun levelFor(totalXp: Double, scale: Double = 1.0): LevelInfo {
        var level = 1
        var left = totalXp
        while (level < MAX_LEVEL && left >= levelCost(level) * scale - 1e-6) {
            left -= levelCost(level) * scale
            level += 1
        }
        val cost = if (level < MAX_LEVEL) pyRound(levelCost(level) * scale) else null
        return LevelInfo(
            level = level, tier = tier(level),
            levelXp = if (cost != null && cost != 0) pyRound(left) else 0,
            levelCost = cost,
            xpToNext = if (cost != null && cost != 0) pyRound(cost - left) else 0,
        )
    }

    val STAT_RANKS = listOf(27 to "Novice", 55 to "Adept", 83 to "Expert", MAX_LEVEL to "Master")

    /** 8 at level 1 up to 20 at level 110, and its modifier (score - 10) / 2 rounded down, so -1 to +5. */
    fun abilityScore(level: Int): Pair<Int, Int> {
        val score = 8 + level * 12 / MAX_LEVEL
        return score to Math.floorDiv(score - 10, 2)
    }

    data class Line(val key: String, val label: String, val stat: String, var xp: Int, val max: Int, val detail: String)

    data class Ctx(
        val rhrBase: Double? = null, val hrvBase: Double? = null, val prevEquity: Double? = null,
        val youtube: Double = 0.0,
    )

    private fun truthy(v: Double?) = v != null && v != 0.0
    private fun f0(v: Double) = String.format(Locale.UK, "%.0f", v)

    /** One line per stat. m is the day's metrics from [DayMetrics.build]; ctx holds baselines from earlier days. */
    fun scoreDay(m: Map<String, Double>, ctx: Ctx): List<Line> {
        val lines = mutableListOf<Line>()

        // Sleep: 7.5 h asleep, plus deep and REM together making up 35% or more of it.
        val sleep = m["sleep.total_minutes"]
        if (truthy(sleep)) {
            val s = sleep!!
            val deepRem = (m["sleep.deep_minutes"] ?: 0.0) + (m["sleep.rem_minutes"] ?: 0.0)
            val xp = (if (s >= 450) 75 else 0) + (if (deepRem >= 0.35 * s) 50 else 0)
            lines += Line("sleep", "Sleep", "Rest", xp, 125,
                String.format(Locale.UK, "%.1f h, deep + REM %.0f%%", s / 60, 100 * deepRem / s))
        } else lines += Line("sleep", "Sleep", "Rest", 0, 125, "No sleep synced")

        // Heart: resting HR no more than 2 bpm over its 30-day average, and HRV above its average.
        val rhr = m["health.resting_heart_rate"]
        val hrv = m["health.hrv_rmssd"]
        var xp = 0
        val parts = mutableListOf<String>()
        if (truthy(rhr)) {
            val base = ctx.rhrBase
            if (base == null || rhr!! <= base + 2) xp += 40
            parts += "resting ${f0(rhr!!)} bpm" + (if (truthy(base)) " (avg ${f0(base!!)})" else "")
        }
        if (truthy(hrv)) {
            val base = ctx.hrvBase
            if (base == null || hrv!! > base) xp += 35
            parts += "HRV ${f0(hrv!!)} ms"
        } else parts += "no HRV from Samsung"
        lines += Line("heart", "Heart rate & HRV", "Pulse", xp, 75, parts.joinToString(", "))

        val steps = m["health.steps"] ?: 0.0
        lines += Line("steps", "Steps", "Endurance", minOf(Math.floor(steps / 1000).toInt() * 10, 100), 100,
            String.format(Locale.UK, "%,.0f steps", steps))

        val sessions = m["workout.sessions"] ?: 0.0
        val minutes = m["workout.minutes"] ?: 0.0
        xp = (if (sessions != 0.0) 75 else 0) + (if (sessions != 0.0 && minutes >= 30) 40 else 0)
        lines += Line("workout", "Workout", "Agility", xp, 150,
            if (sessions != 0.0) "${f0(sessions)} session(s), ${f0(minutes)} min" else "No workout")

        val kb = m.any { (k, v) -> v > 0 && k.startsWith("kb.") && (k.endsWith(".sets") || k.endsWith(".done")) }
        lines += Line("kettlebell", "Kettlebell", "STR", if (kb) 75 else 0, 75, if (kb) "Session logged" else "No session")

        val catch = m["football.max_catch_yards"]
        lines += Line("catch", "Catch drill", "DEX", if (truthy(catch)) 50 else 0, 50,
            if (truthy(catch)) "${f0(catch!!)} yd" else "No drill")

        val commits = m["github.commits"] ?: 0.0
        lines += Line("github", "GitHub commits", "Craft", 25 * minOf(commits.toInt(), 4), 100, "${f0(commits)} commit(s)")

        val unread = m["email.unread"]
        lines += Line("email", "Inbox", "Guild", if (unread != null && unread <= 10) 50 else 0, 50,
            if (unread != null) "${f0(unread)} unread" else "Gmail not connected yet")

        val journal = m["journal.entries"] ?: 0.0
        xp = (if (journal >= 1) 75 else 0) + (if (journal >= 2) 25 else 0)
        lines += Line("journal", "Journal", "Logbook", xp, 100,
            if (journal != 0.0) "${f0(journal)} entr${if (journal == 1.0) "y" else "ies"}" else "No entry yet")

        // Screen time only counts once the phone has synced that day, so a missing day isn't read as 0 minutes.
        val synced = "screen.total_minutes" in m
        val yt = ctx.youtube
        lines += Line("youtube", "YouTube", "Lore", if (synced && yt <= 60) 50 else 0, 50,
            if (synced) "${f0(yt)} min (limit 60)" else "Phone not synced")

        val social = m["screen.social_media_minutes"] ?: 0.0
        xp = if (!synced) 0 else if (social < 30) 100 else if (social > 60) -50 else 0
        lines += Line("social", "Social media", "Sanity", xp, 100,
            if (synced) "${f0(social)} min (under 30 earns XP, over 60 costs 50)" else "Phone not synced")

        // Stocks: a day that ends flat or up, plus money added (equity rose by more than the day's P/L).
        val pl = m["trading.pl_day"]
        if (pl != null) {
            xp = if (pl >= 0) 50 else 0
            var detail = String.format(Locale.UK, "Day P/L $%,.2f", pl)
            val prev = ctx.prevEquity
            val equity = m["trading.equity"]
            if (prev != null && equity != null && equity - prev - pl > 1) {
                xp += 25
                detail += ", capital added"
            }
            lines += Line("stocks", "Stocks", "Treasury", xp, 75, detail)
        } else lines += Line("stocks", "Stocks", "Treasury", 0, 75, "Market closed or no data")
        return lines
    }

    val DAILY_MAX: Int = scoreDay(emptyMap(), Ctx()).sumOf { it.max }

    const val WELL_RESTED_MINUTES = 450.0
    const val WELL_RESTED_BONUS = 0.10

    data class StatSheet(
        val key: String, val label: String, val stat: String, val xp: Int, val level: Int, val rank: String,
        val score: Int, val modifier: Int, val levelXp: Int, val levelCost: Int?, val xpToNext: Int,
    )

    fun statSheet(key: String, label: String, stat: String, xp: Double, maxXp: Int): StatSheet {
        val lv = levelFor(xp, maxXp.toDouble() / DAILY_MAX)
        val (score, mod) = abilityScore(lv.level)
        return StatSheet(key, label, stat, pyRound(xp), lv.level, STAT_RANKS.first { lv.level <= it.first }.second,
            score, mod, lv.levelXp, lv.levelCost, lv.xpToNext)
    }

    private fun sheets(statXp: Map<String, Double>): List<StatSheet> =
        scoreDay(emptyMap(), Ctx()).map { statSheet(it.key, it.label, it.stat, statXp[it.key] ?: 0.0, it.max) }

    private val POWER_ORDER = compareBy<Pair<Int, Int>>({ it.first }, { it.second })

    /** Each group's (highest stat level, that stat's XP). */
    fun groupPower(stats: List<StatSheet>): Map<String, Pair<Int, Int>> {
        val byKey = stats.associateBy { it.key }
        return Perks.GROUPS.mapValues { (_, g) ->
            g.stats.map { byKey.getValue(it).level to byKey.getValue(it).xp }.maxWithOrNull(POWER_ORDER)!!
        }
    }

    data class CharacterClass(val name: String, val groups: List<String>, val detail: String)

    fun characterClass(stats: List<StatSheet>): CharacterClass {
        val power = groupPower(stats)
        val order = power.keys.sortedWith { a, b -> POWER_ORDER.compare(power.getValue(b), power.getValue(a)) }
        val first = order[0]
        val second = order[1]
        val ability = { g: String -> Perks.GROUPS.getValue(g).ability }
        val p1 = power.getValue(first).first
        val p2 = power.getValue(second).first
        return when {
            p1 < 2 -> CharacterClass("Adventurer", emptyList(), "Level a stat to 2 to find your class")
            p1 >= 2 * p2 -> CharacterClass(Perks.PURE_CLASSES.getValue(first), listOf(first), "Pure ${ability(first)}")
            else -> CharacterClass(Perks.DUAL_CLASSES.getValue(setOf(first, second)), listOf(first, second),
                "${ability(first)} + ${ability(second)}")
        }
    }

    data class GroupSheet(
        val key: String, val name: String, val ability: String, val level: Int, val tier: Int,
        val score: Int, val modifier: Int, val title: String?, val nextTierLevel: Int?,
        val earnedPerks: List<Pair<Int, Perks.Perk>>, val claimed: Map<Int, String>,
    )

    fun groupSheets(stats: List<StatSheet>, claims: Map<Pair<String, Int>, String>): List<GroupSheet> {
        val power = groupPower(stats)
        return Perks.GROUPS.map { (g, group) ->
            val level = power.getValue(g).first
            val tier = Perks.tierFor(level)
            val nextTier = Perks.TIERS.keys.firstOrNull { it > tier }
            val (score, mod) = abilityScore(level)
            GroupSheet(
                key = g, name = group.name, ability = group.ability, level = level, tier = tier,
                score = score, modifier = mod,
                title = if (tier > 0) Perks.PERKS.getValue(g to tier).title else null,
                nextTierLevel = nextTier?.let { Perks.TIERS.getValue(it).second },
                earnedPerks = Perks.TIERS.filter { (_, v) -> level >= v.second }.keys.map { it to Perks.PERKS.getValue(g to it) },
                claimed = claims.filterKeys { it.first == g }.mapKeys { it.key.second },
            )
        }
    }

    data class Buff(val name: String, val detail: String, val xp: Int)
    data class HistoryDay(val date: LocalDate, val xp: Int, val wellRested: Boolean, val socialOk: Boolean, val committed: Boolean)
    data class Streak(val key: String, val label: String, val days: Int, val unit: String)

    val STREAKS = listOf(
        Triple("social_ok", "social under 30 min", "day"),
        Triple("well_rested", "Well-Rested", "night"),
        Triple("committed", "any commit", "day"),
    )

    private fun kept(h: HistoryDay, key: String) = when (key) {
        "social_ok" -> h.socialOk
        "well_rested" -> h.wellRested
        else -> h.committed
    }

    /** Days in a row each habit was kept. Today counts as in progress, so a streak runs to yesterday until today meets it. */
    fun streaks(history: List<HistoryDay>): List<Streak> = STREAKS.map { (key, label, unit) ->
        val days = if (history.isNotEmpty() && !kept(history.last(), key)) history.dropLast(1) else history
        Streak(key, label, days.asReversed().takeWhile { kept(it, key) }.size, unit)
    }

    fun todayElapsed(day: LocalDate, now: LocalDateTime = LocalDateTime.now()): Double = when {
        day < now.toLocalDate() -> 1.0
        day > now.toLocalDate() -> 0.0
        else -> (now.hour * 3600 + now.minute * 60 + now.second) / 86400.0
    }

    data class State(
        val date: LocalDate, val level: LevelInfo, val totalXp: Int, val dayNumber: Int,
        val characterClass: CharacterClass, val title: String?, val todayXp: Int, val dailyMax: Int,
        val buffs: List<Buff>, val lines: List<Line>, val avgDailyXp14d: Int?, val daysToMaxLevel: Int?,
        val streaks: List<Streak>, val vitals: List<Vitals.BarState>, val stats: List<StatSheet>,
        val groups: List<GroupSheet>, val history: List<HistoryDay>,
    )

    private fun avg(values: List<Double>): Double? = if (values.isEmpty()) null else values.sum() / values.size

    /**
     * Scores every day from [gameStart] to [day]. [days] maps a date to that day's metrics (from [DayMetrics.build]).
     */
    fun build(
        days: Map<LocalDate, Map<String, Double>>, day: LocalDate, gameStart: LocalDate = DEFAULT_GAME_START,
        now: LocalDateTime = LocalDateTime.now(),
    ): State {
        val lookback = gameStart.minusDays(30)
        val sortedDates = days.keys.sorted()

        // When each perk token was first logged as claimed. A claim only counts once the token was earned.
        val logged = Perks.PERKS.keys.associateWith { (g, t) ->
            sortedDates.firstOrNull { (days.getValue(it)[Perks.claimMetric(g, t)] ?: 0.0) > 0 }
        }
        val claims = linkedMapOf<Pair<String, Int>, LocalDate>()

        val history = mutableListOf<HistoryDay>()
        val statXp = mutableMapOf<String, Double>()
        val rhrSeen = mutableListOf<Double>()
        val hrvSeen = mutableListOf<Double>()
        var prevEquity: Double? = null
        var todayLines: List<Line> = emptyList()
        var todayBuffs: List<Buff> = emptyList()
        val vitalDays = mutableListOf<Vitals.DayInput>()

        var d = lookback
        while (d <= day) {
            val m = days[d] ?: emptyMap()
            val youtube = m["screen.youtube_minutes"] ?: 0.0
            if (d >= gameStart) {
                val power = groupPower(sheets(statXp))
                for ((gt, on) in logged) {
                    if (on == d && gt !in claims && power.getValue(gt.first).first >= Perks.TIERS.getValue(gt.second).second) {
                        claims[gt] = d
                    }
                }
                val yesterday = d.minusDays(1)
                val active = claims.filterValues { it == d }.keys
                val buffs = mutableListOf<Buff>()

                val ctx = Ctx(
                    rhrBase = avg(rhrSeen.takeLast(30)), hrvBase = avg(hrvSeen.takeLast(30)),
                    prevEquity = prevEquity, youtube = youtube,
                )
                val lines = scoreDay(m, ctx)
                vitalDays += Vitals.DayInput(d, m, youtube)
                val byKey = lines.associateBy { it.key }
                if (("wis" to 1) in active) {
                    byKey.getValue("social").xp = maxOf(0, byKey.getValue("social").xp)
                    byKey.getValue("youtube").xp = byKey.getValue("youtube").max
                    buffs += Buff("Unrestricted Media Pass", "Social media and YouTube can't cost XP today", 0)
                }

                val base = maxOf(0, lines.sumOf { it.xp })
                val rested = (m["sleep.total_minutes"] ?: 0.0) >= WELL_RESTED_MINUTES
                val passUsed = listOf("con" to 1, "con" to 2).firstOrNull { it in active }?.let { Perks.PERKS.getValue(it).token }
                // The buff adds to the character's XP only; each stat keeps the XP it earned itself.
                val bonus = if (rested || passUsed != null) pyRound(base * WELL_RESTED_BONUS) else 0
                if (bonus != 0) {
                    buffs += Buff("Well-Rested",
                        if (passUsed != null && !rested) "$passUsed: +10% XP today" else "7.5 h+ sleep: +10% XP today", bonus)
                }
                var xp = base + bonus
                if (("wis" to 4) in active || claims["wis" to 4] == yesterday) {
                    val floor = pyRound(avg(history.takeLast(14).map { it.xp.toDouble() }) ?: 0.0)
                    if (floor > xp) {
                        buffs += Buff("Digital Detox Retreat", "Scored at your 14-day average", floor - xp)
                        xp = floor
                    }
                }
                val synced = "screen.total_minutes" in m
                history += HistoryDay(d, xp, bonus != 0,
                    synced && (m["screen.social_media_minutes"] ?: 0.0) < 30, (m["github.commits"] ?: 0.0) > 0)
                for (line in lines) {
                    // A stat's XP never drops below 0, so a bad social media day can't undo levels.
                    statXp[line.key] = maxOf(0.0, (statXp[line.key] ?: 0.0) + line.xp)
                }
                if (d == day) {
                    todayLines = lines
                    todayBuffs = buffs
                }
            }
            m["health.resting_heart_rate"]?.let { rhrSeen += it }
            m["health.hrv_rmssd"]?.let { hrvSeen += it }
            m["trading.equity"]?.let { prevEquity = it }
            d = d.plusDays(1)
        }

        val total = history.sumOf { it.xp }
        val vitalBars = Vitals.run(vitalDays, todayElapsed(day, now))
        val stats = sheets(statXp)
        val groups = groupSheets(stats, claims.mapValues { it.value.toString() })
        val top = groups.maxWithOrNull(compareBy<GroupSheet>({ it.tier }, { it.level }))!!
        val recent = avg(history.takeLast(14).map { it.xp.toDouble() })
        val remaining = maxOf(0, XP_TO_MAX - total)
        val daysLeft = when {
            remaining == 0 -> 0
            recent != null && recent != 0.0 -> pyRound(remaining / recent)
            else -> null
        }
        return State(
            date = day, level = levelFor(total.toDouble()), totalXp = total, dayNumber = history.size,
            characterClass = characterClass(stats), title = top.title,
            todayXp = history.lastOrNull()?.xp ?: 0, dailyMax = DAILY_MAX, buffs = todayBuffs, lines = todayLines,
            avgDailyXp14d = recent?.let { pyRound(it) }, daysToMaxLevel = daysLeft,
            streaks = streaks(history), vitals = vitalBars, stats = stats, groups = groups,
            history = history.takeLast(30),
        )
    }
}

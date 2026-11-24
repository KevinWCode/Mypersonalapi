package com.kevinwcode.personalapi

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * "Test B": a second set of vitals rules (from Kevin's "G feed back" notes, 2026-11-22) worked out side by side with
 * the live ones, so the two can be compared on the same logged data. Nothing here changes the live game: XP, stats,
 * perks and classes come from [Game] unchanged, and only two things differ:
 *
 *  - the vitals ([VitalsB]): a 7.5 h sleep target with an 8 h overcharge to 110, a Mana morning reset, gentler
 *    waking drains, a 30 g sugar buffer, food, workout and study as spend-and-reward, and named debuffs;
 *  - "Prime Condition": on a day all four bars wake up at 70+ (and HP, Stamina and Focus at 80+, so 6 h of sleep or
 *    more), kettlebell, commits and journal XP get +10%. The other end of the scale is a "Rest Day": HP, Stamina or Focus
 *    woke under 80 (under 6 h of sleep). It costs no XP. The drain is the same every day, so a low start runs out sooner.
 */
object TestB {
    const val PRIME_MIN = 70.0
    const val PRIME_BONUS = 0.10
    /** Stats that come from something done by hand. Study has no XP line yet, so it isn't in here. */
    val MANUAL_STATS = setOf("kettlebell", "github", "journal")

    /** The day's character with the Test B vitals and Prime Condition. Same arguments as [Game.build]. */
    fun build(
        days: Map<LocalDate, Map<String, Double>>, day: LocalDate, gameStart: LocalDate = Game.DEFAULT_GAME_START,
        now: LocalDateTime = LocalDateTime.now(), profile: Profile = Profile.ALL,
    ): Game.State = buildWithCondition(days, day, gameStart, now, profile).first

    /** [build], plus the day's condition (Prime Condition, Steady or Rest Day) for the badge. */
    fun buildWithCondition(
        allDays: Map<LocalDate, Map<String, Double>>, day: LocalDate, gameStart: LocalDate = Game.DEFAULT_GAME_START,
        now: LocalDateTime = LocalDateTime.now(), profile: Profile = Profile.ALL,
    ): Pair<Game.State, VitalsB.Condition> {
        val days = profile.stripDays(allDays)
        val inputs = mutableListOf<Vitals.DayInput>()
        var d = gameStart
        while (d <= day) {
            val m = days[d] ?: emptyMap()
            inputs += Vitals.DayInput(d, m, m["screen.youtube_minutes"] ?: 0.0)
            d = d.plusDays(1)
        }
        val result = VitalsB.run(inputs, Game.todayElapsed(day, now))
        val g = Game.build(
            days, day, gameStart, now, profile,
            extraBuff = { date, lines -> if (result.prime[date] == true) primeBuff(lines) else null },
            vitalsRun = { _, _ -> result.bars },
        )
        return g to (result.condition[day] ?: VitalsB.Condition.NO_SLEEP)
    }

    fun primeBuff(lines: List<Game.Line>): Game.Buff {
        val manual = lines.filter { it.key in MANUAL_STATS }.sumOf { maxOf(0, it.xp) }
        return Game.Buff("Prime Condition", "Woke up with all four vitals 70+: +10% XP on kettlebell, commits and journal",
            Game.pyRound(manual * PRIME_BONUS))
    }

    /** The same JSON as GET /game, plus what the Test B page needs: the rule set, debuff names, and the Prime line. */
    fun toJson(g: Game.State, condition: VitalsB.Condition? = null): JSONObject {
        val out = GameJson.toJson(g, VitalsB.BARS).put("rules", "test_b").put("prime_min", PRIME_MIN)
            .put("pace", JSONObject().put("end_level", VitalsB.END_LEVEL).put("prime", VitalsB.PRIME_PACE).put("low", VitalsB.LOW_PACE)
                .put("prime_wake", VitalsB.PRIME_WAKE).put("prime_floor", VitalsB.PRIME_FLOOR))
        if (condition != null) {
            out.put("condition", JSONObject().put("state", condition.state).put("name", condition.title).put("detail", condition.detail))
        }
        val vitals = out.getJSONArray("vitals")
        for (i in 0 until vitals.length()) {
            val v = vitals.getJSONObject(i)
            val debuff = VitalsB.DEBUFFS.getValue(v.getString("key"))
            v.put("debuff", JSONObject().put("name", debuff.name).put("why", debuff.why).put("fix", debuff.fix))
        }
        return out
    }
}

/**
 * The Test B vital bars. Same shapes as [Vitals] (bars, effects, start, end), different numbers. Rules, in order:
 *
 *  - Sleep sets HP, Stamina and Focus to 100 x minutes / 450 up to 7.5 h, then on up to 110 at 8 h ("Overcharge").
 *  - Sleep sets Mana to 52, +20 for 7.5 h or more, +25 when deep + REM is 35% or more of the night.
 *  - No sleep logged: HP, Stamina and Focus start the day at 70 and Mana at 52 (missing data is not a bad night).
 *  - Over the waking day (07:00 to 23:00) Stamina and Focus drain 90 and Mana 62: the same every day, so a full
 *    night's start (100, Mana 72) ends close to empty at 10, and a short night runs out sooner. What you gain (water,
 *    study...) and lose (screens...) on the way moves that end up or down. HP regenerates 12 a day up to 100.
 *  - Sugar: -1 HP per 2 g above a 30 g buffer; a sugar rush of +1 Stamina per 2 g (up to +15, that day only).
 *  - Food: +10 HP for 135 g protein or more; +8 HP for 2,150 kcal or less over at least two meals.
 *  - Water +2 Stamina and +2 Focus per 250 ml. Coffee +3 Focus a cup (first 2); cups 3 and on cost 6 HP each.
 *  - Workout (same counting as live): -25 Stamina, +15 Focus, +5 HP today; +12 Stamina tomorrow, +20 if that day's
 *    protein reached 135 g.
 *  - Study, per 30 min up to 90 min: -5 Mana, +10 Focus ("Flow State"). Study never costs HP; low Mana is shown
 *    as the "Brain Fog" debuff only.
 *  - Reading +15 Mana and +5 Focus per 30 min. A journal entry +20 Mana, +8 Focus.
 *  - Social media drains as live (-1 HP and Mana per 5 min, -1 Focus per 2 min) after 10 free minutes a day;
 *    YouTube as live. Under 30 min of social media yesterday gives +15 Focus.
 */
object VitalsB {
    const val TOP = 100.0
    const val OVERCHARGE_TOP = 110.0
    const val LOW = 25.0
    /** Where Stamina, Mana and Focus are drained down to by 23:00 before the day's gains and costs. */
    const val END_LEVEL = 10.0
    /** Each bar's own state compares it with a good day's line (100 on waking, END_LEVEL at 23:00; HP stays at 100). */
    const val PRIME_PACE = 0.85
    const val LOW_PACE = 0.5
    /** A bar shows Prime (and glows) only if it also woke up at this or more and is still above [PRIME_FLOOR]. */
    const val PRIME_WAKE = 85.0
    const val PRIME_FLOOR = 25.0
    /** HP, Stamina or Focus waking under this (under 6 h of sleep) makes the day a Rest Day. */
    const val REST_DAY_BELOW = 80.0
    const val NO_SLEEP_START = 70.0

    val BARS = listOf(
        Vitals.Bar("hp", "HP", "Sleep sets it (7.5 h = 100, 8 h = 110); regenerates 12 a day; sugar over 30 g, coffee after 2 cups and screens lower it; protein, sensible calories and workouts raise it", 12.0),
        Vitals.Bar("stamina", "Stamina", "Sleep sets it (7.5 h = 100, 8 h = 110); drains 90 over the waking day (100 down to 10); workouts spend it today and pay it back tomorrow; water and a sugar rush top it up", -90.0),
        Vitals.Bar("mana", "Mana", "Resets to 52 each morning (+20 for 7.5 h sleep, +25 for deep + REM 35%); drains 62 over the waking day (72 down to 10); study spends it, reading and journal refill it", -62.0),
        Vitals.Bar("focus", "Focus", "Sleep sets it (7.5 h = 100, 8 h = 110); drains 90 over the waking day (100 down to 10); study, reading, water, workouts and a calm social day yesterday raise it", -90.0),
    )

    fun topOf(key: String) = if (key == "mana") TOP else OVERCHARGE_TOP

    const val SLEEP_FULL_MINUTES = 450.0
    const val SLEEP_OVERCHARGE_MINUTES = 480.0

    const val MANA_RESET = 52.0
    const val MANA_RESTED = 20.0
    const val MANA_DEEP_REM = 25.0

    const val SUGAR_BUFFER_G = 30.0
    const val SUGAR_RUSH_CAP = 15.0
    const val PROTEIN_TARGET_G = 135.0
    const val PROTEIN_HP = 10.0
    const val KCAL_LIMIT = 2150.0
    const val KCAL_HP = 8.0
    const val COFFEE_FREE_CUPS = 2.0
    const val COFFEE_HP = -6.0

    const val WORKOUT_STAMINA = -25.0
    const val WORKOUT_FOCUS = 15.0
    const val WORKOUT_HP = 5.0
    const val WORKOUT_NEXT_DAY_STAMINA = 12.0
    const val WORKOUT_NEXT_DAY_STAMINA_FED = 20.0

    const val STUDY_MAX_MINUTES = 90.0
    const val STUDY_MANA = -5.0
    const val STUDY_FOCUS = 10.0
    const val READING_MANA = 15.0
    const val READING_FOCUS = 5.0
    const val JOURNAL_MANA = 20.0
    const val JOURNAL_FOCUS = 8.0

    const val SOCIAL_GRACE_MINUTES = 10.0
    const val CALM_SOCIAL_MINUTES = 30.0
    const val CALM_NEXT_DAY_FOCUS = 15.0


    data class Debuff(val name: String, val why: String, val fix: String)

    val DEBUFFS = mapOf(
        "hp" to Debuff("Sluggish", "Sugar crash or poor sleep",
            "sleep 7.5 h, keep sugar under 30 g, hit 135 g protein (+10) or work out (+5)."),
        "stamina" to Debuff("Fatigued", "Needs hydration or a rest day",
            "drink water (+2 per 250 ml) and sleep 7.5 h; yesterday's workout pays back +12 (+20 with 135 g protein)."),
        "mana" to Debuff("Brain Fog", "Mana is running dry",
            "read 30 min (+15), write a journal entry (+20) or sleep 7.5 h for a bigger morning reset."),
        "focus" to Debuff("Scattered", "Social media or YouTube overload",
            "study (+10 per 30 min), drink water, and keep social media under 30 min for +15 tomorrow."),
    )

    fun afterSleep(minutes: Double): Double {
        if (minutes <= SLEEP_FULL_MINUTES) return TOP * minutes / SLEEP_FULL_MINUTES
        val extra = minOf(minutes, SLEEP_OVERCHARGE_MINUTES) - SLEEP_FULL_MINUTES
        return TOP + (OVERCHARGE_TOP - TOP) * extra / (SLEEP_OVERCHARGE_MINUTES - SLEEP_FULL_MINUTES)
    }

    fun deepRemOk(m: Map<String, Double>, sleep: Double): Boolean =
        (m["sleep.deep_minutes"] ?: 0.0) + (m["sleep.rem_minutes"] ?: 0.0) >= 0.35 * sleep

    fun manaReset(m: Map<String, Double>, sleep: Double): Double =
        MANA_RESET + (if (sleep >= SLEEP_FULL_MINUTES) MANA_RESTED else 0.0) + (if (deepRemOk(m, sleep)) MANA_DEEP_REM else 0.0)

    fun proteinHit(m: Map<String, Double>) = (m["food.protein_g"] ?: 0.0) >= PROTEIN_TARGET_G

    fun mealsLogged(m: Map<String, Double>) = Food.MEALS.count { (m["food.$it.kcal"] ?: 0.0) > 0 }

    /** The day's changes to each bar. */
    fun effects(
        m: Map<String, Double>, youtubeMinutes: Double, yesterday: Map<String, Double>,
    ): Map<String, MutableList<Vitals.Effect>> {
        val out = BARS.associate { it.key to mutableListOf<Vitals.Effect>() }
        fun add(bar: String, label: String, points: Double, temporary: Boolean = false) {
            val p = Math.rint(points * 10) / 10
            if (p != 0.0) out.getValue(bar).add(Vitals.Effect(label, p, temporary))
        }

        val synced = "screen.total_minutes" in m
        val social = m["screen.social_media_minutes"] ?: 0.0
        val counted = maxOf(0.0, social - SOCIAL_GRACE_MINUTES)
        if (synced) {
            for (bar in listOf("hp", "mana", "focus")) {
                val every = if (bar == "focus") Vitals.SOCIAL_FOCUS_MINUTES else Vitals.SOCIAL_HP_MANA_MINUTES
                add(bar, "Social media (${social.toInt()} min, first ${SOCIAL_GRACE_MINUTES.toInt()} free)",
                    Vitals.screenDrain(counted, every))
            }
        }
        for (bar in listOf("hp", "mana", "focus")) {
            val every = (if (bar == "focus") Vitals.SOCIAL_FOCUS_MINUTES else Vitals.SOCIAL_HP_MANA_MINUTES) * Vitals.YOUTUBE_FACTOR
            add(bar, "YouTube (${youtubeMinutes.toInt()} min)", Vitals.screenDrain(youtubeMinutes, every))
        }

        // Food.
        val sugar = m["food.sugar_g"] ?: 0.0
        val sugarLabel = Math.rint(sugar).toInt()
        add("hp", "Sugar ($sugarLabel g, ${SUGAR_BUFFER_G.toInt()} g free)", -Math.floor(maxOf(0.0, sugar - SUGAR_BUFFER_G) / 2))
        add("stamina", "Sugar rush ($sugarLabel g, today only)", minOf(SUGAR_RUSH_CAP, Math.floor(sugar / 2)), temporary = true)
        if (proteinHit(m)) add("hp", "Protein ${PROTEIN_TARGET_G.toInt()} g+", PROTEIN_HP)
        val kcal = m["food.kcal"] ?: 0.0
        if (kcal > 0 && kcal <= KCAL_LIMIT && mealsLogged(m) >= 2) add("hp", "Calories on target (${kcal.toInt()} kcal)", KCAL_HP)

        // Drinks.
        val water = Vitals.WATER_PER_250_ML * Math.floor((m["water.ml"] ?: 0.0) / 250)
        add("stamina", "Water", water)
        add("focus", "Water", water)
        val cups = m["coffee.cups"] ?: 0.0
        add("focus", "Coffee", 3 * minOf(cups, COFFEE_FREE_CUPS))
        add("hp", "Coffee past ${COFFEE_FREE_CUPS.toInt()} cups", COFFEE_HP * maxOf(0.0, Math.floor(cups) - COFFEE_FREE_CUPS))

        // Workouts: spend today, recover tomorrow.
        if (Vitals.workedOut(yesterday)) {
            if (proteinHit(yesterday)) add("stamina", "Recovery: yesterday's workout + protein", WORKOUT_NEXT_DAY_STAMINA_FED)
            else add("stamina", "Recovery: yesterday's workout", WORKOUT_NEXT_DAY_STAMINA)
        }
        if (Vitals.workedOut(m)) {
            add("stamina", "Workout", WORKOUT_STAMINA)
            add("focus", "Workout", WORKOUT_FOCUS)
            add("hp", "Workout", WORKOUT_HP)
        }

        // Mind: reading and journal refill Mana, study spends it.
        val reading = m["reading.minutes"] ?: 0.0
        add("mana", "Reading", READING_MANA * reading / 30)
        add("focus", "Reading", READING_FOCUS * reading / 30)
        val journal = (m["journal.entries"] ?: 0.0) >= 1
        if (journal) {
            add("mana", "Journal entry", JOURNAL_MANA)
            add("focus", "Journal entry", JOURNAL_FOCUS)
        }
        val blocks = minOf(m["study.minutes"] ?: 0.0, STUDY_MAX_MINUTES) / 30
        if (blocks > 0) {
            add("mana", "Study", STUDY_MANA * blocks)
            add("focus", "Flow State (study)", STUDY_FOCUS * blocks)
        }

        val calm = "screen.total_minutes" in yesterday && Profile.SOCIAL_OFF !in yesterday && (yesterday["screen.social_media_minutes"] ?: 0.0) < CALM_SOCIAL_MINUTES
        if (calm) add("focus", "Social media under ${CALM_SOCIAL_MINUTES.toInt()} min yesterday", CALM_NEXT_DAY_FOCUS)
        return out
    }

    fun tickShare(bar: Vitals.Bar, dayShare: Double) = if (bar.tickPerDay > 0) dayShare else Vitals.awakeShare(dayShare)

    fun endValue(key: String, start: Double, gained: Double, tick: Double): Double {
        val top = topOf(key)
        return Vitals.applyTick((start + gained).coerceIn(0.0, top), tick).coerceIn(0.0, top)
    }

    /** The day's overall state, judged on how you woke up, shown as the badge above the bars. */
    data class Condition(val state: String, val title: String, val detail: String) {
        companion object {
            val PRIME = Condition("prime", "Prime Condition", "You woke up with all four bars at 70+: kettlebell, commits and journal earn +10% XP all day.")
            val STEADY = Condition("steady", "Steady", "A fair start. Wake up with all four bars at 70+ (about 7.5 h of good sleep) for Prime Condition: +10% XP on kettlebell, commits and journal.")
            val REST = Condition("rest", "Rest Day", "A short night (under 6 h), so your bars started low. No XP is lost, but they drain at the usual pace, so they run low sooner today. Top up with water, go easy, and an early night brings Prime back.")
            val NO_SLEEP = Condition("unknown", "Steady", "No sleep logged yet, so the bars start at 70. Sync your watch to see today's real condition.")
        }
    }

    fun condition(start: Map<String, Double>, sleepLogged: Boolean): Condition = when {
        !sleepLogged -> Condition.NO_SLEEP
        listOf("hp", "stamina", "focus").any { start.getValue(it) < REST_DAY_BELOW } -> Condition.REST
        BARS.all { start.getValue(it.key) >= TestB.PRIME_MIN } -> Condition.PRIME
        else -> Condition.STEADY
    }

    /** The day's tick: the same every day, whatever the start (no gentler drain on a Rest Day). */
    fun dayTick(bar: Vitals.Bar): Double = bar.tickPerDay

    /** A good day's line at this point of the day: 100 on waking, falling to END_LEVEL at 23:00 (HP stays at 100). */
    fun paceLine(bar: Vitals.Bar, dayShare: Double): Double =
        if (bar.tickPerDay > 0) TOP else TOP - (TOP - END_LEVEL) * Vitals.awakeShare(dayShare)

    class Result(val bars: List<Vitals.BarState>, val prime: Map<LocalDate, Boolean>, val condition: Map<LocalDate, Condition> = emptyMap())

    /** Past days apply in full; the last day only ticks by the share of it that has passed (0 to 1). */
    fun run(days: List<Vitals.DayInput>, todayElapsed: Double = 1.0): Result {
        val values = BARS.associate { it.key to Vitals.START_VALUE }.toMutableMap()
        val last = mutableMapOf<String, Triple<Double, Double, List<Vitals.Effect>>>()
        val prime = linkedMapOf<LocalDate, Boolean>()
        val conditions = linkedMapOf<LocalDate, Condition>()
        val dayTicks = mutableMapOf<String, Double>()
        var lastShare = 1.0
        var yesterday: Map<String, Double> = emptyMap()
        days.forEachIndexed { i, d ->
            val m = d.metrics
            val isToday = i == days.size - 1
            val share = if (isToday) todayElapsed else 1.0
            val start = values.toMutableMap()
            val resets = mutableMapOf<String, String>()
            val sleep = m["sleep.total_minutes"]
            if (sleep != null && sleep != 0.0) {
                val hours = "%.1f".format(java.util.Locale.UK, sleep / 60)
                for (k in listOf("hp", "stamina", "focus")) {
                    start[k] = afterSleep(sleep)
                    resets[k] = "Slept $hours h: set to ${Math.rint(start.getValue(k)).toInt()}" +
                        if (sleep > SLEEP_FULL_MINUTES) " (Overcharge)" else ""
                }
                start["mana"] = manaReset(m, sleep)
                resets["mana"] = "Morning reset to ${start.getValue("mana").toInt()}" + listOfNotNull(
                    if (sleep >= SLEEP_FULL_MINUTES) "7.5 h sleep +20" else null,
                    if (deepRemOk(m, sleep)) "deep + REM +25" else null,
                ).joinToString("") { ", $it" }
            } else {
                // Missing data is not a bad night: start from a neutral 70 (Mana its usual 52) rather than last night's low.
                for (k in listOf("hp", "stamina", "focus")) {
                    start[k] = NO_SLEEP_START
                    resets[k] = "No sleep logged: starts at ${NO_SLEEP_START.toInt()}"
                }
                start["mana"] = MANA_RESET
                resets["mana"] = "No sleep logged: morning reset to ${MANA_RESET.toInt()}"
            }
            val fx = effects(m, d.youtubeMinutes, yesterday)
            for ((k, label) in resets) fx.getValue(k).add(0, Vitals.Effect(label, 0.0))
            yesterday = m

            // The day's condition is judged on how you woke up, after the morning reset. It then lasts the day.
            val cond = condition(start, sleep != null && sleep != 0.0)
            conditions[d.date] = cond
            prime[d.date] = cond == Condition.PRIME
            lastShare = share
            for (bar in BARS) {
                val all = fx.getValue(bar.key)
                val gained = all.sumOf { it.points }
                // The sugar rush shows on its own day but has worn off by the next.
                val lasting = gained - all.filter { it.temporary }.sumOf { it.points }
                val s = start.getValue(bar.key)
                val tick = dayTick(bar)
                values[bar.key] = endValue(bar.key, s, if (isToday) gained else lasting, tick * tickShare(bar, share))
                last[bar.key] = Triple(s, gained, all)
                dayTicks[bar.key] = tick
            }
        }
        val bars = BARS.map { bar ->
            val (start, gained, fx) = last[bar.key] ?: Triple(Vitals.START_VALUE, 0.0, emptyList())
            val value = values.getValue(bar.key)
            Vitals.BarState(
                key = bar.key, name = bar.name, about = bar.about, value = Math.rint(value).toInt(),
                low = value < LOW_PACE * paceLine(bar, lastShare), top = topOf(bar.key),
                startOfDay = Math.rint(start * 10) / 10, gainedToday = Math.rint(gained * 10) / 10,
                endOfDay = Math.rint(endValue(bar.key, start, gained, dayTicks[bar.key] ?: bar.tickPerDay)).toInt(), today = fx,
                tickPerDay = Math.rint((dayTicks[bar.key] ?: bar.tickPerDay) * 10) / 10,
            )
        }
        return Result(bars, prime, conditions)
    }
}

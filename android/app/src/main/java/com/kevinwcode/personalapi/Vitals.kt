package com.kevinwcode.personalapi

import java.time.LocalDate

/**
 * Four vital bars, each 0 to 100: HP, Stamina, Mana and Focus. A port of the server's app/vitals.py; the numbers
 * and rules are the same, so the phone and the PC show the same bars. See that file for the full description.
 */
object Vitals {
    const val START_VALUE = 70.0
    const val TOP = 100.0
    const val LOW = 25.0

    data class Bar(val key: String, val name: String, val about: String, val tickPerDay: Double)

    val BARS = listOf(
        Bar("hp", "HP", "Reset by sleep, regenerates slowly, lowered by coffee, sugar, social media and YouTube", 12.0),
        Bar("stamina", "Stamina", "Reset by sleep (10 an hour), drains 60 over the waking day, water, workouts and a sugar rush move it", -60.0),
        Bar("mana", "Mana", "Four refills of up to 25: sleep, one journal entry, reading, deep + REM sleep; social media drains it", -60.0),
        Bar("focus", "Focus", "Reset by sleep (12 an hour), drains 60 over the waking day, water, workouts and study top it up", -60.0),
    )

    const val AWAKE_FROM_HOUR = 7
    const val AWAKE_HOURS = 16
    const val WATER_PER_250_ML = 2

    fun awakeShare(dayShare: Double): Double = ((dayShare * 24 - AWAKE_FROM_HOUR) / AWAKE_HOURS).coerceIn(0.0, 1.0)

    const val STAMINA_PER_SLEEP_HOUR = 10.0
    const val FOCUS_PER_SLEEP_HOUR = 12.0
    const val WORKOUT_STAMINA = -33.0
    const val WORKOUT_NEXT_DAY_STAMINA = 10.0
    const val WORKOUT_FOCUS = 15.0

    const val HP_FULL_MINUTES = 420.0
    const val HP_BONUS_MINUTES = 480.0
    const val HP_BONUS_TOP = 110.0
    const val HP_COFFEE = -5.0
    const val SOCIAL_HP_MANA_MINUTES = 5.0
    const val SOCIAL_FOCUS_MINUTES = 2.0
    const val YOUTUBE_FACTOR = 3.0
    const val CALM_SOCIAL_MINUTES = 25.0
    const val CALM_NEXT_DAY_FOCUS = 15.0
    const val MANA_PART = 25.0
    // Sugar: -1 HP per full gram, and a temporary +1 Stamina per full 2 g (that day only; gone by the next).
    const val SUGAR_HP_PER_G = 1.0
    const val SUGAR_STAMINA_EVERY_G = 2.0

    fun screenDrain(minutes: Double, every: Double): Double = -Math.floor(minutes / every)

    fun hpAfterSleep(minutes: Double): Double {
        if (minutes <= HP_FULL_MINUTES) return TOP * minutes / HP_FULL_MINUTES
        val extra = minOf(minutes, HP_BONUS_MINUTES) - HP_FULL_MINUTES
        return TOP + (HP_BONUS_TOP - TOP) * extra / (HP_BONUS_MINUTES - HP_FULL_MINUTES)
    }

    fun manaFromSleep(minutes: Double): Double {
        val hours = minutes / 60
        if (hours <= 6) return 15 * hours / 6
        return minOf(MANA_PART, 15 + 5 * (hours - 6))
    }

    fun workedOut(m: Map<String, Double>): Boolean =
        (m["workout.counted_sessions"] ?: 0.0) > 0 ||
            m.any { (k, v) -> v > 0 && k.startsWith("kb.") && (k.endsWith(".sets") || k.endsWith(".done")) }

    fun afterSleep(minutes: Double): Map<String, Double> {
        val hours = minutes / 60
        return linkedMapOf(
            "hp" to hpAfterSleep(minutes),
            "stamina" to minOf(TOP, STAMINA_PER_SLEEP_HOUR * hours),
            "focus" to minOf(TOP, FOCUS_PER_SLEEP_HOUR * hours),
        )
    }

    data class Effect(val label: String, val points: Double, val temporary: Boolean = false)

    fun effects(
        m: Map<String, Double>, youtubeMinutes: Double = 0.0, workoutYesterday: Boolean = false,
        calmYesterday: Boolean = false,
    ): Map<String, MutableList<Effect>> {
        val out = BARS.associate { it.key to mutableListOf<Effect>() }
        fun add(bar: String, label: String, points: Double, temporary: Boolean = false) {
            val p = Math.rint(points * 10) / 10
            if (p != 0.0) out.getValue(bar).add(Effect(label, p, temporary))
        }

        val sleep = m["sleep.total_minutes"]
        val journal = m["journal.entries"] ?: 0.0
        val cups = minOf(m["coffee.cups"] ?: 0.0, 2.0)
        val synced = "screen.total_minutes" in m
        val social = m["screen.social_media_minutes"] ?: 0.0

        add("hp", "Coffee", HP_COFFEE * (m["coffee.cups"] ?: 0.0))
        if (synced) {
            for (bar in listOf("hp", "mana", "focus")) {
                val every = if (bar == "focus") SOCIAL_FOCUS_MINUTES else SOCIAL_HP_MANA_MINUTES
                add(bar, "Social media (${social.toInt()} min)", screenDrain(social, every))
            }
        }
        for (bar in listOf("hp", "mana", "focus")) {
            val every = (if (bar == "focus") SOCIAL_FOCUS_MINUTES else SOCIAL_HP_MANA_MINUTES) * YOUTUBE_FACTOR
            add(bar, "YouTube (${youtubeMinutes.toInt()} min)", screenDrain(youtubeMinutes, every))
        }

        val workout = workedOut(m)
        if (workoutYesterday) add("stamina", "Yesterday's workout", WORKOUT_NEXT_DAY_STAMINA)
        if (workout) add("stamina", "Workout", WORKOUT_STAMINA)
        val sugar = m["food.sugar_g"] ?: 0.0
        val sugarLabel = Math.rint(sugar).toInt()
        add("hp", "Sugar ($sugarLabel g)", -SUGAR_HP_PER_G * Math.floor(sugar))
        add("stamina", "Sugar rush ($sugarLabel g, today only)", Math.floor(sugar / SUGAR_STAMINA_EVERY_G), temporary = true)
        val water = WATER_PER_250_ML * Math.floor((m["water.ml"] ?: 0.0) / 250)
        add("stamina", "Water", water)
        add("focus", "Water", water)

        if (sleep != null && sleep != 0.0) {
            add("mana", "Sleep", manaFromSleep(sleep))
            val deepRem = (m["sleep.deep_minutes"] ?: 0.0) + (m["sleep.rem_minutes"] ?: 0.0)
            add("mana", "Deep + REM sleep", if (deepRem >= 0.35 * sleep) MANA_PART else 0.0)
        }
        add("mana", "Journal entry", if (journal >= 1) MANA_PART else 0.0)
        add("mana", "Reading", minOf(MANA_PART, 12.5 * (m["reading.minutes"] ?: 0.0) / 30))

        if (workout) add("focus", "Workout", WORKOUT_FOCUS)
        add("focus", "Study", 8 * minOf((m["study.minutes"] ?: 0.0) / 30, 3.0))
        add("focus", "Journal entry", if (journal >= 1) 8.0 else 0.0)
        add("focus", "Coffee", 3 * cups)
        if (calmYesterday) add("focus", "Social media under ${CALM_SOCIAL_MINUTES.toInt()} min yesterday", CALM_NEXT_DAY_FOCUS)
        return out
    }

    private fun clamp(v: Double, top: Double = TOP) = v.coerceIn(0.0, top)

    fun applyTick(value: Double, tick: Double): Double =
        if (tick <= 0) value + tick else maxOf(value, minOf(TOP, value + tick))

    fun endValue(key: String, start: Double, gained: Double, tick: Double): Double {
        val top = if (key == "hp") HP_BONUS_TOP else TOP
        return clamp(applyTick(clamp(start + gained, top), tick), top)
    }

    data class DayInput(val date: LocalDate, val metrics: Map<String, Double>, val youtubeMinutes: Double)

    data class BarState(
        val key: String, val name: String, val about: String, val value: Int, val low: Boolean,
        val top: Double, val startOfDay: Double, val gainedToday: Double, val endOfDay: Int, val today: List<Effect>,
    )

    /** Past days apply in full; the last day only ticks by the share of it that has passed (0 to 1). */
    fun run(days: List<DayInput>, todayElapsed: Double = 1.0): List<BarState> {
        val values = BARS.associate { it.key to START_VALUE }.toMutableMap()
        val last = mutableMapOf<String, Triple<Double, Double, List<Effect>>>()
        var yesterday: Map<String, Double> = emptyMap()
        days.forEachIndexed { i, d ->
            val m = d.metrics
            val calm = "screen.total_minutes" in yesterday &&
                (yesterday["screen.social_media_minutes"] ?: 0.0) < CALM_SOCIAL_MINUTES
            val fx = effects(m, d.youtubeMinutes, workedOut(yesterday), calm)
            yesterday = m
            val share = if (i == days.size - 1) todayElapsed else 1.0
            val start = values.toMutableMap()
            val sleep = m["sleep.total_minutes"]
            if (sleep != null && sleep != 0.0) {
                for ((k, v) in afterSleep(sleep)) {
                    start[k] = v
                    fx.getValue(k).add(0, Effect("Slept ${"%.1f".format(java.util.Locale.UK, sleep / 60)} h: reset to ${Math.rint(v).toInt()}", 0.0))
                }
            }
            val isToday = i == days.size - 1
            for (bar in BARS) {
                val gained = fx.getValue(bar.key).sumOf { it.points }
                // A temporary boost (the sugar rush) shows on its own day but has worn off by the next.
                val lasting = gained - fx.getValue(bar.key).filter { it.temporary }.sumOf { it.points }
                val tick = bar.tickPerDay * (if (bar.tickPerDay > 0) share else awakeShare(share))
                values[bar.key] = endValue(bar.key, start.getValue(bar.key), if (isToday) gained else lasting, tick)
                last[bar.key] = Triple(start.getValue(bar.key), gained, fx.getValue(bar.key))
            }
        }
        return BARS.map { bar ->
            val (start, gained, fx) = last[bar.key] ?: Triple(START_VALUE, 0.0, emptyList())
            val value = values.getValue(bar.key)
            BarState(
                key = bar.key, name = bar.name, about = bar.about, value = Math.rint(value).toInt(),
                low = value <= LOW, top = if (bar.key == "hp") HP_BONUS_TOP else TOP,
                startOfDay = Math.rint(start * 10) / 10, gainedToday = Math.rint(gained * 10) / 10,
                endOfDay = Math.rint(endValue(bar.key, start, gained, bar.tickPerDay)).toInt(), today = fx,
            )
        }
    }
}

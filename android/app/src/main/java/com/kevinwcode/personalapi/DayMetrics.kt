package com.kevinwcode.personalapi

import java.time.LocalDate
import java.util.Locale

/**
 * Turns one day's stored rows into the numbers the app shows, including the social media total.
 * The same rules as the server's app/metrics.py, so the phone and the PC agree.
 */
object DayMetrics {
    // Android package names counted as social media.
    val SOCIAL_MEDIA_APPS = mapOf(
        "com.zhiliaoapp.musically" to "tiktok",
        "com.ss.android.ugc.trill" to "tiktok", // TikTok's package name in some regions
        "com.instagram.android" to "instagram",
        "com.facebook.katana" to "facebook",
        "com.facebook.lite" to "facebook",
        "com.reddit.frontpage" to "reddit",
    )

    // Tracked on their own and left out of the social media total.
    val SEPARATE_APPS = mapOf(
        "com.google.android.youtube" to "youtube",
    )

    const val APP_PREFIX = "screen.app."

    /** metric to value for one day. Rows from different sources for the same metric are added together. */
    fun build(rows: List<Pair<String, Double>>): Map<String, Double> {
        val day = mutableMapOf<String, Double>()
        fun add(key: String, v: Double) = day.merge(key, v, Double::plus)
        for ((metric, value) in rows) {
            if (!metric.startsWith(APP_PREFIX)) {
                add(metric, value)
                continue
            }
            // Every app counts toward total screen time.
            add("screen.total_minutes", value)
            val pkg = metric.removePrefix(APP_PREFIX)
            val name = SOCIAL_MEDIA_APPS[pkg] ?: SEPARATE_APPS[pkg] ?: continue
            add("screen.${name}_minutes", value)
            if (pkg in SOCIAL_MEDIA_APPS) add("screen.social_media_minutes", value)
        }
        return day
    }

    const val FOOD_PREFIX = "food."

    // From this day on, food comes only from meals logged in the app; the PC's food (its food AI files) is ignored.
    // Earlier days keep the PC's food, so past XP doesn't change, unless the app has meals for that day.
    val PHONE_FOOD_SINCE: LocalDate = LocalDate.of(2026, 11, 20)

    /**
     * rows: (date, source, metric to value). A day with rows from [pcSource] uses those (the PC's day totals, already
     * built), with food from the phone's own meals ([foodSource]) as above. Any other day is built from the phone's
     * own rows.
     */
    fun mergeDays(
        rows: List<Triple<String, String, Pair<String, Double>>>, pcSource: String, foodSource: String = "food",
        phoneFoodSince: LocalDate = PHONE_FOOD_SINCE,
    ): Map<LocalDate, Map<String, Double>> =
        rows.groupBy { it.first }.entries.associate { (date, dayRows) ->
            val day = LocalDate.parse(date)
            val pc = dayRows.filter { it.second == pcSource }.map { it.third }
            val food = dayRows.filter { it.second == foodSource }.map { it.third }
            val phoneFood = day >= phoneFoodSince || food.any { it.second > 0 }
            day to when {
                pc.isEmpty() -> build(dayRows.map { it.third })
                phoneFood -> pc.filterNot { it.first.startsWith(FOOD_PREFIX) }.toMap() + build(food)
                else -> pc.toMap()
            }
        }

    /** Short lines for the app's "Today on this phone" card. Stats with no data are left out. */
    fun summaryLines(day: Map<String, Double>): List<String> {
        val out = mutableListOf<String>()
        day["sleep.total_minutes"]?.let { out += "Sleep: ${hoursMinutes(it)}" }
        day["health.steps"]?.let { out += "Steps: ${String.format(Locale.UK, "%,d", it.toLong())}" }
        day["health.resting_heart_rate"]?.let { out += "Resting heart rate: ${it.toInt()} bpm" }
        day["workout.sessions"]?.let { s ->
            out += "Workouts: ${s.toInt()}" + (day["workout.minutes"]?.let { ", ${it.toInt()} min" } ?: "")
        }
        day["screen.social_media_minutes"]?.let { out += "Social media: ${it.toInt()} min" }
        day["screen.youtube_minutes"]?.let { out += "YouTube: ${it.toInt()} min" }
        day["screen.total_minutes"]?.let { out += "Phone use: ${hoursMinutes(it)}" }
        val kbSets = day.filterKeys { it.startsWith("kb.") && it.endsWith(".sets") }.values.sum()
        if (kbSets > 0) out += "Kettlebell sets: ${kbSets.toInt()}"
        day["football.max_catch_yards"]?.let { if (it > 0) out += "Longest catch: ${it.toInt()} yd" }
        return out
    }

    private fun hoursMinutes(minutes: Double): String {
        val m = minutes.toInt()
        return "${m / 60} h ${m % 60} min"
    }
}

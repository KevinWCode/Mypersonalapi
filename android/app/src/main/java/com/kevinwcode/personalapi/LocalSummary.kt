package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * The widget's numbers worked out from the phone's own days: a port of the server's app/summary.py.
 * The last trade comes from the Alpaca fills saved on the phone.
 */
object LocalSummary {
    const val SOCIAL_LIMIT_MINUTES = 60.0

    // A day only counts once the phone has sent its screen time, so a missing day isn't read as 0 minutes.
    private fun social(m: Map<String, Double>): Double? =
        if ("screen.total_minutes" !in m) null else m["screen.social_media_minutes"] ?: 0.0

    private fun avg(v: List<Double>): Double? = if (v.isEmpty()) null else v.sum() / v.size

    fun build(days: Map<LocalDate, Map<String, Double>>, day: LocalDate, limit: Double = SOCIAL_LIMIT_MINUTES,
              lastTrade: Fill? = null): Summary {
        fun on(d: LocalDate) = days[d] ?: emptyMap()
        fun week(offset: Int) = (offset until offset + 7).mapNotNull { social(on(day.minusDays(it.toLong()))) }
        val thisWeek = week(0)
        val lastWeek = week(7)
        val reclaimed = if (thisWeek.isNotEmpty() && lastWeek.isNotEmpty()) avg(lastWeek)!! * thisWeek.size - thisWeek.sum() else null
        val today = on(day)
        val weekDays = (0 until 7).map { on(day.minusDays(it.toLong())) }
        fun weekValues(key: String) = weekDays.mapNotNull { it[key] }
        fun youtube(d: Map<String, Double>) = if ("screen.total_minutes" in d) d["screen.youtube_minutes"] ?: 0.0 else null
        val synced = weekDays.any { "health.steps" in it || "screen.total_minutes" in it }
        // trading.pl_day over the 7 days up to today, rounded to cents like the server.
        val pl = (0 until 7).sumOf { on(day.minusDays(it.toLong()))["trading.pl_day"] ?: 0.0 }
        return Summary(
            date = day,
            reclaimedMinutes = reclaimed,
            weekMinutes = if (thisWeek.isNotEmpty()) thisWeek.sum() else null,
            weekPerDay = avg(thisWeek),
            lastWeekPerDay = avg(lastWeek),
            kcal = today["food.kcal"],
            proteinG = today["food.protein_g"],
            sleepMinutes = today["sleep.total_minutes"],
            steps = today["health.steps"],
            stepsPerDay = avg(weekValues("health.steps")),
            restingHr = avg(weekValues("health.resting_heart_rate")),
            workoutsWeek = if (synced) weekValues("workout.sessions").sum() else null,
            studyHoursWeek = weekValues("study.minutes").sum() / 60,
            readingHoursWeek = weekValues("reading.minutes").sum() / 60,
            pl7d = Math.rint(pl * 100) / 100,
            lastTrade = lastTrade?.toTrade(),
            socialDays = (6 downTo 0).map { social(on(day.minusDays(it.toLong()))) },
            limitMinutes = limit,
            youtubeToday = youtube(today),
            youtubeWeekPerDay = avg(weekDays.mapNotNull(::youtube)),
        )
    }

    /** The summary in the server's GET /summary shape, which the widget caches. */
    fun toJson(s: Summary): JSONObject {
        val t = s.lastTrade
        fun n(v: Double?): Any = v ?: JSONObject.NULL
        return JSONObject()
            .put("date", s.date.toString())
            .put("social", JSONObject()
                .put("reclaimed_minutes", n(s.reclaimedMinutes)).put("week_minutes", n(s.weekMinutes))
                .put("week_per_day", n(s.weekPerDay)).put("last_week_per_day", n(s.lastWeekPerDay))
                .put("limit_minutes", s.limitMinutes)
                .put("days", JSONArray(s.socialDays.mapIndexed { i, m ->
                    JSONObject().put("date", s.date.minusDays((s.socialDays.size - 1 - i).toLong()).toString()).put("minutes", n(m))
                })))
            .put("youtube", JSONObject().put("today_minutes", n(s.youtubeToday)).put("week_per_day", n(s.youtubeWeekPerDay)))
            .put("kcal", n(s.kcal)).put("protein_g", n(s.proteinG)).put("sleep_minutes", n(s.sleepMinutes))
            .put("steps", n(s.steps)).put("steps_per_day", n(s.stepsPerDay)).put("resting_hr", n(s.restingHr))
            .put("workouts_week", n(s.workoutsWeek)).put("study_hours_week", n(s.studyHoursWeek))
            .put("reading_hours_week", n(s.readingHoursWeek)).put("pl_7d", n(s.pl7d))
            .put("last_trade", if (t == null) JSONObject.NULL else JSONObject().put("symbol", t.symbol).put("side", t.side)
                .put("qty", t.qty).put("price", t.price).put("filled_at", t.filledAt?.toString() ?: JSONObject.NULL))
    }
}

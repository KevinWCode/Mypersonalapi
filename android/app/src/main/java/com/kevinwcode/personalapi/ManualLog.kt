package com.kevinwcode.personalapi

import android.content.Context
import java.time.LocalDate

/**
 * The few stats no sensor can measure. Keeps a running total per day on the phone and always uploads
 * the day's totals, so the server's overwrite-per-day storage stays correct.
 */
class ManualLog(context: Context) {
    private val prefs = context.getSharedPreferences("manual", Context.MODE_PRIVATE)

    fun addKettlebellSet(move: String, reps: Int, weightLb: Double, day: LocalDate = LocalDate.now()) {
        add(day, "kb.$move.reps", reps.toDouble(), "count", sum = true)
        add(day, "kb.$move.sets", 1.0, "count", sum = true)
        add(day, "kb.$move.top_weight_lb", weightLb, "lb", sum = false)
    }

    fun setCatchDistance(yards: Double, day: LocalDate = LocalDate.now()) =
        add(day, "football.max_catch_yards", yards, "yards", sum = false)

    fun today(day: LocalDate = LocalDate.now()): List<Observation> =
        prefs.all.mapNotNull { (key, v) ->
            val parts = key.split("|")
            if (parts.size != 3 || parts[0] != day.toString()) null
            else Observation(day, parts[1], (v as String).toDouble(), parts[2])
        }

    /** Zeroes today's entries (they're re-sent as 0 so the server forgets them too). */
    fun clearToday(day: LocalDate = LocalDate.now()): List<Observation> {
        val zeroed = today(day).map { it.copy(value = 0.0) }
        prefs.edit().apply { prefs.all.keys.filter { it.startsWith("$day|") }.forEach { remove(it) } }.apply()
        return zeroed
    }

    private fun add(day: LocalDate, metric: String, value: Double, unit: String, sum: Boolean) {
        val key = "$day|$metric|$unit"
        val old = prefs.getString(key, null)?.toDouble()
        val new = when {
            old == null -> value
            sum -> old + value
            else -> maxOf(old, value)
        }
        prefs.edit().putString(key, new.toString()).apply()
    }
}

package com.kevinwcode.personalapi

import android.content.Context
import java.time.LocalDate

/**
 * What you log by hand in the app: quick log taps (water, coffee, reading, study, journal, workout done),
 * kettlebell sets, catch distance and perk claims. Keeps a running total per day on the phone; each sync saves the
 * last days' totals in the database (and the PC, while it's on), which overwrite per day, so nothing counts twice.
 */
class ManualLog(context: Context) {
    private val prefs = context.getSharedPreferences("manual", Context.MODE_PRIVATE)
    private val undo = context.getSharedPreferences("manual_undo", Context.MODE_PRIVATE)

    enum class Mode { ADD, SET, MAX }

    data class Entry(val metric: String, val value: Double, val unit: String, val mode: Mode = Mode.ADD)

    fun addKettlebellSet(move: String, reps: Int, weightLb: Double, day: LocalDate = LocalDate.now()) = log(
        day,
        Entry("kb.$move.reps", reps.toDouble(), "count"),
        Entry("kb.$move.sets", 1.0, "count"),
        Entry("kb.$move.top_weight_lb", weightLb, "lb", Mode.MAX),
    )

    fun setCatchDistance(yards: Double, day: LocalDate = LocalDate.now()) =
        log(day, Entry("football.max_catch_yards", yards, "yards", Mode.MAX))

    /** Applies the entries and remembers what they replaced, so [undoLast] can put it back. */
    fun log(day: LocalDate, vararg entries: Entry) {
        val undoEdit = undo.edit().clear()
        entries.forEach { e -> undoEdit.putString(key(day, e), prefs.getString(key(day, e), null) ?: NONE) }
        undoEdit.apply()
        val edit = prefs.edit()
        for (e in entries) {
            val k = key(day, e)
            val old = prefs.getString(k, null)?.toDouble()
            edit.putString(k, apply(old, e).toString())
        }
        edit.apply()
    }

    /** Puts back what the last tap changed. Returns false when there's nothing to undo. */
    fun undoLast(): Boolean {
        val saved = undo.all
        if (saved.isEmpty()) return false
        val edit = prefs.edit()
        // A value that didn't exist before is set to 0 rather than removed, so the next sync overwrites it everywhere.
        saved.forEach { (k, v) -> edit.putString(k, if (v == NONE) "0.0" else v as String) }
        edit.apply()
        undo.edit().clear().apply()
        return true
    }

    fun value(metric: String, day: LocalDate = LocalDate.now()): Double =
        prefs.all.entries.firstOrNull { it.key.startsWith("$day|$metric|") }?.let { (it.value as String).toDouble() } ?: 0.0

    fun day(day: LocalDate): List<Observation> =
        prefs.all.mapNotNull { (key, v) ->
            val parts = key.split("|")
            if (parts.size != 3 || parts[0] != day.toString()) null
            else Observation(day, parts[1], (v as String).toDouble(), parts[2])
        }

    fun today(day: LocalDate = LocalDate.now()): List<Observation> = day(day)

    /** Zeroes today's entries (they're saved as 0 so the database and the PC forget them too). */
    fun clearToday(day: LocalDate = LocalDate.now()): List<Observation> {
        val zeroed = today(day).map { it.copy(value = 0.0) }
        prefs.edit().apply { prefs.all.keys.filter { it.startsWith("$day|") }.forEach { remove(it) } }.apply()
        undo.edit().clear().apply()
        return zeroed
    }

    private fun key(day: LocalDate, e: Entry) = "$day|${e.metric}|${e.unit}"

    /** A tap's quick log button: label, entries, and what the toast says. Same amounts as the dashboard's. */
    data class Quick(val label: String, val entries: List<Entry>, val done: String)

    companion object {
        private const val NONE = "-"

        /** The new value after an entry; never below 0. */
        fun apply(old: Double?, e: Entry): Double = maxOf(
            0.0,
            when {
                old == null || e.mode == Mode.SET -> e.value
                e.mode == Mode.ADD -> old + e.value
                else -> maxOf(old, e.value)
            },
        )

        val QUICK = listOf(
            Quick("+250 ml water", listOf(Entry("water.ml", 250.0, "ml")), "Added 250 ml water"),
            Quick("+1 coffee", listOf(Entry("coffee.cups", 1.0, "cups")), "Added a coffee"),
            Quick("+1 hour reading", listOf(Entry("reading.minutes", 60.0, "minutes")), "Added 1 hour of reading"),
            Quick("+30 min study", listOf(Entry("study.minutes", 30.0, "minutes")), "Added 30 min of study"),
            Quick("Journal entry", listOf(Entry("journal.entries", 1.0, "count")), "Journal entry logged"),
            Quick(
                "Workout done",
                listOf("swing", "clean", "clean_press").map { Entry("kb.$it.done", 1.0, "count", Mode.SET) },
                "Ticked Swing, Clean and Clean & press for today",
            ),
        )

        fun perkClaim(group: String, tier: Int) = Entry(Perks.claimMetric(group, tier), 1.0, "count", Mode.SET)
    }
}

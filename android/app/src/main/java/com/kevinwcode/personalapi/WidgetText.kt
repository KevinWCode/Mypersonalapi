package com.kevinwcode.personalapi

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** The numbers the server's GET /summary returns for one day. Null means nothing came in yet. */
data class Summary(
    val date: LocalDate,
    val reclaimedMinutes: Double?,
    val weekMinutes: Double?,
    val weekPerDay: Double?,
    val lastWeekPerDay: Double?,
    val kcal: Double?,
    val proteinG: Double?,
    val sleepMinutes: Double?,
    val steps: Double?,
)

/** The text on the widget. */
data class WidgetLines(
    val label: String,
    val headline: String,
    val note: String,
    val food: String,
    val protein: String,
    val sleep: String,
    val sleepWhen: String,
    val steps: String,
    val stepsWhen: String,
    val updated: String,
    val offline: Boolean,
)

/** Turns a summary into the widget's text, kept apart from the widget so it can be unit tested. */
object WidgetText {
    private val CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    private const val NONE = "—"

    /** Minutes as the dashboard writes them: 116 -> "1h 56m", 45 -> "45m". */
    fun hm(minutes: Double): String {
        val m = abs(minutes).roundToLong()
        val text = if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
        return if (minutes < 0 && m > 0) "−$text" else text
    }

    fun count(v: Double): String = String.format(Locale.UK, "%,d", v.roundToLong())

    private fun day(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.UK)

    fun lines(s: Summary?, today: LocalDate, fetchedAt: LocalDateTime?, offline: Boolean): WidgetLines {
        val time = fetchedAt?.let { if (it.toLocalDate() == today) CLOCK.format(it) else "${day(it.toLocalDate())} ${CLOCK.format(it)}" }
        val updated = listOfNotNull(if (offline) "Offline" else null, time).joinToString(" · ")
        if (s == null) {
            return WidgetLines("Reclaimed from social media", NONE, if (offline) "Can't reach the server yet" else "Loading…",
                NONE, "", NONE, "", NONE, "", updated, offline)
        }

        // Same headline as the dashboard: time reclaimed once there's a last week to compare with.
        val (label, headline, note) = when {
            s.reclaimedMinutes != null -> Triple("Reclaimed from social media", hm(s.reclaimedMinutes),
                "${s.weekPerDay?.roundToLong()} min/day this week, ${s.lastWeekPerDay?.roundToLong()} last week")
            s.weekMinutes != null -> Triple("Social media this week", hm(s.weekMinutes), "${s.weekPerDay?.roundToLong()} min/day")
            else -> Triple("Reclaimed from social media", NONE, "No screen time from the phone yet")
        }
        // If the server was off, these are an earlier day's numbers, so say which day.
        val isToday = s.date == today
        return WidgetLines(
            label = label,
            headline = headline,
            note = note,
            food = s.kcal?.let { "${count(it)} kcal" } ?: NONE,
            protein = s.proteinG?.let { "${it.roundToLong()} g protein" } ?: if (s.kcal == null) "nothing yet" else "",
            sleep = s.sleepMinutes?.let { hm(it) } ?: NONE,
            sleepWhen = if (isToday) "last night" else "woke ${day(s.date)}",
            steps = s.steps?.let { count(it) } ?: NONE,
            stepsWhen = if (isToday) "today" else day(s.date),
            updated = updated,
            offline = offline,
        )
    }
}

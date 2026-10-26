package com.kevinwcode.personalapi

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** A fill from Alpaca, as the server's GET /summary returns it. */
data class Trade(val symbol: String, val side: String, val qty: Double, val price: Double, val filledAt: Instant?)

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
    val stepsPerDay: Double? = null,
    val restingHr: Double? = null,
    val workoutsWeek: Double? = null,
    val studyHoursWeek: Double? = null,
    val readingHoursWeek: Double? = null,
    val pl7d: Double? = null,
    val lastTrade: Trade? = null,
)

/** One stat on the widget: the big value and the small line under it. */
data class Tile(val value: String, val sub: String)

/**
 * The text on the widget. Tiles are in the layout's order: food, protein, sleep, steps, resting heart rate,
 * workouts, study, reading, P/L.
 */
data class WidgetLines(
    val label: String,
    val headline: String,
    val note: String,
    val tiles: List<Tile>,
    /** True when the P/L is up, false when down, null when there's none. */
    val plUp: Boolean?,
    val trade: String,
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

    /** 1.5 -> "1.5 h", 2.0 -> "2 h". */
    fun hours(h: Double): String = "${trim(Math.round(h * 10) / 10.0)} h"

    fun money(v: Double, signed: Boolean = false): String {
        val sign = if (v < 0) "−" else if (signed && v > 0) "+" else ""
        return sign + String.format(Locale.UK, "$%,.2f", abs(v))
    }

    private fun trim(v: Double) = if (v == Math.floor(v)) v.roundToLong().toString() else v.toString()

    private fun day(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.UK)

    private fun stamp(at: LocalDateTime, today: LocalDate) =
        if (at.toLocalDate() == today) CLOCK.format(at) else "${day(at.toLocalDate())} ${CLOCK.format(at)}"

    fun trade(t: Trade, today: LocalDate, zone: ZoneId): String {
        val verb = if (t.side.startsWith("buy")) "Bought" else "Sold"
        val at = t.filledAt?.let { " · " + stamp(it.atZone(zone).toLocalDateTime(), today) } ?: ""
        return "$verb ${trim(t.qty)} ${t.symbol} at ${money(t.price)}$at"
    }

    fun lines(s: Summary?, today: LocalDate, fetchedAt: LocalDateTime?, offline: Boolean, zone: ZoneId = ZoneId.systemDefault()): WidgetLines {
        val updated = listOfNotNull(if (offline) "Offline" else null, fetchedAt?.let { stamp(it, today) }).joinToString(" · ")
        if (s == null) {
            return WidgetLines("Reclaimed from social media", NONE, if (offline) "Can't reach the server yet" else "Loading…",
                List(9) { Tile(NONE, "") }, null, "", updated, offline)
        }

        // Same headline as the dashboard: time reclaimed once there's a last week to compare with.
        val (label, headline, note) = when {
            s.reclaimedMinutes != null -> Triple("Reclaimed from social media", hm(s.reclaimedMinutes),
                "${s.weekPerDay?.roundToLong()} min/day this week, ${s.lastWeekPerDay?.roundToLong()} last week")
            s.weekMinutes != null -> Triple("Social media this week", hm(s.weekMinutes), "${s.weekPerDay?.roundToLong()} min/day")
            else -> Triple("Reclaimed from social media", NONE, "No screen time from the phone yet")
        }
        // If the server was off, these are an earlier day's numbers, so say which day.
        val todayWord = if (s.date == today) "today" else day(s.date)
        val tiles = listOf(
            Tile(s.kcal?.let { count(it) } ?: NONE, "kcal $todayWord"),
            Tile(s.proteinG?.let { "${it.roundToLong()} g" } ?: NONE, todayWord),
            Tile(s.sleepMinutes?.let { hm(it) } ?: NONE, if (s.date == today) "last night" else "woke ${day(s.date)}"),
            Tile(s.stepsPerDay?.let { count(it) } ?: NONE, "/day · " + (s.steps?.let { count(it) } ?: "0") + " $todayWord"),
            Tile(s.restingHr?.let { "${it.roundToLong()} bpm" } ?: NONE, "7-day average"),
            Tile(s.workoutsWeek?.let { count(it) } ?: NONE, "last 7 days"),
            Tile(s.studyHoursWeek?.let { hours(it) } ?: NONE, "last 7 days"),
            Tile(s.readingHoursWeek?.let { hours(it) } ?: NONE, "last 7 days"),
            Tile(s.pl7d?.let { money(it, signed = true) } ?: NONE, "last 7 days"),
        )
        val plUp = s.pl7d?.takeIf { it != 0.0 }?.let { it > 0 }
        val trade = s.lastTrade?.let { "Last trade: " + trade(it, today, zone) } ?: "No trades yet"
        return WidgetLines(label, headline, note, tiles, plUp, trade, updated, offline)
    }
}

package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WidgetTextTest {
    private val monday = LocalDate.parse("2026-10-26")
    private val london = ZoneId.of("Europe/London")
    private val summary = Summary(
        date = monday, reclaimedMinutes = 116.0, weekMinutes = 342.0, weekPerDay = 57.0, lastWeekPerDay = 73.4,
        kcal = 1019.0, proteinG = 56.2, sleepMinutes = 432.0, steps = 801.0,
        stepsPerDay = 8240.4, restingHr = 58.3, workoutsWeek = 2.0, studyHoursWeek = 1.5, readingHoursWeek = 2.0,
        pl7d = 12.4, lastTrade = Trade("AAPL", "buy", 2.0, 231.4, Instant.parse("2026-10-23T14:32:05Z")),
        socialDays = listOf(20.0, 45.0, null, 60.0, 61.0, 90.0, 30.0), limitMinutes = 60.0, youtubeWeekPerDay = 24.6,
    )

    @Test
    fun todaysNumbers() {
        val t = WidgetText.lines(summary, monday, LocalDateTime.parse("2026-10-26T07:42:10"), offline = false, zone = london)
        assertEquals("57 min/day · 1h 56m reclaimed · YouTube 25 min/day", t.note)
        assertEquals(
            listOf("1,019", "56 g", "7h 12m", "8,240", "58 bpm", "2", "1.5 h", "2 h", "+$12.40"),
            t.tiles.map { it.value })
        assertEquals("kcal today", t.tiles[0].sub)
        assertEquals("/day · 801 today", t.tiles[3].sub)
        assertEquals(true, t.plUp)
        assertEquals("Last trade: Bought 2 AAPL at $231.40 · Fri 15:32", t.trade)
        assertEquals("07:42", t.updated)
    }

    @Test
    fun serverOffOvernightSaysWhichDay() {
        val sunday = summary.copy(date = monday.minusDays(1))
        val t = WidgetText.lines(sunday, monday, LocalDateTime.parse("2026-10-25T23:10:00"), offline = true)
        assertEquals("Offline · Sun 23:10", t.updated)
        assertEquals("kcal Sun", t.tiles[0].sub)
        assertEquals("woke Sun", t.tiles[2].sub)
    }

    @Test
    fun noDataYet() {
        val empty = Summary(monday, null, null, null, null, null, null, null, null)
        val t = WidgetText.lines(empty, monday, LocalDateTime.parse("2026-10-26T07:00:00"), offline = false)
        assertEquals("No screen time from the phone yet", t.note)
        assertEquals(List(7) { Level.NONE }, t.bars.map { it.level })
        assertEquals(List(9) { "—" }, t.tiles.map { it.value })
        assertNull(t.plUp)
        assertEquals("No trades yet", t.trade)
        assertEquals("Can't reach the server yet", WidgetText.lines(null, monday, null, offline = true).note)
    }

    @Test
    fun lossesAndSells() {
        val t = WidgetText.lines(summary.copy(reclaimedMinutes = -12.4, pl7d = -3.1,
            lastTrade = Trade("NVDA", "sell_short", 0.5, 1180.0, null)), monday, null, offline = false)
        assertEquals("57 min/day · 12m over last week · YouTube 25 min/day", t.note)
        assertEquals("−$3.10", t.tiles[8].value)
        assertEquals(false, t.plUp)
        assertEquals("Last trade: Sold 0.5 NVDA at $1,180.00", t.trade)
        assertEquals("2h 0m", WidgetText.hm(120.0))
    }

    @Test
    fun socialBarsAgainstTheLimit() {
        val t = WidgetText.lines(summary, monday, null, offline = false)
        assertEquals(
            listOf(Level.GOOD, Level.WARN, Level.NONE, Level.WARN, Level.OVER, Level.OVER, Level.GOOD),
            t.bars.map { it.level })
        // The tallest day (90) sets the top, so it fills the chart and the limit sits two thirds up.
        assertEquals(1.0, t.bars[5].height, 1e-9)
        assertEquals(0.0, t.bars[2].height, 1e-9)
        assertEquals(60.0 / 90, t.limitAt, 1e-9)
        assertEquals(listOf("20\nTue", "—\nThu", "30\nMon"), listOf(t.bars[0].label, t.bars[2].label, t.bars[6].label))
        // Quiet weeks still leave room over the limit.
        assertEquals(1 / 1.5, WidgetText.bars(listOf(10.0), 60.0, monday).second, 1e-9)
    }

    @Test
    fun syncOnlyRunsInTheDay() {
        assertEquals(false, SyncHours.allows(java.time.LocalTime.of(6, 59)))
        assertEquals(true, SyncHours.allows(java.time.LocalTime.of(7, 0)))
        assertEquals(true, SyncHours.allows(java.time.LocalTime.of(21, 59)))
        assertEquals(false, SyncHours.allows(java.time.LocalTime.of(22, 0)))
    }
}

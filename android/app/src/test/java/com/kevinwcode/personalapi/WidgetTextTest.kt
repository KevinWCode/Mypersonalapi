package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WidgetTextTest {
    private val monday = LocalDate.parse("2026-10-26")
    private val summary = Summary(
        date = monday, reclaimedMinutes = 116.0, weekMinutes = 342.0, weekPerDay = 57.0, lastWeekPerDay = 73.4,
        kcal = 1019.0, proteinG = 56.2, sleepMinutes = 432.0, steps = 10940.0,
    )

    @Test
    fun todaysNumbers() {
        val t = WidgetText.lines(summary, monday, LocalDateTime.parse("2026-10-26T07:42:10"), offline = false)
        assertEquals("1h 56m", t.headline)
        assertEquals("57 min/day this week, 73 last week", t.note)
        assertEquals(listOf("1,019 kcal", "56 g protein", "7h 12m", "last night", "10,940", "today", "07:42"),
            listOf(t.food, t.protein, t.sleep, t.sleepWhen, t.steps, t.stepsWhen, t.updated))
    }

    @Test
    fun serverOffOvernightSaysWhichDay() {
        val sunday = summary.copy(date = monday.minusDays(1))
        val t = WidgetText.lines(sunday, monday, LocalDateTime.parse("2026-10-25T23:10:00"), offline = true)
        assertEquals("Offline · Sun 23:10", t.updated)
        assertEquals("woke Sun", t.sleepWhen)
        assertEquals("Sun", t.stepsWhen)
    }

    @Test
    fun noDataYet() {
        val empty = Summary(monday, null, null, null, null, null, null, null, null)
        val t = WidgetText.lines(empty, monday, LocalDateTime.parse("2026-10-26T07:00:00"), offline = false)
        assertEquals(listOf("—", "—", "nothing yet", "—", "—"), listOf(t.headline, t.food, t.protein, t.sleep, t.steps))
        assertEquals("Can't reach the server yet", WidgetText.lines(null, monday, null, offline = true).note)
    }

    @Test
    fun moreSocialThanLastWeek() {
        val t = WidgetText.lines(summary.copy(reclaimedMinutes = -12.4), monday, null, offline = false)
        assertEquals("−12m", t.headline)
        assertEquals("2h 0m", WidgetText.hm(120.0))
    }
}

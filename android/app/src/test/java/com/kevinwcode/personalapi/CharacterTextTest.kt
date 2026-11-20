package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CharacterTextTest {
    @Test
    fun cardShowsLevelXpAndVitals() {
        val day = LocalDate.of(2026, 10, 23)
        val g = Game.build(
            mapOf(day to mapOf("sleep.total_minutes" to 480.0, "health.steps" to 12000.0, "screen.total_minutes" to 100.0)),
            day, now = day.plusDays(1).atStartOfDay(),
        )
        val lines = CharacterText.lines(g)
        assertTrue(lines[0], lines[0].startsWith("Level 1 Initiate · "))
        assertTrue(lines[1], lines[1].endsWith("XP to level 2"))
        assertTrue(lines[2], lines[2].startsWith("Today: ") && lines[2].contains("Well-Rested"))
        assertTrue(lines[3], lines[3].startsWith("HP ") && lines[3].contains("Focus "))
        assertEquals("Sleep: +75 (8.0 h, deep + REM 0%)", CharacterText.todayLines(g).first { it.startsWith("Sleep") })
    }
}

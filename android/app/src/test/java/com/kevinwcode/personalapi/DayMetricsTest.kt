package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayMetricsTest {
    @Test
    fun socialMediaAddsTheFourAppsAndLeavesYouTubeOut() {
        val day = DayMetrics.build(
            listOf(
                "screen.app.com.zhiliaoapp.musically" to 20.0,
                "screen.app.com.instagram.android" to 10.0,
                "screen.app.com.facebook.katana" to 5.0,
                "screen.app.com.reddit.frontpage" to 4.0,
                "screen.app.com.google.android.youtube" to 30.0,
                "screen.app.com.android.chrome" to 15.0,
            )
        )
        assertEquals(39.0, day.getValue("screen.social_media_minutes"), 0.001)
        assertEquals(30.0, day.getValue("screen.youtube_minutes"), 0.001)
        assertEquals(84.0, day.getValue("screen.total_minutes"), 0.001)
        assertEquals(20.0, day.getValue("screen.tiktok_minutes"), 0.001)
        assertNull(day["screen.chrome_minutes"])
    }

    @Test
    fun otherMetricsFromSeveralSourcesAreAdded() {
        val day = DayMetrics.build(listOf("water.ml" to 250.0, "water.ml" to 500.0, "health.steps" to 8000.0))
        assertEquals(750.0, day.getValue("water.ml"), 0.001)
        assertEquals(8000.0, day.getValue("health.steps"), 0.001)
    }

    @Test
    fun summaryShowsOnlyStatsWithData() {
        val lines = DayMetrics.summaryLines(
            mapOf(
                "sleep.total_minutes" to 452.0,
                "health.steps" to 10234.0,
                "screen.social_media_minutes" to 39.4,
                "kb.swing.sets" to 3.0,
                "kb.clean.sets" to 2.0,
            )
        )
        assertEquals(
            listOf("Sleep: 7 h 32 min", "Steps: 10,234", "Social media: 39 min", "Kettlebell sets: 5"),
            lines,
        )
    }

    @Test
    fun aDayCopiedFromThePcUsesThePcTotalsOnly() {
        val days = DayMetrics.mergeDays(
            listOf(
                Triple("2026-11-01", "phone", "screen.app.com.instagram.android" to 20.0),
                Triple("2026-11-01", "pc", "screen.social_media_minutes" to 20.0),
                Triple("2026-11-01", "pc", "journal.entries" to 1.0),
                // The PC's food (from its food AI files) is ignored; the phone's own meals count instead.
                Triple("2026-11-01", "pc", "food.kcal" to 900.0),
                Triple("2026-11-01", "food", "food.kcal" to 350.0),
                Triple("2026-11-02", "phone", "screen.app.com.instagram.android" to 15.0),
                Triple("2026-11-02", "manual", "kb.swing.sets" to 2.0),
                // Before the switch, a PC day with no meals in the app keeps the PC's food.
                Triple("2026-10-30", "pc", "food.kcal" to 700.0),
            ),
            "pc",
            phoneFoodSince = java.time.LocalDate.parse("2026-10-31"),
        )
        assertEquals(mapOf("food.kcal" to 700.0), days[java.time.LocalDate.parse("2026-10-30")])
        assertEquals(mapOf("screen.social_media_minutes" to 20.0, "journal.entries" to 1.0, "food.kcal" to 350.0), days[java.time.LocalDate.parse("2026-11-01")])
        val second = days.getValue(java.time.LocalDate.parse("2026-11-02"))
        assertEquals(15.0, second.getValue("screen.social_media_minutes"), 0.001)
        assertEquals(2.0, second.getValue("kb.swing.sets"), 0.001)
    }
}

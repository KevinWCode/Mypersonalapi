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
}

package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class HeartMathTest {
    private val t0 = Instant.parse("2026-10-26T03:00:00Z")
    private fun at(min: Long, bpm: Long) = t0.plusSeconds(min * 60) to bpm

    @Test
    fun picksTheCalmestFiveMinutes() {
        val samples = listOf(at(0, 70), at(1, 68), at(10, 52), at(11, 50), at(12, 54), at(20, 65))
        assertEquals(52.0, HeartMath.lowestRollingAverage(samples)!!, 0.001)
    }

    @Test
    fun sparseSamplesUseSingleReadings() {
        val samples = listOf(at(0, 60), at(10, 55), at(20, 58))
        assertEquals(55.0, HeartMath.lowestRollingAverage(samples)!!, 0.001)
    }

    @Test
    fun noSamplesGivesNull() {
        assertNull(HeartMath.lowestRollingAverage(emptyList()))
    }
}

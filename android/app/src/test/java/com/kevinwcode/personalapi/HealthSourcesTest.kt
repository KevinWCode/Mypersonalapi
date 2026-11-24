package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HealthSourcesTest {
    private fun at(h: Int, m: Int = 0) = Instant.parse("2026-11-24T00:00:00Z").plusSeconds(h * 3600L + m * 60L)
    private fun s(origin: String, from: Instant, to: Instant) = HealthSources.Session(origin, from, to, origin)

    @Test
    fun oneAppsSleepPerNight() {
        val watch = s("com.fitbit.FitbitMobile", at(0), at(7))
        val phone = s("android", at(1), at(6))
        assertEquals(listOf(watch), HealthSources.pickSleep(listOf(watch, phone), ""))
        assertEquals(listOf(phone), HealthSources.pickSleep(listOf(watch, phone), "android"))
        // The chosen app wrote nothing that night: use the longest.
        assertEquals(listOf(watch), HealthSources.pickSleep(listOf(watch, phone), "com.ouraring.oura"))
        // Two naps from the same app both count.
        val nap = s("android", at(14), at(15))
        assertEquals(listOf(phone, nap), HealthSources.pickSleep(listOf(phone, nap), "android"))
    }

    @Test
    fun overlappingWorkoutsFromTwoAppsCountOnce() {
        val garmin = s("com.garmin.android.apps.connectmobile", at(18), at(19))
        val strava = s("com.strava", at(18, 2), at(18, 58))
        val laterRun = s("com.strava", at(20), at(20, 40))
        assertEquals(listOf(garmin, laterRun), HealthSources.dedupeWorkouts(listOf(strava, garmin, laterRun), "com.garmin.android.apps.connectmobile"))
        assertEquals(listOf(strava, laterRun), HealthSources.dedupeWorkouts(listOf(strava, garmin, laterRun), "com.strava"))
        // Back-to-back sessions in one app are both kept.
        val second = s("com.garmin.android.apps.connectmobile", at(19), at(19, 30))
        assertEquals(listOf(garmin, second), HealthSources.dedupeWorkouts(listOf(second, garmin), ""))
        assertEquals("Fitbit", HealthSources.name("com.fitbit.FitbitMobile"))
    }
}

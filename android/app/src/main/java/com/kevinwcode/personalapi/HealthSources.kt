package com.kevinwcode.personalapi

import java.time.Duration
import java.time.Instant

/**
 * Which health app's data to use when more than one writes into Health Connect (say a Fitbit and the phone's own
 * step counter), so sleep and workouts aren't counted twice. Kept apart from Android so it can be unit tested.
 */
object HealthSources {
    /** A sleep or workout session as written by one app ([origin] is its package name). */
    data class Session<T>(val origin: String, val start: Instant, val end: Instant, val record: T) {
        val minutes: Long get() = Duration.between(start, end).toMinutes()
    }

    /** Friendly names for the apps that commonly write into Health Connect. */
    val NAMES: Map<String, String> = linkedMapOf(
        "com.sec.android.app.shealth" to "Samsung Health",
        "com.fitbit.FitbitMobile" to "Fitbit",
        "com.google.android.apps.fitness" to "Google Fit / Pixel Watch",
        "com.garmin.android.apps.connectmobile" to "Garmin Connect",
        "com.ouraring.oura" to "Oura",
        "com.whoop.android" to "Whoop",
        "com.withings.wiscale2" to "Withings",
        "fi.polar.polarflow" to "Polar Flow",
        "com.mi.health" to "Mi Fitness",
        "com.xiaomi.wearable" to "Mi Fitness",
        "com.strava" to "Strava",
        "com.google.android.apps.healthdata" to "Health Connect",
        "android" to "This phone",
    )

    fun name(origin: String): String = NAMES[origin] ?: origin

    /**
     * One night's sleep from one app only: the chosen [preferred] app if it has any, otherwise the app with the most
     * time asleep that night.
     */
    fun <T> pickSleep(sessions: List<Session<T>>, preferred: String): List<Session<T>> {
        if (sessions.isEmpty()) return sessions
        val byOrigin = sessions.groupBy { it.origin }
        byOrigin[preferred]?.let { return it }
        return byOrigin.values.maxBy { list -> list.sumOf { it.minutes } }
    }

    /**
     * Workouts with duplicates from other apps dropped: the [preferred] app's first, then any other app's session
     * that doesn't overlap one already kept by more than half its length.
     */
    fun <T> dedupeWorkouts(sessions: List<Session<T>>, preferred: String): List<Session<T>> {
        val ordered = sessions.sortedWith(compareBy<Session<T>>({ it.origin != preferred }, { it.start }))
        val kept = mutableListOf<Session<T>>()
        for (s in ordered) {
            val dup = kept.any { k ->
                k.origin != s.origin && overlap(k, s) * 2 > minOf(k.minutes, s.minutes).coerceAtLeast(1)
            }
            if (!dup) kept += s
        }
        return kept.sortedBy { it.start }
    }

    private fun overlap(a: Session<*>, b: Session<*>): Long {
        val start = maxOf(a.start, b.start)
        val end = minOf(a.end, b.end)
        return if (end > start) Duration.between(start, end).toMinutes() else 0
    }
}

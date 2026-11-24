package com.kevinwcode.personalapi

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * Reads what your watch or phone's health app writes into Health Connect: Samsung Health, Fitbit, Pixel Watch,
 * Garmin, Oura and so on. With more than one app, sleep and workouts come from one source (see [HealthSources]).
 */
object Health {
    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        // Lets the hourly sync read while the app is closed (Android 14+).
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
    )

    fun available(context: Context) =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** Apps that wrote sleep, workouts or steps in the last 14 days, for the "Main health app" choice. */
    suspend fun recentSources(context: Context): Set<String> {
        val client = HealthConnectClient.getOrCreate(context)
        val range = TimeRangeFilter.between(Instant.now().minus(Duration.ofDays(14)), Instant.now())
        return (client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records.map { it.metadata.dataOrigin.packageName } +
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records.map { it.metadata.dataOrigin.packageName } +
            client.readRecords(ReadRecordsRequest(StepsRecord::class, range, pageSize = 200)).records.map { it.metadata.dataOrigin.packageName })
            .toSet()
    }

    suspend fun collect(context: Context, days: List<LocalDate>, preferred: String = ""): List<Observation> {
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault()
        val first = days.min()
        val last = days.max()
        val out = mutableListOf<Observation>()

        // Steps, heart rate: daily aggregates, which Health Connect de-duplicates across phone and watch.
        val groups = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    HeartRateRecord.BPM_AVG,
                    RestingHeartRateRecord.BPM_AVG,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(first.atStartOfDay(), last.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            )
        )
        val restingFromRecords = mutableSetOf<LocalDate>()
        for (g in groups) {
            val day = g.startTime.toLocalDate()
            g.result[StepsRecord.COUNT_TOTAL]?.let { out += Observation(day, "health.steps", it.toDouble(), "count") }
            g.result[HeartRateRecord.BPM_AVG]?.let { out += Observation(day, "health.heart_rate_avg", it.toDouble(), "bpm") }
            g.result[RestingHeartRateRecord.BPM_AVG]?.let {
                out += Observation(day, "health.resting_heart_rate", it.toDouble(), "bpm")
                restingFromRecords += day
            }
            g.result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.let {
                out += Observation(day, "workout.minutes", it.toMinutes().toDouble(), "minutes")
            }
        }

        val start = first.atStartOfDay(zone).toInstant()
        val end = last.plusDays(1).atStartOfDay(zone).toInstant()

        // Sleep counts toward the day you woke up.
        val sleep = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end))).records
        sleep.groupBy { it.endTime.atZone(zone).toLocalDate() }.forEach { (day, all) ->
            // One app's sleep only, so a watch and a phone don't add up to two nights.
            val sessions = HealthSources.pickSleep(
                all.map { HealthSources.Session(it.metadata.dataOrigin.packageName, it.startTime, it.endTime, it) }, preferred,
            ).map { it.record }
            out += sleepObservations(day, sessions)
            // Some apps (Samsung Health among them) don't share resting heart rate: use the calmest 5 minutes of the night.
            if (day !in restingFromRecords) {
                val samples = sessions.flatMap { s ->
                    client.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(s.startTime, s.endTime)))
                        .records.flatMap { r -> r.samples.map { it.time to it.beatsPerMinute } }
                }
                HeartMath.lowestRollingAverage(samples)?.let {
                    out += Observation(day, "health.resting_heart_rate", it, "bpm")
                }
            }
        }

        val spo2 = client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(start, end))).records
        spo2.groupBy { it.time.atZone(zone).toLocalDate() }.forEach { (day, rs) ->
            out += Observation(day, "health.spo2_avg", rs.map { it.percentage.value }.average(), "percent")
        }

        val hrv = client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, TimeRangeFilter.between(start, end))).records
        hrv.groupBy { it.time.atZone(zone).toLocalDate() }.forEach { (day, rs) ->
            out += Observation(day, "health.hrv_rmssd", rs.map { it.heartRateVariabilityMillis }.average(), "ms")
        }

        val sessions = HealthSources.dedupeWorkouts(
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))).records
                .map { HealthSources.Session(it.metadata.dataOrigin.packageName, it.startTime, it.endTime, it) },
            preferred,
        ).map { it.record }
        sessions.groupBy { it.startTime.atZone(zone).toLocalDate() }.forEach { (day, rs) ->
            out += Observation(day, "workout.sessions", rs.size.toDouble(), "count")
            // Sessions that count as a real workout for the vital bars: 30+ minutes and not a walk.
            val counted = rs.count {
                it.exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_WALKING &&
                    Duration.between(it.startTime, it.endTime).toMinutes() >= 30
            }
            out += Observation(day, "workout.counted_sessions", counted.toDouble(), "count")
        }

        return out.filter { it.date in days }
    }

    private fun sleepObservations(day: LocalDate, sessions: List<SleepSessionRecord>): List<Observation> {
        val byStage = mutableMapOf<String, Long>()
        var asleep = 0L
        for (s in sessions) {
            if (s.stages.isEmpty()) {
                asleep += minutes(s.startTime, s.endTime)
                continue
            }
            for (st in s.stages) {
                val m = minutes(st.startTime, st.endTime)
                val name = when (st.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> "deep"
                    SleepSessionRecord.STAGE_TYPE_REM -> "rem"
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> "light"
                    SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "awake"
                    else -> "other"
                }
                byStage.merge(name, m, Long::plus)
                if (name != "awake") asleep += m
            }
        }
        return listOf(Observation(day, "sleep.total_minutes", asleep.toDouble(), "minutes")) +
            byStage.filterKeys { it != "other" }.map { (k, v) -> Observation(day, "sleep.${k}_minutes", v.toDouble(), "minutes") }
    }

    private fun minutes(a: Instant, b: Instant) = Duration.between(a, b).toMinutes()
}

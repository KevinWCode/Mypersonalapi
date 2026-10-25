package com.kevinwcode.personalapi

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** Minutes each app was on screen per day, from Android's usage events. */
object ScreenTime {
    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun collect(context: Context, days: List<LocalDate>): List<Observation> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val zone = ZoneId.systemDefault()
        return days.flatMap { day ->
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = minOf(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), System.currentTimeMillis())
            foregroundMillis(usm.queryEvents(start, end), end)
                .filterValues { it >= 60_000 }
                .map { (pkg, ms) -> Observation(day, "screen.app.$pkg", ms / 60_000.0, "minutes") }
        }
    }

    internal fun foregroundMillis(events: UsageEvents, windowEnd: Long): Map<String, Long> {
        val openedAt = mutableMapOf<String, Long>()
        val total = mutableMapOf<String, Long>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> openedAt.putIfAbsent(e.packageName, e.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = openedAt.remove(e.packageName) ?: continue
                    total.merge(e.packageName, e.timeStamp - start, Long::plus)
                }
            }
        }
        // Apps still open when the window ends count up to the end.
        openedAt.forEach { (pkg, start) -> total.merge(pkg, windowEnd - start, Long::plus) }
        return total
    }
}

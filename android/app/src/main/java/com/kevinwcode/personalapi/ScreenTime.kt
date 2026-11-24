package com.kevinwcode.personalapi

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** Minutes each app was on screen per day, from Android's usage events. */
object ScreenTime {
    // Not real phone use: the screensaver, system UI, this app, and the home screens of the common phone makers
    // (Samsung, Google Pixel, Xiaomi/Redmi/POCO, OnePlus, Oppo/Realme, Motorola, Nothing, Huawei/Honor, Sony, Asus,
    // plus Android's own and two popular third-party ones). Whatever home screen the phone actually uses is skipped
    // too (see homeScreens), so a phone not on this list works as well.
    internal val IGNORED_PREFIXES = listOf(
        "com.android.dreams.",
        "com.android.systemui",
        "com.kevinwcode.personalapi",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.mi.android.globallauncher",
        "net.oneplus.launcher",
        "com.oppo.launcher",
        "com.android.launcher",
        "com.motorola.launcher3",
        "com.nothing.launcher",
        "com.huawei.android.launcher",
        "com.hihonor.android.launcher",
        "com.sonymobile.home",
        "com.asus.launcher",
        "com.teslacoilsw.launcher",
        "com.microsoft.launcher",
    )

    /** Every app that can be this phone's home screen, asked from Android, so it works on any make of phone. */
    fun homeScreens(context: Context): Set<String> = runCatching {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            // Settings has a stand-by home screen used while the phone starts up; its own time still counts.
            .filter { it != "com.android.settings" }.toSet()
    }.getOrDefault(emptySet())

    /** Whether minutes in this app count as phone use. */
    internal fun counts(pkg: String, homes: Set<String>): Boolean =
        pkg !in homes && IGNORED_PREFIXES.none { pkg.startsWith(it) }

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun collect(context: Context, days: List<LocalDate>): List<Observation> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val zone = ZoneId.systemDefault()
        val homes = homeScreens(context)
        return days.flatMap { day ->
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = minOf(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), System.currentTimeMillis())
            foregroundMillis(usm.queryEvents(start, end), end)
                .filterKeys { pkg -> counts(pkg, homes) }
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

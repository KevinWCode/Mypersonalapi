package com.kevinwcode.personalapi

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Runs every 15 minutes (the most often Android allows) in the background: reads screen time and health data for the
 * last 3 days, saves it on the phone and, if a PC server is set up, uploads it there too. Outside [SyncHours] it does nothing until the morning.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SyncHours.allows(LocalTime.now())) return Result.success()
        val settings = Settings(applicationContext)
        val result = try {
            settings.lastSync = "${now()}: ${syncAll(applicationContext)}"
            Result.success()
        } catch (e: Exception) {
            settings.lastSync = "${now()}: failed, ${e.message}"
            Result.retry()
        }
        // Show the new numbers on the home screen widget too.
        HomeWidget.refresh(applicationContext)
        return result
    }

    companion object {
        fun schedule(context: Context) {
            // No network needed: everything is saved on the phone first, and the PC upload just retries next time.
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            // UPDATE so phones that already had the old hourly schedule switch to the new one.
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /**
         * Reads every source, saves it in the phone's own database first, then copies it to the PC server if one is set
         * up. The phone keeps working with no server at all; an upload that fails is simply tried again next sync,
         * since each sync re-reads the last 3 days. Returns a short summary for the status line.
         */
        suspend fun syncAll(context: Context): String {
            val settings = Settings(context)
            val store = LocalStore.get(context)
            val today = LocalDate.now()
            val days = (2 downTo 0).map { today.minusDays(it.toLong()) }
            val parts = mutableListOf<String>()
            val batches = mutableListOf<Pair<String, List<Observation>>>()

            if (ScreenTime.hasAccess(context)) {
                val obs = ScreenTime.collect(context, days)
                store.save("phone", obs)
                batches += "phone" to obs
                parts += "screen ${obs.size}"
            } else parts += "screen: no usage access"

            if (Health.available(context)) {
                try {
                    val obs = Health.collect(context, days)
                    store.save("phone", obs)
                    batches += "phone" to obs
                    parts += "health ${obs.size}"
                } catch (e: SecurityException) {
                    parts += "health: permission missing"
                }
            } else parts += "health: Health Connect not available"

            val manual = ManualLog(context).today()
            store.save("manual", manual)
            batches += "manual" to manual
            parts += "manual ${manual.size}"
            parts += "saved on phone"

            parts += uploadToServer(settings, batches)
            return parts.joinToString(", ")
        }

        /** Copies to the PC server when one is set up. Never throws: the data is already safe on the phone. */
        fun uploadToServer(settings: Settings, batches: List<Pair<String, List<Observation>>>): String {
            if (!settings.serverEnabled) return "PC upload off"
            return try {
                val sent = batches.sumOf { (source, obs) -> Uploader.send(settings, source, obs) }
                "sent $sent to PC"
            } catch (e: Exception) {
                "PC upload failed (${e.message}), will retry"
            }
        }

        private fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d HH:mm"))
    }
}

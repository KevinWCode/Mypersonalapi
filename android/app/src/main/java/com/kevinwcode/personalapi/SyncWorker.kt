package com.kevinwcode.personalapi

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Runs every 15 minutes (the most often Android allows) in the background: reads screen time and health data, uploads the last 3 days. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val result = try {
            settings.lastSync = "${now()}: ${syncAll(applicationContext)}"
            Result.success()
        } catch (e: Exception) {
            settings.lastSync = "${now()}: failed, ${e.message}"
            Result.retry()
        }
        // The server has the new numbers now, so show them on the home screen widget too.
        HomeWidget.refresh(applicationContext)
        return result
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            // UPDATE so phones that already had the old hourly schedule switch to the new one.
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Returns a short summary for the status line. Each source fails on its own. */
        suspend fun syncAll(context: Context): String {
            val settings = Settings(context)
            val today = LocalDate.now()
            val days = (2 downTo 0).map { today.minusDays(it.toLong()) }
            val parts = mutableListOf<String>()

            parts += if (ScreenTime.hasAccess(context)) {
                "screen ${Uploader.send(settings, "phone", ScreenTime.collect(context, days))}"
            } else "screen: no usage access"

            parts += if (Health.available(context)) {
                try {
                    "health ${Uploader.send(settings, "phone", Health.collect(context, days))}"
                } catch (e: SecurityException) {
                    "health: permission missing"
                }
            } else "health: Health Connect not available"

            parts += "manual ${Uploader.send(settings, "manual", ManualLog(context).today())}"
            return parts.joinToString(", ")
        }

        private fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d HH:mm"))
    }
}

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
        const val PHONE_FOOD_SOURCE = "phone-food"
        private const val UPLOADED = "sent"

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

            // The last 3 days, so a late entry or a perk claimed just after midnight still gets saved and sent.
            val log = ManualLog(context)
            val manual = days.flatMap { log.day(it) }
            store.save("manual", manual)
            batches += "manual" to manual
            parts += "manual ${manual.size}"
            // Meals pasted in the app go to the PC as their own source, so they add to the PC's meals, not replace them.
            // Always sent, zeros too, so a meal removed in the app is removed on the PC as well.
            val food = days.flatMap { store.foodTotals(it) }
            batches += PHONE_FOOD_SOURCE to food
            parts += collectOnline(settings, store, today)
            parts += "saved on phone"

            val upload = uploadToServer(settings, batches)
            parts += upload
            // Until the server is switched off, also take the PC's totals for these days, which include what was
            // logged on the dashboard, so the phone's game scores the same as the PC's. Only after a successful
            // upload: otherwise the copy would be missing what was just logged on the phone.
            if (upload.startsWith(UPLOADED)) {
                parts += runCatching { "copied ${PcHistory.copy(settings, store, days.first(), days.last())} days from PC" }
                    .getOrElse { "PC copy failed (${it.message})" }
            }
            return parts.joinToString(", ")
        }

        /**
         * GitHub commits and Alpaca P/L, trades and positions, read straight from their websites when keys are saved.
         * Kept on the phone only (the PC reads its own copy while it runs). A failure is reported, never thrown, so
         * the rest of the sync still counts.
         */
        fun collectOnline(settings: Settings, store: LocalStore, today: LocalDate, http: Http = Http.DEFAULT): List<String> {
            val parts = mutableListOf<String>()
            if (settings.githubToken.isNotEmpty()) parts += runCatching {
                "github ${store.save(GitHub.SOURCE, GitHub.fetch(settings.githubToken, today, http = http))}"
            }.getOrElse { "github failed (${it.message})" }
            val accounts = settings.alpacaAccounts
            if (accounts.isNotEmpty()) parts += runCatching {
                val r = Alpaca.fetch(accounts, settings.alpacaUrl, http)
                r.rows.forEach { (source, obs) -> store.save(source, obs) }
                store.saveTrading(r.fills, r.positions)
                "alpaca ${accounts.size} account(s)"
            }.getOrElse { "alpaca failed (${it.message})" }
            return parts
        }

        /** Copies to the PC server when one is set up. Never throws: the data is already safe on the phone. */
        fun uploadToServer(settings: Settings, batches: List<Pair<String, List<Observation>>>): String {
            if (!settings.serverEnabled) return "PC upload off"
            return try {
                val sent = batches.sumOf { (source, obs) -> Uploader.send(settings, source, obs) }
                "$UPLOADED $sent to PC"
            } catch (e: Exception) {
                "PC upload failed (${e.message}), will retry"
            }
        }

        private fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d HH:mm"))
    }
}

package com.kevinwcode.personalapi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Home screen widget: social media for the last 7 days as bars (green, amber, red against the daily limit), food, sleep, steps, heart rate, workouts, study and reading,
 * trading P/L and the last trade, from the server's GET /summary.
 * It refreshes after each phone sync (every 15 minutes) and when you tap the refresh arrow. Tapping anywhere else
 * opens the dashboard. When the server can't be reached it keeps showing the last numbers it got.
 */
class HomeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        show(context)
        // The sync's periodic work also keeps WorkManager from switching its components on and off, which would
        // call onUpdate again.
        SyncWorker.schedule(context)
        refreshSoon(context, ExistingWorkPolicy.KEEP)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            show(context, updating = true)
            refreshSoon(context, ExistingWorkPolicy.REPLACE)
        }
    }

    class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            refresh(applicationContext)
            return Result.success()
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.kevinwcode.personalapi.REFRESH_WIDGET"
        // In WidgetLines.tiles order: food, protein, sleep, steps, resting HR, workouts, study, reading, P/L.
        private val TILE_VALUES = intArrayOf(
            R.id.tile0_value, R.id.tile1_value, R.id.tile2_value, R.id.tile3_value, R.id.tile4_value,
            R.id.tile5_value, R.id.tile6_value, R.id.tile7_value, R.id.tile8_value,
        )
        private val TILE_SUBS = intArrayOf(
            R.id.tile0_sub, R.id.tile1_sub, R.id.tile2_sub, R.id.tile3_sub, R.id.tile4_sub,
            R.id.tile5_sub, R.id.tile6_sub, R.id.tile7_sub, R.id.tile8_sub,
        )

        private val BAR_LABELS = intArrayOf(
            R.id.bar0_label, R.id.bar1_label, R.id.bar2_label, R.id.bar3_label, R.id.bar4_label, R.id.bar5_label, R.id.bar6_label,
        )
        // The chart is drawn at this size and stretched to the widget's width; bars are plain shapes, so that's fine.
        private const val CHART_W = 700
        private const val CHART_H = 140
        // Widgets shorter than this get the compact layout.
        private const val COMPACT_BELOW_DP = 330f

        private fun refreshSoon(context: Context, policy: ExistingWorkPolicy) {
            WorkManager.getInstance(context).enqueueUniqueWork("widget-now", policy, OneTimeWorkRequestBuilder<RefreshWorker>().build())
        }

        /** Fetches the latest numbers and redraws the widget. Does nothing if no widget is on the home screen. */
        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.getAppWidgetIds(ComponentName(context, HomeWidget::class.java)).isEmpty()) return
            val text = withContext(Dispatchers.IO) { runCatching { fetch(Settings(context).serverUrl) }.getOrNull() }
            val edit = prefs(context).edit().putBoolean("offline", text == null)
            if (text != null) edit.putString("summary", text).putLong("fetched_at", System.currentTimeMillis())
            edit.apply()
            show(context)
        }

        private fun fetch(serverUrl: String): String {
            // The phone's own date, so "today" matches the phone's clock.
            val conn = URL("$serverUrl/summary?date=${LocalDate.now()}").openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                check(conn.responseCode == 200) { "Server said ${conn.responseCode}" }
                return conn.inputStream.bufferedReader().readText().also { parse(it) }
            } finally {
                conn.disconnect()
            }
        }

        private fun parse(text: String): Summary {
            val o = JSONObject(text)
            val social = o.getJSONObject("social")
            fun JSONObject.num(key: String) = if (isNull(key)) null else getDouble(key)
            return Summary(
                date = LocalDate.parse(o.getString("date")),
                reclaimedMinutes = social.num("reclaimed_minutes"),
                weekMinutes = social.num("week_minutes"),
                weekPerDay = social.num("week_per_day"),
                lastWeekPerDay = social.num("last_week_per_day"),
                kcal = o.num("kcal"),
                proteinG = o.num("protein_g"),
                sleepMinutes = o.num("sleep_minutes"),
                steps = o.num("steps"),
                stepsPerDay = o.num("steps_per_day"),
                restingHr = o.num("resting_hr"),
                workoutsWeek = o.num("workouts_week"),
                studyHoursWeek = o.num("study_hours_week"),
                readingHoursWeek = o.num("reading_hours_week"),
                pl7d = o.num("pl_7d"),
                lastTrade = o.optJSONObject("last_trade")?.let {
                    Trade(it.getString("symbol"), it.getString("side"), it.getDouble("qty"), it.getDouble("price"),
                        runCatching { OffsetDateTime.parse(it.getString("filled_at")).toInstant() }.getOrNull())
                },
                // Older servers don't send these, so each one is optional.
                socialDays = social.optJSONArray("days")?.let { a -> List(a.length()) { a.getJSONObject(it).num("minutes") } } ?: emptyList(),
                limitMinutes = if (social.has("limit_minutes")) social.getDouble("limit_minutes") else 60.0,
                youtubeToday = o.optJSONObject("youtube")?.num("today_minutes"),
                youtubeWeekPerDay = o.optJSONObject("youtube")?.num("week_per_day"),
            )
        }

        private fun color(context: Context, level: Level) = ContextCompat.getColor(context, when (level) {
            Level.GOOD -> R.color.widget_good
            Level.WARN -> R.color.widget_warn
            Level.OVER -> R.color.widget_bad
            Level.NONE -> R.color.widget_empty
        })

        /** Seven rounded bars in equal slots, matching the seven day labels under them, and a line at the limit. */
        private fun chart(context: Context, bars: List<Bar>, limitAt: Double): Bitmap {
            val bitmap = Bitmap.createBitmap(CHART_W, CHART_H, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val slot = CHART_W / bars.size.toFloat()
            val minH = 6f
            bars.forEachIndexed { i, bar ->
                paint.color = color(context, bar.level)
                val h = maxOf(minH, (bar.height * CHART_H).toFloat())
                val left = i * slot + slot * 0.2f
                canvas.drawRoundRect(RectF(left, CHART_H - h, left + slot * 0.6f, CHART_H.toFloat()), 8f, 8f, paint)
            }
            paint.color = ContextCompat.getColor(context, R.color.widget_sub)
            paint.strokeWidth = 3f
            val y = (CHART_H - limitAt * CHART_H).toFloat()
            var x = 0f
            while (x < CHART_W) {
                canvas.drawLine(x, y, minOf(x + 14f, CHART_W.toFloat()), y, paint)
                x += 24f
            }
            return bitmap
        }

        private fun prefs(context: Context) = context.getSharedPreferences("widget", Context.MODE_PRIVATE)

        private fun show(context: Context, updating: Boolean = false) {
            val cache = prefs(context)
            val summary = cache.getString("summary", null)?.let { runCatching { parse(it) }.getOrNull() }
            val fetchedAt = cache.getLong("fetched_at", 0).takeIf { it > 0 }
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
            val t = WidgetText.lines(summary, LocalDate.now(), fetchedAt, cache.getBoolean("offline", false))

            val full = fill(context, RemoteViews(context.packageName, R.layout.home_widget), t, updating, compact = false)
            // Android 12+ picks the layout that fits the widget's size; below a 4th row it gets the compact one.
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val compact = fill(context, RemoteViews(context.packageName, R.layout.home_widget_compact), t, updating, compact = true)
                RemoteViews(mapOf(SizeF(180f, 180f) to compact, SizeF(180f, COMPACT_BELOW_DP) to full))
            } else full
            AppWidgetManager.getInstance(context).updateAppWidget(ComponentName(context, HomeWidget::class.java), views)
        }

        private fun fill(context: Context, views: RemoteViews, t: WidgetLines, updating: Boolean, compact: Boolean): RemoteViews {
            views.setTextViewText(R.id.label, t.label)
            views.setImageViewBitmap(R.id.chart, chart(context, t.bars, t.limitAt))
            // The compact layout has room for the day names only.
            t.bars.forEachIndexed { i, bar -> views.setTextViewText(BAR_LABELS[i], if (compact) bar.label.substringAfter("\n") else bar.label) }
            views.setTextViewText(R.id.note, t.note)
            t.tiles.forEachIndexed { i, tile ->
                views.setTextViewText(TILE_VALUES[i], tile.value)
                views.setTextViewText(TILE_SUBS[i], tile.sub)
            }
            val pl = when (t.plUp) { true -> R.color.widget_good; false -> R.color.widget_warn; null -> R.color.widget_text }
            views.setTextColor(TILE_VALUES[8], ContextCompat.getColor(context, pl))
            views.setTextViewText(R.id.trade, t.trade)
            views.setTextViewText(R.id.updated, if (updating) "Updating…" else t.updated)
            views.setTextColor(R.id.updated, ContextCompat.getColor(context, if (t.offline && !updating) R.color.widget_warn else R.color.widget_sub))

            val open = Intent(context, OpenDashboardActivity::class.java)
            views.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE))
            val refresh = Intent(context, HomeWidget::class.java).setAction(ACTION_REFRESH)
            views.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(context, 0, refresh, PendingIntent.FLAG_IMMUTABLE))
            return views
        }
    }
}

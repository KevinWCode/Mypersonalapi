package com.kevinwcode.personalapi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The home screen widgets you can pick from, besides the Today one ([HomeWidget]):
 *
 *  - Vitals: the day badge (Prime Condition, Steady, Rest Day) and HP, Stamina, Mana and Focus with each bar's state;
 *  - Character: level, class, XP to the next level and today's quests (the list shows when the widget is tall enough);
 *  - Food log: the two-tap meal tiles. Tap a meal, then how it was; tap the same choice again to undo it;
 *  - Quick log: +250 ml water, +1 coffee and a journal entry in one tap.
 *
 * All of them follow the theme chosen in the app ([WidgetTheme]) and are worked out from the phone's own data, the same
 * answers the dashboard gets ([PhoneApi], [WidgetCards]). They redraw with the Today widget: after each sync, after
 * logging in the app, and right after a tap on a widget. Tapping a widget anywhere else opens the app.
 */
abstract class ThemedWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // As for the Today widget: the sync's periodic work keeps WorkManager from switching its parts on and off.
        SyncWorker.schedule(context)
        Widgets.redrawSoon(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        Widgets.redrawSoon(context)
    }
}

class VitalsWidget : ThemedWidget()
class CharacterWidget : ThemedWidget()
class FoodWidget : ThemedWidget()
class QuickLogWidget : ThemedWidget()

/** Taps on the widgets' buttons. Logging goes to the phone's database like the app's own buttons. */
class WidgetActions : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Widgets.handle(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }
}

object Widgets {
    private const val ACTION_OPEN_MEAL = "com.kevinwcode.personalapi.widget.OPEN_MEAL"
    private const val ACTION_CLOSE_MEAL = "com.kevinwcode.personalapi.widget.CLOSE_MEAL"
    private const val ACTION_PICK = "com.kevinwcode.personalapi.widget.PICK"
    private const val ACTION_QUICK = "com.kevinwcode.personalapi.widget.QUICK"
    private const val EXTRA_MEAL = "meal"
    private const val EXTRA_TIER = "tier"
    private const val EXTRA_WHAT = "what"
    /** An opened meal on the Food log widget folds back after this long, so it never stays stuck on one meal. */
    private const val OPEN_FOR_MS = 3 * 60 * 1000L

    private const val BAR_W = 600
    private const val BAR_H = 30

    private val VITAL_ROWS = listOf(
        intArrayOf(R.id.v0_row, R.id.v0_name, R.id.v0_bar, R.id.v0_value, R.id.v0_tag),
        intArrayOf(R.id.v1_row, R.id.v1_name, R.id.v1_bar, R.id.v1_value, R.id.v1_tag),
        intArrayOf(R.id.v2_row, R.id.v2_name, R.id.v2_bar, R.id.v2_value, R.id.v2_tag),
        intArrayOf(R.id.v3_row, R.id.v3_name, R.id.v3_bar, R.id.v3_value, R.id.v3_tag),
    )
    private val QUEST_ROWS = listOf(
        intArrayOf(R.id.q0, R.id.q0_label, R.id.q0_pts), intArrayOf(R.id.q1, R.id.q1_label, R.id.q1_pts),
        intArrayOf(R.id.q2, R.id.q2_label, R.id.q2_pts), intArrayOf(R.id.q3, R.id.q3_label, R.id.q3_pts),
        intArrayOf(R.id.q4, R.id.q4_label, R.id.q4_pts),
    )
    private val FOOD_TILES = listOf(
        intArrayOf(R.id.f0, R.id.f0_dot, R.id.f0_title, R.id.f0_sub), intArrayOf(R.id.f1, R.id.f1_dot, R.id.f1_title, R.id.f1_sub),
        intArrayOf(R.id.f2, R.id.f2_dot, R.id.f2_title, R.id.f2_sub), intArrayOf(R.id.f3, R.id.f3_dot, R.id.f3_title, R.id.f3_sub),
    )
    private val QUICK_TILES = listOf(
        intArrayOf(R.id.k0, R.id.k0_title, R.id.k0_sub), intArrayOf(R.id.k1, R.id.k1_title, R.id.k1_sub),
        intArrayOf(R.id.k2, R.id.k2_title, R.id.k2_sub),
    )

    // ---- theme ----

    fun prefs(context: Context) = context.getSharedPreferences("widget", Context.MODE_PRIVATE)

    fun theme(context: Context): WidgetTheme = WidgetTheme.of(prefs(context).getString("theme", null))

    /** The page's theme changed (or the page just opened): remember it and redraw the widgets if it's new. */
    fun setTheme(context: Context, id: String) {
        val t = WidgetTheme.of(id)
        if (prefs(context).getString("theme", null) == t.id) return
        prefs(context).edit().putString("theme", t.id).apply()
        redrawSoon(context)
    }

    fun background(t: WidgetTheme): Int = when (t.id) {
        "dark" -> R.drawable.widget_bg_dark
        "glass" -> R.drawable.widget_bg_glass
        "fantasy" -> R.drawable.widget_bg_fantasy
        "cyber" -> R.drawable.widget_bg_cyber
        "system" -> R.drawable.widget_bg_system
        "gothic" -> R.drawable.widget_bg_gothic
        "board" -> R.drawable.widget_bg_board
        else -> R.drawable.widget_bg_light
    }

    private fun tile(t: WidgetTheme, on: Boolean = false): Int = if (on) when (t.id) {
        "dark" -> R.drawable.widget_tile_on_dark
        "glass" -> R.drawable.widget_tile_on_glass
        "fantasy" -> R.drawable.widget_tile_on_fantasy
        "cyber" -> R.drawable.widget_tile_on_cyber
        "system" -> R.drawable.widget_tile_on_system
        "gothic" -> R.drawable.widget_tile_on_gothic
        "board" -> R.drawable.widget_tile_on_board
        else -> R.drawable.widget_tile_on_light
    } else when (t.id) {
        "dark" -> R.drawable.widget_tile_dark
        "glass" -> R.drawable.widget_tile_glass
        "fantasy" -> R.drawable.widget_tile_fantasy
        "cyber" -> R.drawable.widget_tile_cyber
        "system" -> R.drawable.widget_tile_system
        "gothic" -> R.drawable.widget_tile_gothic
        "board" -> R.drawable.widget_tile_board
        else -> R.drawable.widget_tile_light
    }

    /** A rounded bar: the track, then [fraction] of it filled. A Prime bar gets a light streak along the top. */
    fun bar(t: WidgetTheme, fraction: Double, fill: Int, glow: Boolean = false, height: Int = BAR_H): Bitmap {
        val bitmap = Bitmap.createBitmap(BAR_W, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = minOf(t.barRadius * height / BAR_H * 3, height / 2f)
        paint.color = t.track
        canvas.drawRoundRect(RectF(0f, 0f, BAR_W.toFloat(), height.toFloat()), r, r, paint)
        if (fraction > 0) {
            val w = maxOf(height.toFloat(), (fraction * BAR_W).toFloat())
            paint.color = fill
            canvas.drawRoundRect(RectF(0f, 0f, w, height.toFloat()), r, r, paint)
            if (glow) {
                paint.color = 0x55FFFFFF
                canvas.drawRoundRect(RectF(height * 0.3f, height * 0.18f, w - height * 0.3f, height * 0.42f), r, r, paint)
            }
        }
        return bitmap
    }

    // ---- drawing ----

    private fun ids(context: Context, cls: Class<*>) =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))

    /** Redraws all the widgets in the background (one worker draws them all, with the Today widget). */
    fun redrawSoon(context: Context) = HomeWidget.refreshFromPhone(context)

    /** Works out and draws every widget of these kinds that's on the home screen. Call off the main thread. */
    fun drawAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val vitals = ids(context, VitalsWidget::class.java)
        val character = ids(context, CharacterWidget::class.java)
        val food = ids(context, FoodWidget::class.java)
        val quick = ids(context, QuickLogWidget::class.java)
        if (vitals.isEmpty() && character.isEmpty() && food.isEmpty() && quick.isEmpty()) return
        val t = theme(context)
        val data = DashboardActivity.StoreData(context)
        if (vitals.isNotEmpty() || character.isNotEmpty()) {
            val game = runCatching { PhoneApi(data).handle("GET", "/game-b", null) }.getOrNull()?.takeIf { it.status == 200 }?.body
            if (vitals.isNotEmpty()) {
                val card = game?.let { runCatching { WidgetCards.vitals(it, LocalDateTime.now()) }.getOrNull() }
                manager.updateAppWidget(vitals, vitalsViews(context, t, card))
            }
            if (character.isNotEmpty()) {
                val card = game?.let { runCatching { WidgetCards.character(it) }.getOrNull() }
                manager.updateAppWidget(character, characterViews(context, t, card))
            }
        }
        if (food.isNotEmpty()) drawFood(context, t, data, food)
        if (quick.isNotEmpty()) {
            val day = data.days()[LocalDate.now()] ?: emptyMap()
            manager.updateAppWidget(quick, quickViews(context, t, WidgetCards.quick(day, data.profile())))
        }
    }

    private fun drawFood(context: Context, t: WidgetTheme, data: DashboardActivity.StoreData, ids: IntArray) {
        val card = WidgetCards.food(data.meals(LocalDate.now()), data.profile(), openMeal(context))
        AppWidgetManager.getInstance(context).updateAppWidget(ids, foodViews(context, t, card))
    }

    private fun openMeal(context: Context): String? {
        val p = prefs(context)
        val at = p.getLong("food_open_at", 0)
        return if (System.currentTimeMillis() - at < OPEN_FOR_MS) p.getString("food_open", null) else null
    }

    /** The theme's background, and a tap anywhere opens the app. */
    private fun base(context: Context, layout: Int, t: WidgetTheme): RemoteViews {
        val views = RemoteViews(context.packageName, layout)
        views.setInt(R.id.root, "setBackgroundResource", background(t))
        val open = Intent(context, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE))
        return views
    }

    private fun action(context: Context, action: String, vararg extras: Pair<String, String>): PendingIntent {
        val intent = Intent(context, WidgetActions::class.java).setAction(action)
            // A different address per button, so each button keeps its own extras.
            .setData(Uri.parse("personalapi-widget://" + action.substringAfterLast('.') + "/" + extras.joinToString("/") { it.second }))
        extras.forEach { (k, v) -> intent.putExtra(k, v) }
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun vitalsViews(context: Context, t: WidgetTheme, v: WidgetCards.Vitals?): RemoteViews {
        val views = base(context, R.layout.widget_vitals, t)
        views.setTextViewText(R.id.label, if (v == null) "Vitals" else "Vitals · ${v.level}")
        views.setTextColor(R.id.label, t.sub)
        views.setTextViewText(R.id.condition, v?.condition ?: "Open the app once")
        views.setTextColor(R.id.condition, when (v?.conditionState) {
            "prime" -> t.accent
            "rest" -> t.warn
            else -> t.ink
        })
        VITAL_ROWS.forEachIndexed { i, (row, name, barId, value, tag) ->
            val r = v?.rows?.getOrNull(i)
            views.setViewVisibility(row, if (r == null && v != null) View.GONE else View.VISIBLE)
            views.setTextColor(name, t.ink)
            views.setTextColor(value, t.ink)
            if (r == null) {
                views.setImageViewBitmap(barId, bar(t, 0.0, t.accent))
                views.setTextViewText(value, "")
                views.setTextViewText(tag, "")
                return@forEachIndexed
            }
            val glow = r.state == "prime" || r.state == "over"
            views.setTextViewText(name, r.name)
            views.setImageViewBitmap(barId, bar(t, r.fraction, WidgetTheme.VITAL_COLORS[r.key] ?: t.accent, glow))
            views.setTextViewText(value, r.value.toString())
            views.setTextViewText(tag, r.tag)
            views.setTextColor(tag, when (r.state) {
                "prime", "over" -> t.accent
                "low" -> t.bad
                else -> t.sub
            })
        }
        return views
    }

    private fun characterViews(context: Context, t: WidgetTheme, c: WidgetCards.Character?): RemoteViews {
        fun fill(layout: Int, rows: Int): RemoteViews {
            val views = base(context, layout, t)
            views.setTextColor(R.id.label, t.sub)
            views.setTextColor(R.id.today, t.ink)
            views.setTextColor(R.id.level_word, t.sub)
            views.setTextColor(R.id.level, t.accent)
            views.setTextColor(R.id.heading, t.ink)
            views.setTextColor(R.id.xp_line, t.sub)
            views.setTextColor(R.id.today_line, t.ink)
            views.setTextViewText(R.id.label, "Character")
            if (c == null) {
                views.setTextViewText(R.id.today, "")
                views.setTextViewText(R.id.level, "–")
                views.setTextViewText(R.id.heading, "Open the app once")
                views.setImageViewBitmap(R.id.xp_bar, bar(t, 0.0, t.accent))
                views.setTextViewText(R.id.xp_line, "")
                views.setTextViewText(R.id.today_line, "")
            } else {
                views.setTextViewText(R.id.today, c.questsLine)
                views.setTextViewText(R.id.level, c.level.toString())
                views.setTextViewText(R.id.heading, c.heading)
                views.setImageViewBitmap(R.id.xp_bar, bar(t, c.xpFraction, t.accent))
                views.setTextViewText(R.id.xp_line, c.xpLine)
                views.setTextViewText(R.id.today_line, c.xpToday)
            }
            QUEST_ROWS.take(rows).forEachIndexed { i, (row, label, pts) ->
                val q = c?.quests?.getOrNull(i)
                views.setViewVisibility(row, if (q == null) View.INVISIBLE else View.VISIBLE)
                if (q == null) return@forEachIndexed
                views.setTextViewText(label, (if (q.done) "✓ " else "○ ") + q.label)
                views.setTextColor(label, if (q.done) t.sub else t.ink)
                views.setTextViewText(pts, q.points)
                views.setTextColor(pts, if (q.done) t.good else t.sub)
            }
            return views
        }
        val short = fill(R.layout.widget_character, 0)
        // Android 12+ picks the layout that fits; the tall one has room for the next five quests.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RemoteViews(mapOf(SizeF(180f, 90f) to short, SizeF(180f, 200f) to fill(R.layout.widget_character_tall, QUEST_ROWS.size)))
        } else short
    }

    private fun foodViews(context: Context, t: WidgetTheme, f: WidgetCards.FoodLog): RemoteViews {
        val views = base(context, R.layout.widget_food, t)
        views.setTextColor(R.id.label, t.sub)
        views.setTextColor(R.id.total, t.ink)
        views.setTextColor(R.id.back, t.accent)
        views.setTextColor(R.id.detail, t.sub)
        val open = f.open
        if (open == null) {
            views.setTextViewText(R.id.label, "Food today")
            views.setTextViewText(R.id.total, f.total)
            views.setViewVisibility(R.id.total, View.VISIBLE)
            views.setViewVisibility(R.id.back, View.GONE)
            views.setTextViewText(R.id.detail, f.detail)
        } else {
            views.setTextViewText(R.id.label, "${open.replaceFirstChar { it.uppercase() }} · how was it?")
            views.setViewVisibility(R.id.total, View.GONE)
            views.setViewVisibility(R.id.back, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.back, action(context, ACTION_CLOSE_MEAL))
            views.setTextViewText(R.id.detail, if (open == "snack") "Each tap adds a snack (rough estimate)" else "Tap one to log a rough estimate")
        }
        FOOD_TILES.forEachIndexed { i, (tileId, dot, title, sub) ->
            views.setTextColor(title, t.ink)
            views.setTextColor(sub, t.sub)
            if (open == null) {
                val m = f.tiles[i]
                views.setInt(tileId, "setBackgroundResource", tile(t))
                views.setTextViewText(title, m.name)
                views.setTextViewText(sub, m.status)
                val q = m.quality?.let { WidgetTheme.QUALITY_COLORS[it] }
                views.setViewVisibility(dot, if (q == null) View.GONE else View.VISIBLE)
                if (q != null) views.setInt(dot, "setColorFilter", q)
                views.setOnClickPendingIntent(tileId, action(context, ACTION_OPEN_MEAL, EXTRA_MEAL to m.meal))
            } else {
                val c = f.choices.getOrNull(i) ?: return@forEachIndexed
                views.setInt(tileId, "setBackgroundResource", tile(t, on = c.on))
                views.setTextViewText(title, c.tier)
                views.setTextViewText(sub, "${c.kcal} · ${c.hint}")
                views.setViewVisibility(dot, View.VISIBLE)
                views.setInt(dot, "setColorFilter", WidgetTheme.QUALITY_COLORS[c.tier.lowercase()] ?: t.accent)
                views.setOnClickPendingIntent(tileId, action(context, ACTION_PICK, EXTRA_MEAL to open, EXTRA_TIER to c.tier))
            }
        }
        return views
    }

    private fun quickViews(context: Context, t: WidgetTheme, buttons: List<WidgetCards.QuickButton>): RemoteViews {
        val views = base(context, R.layout.widget_quick, t)
        QUICK_TILES.forEachIndexed { i, (tileId, title, sub) ->
            val b = buttons[i]
            views.setInt(tileId, "setBackgroundResource", tile(t))
            views.setTextViewText(title, b.label)
            views.setTextViewText(sub, b.sub)
            views.setTextColor(title, t.ink)
            views.setTextColor(sub, t.sub)
            views.setOnClickPendingIntent(tileId, action(context, ACTION_QUICK, EXTRA_WHAT to b.action))
        }
        val water = buttons.first().fraction ?: 0.0
        views.setImageViewBitmap(R.id.k0_bar, bar(t, water, t.accent, height = 12))
        return views
    }

    // ---- taps ----

    fun handle(context: Context, intent: Intent) {
        val p = prefs(context)
        when (intent.action) {
            ACTION_OPEN_MEAL -> {
                p.edit().putString("food_open", intent.getStringExtra(EXTRA_MEAL)).putLong("food_open_at", System.currentTimeMillis()).apply()
                drawFood(context, theme(context), DashboardActivity.StoreData(context), ids(context, FoodWidget::class.java))
            }
            ACTION_CLOSE_MEAL -> {
                p.edit().remove("food_open").remove("food_open_at").apply()
                drawFood(context, theme(context), DashboardActivity.StoreData(context), ids(context, FoodWidget::class.java))
            }
            ACTION_PICK -> {
                val meal = intent.getStringExtra(EXTRA_MEAL) ?: return
                val tier = intent.getStringExtra(EXTRA_TIER) ?: return
                val store = LocalStore.get(context)
                val today = LocalDate.now()
                val pick = WidgetCards.pick(meal, tier, store.meals(today)) ?: return
                when (pick) {
                    is WidgetCards.Pick.Add -> store.addMeals(listOf(pick.preset.toMeal(today)))
                    is WidgetCards.Pick.Change -> {
                        store.removeMeal(pick.removeId)
                        store.addMeals(listOf(pick.preset.toMeal(today)))
                    }
                    is WidgetCards.Pick.Undo -> store.removeMeal(pick.removeId)
                }
                p.edit().remove("food_open").remove("food_open_at").apply()
                afterLog(context, WidgetCards.pickNote(meal, tier, pick))
            }
            ACTION_QUICK -> {
                val what = intent.getStringExtra(EXTRA_WHAT) ?: return
                val entry = WidgetCards.quickEntry(what) ?: return
                DashboardActivity.StoreData(context).log(LocalDate.now(), listOf(entry))
                afterLog(context, WidgetCards.quickNote(what))
            }
        }
    }

    /** After logging from a widget: redraw these widgets now, then the Today widget, and send it to the PC if that's on. */
    private fun afterLog(context: Context, note: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(context, note, Toast.LENGTH_SHORT).show() }
        drawAll(context)
        HomeWidget.refreshFromPhone(context)
        DashboardActivity.syncSoon(context)
    }
}

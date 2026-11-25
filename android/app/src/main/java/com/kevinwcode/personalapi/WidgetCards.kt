package com.kevinwcode.personalapi

import org.json.JSONObject
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * What the Vitals, Character, Food log and Quick log widgets show, worked out from the same answers the dashboard
 * page gets ([PhoneApi]'s GET /game-b, the day's meals and metrics). Kept apart from Android so it can be unit tested;
 * [Widgets] only puts these on the home screen.
 */
object WidgetCards {
    // ---- Vitals ----

    /** One bar. [fraction] is 0 to 1 of the bar's top (110 for an overcharged bar); [state] is prime, over, steady or low. */
    data class VitalRow(val key: String, val name: String, val value: Int, val fraction: Double, val state: String, val tag: String)

    /** The day badge (Prime Condition, Steady, Rest Day) and the four bars. */
    data class Vitals(val condition: String, val conditionState: String, val rows: List<VitalRow>, val level: String)

    /**
     * Mirrors the page's vitalState: a bar is Prime while it woke up strong and keeps up with a good day's line,
     * low (its debuff name) when it falls under half of that line, otherwise Steady.
     */
    fun vitalState(v: JSONObject, pace: JSONObject?, value: Double, hourOfDay: Double, isToday: Boolean): String {
        if (pace == null) return if (value <= 25) "low" else "steady"
        val tick = v.getDouble("tick_per_day")
        val line = if (tick >= 0) 100.0 else {
            val share = if (!isToday) 1.0 else
                ((hourOfDay - v.optDouble("tick_from_hour", 0.0)) / v.optDouble("tick_hours", 24.0)).coerceIn(0.0, 1.0)
            100 - (100 - pace.getDouble("end_level")) * share
        }
        return when {
            value > 100 -> "over"
            v.getDouble("start_of_day") >= pace.getDouble("prime_wake") && value >= pace.getDouble("prime") * line &&
                value >= pace.getDouble("prime_floor") -> "prime"
            value < pace.getDouble("low") * line -> "low"
            else -> "steady"
        }
    }

    fun vitals(game: JSONObject, now: LocalDateTime): Vitals {
        val pace = game.optJSONObject("pace")
        val isToday = game.getString("date") == now.toLocalDate().toString()
        val hour = now.hour + now.minute / 60.0
        val list = game.optJSONArray("vitals")
        val rows = (0 until (list?.length() ?: 0)).map { i ->
            val v = list!!.getJSONObject(i)
            val value = v.getDouble("value")
            val top = maxOf(100.0, v.optDouble("top", 100.0))
            val state = vitalState(v, pace, value, hour, isToday)
            val tag = when (state) {
                "over" -> "★ Overcharge"
                "prime" -> "★ Prime"
                "low" -> v.optJSONObject("debuff")?.optString("name")?.takeIf { it.isNotEmpty() } ?: "Low"
                else -> "Steady"
            }
            VitalRow(v.getString("key"), v.getString("name"), value.roundToInt(), (value / top).coerceIn(0.0, 1.0), state, tag)
        }
        val c = game.optJSONObject("condition")
        val conditionState = c?.optString("state") ?: "unknown"
        val condition = when (conditionState) {
            "prime" -> "★ Prime Condition"
            else -> c?.optString("name")?.takeIf { it.isNotEmpty() } ?: "Steady"
        }
        return Vitals(condition, conditionState, rows, "Level ${game.optInt("level", 1)}")
    }

    // ---- Character and daily quests ----

    data class Quest(val label: String, val xp: Int, val max: Int) {
        val done get() = xp >= max
        val points get() = "${if (xp > 0) "+" else ""}$xp / $max"
    }

    data class Character(
        val level: Int, val heading: String, val xpFraction: Double, val xpLine: String, val questsLine: String, val xpToday: String,
        /** Unfinished quests first (most XP left first), then the finished ones. */
        val quests: List<Quest>,
    )

    fun character(game: JSONObject): Character {
        val level = game.optInt("level", 1)
        val cost = if (game.isNull("level_cost")) 0 else game.optInt("level_cost", 0)
        val cls = game.optJSONObject("class")?.optString("name").orEmpty()
        val title = if (game.isNull("title")) "" else game.optString("title", "")
        val today = game.optJSONObject("today")
        val metrics = today?.optJSONArray("metrics")
        val quests = (0 until (metrics?.length() ?: 0)).map { i ->
            val m = metrics!!.getJSONObject(i)
            Quest(m.getString("label"), m.getInt("xp"), m.getInt("max"))
        }
        val done = quests.count { it.done }
        val ordered = quests.filter { !it.done }.sortedByDescending { it.max - it.xp } + quests.filter { it.done }
        return Character(
            level = level,
            heading = listOf(cls, title).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { "Adventurer" },
            xpFraction = if (cost > 0) (game.optInt("level_xp") / cost.toDouble()).coerceIn(0.0, 1.0) else 1.0,
            xpLine = if (cost > 0) "${WidgetText.count(game.optDouble("xp_to_next", 0.0))} XP to level ${level + 1}" else "Max level",
            questsLine = "$done of ${quests.size} quests",
            xpToday = "${WidgetText.count(today?.optDouble("xp", 0.0) ?: 0.0)} / ${WidgetText.count(today?.optDouble("max", 0.0) ?: 0.0)} XP today",
            quests = ordered,
        )
    }

    // ---- Food log (the two-tap meal tiles) ----

    /** A meal logged by a tile has a description like "green lunch (estimate)", the same test as the page's TILE_MEAL. */
    private val TILE_MEAL = Regex("^(green|amber|red|protein-rich|light|standard|heavy) (breakfast|lunch|dinner|snack) \\(estimate\\)$")

    /** The quality of a meal logged by a tile, or null for a pasted or typed meal. */
    fun tileQuality(m: Food.Meal): String? = TILE_MEAL.find(m.description)?.groupValues?.get(1)

    data class MealTile(val meal: String, val name: String, val status: String, val quality: String?)
    data class Choice(val tier: String, val kcal: String, val hint: String, val on: Boolean)

    data class FoodLog(
        val total: String, val detail: String,
        val tiles: List<MealTile>,
        /** The meal whose four choices are showing, or null for the four meals. */
        val open: String?, val choices: List<Choice>,
    )

    private fun title(meal: String) = meal.replaceFirstChar { it.uppercase() }

    fun food(meals: List<Pair<String, Food.Meal>>, profile: Profile, open: String?): FoodLog {
        val list = meals.map { it.second }
        val kcal = list.sumOf { it.kcal }
        val total = if (list.isEmpty()) "Nothing logged yet" else
            "${WidgetText.count(kcal)} / ${WidgetText.count(profile.kcalTarget)} kcal"
        val detail = if (list.isEmpty()) "Tap a meal, then how it was" else
            "${list.sumOf { it.proteinG }.roundToInt()} g protein · ${list.sumOf { it.sugarG }.roundToInt()} g sugar"
        val tiles = Food.MEALS.map { meal ->
            val all = list.filter { it.meal == meal }
            val q = if (meal == "snack") null else all.lastOrNull { tileQuality(it) != null }?.let(::tileQuality)
            val what = when {
                all.isEmpty() -> "Tap to log"
                meal == "snack" -> "${all.size} · ${WidgetText.count(all.sumOf { it.kcal })} kcal"
                else -> "${q?.let(::title) ?: "Logged"} · ${WidgetText.count(all.sumOf { it.kcal })}"
            }
            MealTile(meal, title(meal), what, q)
        }
        val shown = open?.takeIf { it in Food.MEALS }
        val current = shown?.let { m -> if (m == "snack") null else list.lastOrNull { it.meal == m && tileQuality(it) != null }?.let(::tileQuality) }
        val choices = shown?.let { m ->
            Food.PRESETS.filter { it.meal == m }.map { p ->
                val on = current == p.tier.lowercase()
                Choice(p.tier, "${WidgetText.count(p.kcal)} kcal", if (on) "Logged · tap to undo" else p.hint, on)
            }
        } ?: emptyList()
        return FoodLog(total, detail, tiles, shown, choices)
    }

    /** What a tap on a choice does, the same as the page: snacks add; a meal adds, changes, or (same choice again) undoes. */
    sealed class Pick {
        data class Add(val preset: Food.Preset) : Pick()
        data class Change(val removeId: String, val preset: Food.Preset) : Pick()
        data class Undo(val removeId: String) : Pick()
    }

    fun pick(meal: String, tier: String, meals: List<Pair<String, Food.Meal>>): Pick? {
        val preset = Food.PRESETS.firstOrNull { it.meal == meal && it.tier == tier } ?: return null
        if (meal == "snack") return Pick.Add(preset)
        val old = meals.lastOrNull { it.second.meal == meal && tileQuality(it.second) != null } ?: return Pick.Add(preset)
        return if (tileQuality(old.second) == tier.lowercase()) Pick.Undo(old.first) else Pick.Change(old.first, preset)
    }

    /** A short line for the widget after a tap, like the page's toast. */
    fun pickNote(meal: String, tier: String, p: Pick): String = when (p) {
        is Pick.Add -> "Logged ${tier.lowercase()} $meal"
        is Pick.Change -> "Changed $meal to ${tier.lowercase()}"
        is Pick.Undo -> "Removed $meal"
    }

    // ---- Quick log ----

    data class QuickButton(val action: String, val label: String, val sub: String, val fraction: Double?)

    fun quick(day: Map<String, Double>, profile: Profile): List<QuickButton> {
        val water = day["water.ml"] ?: 0.0
        val cups = (day["coffee.cups"] ?: 0.0).roundToInt()
        val journal = (day["journal.entries"] ?: 0.0).roundToInt()
        val litres = String.format(java.util.Locale.UK, "%.2f", water / 1000).trimEnd('0').trimEnd('.')
        // The water target is in litres, like the page's.
        val target = String.format(java.util.Locale.UK, "%.1f", profile.waterTarget).removeSuffix(".0")
        return listOf(
            QuickButton(ACTION_WATER, "+250 ml", "Water $litres / $target L",
                if (profile.waterTarget > 0) (water / 1000 / profile.waterTarget).coerceIn(0.0, 1.0) else null),
            QuickButton(ACTION_COFFEE, "+1 coffee", if (cups == 1) "1 cup" else "$cups cups", null),
            QuickButton(ACTION_JOURNAL, "Journal", if (journal == 0) "None yet" else if (journal == 1) "1 entry" else "$journal entries", null),
        )
    }

    /** What each quick log button records, the same entries as the page's Quick log. */
    fun quickEntry(action: String): ManualLog.Entry? = when (action) {
        ACTION_WATER -> ManualLog.Entry("water.ml", 250.0, "ml")
        ACTION_COFFEE -> ManualLog.Entry("coffee.cups", 1.0, "cups")
        ACTION_JOURNAL -> ManualLog.Entry("journal.entries", 1.0, "count")
        else -> null
    }

    fun quickNote(action: String): String = when (action) {
        ACTION_WATER -> "Added 250 ml water"
        ACTION_COFFEE -> "Added a coffee"
        ACTION_JOURNAL -> "Journal entry logged"
        else -> ""
    }

    const val ACTION_WATER = "water"
    const val ACTION_COFFEE = "coffee"
    const val ACTION_JOURNAL = "journal"
}

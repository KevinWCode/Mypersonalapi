package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Answers the dashboard page's requests (GET /days, /game, /meals, /trading, /status, /food/presets, POST /log, POST /meals and
 * DELETE /meals/<id>) from the phone's own data, in the same shapes as the PC server, so the page runs unchanged
 * inside the app. Kept apart from Android so it can be unit tested.
 */
class PhoneApi(private val data: Data, private val today: () -> LocalDate = LocalDate::now,
               private val now: () -> LocalDateTime = LocalDateTime::now) {

    /** Where the answers come from: the phone's database and manual log. */
    interface Data {
        fun days(): Map<LocalDate, Map<String, Double>>
        fun meals(day: LocalDate): List<Pair<String, Food.Meal>>
        fun addMeals(meals: List<Food.Meal>): List<String>
        fun removeMeal(id: String): Boolean
        /** Applies quick log entries to a day and returns the new values. */
        fun log(day: LocalDate, entries: List<ManualLog.Entry>): Map<String, Double>
        /** Most recent save time (UTC, ISO) per source. */
        fun lastUpdates(): Map<String, String>
        /** Saved Alpaca fills and positions. */
        fun trading(): Trading = Trading.EMPTY
    }

    class Answer(val status: Int, val body: JSONObject)

    private fun error(status: Int, detail: String) = Answer(status, JSONObject().put("detail", detail))

    fun handle(method: String, url: String, body: String?): Answer = try {
        val path = url.substringBefore("?").substringBefore("#").let { p ->
            // Absolute URLs too, in case the page builds one.
            if (p.contains("://")) "/" + p.substringAfter("://").substringAfter("/", "") else p
        }
        val query = url.substringAfter("?", "").substringBefore("#").split("&").filter { it.contains("=") }
            .associate { URLDecoder.decode(it.substringBefore("="), "UTF-8") to URLDecoder.decode(it.substringAfter("="), "UTF-8") }
        fun date(key: String) = query[key]?.takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
        when {
            method == "GET" && path == "/days" -> {
                val end = date("end") ?: today()
                val start = date("start") ?: end.minusDays(6)
                if (start > end) error(400, "start must be on or before end")
                else Answer(200, JSONObject().put("days", daysJson(data.days(), start, end)))
            }
            method == "GET" && path == "/game" -> {
                val day = date("date") ?: today()
                Answer(200, GameJson.toJson(Game.build(data.days(), day, now = now())))
            }
            method == "GET" && path == "/summary" -> Answer(200, JSONObject().put("date", (date("date") ?: today()).toString()))
            method == "GET" && path == "/trading" -> Answer(200, tradingJson(data.days(), today(), data.trading()))
            method == "GET" && path == "/status" -> Answer(200, JSONObject().put("last_update", JSONObject(data.lastUpdates())))
            method == "GET" && path == "/meals" -> {
                val day = date("date") ?: return error(400, "date is missing")
                Answer(200, JSONObject().put("meals", JSONArray(data.meals(day).map { (id, m) -> mealJson(id, m) })))
            }
            method == "POST" && path == "/log" -> log(JSONObject(body ?: "{}"))
            method == "POST" && path == "/meals" -> {
                val b = JSONObject(body ?: "{}")
                val day = LocalDate.parse(b.getString("date"))
                val meals = Food.parse(b.getJSONArray("meals").toString(), day)
                Answer(200, JSONObject().put("ids", JSONArray(data.addMeals(meals))))
            }
            method == "DELETE" && path.startsWith("/meals/") -> {
                val id = URLDecoder.decode(path.removePrefix("/meals/"), "UTF-8")
                if (data.removeMeal(id)) Answer(200, JSONObject().put("ok", true)) else error(404, "no such meal")
            }
            method == "GET" && path == "/food/presets" -> Answer(200, JSONObject().put("presets", presetsJson()))
            method == "GET" && path == "/health" -> Answer(200, JSONObject().put("ok", true))
            else -> error(404, "Not Found")
        }
    } catch (e: Food.BadMeal) {
        error(422, e.message ?: "bad meal")
    } catch (e: Exception) {
        error(400, e.message ?: e.javaClass.simpleName)
    }

    private fun log(b: JSONObject): Answer {
        val day = LocalDate.parse(b.getString("date"))
        val list = b.optJSONArray("entries") ?: JSONArray()
        if (list.length() > 20) return error(422, "at most 20 entries")
        val entries = (0 until list.length()).map { i ->
            val e = list.getJSONObject(i)
            val metric = e.getString("metric")
            if (!LOG_METRICS.matches(metric)) return error(422, "$metric can't be logged here")
            val value = e.getDouble("value")
            if (value < -100000 || value > 100000) return error(422, "value is out of range")
            val mode = when (e.optString("mode", "add")) {
                "add" -> ManualLog.Mode.ADD
                "set" -> ManualLog.Mode.SET
                else -> return error(422, "mode must be add or set")
            }
            ManualLog.Entry(metric, value, e.getString("unit"), mode)
        }
        return Answer(200, JSONObject().put("values", JSONObject(data.log(day, entries))))
    }

    companion object {
        /** What the dashboard may log: the same list as the server's LOG_METRICS. */
        val LOG_METRICS = Regex(
            "^(water\\.ml|reading\\.minutes|study\\.minutes|coffee\\.cups|food\\.protein_g|journal\\.entries" +
                "|kb\\.[a-z_]{1,30}\\.done|perk\\.(str|con|int|wis|cha|per)\\.[1-4]\\.claimed)$"
        )

        fun daysJson(days: Map<LocalDate, Map<String, Double>>, start: LocalDate, end: LocalDate): JSONArray =
            JSONArray(days.filterKeys { it in start..end }.toSortedMap().map { (d, m) ->
                JSONObject().put("date", d.toString()).put("metrics", JSONObject(m.toSortedMap()))
            })

        /** The Food tab's buttons: the meal matrix in [Food.PRESETS]. */
        fun presetsJson(): JSONArray = JSONArray(Food.PRESETS.map { p ->
            JSONObject().put("meal", p.meal).put("tier", p.tier).put("kcal", p.kcal).put("protein_g", p.proteinG)
                .put("carbs_g", p.carbsG).put("fat_g", p.fatG).put("sugar_g", p.sugarG)
        })

        fun mealJson(id: String, m: Food.Meal): JSONObject = JSONObject()
            .put("id", id).put("meal", m.meal).put("kcal", m.kcal).put("protein_g", m.proteinG).put("carbs_g", m.carbsG)
            .put("fat_g", m.fatG).put("sugar_g", m.sugarG).put("description", m.description).put("origin", "phone")

        fun fillJson(f: Fill, withSide: Boolean = true): JSONObject = JSONObject().put("symbol", f.symbol)
            .also { if (withSide) it.put("side", f.side) }.put("qty", f.qty).put("price", f.price).put("filled_at", f.filledAt)

        /** The server's GET /trading: P/L and equity from the day totals, plus the saved fills and positions. */
        fun tradingJson(days: Map<LocalDate, Map<String, Double>>, today: LocalDate, t: Trading = Trading.EMPTY): JSONObject {
            fun pl(n: Int) = Math.rint((0 until n).sumOf { days[today.minusDays(it.toLong())]?.get("trading.pl_day") ?: 0.0 } * 100) / 100
            // All accounts added together on the latest day.
            val equity = days.filterValues { "trading.equity" in it }.maxByOrNull { it.key }?.value?.get("trading.equity")
            return JSONObject()
                .put("equity", equity ?: JSONObject.NULL)
                .put("pl_7d", pl(7)).put("pl_30d", pl(30))
                .put("last_buy", t.lastBuy?.let { fillJson(it, withSide = false) } ?: JSONObject.NULL)
                .put("last_sell", t.lastSell?.let { fillJson(it, withSide = false) } ?: JSONObject.NULL)
                .put("recent_trades", JSONArray(t.recent.map { fillJson(it) }))
                .put("top_positions", JSONArray(t.top.map {
                    JSONObject().put("symbol", it.symbol).put("qty", it.qty).put("market_value", it.marketValue)
                        .put("unrealized_pl", it.unrealizedPl)
                }))
        }
    }
}

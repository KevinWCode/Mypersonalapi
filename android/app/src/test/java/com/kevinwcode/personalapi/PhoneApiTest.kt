package com.kevinwcode.personalapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PhoneApiTest {
    private val today = LocalDate.of(2026, 11, 20)

    private class Fake : PhoneApi.Data {
        val days = mutableMapOf(
            LocalDate.of(2026, 11, 19) to mapOf("health.steps" to 8000.0, "trading.pl_day" to 12.5, "trading.equity" to 1500.0),
            LocalDate.of(2026, 11, 20) to mapOf("water.ml" to 500.0, "trading.pl_day" to -2.25),
        )
        val meals = mutableListOf<Pair<String, Food.Meal>>()
        val logged = mutableMapOf<String, Double>()
        override fun days() = days
        override fun meals(day: LocalDate) = meals.filter { it.second.date == day }
        override fun addMeals(meals: List<Food.Meal>) = meals.mapIndexed { i, m -> "id$i".also { this.meals += it to m } }
        override fun removeMeal(id: String) = meals.removeIf { it.first == id }
        override fun log(day: LocalDate, entries: List<ManualLog.Entry>): Map<String, Double> {
            entries.forEach { e -> logged[e.metric] = ManualLog.apply(logged[e.metric], e) }
            return entries.associate { it.metric to logged.getValue(it.metric) }
        }
        override fun lastUpdates() = mapOf("phone" to "2026-11-20T10:00:00Z")
    }

    private val data = Fake()
    private val api = PhoneApi(data, { today }, { LocalDateTime.of(2026, 11, 20, 12, 0) })

    @Test
    fun daysAreFilteredAndSorted() {
        val a = api.handle("GET", "/days?start=2026-11-20&end=2026-11-20", null)
        assertEquals(200, a.status)
        val days = a.body.getJSONArray("days")
        assertEquals(1, days.length())
        assertEquals(500.0, days.getJSONObject(0).getJSONObject("metrics").getDouble("water.ml"), 0.0)
        assertEquals(400, api.handle("GET", "/days?start=2026-11-21&end=2026-11-20", null).status)
    }

    @Test
    fun logAddsAndRejectsOtherMetrics() {
        val body = """{"date":"2026-11-20","entries":[{"metric":"water.ml","value":250,"unit":"ml","mode":"add"}]}"""
        api.handle("POST", "/log", body)
        val a = api.handle("POST", "/log", body)
        assertEquals(500.0, a.body.getJSONObject("values").getDouble("water.ml"), 0.0)
        val bad = """{"date":"2026-11-20","entries":[{"metric":"health.steps","value":1,"unit":"count"}]}"""
        assertEquals(422, api.handle("POST", "/log", bad).status)
        val claim = """{"date":"2026-11-20","entries":[{"metric":"perk.wis.1.claimed","value":1,"unit":"count","mode":"set"}]}"""
        assertEquals(200, api.handle("POST", "/log", claim).status)
    }

    @Test
    fun mealsAddListAndRemove() {
        val add = api.handle("POST", "/meals", """{"date":"2026-11-20","meals":[{"meal":"lunch","kcal":600,"sugar_g":9}]}""")
        assertEquals(200, add.status)
        val id = add.body.getJSONArray("ids").getString(0)
        val list = api.handle("GET", "/meals?date=2026-11-20", null).body.getJSONArray("meals")
        assertEquals(9.0, list.getJSONObject(0).getDouble("sugar_g"), 0.0)
        assertEquals(200, api.handle("DELETE", "/meals/$id", null).status)
        assertEquals(404, api.handle("DELETE", "/meals/$id", null).status)
        assertEquals(422, api.handle("POST", "/meals", """{"date":"2026-11-20","meals":[{"meal":"brunch","kcal":1}]}""").status)
    }

    @Test
    fun tradingStatusAndGame() {
        val t = api.handle("GET", "/trading", null).body
        assertEquals(10.25, t.getDouble("pl_7d"), 0.0)
        assertEquals(1500.0, t.getDouble("equity"), 0.0)
        assertEquals(0, t.getJSONArray("recent_trades").length())
        assertEquals("2026-11-20T10:00:00Z", api.handle("GET", "/status", null).body.getJSONObject("last_update").getString("phone"))
        val g = api.handle("GET", "/game?date=2026-11-20", null)
        assertEquals(200, g.status)
        assertEquals(Game.DAILY_MAX, g.body.getJSONObject("today").getInt("max"))
        assertEquals(404, api.handle("GET", "/nope", null).status)
    }

    @Test
    fun localSummaryJsonReadsBackInTheWidget() {
        val s = LocalSummary.build(data.days, today)
        val json = LocalSummary.toJson(s)
        assertEquals(10.25, json.getDouble("pl_7d"), 0.0)
        assertEquals(JSONObject.NULL, json.get("last_trade"))
        assertEquals(7, json.getJSONObject("social").getJSONArray("days").length())
    }

    @Test
    fun foodPresetsAreTheMealMatrix() {
        val a = api.handle("GET", "/food/presets", null)
        assertEquals(200, a.status)
        val presets = a.body.getJSONArray("presets")
        assertEquals(12, presets.length())
        val heavySnack = (0 until presets.length()).map { presets.getJSONObject(it) }
            .single { it.getString("meal") == "snack" && it.getString("tier") == "Heavy" }
        assertEquals(500.0, heavySnack.getDouble("kcal"), 0.0)
        assertEquals(34.0, heavySnack.getDouble("sugar_g"), 0.0)
    }
}

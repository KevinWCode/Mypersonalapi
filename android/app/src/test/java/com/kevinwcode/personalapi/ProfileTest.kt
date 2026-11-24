package com.kevinwcode.personalapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ProfileTest {
    private val day = LocalDate.of(2026, 11, 24)
    private val evening = LocalDateTime.of(2026, 11, 24, 21, 0)

    private val busyDay = mapOf(
        "sleep.total_minutes" to 460.0, "health.steps" to 9000.0, "kb.swing.sets" to 3.0,
        "football.max_catch_yards" to 15.0, "github.commits" to 2.0, "study.minutes" to 60.0,
        "screen.total_minutes" to 200.0, "screen.social_media_minutes" to 90.0, "screen.tiktok_minutes" to 90.0,
        "food.sugar_g" to 80.0, "coffee.cups" to 4.0,
    )

    @Test
    fun allIsTheOldBehaviour() {
        val days = mapOf(day to busyDay)
        val old = Game.build(days, day, day, evening)
        val withAll = Game.build(days, day, day, evening, Profile.ALL)
        assertEquals(old.todayXp, withAll.todayXp)
        assertEquals(Game.DAILY_MAX, withAll.dailyMax)
        assertEquals(old.lines.map { it.key }, withAll.lines.map { it.key })
        assertEquals("Kettlebell", withAll.lines.first { it.key == "kettlebell" }.label)
    }

    @Test
    fun switchedOffLinesLeaveTheQuestsAndTheDailyMax() {
        val p = Profile(strength = "none", off = setOf("catch", "github", "social"))
        val g = Game.build(mapOf(day to busyDay), day, day, evening, p)
        val keys = g.lines.map { it.key }
        listOf("kettlebell", "catch", "github", "social").forEach { assertFalse(it, it in keys) }
        assertTrue("sleep" in keys && "workout" in keys)
        assertEquals(Game.DAILY_MAX - 75 - 50 - 100 - 100, g.dailyMax)
        assertEquals(g.dailyMax, Game.dailyMax(p))
        // The group lists only the stats that are on.
        val str = GameJson.toJson(g).getJSONArray("groups").getJSONObject(0)
        assertEquals("str", str.getString("key"))
        assertEquals(listOf("workout"), (0 until str.getJSONArray("stats").length()).map { str.getJSONArray("stats").getString(it) })
    }

    @Test
    fun socialMediaOffCostsNothingAndGivesNoCalmBonus() {
        val yesterday = day.minusDays(1)
        val quiet = mapOf("screen.total_minutes" to 100.0, "sleep.total_minutes" to 450.0)
        val days = mapOf(yesterday to quiet, day to busyDay)
        val p = Profile(off = setOf("social"))
        val stripped = p.strip(busyDay)
        assertNull(stripped["screen.social_media_minutes"])
        assertNull(stripped["screen.tiktok_minutes"])
        val focus = TestB.build(days, day, yesterday, evening, p).vitals.first { it.key == "focus" }
        assertTrue(focus.today.none { it.label.startsWith("Social media") })
        val hp = TestB.build(days, day, yesterday, evening, p).vitals.first { it.key == "hp" }
        assertTrue(hp.today.none { it.label.startsWith("Social media") })
        // Not measuring social media doesn't build the "social under 30 min" streak either.
        assertFalse(Game.build(days, day, yesterday, evening, p).history.any { it.socialOk })
    }

    @Test
    fun foodAndCoffeeOffLeaveTheVitalsAlone() {
        val p = Profile(off = setOf("food", "coffee"))
        val hp = TestB.build(mapOf(day to busyDay), day, day, evening, p).vitals.first { it.key == "hp" }
        assertTrue(hp.today.none { it.label.startsWith("Sugar") || it.label.startsWith("Coffee") })
    }

    @Test
    fun aStrengthSessionTapCountsLikeAKettlebellSession() {
        val p = Profile(strength = "gym")
        val g = Game.build(mapOf(day to mapOf(Profile.STRENGTH_DONE to 1.0)), day, day, evening, p)
        val line = g.lines.first { it.key == "kettlebell" }
        assertEquals("Gym session", line.label)
        assertEquals(75, line.xp)
        assertTrue(Vitals.workedOut(mapOf(Profile.STRENGTH_DONE to 1.0)))
    }

    @Test
    fun savedFormRoundTrips() {
        val p = Profile("bodyweight", setOf("catch", "money"))
        assertEquals(p, Profile.decode(p.encode()))
        assertNull(Profile.decode(null))
        assertEquals(Profile("none", setOf("github")), Profile.decode("nonsense|github,unknown"))
        assertEquals(Profile("cardio", emptySet()), Profile.decode("cardio|"))
    }

    @Test
    fun phoneApiServesTheProfileAndAcceptsStrengthTaps() {
        val p = Profile("gym", setOf("money"))
        val api = PhoneApi(object : PhoneApi.Data {
            override fun days() = mapOf(day to emptyMap<String, Double>())
            override fun meals(day: LocalDate) = emptyList<Pair<String, Food.Meal>>()
            override fun addMeals(meals: List<Food.Meal>) = emptyList<String>()
            override fun removeMeal(id: String) = false
            override fun log(day: LocalDate, entries: List<ManualLog.Entry>) = entries.associate { it.metric to it.value }
            override fun lastUpdates() = emptyMap<String, String>()
            override fun profile() = p
        }, today = { day }, now = { evening })
        val body = api.handle("GET", "/profile", null).body
        assertEquals("gym", body.getString("strength"))
        assertEquals("Gym session", body.getString("strength_label"))
        assertEquals("money", body.getJSONArray("off").getString(0))
        val log = JSONObject().put("date", day.toString()).put("entries", org.json.JSONArray()
            .put(JSONObject().put("metric", "strength.done").put("value", 1).put("unit", "count").put("mode", "set")))
        assertEquals(200, api.handle("POST", "/log", log.toString()).status)
    }
}

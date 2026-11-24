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
        "screen.total_minutes" to 200.0, "screen.social_media_minutes" to 90.0,
    )

    @Test
    fun kevinsProfileScoresLikeBefore() {
        val days = mapOf(day to busyDay)
        val g = Game.build(days, day, day, evening, Profile.KEVIN)
        assertEquals(Game.DAILY_MAX, g.dailyMax)
        assertEquals("Kettlebell", g.lines.first { it.key == "kettlebell" }.label)
        val catch = g.lines.first { it.key == "catch" }
        assertEquals("Catch drill", catch.label)
        assertEquals("15 yd", catch.detail)
        assertEquals(50, catch.xp)
    }

    @Test
    fun aNewTesterWithNoTrainingOrGoalHasNoLinesForThem() {
        val g = Game.build(mapOf(day to busyDay), day, day, evening, Profile.NEW_TESTER)
        val keys = g.lines.map { it.key }
        listOf("kettlebell", "catch", "github").forEach { assertFalse(it, it in keys) }
        assertTrue("sleep" in keys && "social" in keys && "workout" in keys)
        assertEquals(Game.DAILY_MAX - 75 - 50 - 100, g.dailyMax)
        val str = GameJson.toJson(g).getJSONArray("groups").getJSONObject(0)
        assertEquals(listOf("workout"), (0 until str.getJSONArray("stats").length()).map { str.getJSONArray("stats").getString(it) })
    }

    @Test
    fun ownTrainingAndGoalNameTheLines() {
        val p = Profile(training = "Spin class", goal = Profile.Goal("Push-ups in one set", "reps", 40.0)).withExerciseNames(emptyList())
        assertEquals(listOf(Profile.Exercise("spin_class", "Spin class")), p.trackedExercises)
        val m = mapOf("kb.spin_class.done" to 1.0, Profile.GOAL_METRIC to 32.0)
        val g = Game.build(mapOf(day to m), day, day, evening, p)
        val training = g.lines.first { it.key == "kettlebell" }
        assertEquals("Spin class", training.label)
        assertEquals(75, training.xp)
        val goal = g.lines.first { it.key == "catch" }
        assertEquals("Push-ups in one set", goal.label)
        assertEquals("32 reps", goal.detail)
        assertTrue(Vitals.workedOut(m))
    }

    @Test
    fun renamedExercisesKeepTheirKeys() {
        val p = Profile.KEVIN.withExerciseNames(listOf("Swing", "clean & press", "Goblet squat", "Goblet  squat!"))
        assertEquals(listOf("swing", "clean_press", "goblet_squat", "goblet_squat_b"), p.exercises.map { it.key })
        assertEquals("Clean & press", Profile.KEVIN.exercises.last().name)
        val clash = Profile(exercises = listOf(Profile.Exercise("row", "Row"))).withExerciseNames(listOf("Row", "row!"))
        assertEquals(listOf("row", "row_b"), clash.exercises.map { it.key })
        val two = Profile().withExerciseNames(listOf("Row", "Row 2"))
        assertEquals(listOf("row", "row_b"), two.exercises.map { it.key })
        assertTrue(two.exercises.all { PhoneApi.LOG_METRICS.matches("kb.${it.key}.done") })
    }

    @Test
    fun githubOffIsIgnored() {
        val p = Profile.KEVIN.copy(off = setOf("github"))
        assertNull(p.strip(busyDay)["github.commits"])
        assertFalse(Game.build(mapOf(day to busyDay), day, day, evening, p).lines.any { it.key == "github" })
    }

    @Test
    fun savedFormRoundTrips() {
        assertEquals(Profile.KEVIN, Profile.decode(Profile.KEVIN.encode()))
        val p = Profile("Running", listOf("Mon", "Thu"), emptyList(), Profile.Goal("Longest run", "km", null), setOf("money"))
        assertEquals(p, Profile.decode(p.encode()))
        assertNull(Profile.decode(null))
        assertNull(Profile.decode("not json"))
    }

    @Test
    fun phoneApiServesTheProfile() {
        val api = PhoneApi(object : PhoneApi.Data {
            override fun days() = mapOf(day to emptyMap<String, Double>())
            override fun meals(day: LocalDate) = emptyList<Pair<String, Food.Meal>>()
            override fun addMeals(meals: List<Food.Meal>) = emptyList<String>()
            override fun removeMeal(id: String) = false
            override fun log(day: LocalDate, entries: List<ManualLog.Entry>) = entries.associate { it.metric to it.value }
            override fun lastUpdates() = emptyMap<String, String>()
            override fun profile() = Profile.KEVIN
        }, today = { day }, now = { evening })
        val body = api.handle("GET", "/profile", null).body
        assertEquals("Kettlebell", body.getString("training"))
        assertEquals("Monday", body.getJSONArray("sessions").getString(0))
        assertEquals("clean_press", body.getJSONArray("tracked").getJSONObject(2).getString("key"))
        assertEquals(20.0, body.getJSONObject("goal").getDouble("target"), 0.0)
        val log = JSONObject().put("date", day.toString()).put("entries", org.json.JSONArray()
            .put(JSONObject().put("metric", "kb.spin_class.done").put("value", 1).put("unit", "count").put("mode", "set")))
        assertEquals(200, api.handle("POST", "/log", log.toString()).status)
    }

    @Test
    fun calorieTargetMovesTheHpRule() {
        val m = mapOf("food.kcal" to 2300.0, "food.breakfast.kcal" to 800.0, "food.dinner.kcal" to 1500.0)
        fun hp(p: Profile) = TestB.build(mapOf(day to m), day, day, evening, p).vitals.first { it.key == "hp" }.today
        assertFalse(hp(Profile.KEVIN).any { it.label.startsWith("Calories on target") })
        assertTrue(hp(Profile.KEVIN.copy(kcalTarget = 2200.0)).any { it.label.startsWith("Calories on target") })
        assertEquals(2500.0, Profile.decode(Profile.KEVIN.copy(kcalTarget = 2500.0).encode())!!.kcalTarget, 0.0)
        assertEquals(2000.0, Profile.decode("{}")!!.kcalTarget, 0.0)
    }

    @Test
    fun proteinAndWaterTargets() {
        val m = mapOf("food.protein_g" to 120.0)
        fun hp(p: Profile) = TestB.build(mapOf(day to m), day, day, evening, p).vitals.first { it.key == "hp" }.today
        assertFalse(hp(Profile.KEVIN).any { it.label.startsWith("Protein") })
        val lower = Profile.KEVIN.copy(proteinTarget = 130.0)
        assertTrue(hp(lower).any { it.label == "Protein 117 g+" })
        assertTrue(hp(Profile.KEVIN.copy(proteinTarget = 150.0)).none { it.label.startsWith("Protein") })
        val kevin135 = TestB.build(mapOf(day to mapOf("food.protein_g" to 135.0)), day, day, evening, Profile.KEVIN).vitals.first { it.key == "hp" }.today
        assertTrue(kevin135.any { it.label == "Protein 135 g+" })
        val back = Profile.decode(Profile.KEVIN.copy(proteinTarget = 120.0, waterTarget = 2.5).encode())!!
        assertEquals(120.0, back.proteinTarget, 0.0)
        assertEquals(2.5, back.waterTarget, 0.0)
        assertEquals(150.0, Profile.decode("{}")!!.proteinTarget, 0.0)
    }
}

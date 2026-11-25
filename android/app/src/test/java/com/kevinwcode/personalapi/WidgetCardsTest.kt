package com.kevinwcode.personalapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class WidgetCardsTest {
    private val day = LocalDate.of(2026, 11, 24)

    private fun meal(meal: String, tier: String) = Food.PRESETS.first { it.meal == meal && it.tier == tier }.toMeal(day)

    @Test
    fun everyThemeOnThePageHasWidgetColours() {
        val page = File("../../app/static/dashboard.html").readText()
        val ids = Regex("""\{ id: '([a-z]+)', name: '""").findAll(page).map { it.groupValues[1] }.toList()
        assertEquals(8, ids.size)
        assertEquals(ids, WidgetTheme.ALL.map { it.id })
        assertEquals("light", WidgetTheme.of("nope").id)
        assertEquals("cyber", WidgetTheme.of("cyber").id)
        assertEquals(0xFF2457D6.toInt(), WidgetTheme.of("light").accent)
    }

    @Test
    fun vitalsShowTheDayBadgeAndEachBarsState() {
        val now = LocalDateTime.of(2026, 11, 24, 9, 0)
        val days = mapOf(day to mapOf("sleep.total_minutes" to 480.0, "sleep.deep_minutes" to 90.0, "sleep.rem_minutes" to 90.0))
        val (g, c) = TestB.buildWithCondition(days, day, gameStart = day, now = now)
        val card = WidgetCards.vitals(TestB.toJson(g, c), now)
        assertEquals("★ Prime Condition", card.condition)
        assertEquals("prime", card.conditionState)
        assertEquals(listOf("hp", "stamina", "mana", "focus"), card.rows.map { it.key })
        val hp = card.rows.first()
        assertTrue(hp.value > 100)
        assertEquals("over", hp.state)
        assertEquals("★ Overcharge", hp.tag)
        assertTrue(card.rows.all { it.fraction in 0.0..1.0 })
    }

    @Test
    fun aBarWellBehindAGoodDaysLineShowsItsDebuff() {
        val now = LocalDateTime.of(2026, 11, 24, 20, 0)
        // A 4 h night, so the bars start low and the drain takes them under half of a good day's line by evening.
        val days = mapOf(day to mapOf("sleep.total_minutes" to 240.0))
        val (g, c) = TestB.buildWithCondition(days, day, gameStart = day, now = now)
        val card = WidgetCards.vitals(TestB.toJson(g, c), now)
        assertEquals("rest", card.conditionState)
        assertEquals("Rest Day", card.condition)
        val stamina = card.rows.first { it.key == "stamina" }
        assertEquals("low", stamina.state)
        assertEquals("Fatigued", stamina.tag)
    }

    @Test
    fun characterPutsUnfinishedQuestsFirst() {
        val now = LocalDateTime.of(2026, 11, 24, 12, 0)
        val days = mapOf(day to mapOf("sleep.total_minutes" to 480.0, "journal.entries" to 2.0))
        val (g, c) = TestB.buildWithCondition(days, day, gameStart = day, now = now)
        val card = WidgetCards.character(TestB.toJson(g, c))
        assertTrue(card.level >= 1)
        assertTrue(card.xpLine.endsWith("XP to level ${card.level + 1}"))
        val done = card.quests.count { it.done }
        assertTrue(done >= 1)
        assertEquals("$done of ${card.quests.size} quests", card.questsLine)
        assertTrue(card.quests.dropWhile { !it.done }.all { it.done })
        assertTrue(card.xpToday.endsWith("XP today"))
    }

    @Test
    fun characterCopesWithMaxLevelAndNoTitle() {
        val g = JSONObject().put("level", 110).put("level_cost", JSONObject.NULL).put("level_xp", 0).put("xp_to_next", 0)
            .put("title", JSONObject.NULL).put("class", JSONObject().put("name", "Warrior"))
        val card = WidgetCards.character(g)
        assertEquals("Max level", card.xpLine)
        assertEquals("Warrior", card.heading)
        assertEquals(1.0, card.xpFraction, 0.0)
        assertEquals("0 of 0 quests", card.questsLine)
    }

    @Test
    fun foodTilesShowWhatWasLogged() {
        val meals = listOf("a" to meal("breakfast", "Green"), "b" to meal("snack", "Red"), "c" to meal("snack", "Green"),
            "d" to Food.Meal(day, "dinner", 900.0, 40.0, 80.0, 30.0, 12.0, "pasted from the AI"))
        val f = WidgetCards.food(meals, Profile.KEVIN, open = null)
        assertEquals(listOf("Breakfast", "Lunch", "Dinner", "Snack"), f.tiles.map { it.name })
        assertEquals("Green · 400", f.tiles[0].status)
        assertEquals("green", f.tiles[0].quality)
        assertEquals("Tap to log", f.tiles[1].status)
        assertEquals("Logged · 900", f.tiles[2].status)
        assertNull(f.tiles[2].quality)
        assertEquals("2 · 330 kcal", f.tiles[3].status)
        assertEquals("1,630 / ${WidgetText.count(Profile.KEVIN.kcalTarget)} kcal", f.total)
        assertNull(f.open)
        assertTrue(f.choices.isEmpty())
    }

    @Test
    fun anOpenMealShowsItsFourChoicesWithTheLoggedOneMarked() {
        val f = WidgetCards.food(listOf("a" to meal("lunch", "Amber")), Profile.KEVIN, open = "lunch")
        assertEquals("lunch", f.open)
        assertEquals(Food.QUALITIES, f.choices.map { it.tier })
        assertEquals(listOf(false, true, false, false), f.choices.map { it.on })
        assertEquals("Logged · tap to undo", f.choices[1].hint)
        assertEquals("700 kcal", f.choices[1].kcal)
        assertNull(WidgetCards.food(emptyList(), Profile.KEVIN, open = "brunch").open)
    }

    @Test
    fun pickAddsChangesOrUndoesLikeThePage() {
        val meals = listOf("a" to meal("lunch", "Amber"))
        assertEquals(WidgetCards.Pick.Undo("a"), WidgetCards.pick("lunch", "Amber", meals))
        val change = WidgetCards.pick("lunch", "Green", meals)
        assertTrue(change is WidgetCards.Pick.Change && change.removeId == "a" && change.preset.tier == "Green")
        assertTrue(WidgetCards.pick("dinner", "Red", meals) is WidgetCards.Pick.Add)
        // Snacks always add another one.
        val snacks = listOf("s" to meal("snack", "Red"))
        assertTrue(WidgetCards.pick("snack", "Red", snacks) is WidgetCards.Pick.Add)
        assertNull(WidgetCards.pick("lunch", "Purple", meals))
        assertEquals("Changed lunch to green", WidgetCards.pickNote("lunch", "Green", change!!))
    }

    @Test
    fun quickLogButtonsCountTodayAndLogThePagesEntries() {
        val b = WidgetCards.quick(mapOf("water.ml" to 1250.0, "coffee.cups" to 1.0), Profile.KEVIN)
        assertEquals(listOf("+250 ml", "+1 coffee", "Journal"), b.map { it.label })
        assertTrue(b[0].sub.startsWith("Water 1.25 / "))
        assertEquals("1 cup", b[1].sub)
        assertEquals("None yet", b[2].sub)
        assertEquals(1.25 / Profile.KEVIN.waterTarget, b[0].fraction!!, 1e-9)
        assertEquals(ManualLog.Entry("water.ml", 250.0, "ml"), WidgetCards.quickEntry(WidgetCards.ACTION_WATER))
        assertTrue(PhoneApi.LOG_METRICS.matches(WidgetCards.quickEntry(WidgetCards.ACTION_JOURNAL)!!.metric))
        assertNull(WidgetCards.quickEntry("tea"))
    }
}

package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TestBTest {
    private val day = LocalDate.of(2026, 11, 22)

    // The worked example from the feedback notes: 6.6 h sleep, 34% deep + REM, 34 g sugar, 1,250 ml water, 1 coffee,
    // a kettlebell workout, 60 min study, 30 min reading, 18 min social media, 21 min YouTube, seen at 15:30.
    private val example = mapOf(
        "sleep.total_minutes" to 396.0, "sleep.deep_minutes" to 70.0, "sleep.rem_minutes" to 64.0,
        "food.sugar_g" to 34.0, "water.ml" to 1250.0, "coffee.cups" to 1.0, "kb.swing.sets" to 3.0,
        "study.minutes" to 60.0, "reading.minutes" to 30.0, "screen.total_minutes" to 120.0,
        "screen.social_media_minutes" to 18.0,
    )

    private fun bars(days: List<Map<String, Double>>, elapsed: Double = 1.0, youtube: Double = 0.0): Map<String, Vitals.BarState> {
        val inputs = days.mapIndexed { i, m -> Vitals.DayInput(day.minusDays((days.size - 1 - i).toLong()), m, youtube) }
        return VitalsB.run(inputs, elapsed).bars.associateBy { it.key }
    }

    @Test
    fun sleepSetsBarsWithAnOverchargeTo110() {
        assertEquals(100.0, VitalsB.afterSleep(450.0), 1e-9)
        assertEquals(105.0, VitalsB.afterSleep(465.0), 1e-9)
        assertEquals(110.0, VitalsB.afterSleep(540.0), 1e-9)
        assertEquals(88.0, VitalsB.afterSleep(396.0), 1e-9)
    }

    @Test
    fun workedExampleAtHalfPastThree() {
        val b = bars(listOf(emptyMap(), example), elapsed = 15.5 / 24, youtube = 21.0)
        assertEquals(88.0, b.getValue("hp").startOfDay, 0.0)
        assertEquals(45.0, b.getValue("mana").startOfDay, 0.0)
        // HP 88: social (8 min past the free 10) -1, YouTube -1, sugar 4 g over the buffer -2, workout +5, then regen.
        assertEquals(1.0, b.getValue("hp").gainedToday, 0.0)
        assertEquals(97, b.getValue("hp").value)
        // Stamina 88: sugar rush +15 (capped), water +10, workout -25, then 8.5 of 16 waking hours of a 45 drain.
        assertEquals(64, b.getValue("stamina").value)
        // Mana 45: social -1, YouTube -1, reading +15, study -20 = 38, then the 30 drain so far.
        assertEquals(22, b.getValue("mana").value)
        assertTrue(b.getValue("mana").today.any { it.label == "Study" && it.points == -20.0 })
        assertFalse(b.getValue("hp").today.any { it.label.startsWith("Brain Fog") })
        assertEquals(83, b.getValue("focus").value)
        assertEquals(110.0, b.getValue("focus").top, 0.0)
    }

    @Test
    fun studyOnLowManaCausesBrainFog() {
        val m = mapOf("sleep.total_minutes" to 360.0, "study.minutes" to 90.0)
        val b = bars(listOf(m))
        // Mana 45 - 30 = 15 after studying, so each 30 min also costs 4 HP.
        assertTrue(b.getValue("hp").today.any { it.label.startsWith("Brain Fog") && it.points == -12.0 })
        assertTrue(b.getValue("focus").today.any { it.points == 30.0 })
    }

    @Test
    fun foodRewardsAndCoffeePastTwoCups() {
        val m = mapOf("food.protein_g" to 140.0, "food.kcal" to 2000.0, "food.lunch.kcal" to 800.0, "food.dinner.kcal" to 1200.0,
            "food.sugar_g" to 20.0, "coffee.cups" to 3.0)
        val hp = bars(listOf(m)).getValue("hp").today.associate { it.label to it.points }
        assertEquals(10.0, hp.getValue("Protein 135 g+"), 0.0)
        assertEquals(8.0, hp.getValue("Calories on target (2000 kcal)"), 0.0)
        assertEquals(-6.0, hp.getValue("Coffee past 2 cups"), 0.0)
        assertFalse(hp.keys.any { it.startsWith("Sugar") })
    }

    @Test
    fun workoutRecoveryIsBiggerAfterAProteinDay() {
        val yesterday = mapOf("kb.swing.sets" to 2.0, "food.protein_g" to 150.0)
        val st = bars(listOf(yesterday, emptyMap())).getValue("stamina").today
        assertTrue(st.any { it.label == "Recovery: yesterday's workout + protein" && it.points == 20.0 })
    }

    @Test
    fun primeConditionAddsTenPercentToManualXp() {
        // 8 h sleep with good deep + REM, then a workout, journal and commits: every bar stays 70+ at 15:00.
        val m = mapOf("sleep.total_minutes" to 480.0, "sleep.deep_minutes" to 100.0, "sleep.rem_minutes" to 80.0,
            "kb.swing.sets" to 3.0, "journal.entries" to 1.0, "github.commits" to 4.0, "water.ml" to 1500.0)
        val g = TestB.build(mapOf(day to m), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 23, 59))
        val prime = g.buffs.first { it.name == "Prime Condition" }
        // Kettlebell 75 + commits 100 + journal 75 = 250, so +25.
        assertEquals(25, prime.xp)
        val live = Game.build(mapOf(day to m), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 23, 59))
        assertEquals(live.todayXp + 25, g.todayXp)
        assertFalse(live.buffs.any { it.name == "Prime Condition" })
    }

    @Test
    fun liveRulesAreUnchangedAndJsonCarriesDebuffs() {
        val json = TestB.toJson(TestB.build(mapOf(day to example), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 15, 30)))
        assertEquals("test_b", json.getString("rules"))
        val mana = (0 until 4).map { json.getJSONArray("vitals").getJSONObject(it) }.first { it.getString("key") == "mana" }
        assertEquals("Brain Fog", mana.getJSONObject("debuff").getString("name"))
        assertEquals(-30.0, mana.getDouble("tick_per_day"), 0.0)
        assertEquals(-60.0, Vitals.BARS.first { it.key == "mana" }.tickPerDay, 0.0)
    }
}

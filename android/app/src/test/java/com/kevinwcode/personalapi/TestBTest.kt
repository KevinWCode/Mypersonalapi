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
        assertEquals(52.0, b.getValue("mana").startOfDay, 0.0)
        // HP 88: social (8 min past the free 10) -1, YouTube -1, sugar 4 g over the buffer -2, workout +5, then regen.
        assertEquals(1.0, b.getValue("hp").gainedToday, 0.0)
        assertEquals(97, b.getValue("hp").value)
        // Stamina 88: sugar rush +15 (capped), water +10, workout -25, then 8.5 of 16 waking hours of a 78 drain (88 to 10).
        assertEquals(47, b.getValue("stamina").value)
        assertEquals(-78.0, b.getValue("stamina").tickPerDay!!, 0.0)
        // Mana 52: social -1, YouTube -1, reading +15, study -10 = 55, then the 42 drain (52 to 10) so far.
        assertEquals(33, b.getValue("mana").value)
        assertTrue(b.getValue("mana").today.any { it.label == "Study" && it.points == -10.0 })
        assertFalse(b.getValue("hp").today.any { it.label.startsWith("Brain Fog") })
        assertEquals(69, b.getValue("focus").value)
        assertEquals(110.0, b.getValue("focus").top, 0.0)
    }

    @Test
    fun studyNeverCostsHp() {
        val m = mapOf("sleep.total_minutes" to 360.0, "study.minutes" to 120.0)
        val b = bars(listOf(m))
        // Capped at 90 min: -15 Mana, +30 Focus, and nothing taken from HP.
        assertTrue(b.getValue("mana").today.any { it.label == "Study" && it.points == -15.0 })
        assertTrue(b.getValue("focus").today.any { it.points == 30.0 })
        assertTrue(b.getValue("hp").today.none { it.points < 0 })
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
        // 8 h sleep with good deep + REM: every bar wakes up 70+. Then a workout, journal and commits.
        val m = mapOf("sleep.total_minutes" to 480.0, "sleep.deep_minutes" to 100.0, "sleep.rem_minutes" to 80.0,
            "kb.swing.sets" to 3.0, "journal.entries" to 1.0, "github.commits" to 4.0, "water.ml" to 1500.0)
        val g = TestB.build(mapOf(day to m), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 23, 59))
        val prime = g.buffs.first { it.name == "Prime Condition" }
        // Kettlebell 75 + commits 100 + journal 75 = 250, so +25.
        assertEquals(25, prime.xp)
        val live = Game.build(mapOf(day to m), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 23, 59))
        assertEquals(live.todayXp + 25, g.todayXp)
        assertFalse(live.buffs.any { it.name == "Prime Condition" })
        // 6.6 h sleep with little deep + REM: Mana wakes at 52, so no Prime Condition however the day goes.
        val short = TestB.build(mapOf(day to m + mapOf("sleep.total_minutes" to 396.0, "sleep.deep_minutes" to 40.0, "sleep.rem_minutes" to 40.0)), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 23, 59))
        assertFalse(short.buffs.any { it.name == "Prime Condition" })
    }

    @Test
    fun liveRulesAreUnchangedAndJsonCarriesDebuffs() {
        val json = TestB.toJson(TestB.build(mapOf(day to example), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 15, 30)))
        assertEquals("test_b", json.getString("rules"))
        val mana = (0 until 4).map { json.getJSONArray("vitals").getJSONObject(it) }.first { it.getString("key") == "mana" }
        assertEquals("Brain Fog", mana.getJSONObject("debuff").getString("name"))
        assertEquals(-42.0, mana.getDouble("tick_per_day"), 0.0)
        assertEquals(-60.0, Vitals.BARS.first { it.key == "mana" }.tickPerDay, 0.0)
    }

    @Test
    fun barsEndTheDayCloseToEmpty() {
        // A full night and nothing else logged: Stamina, Mana and Focus all drain down to 10 by 23:00; HP doesn't drain.
        val m = mapOf("sleep.total_minutes" to 450.0)
        val b = bars(listOf(m))
        for (k in listOf("stamina", "mana", "focus")) assertEquals(10, b.getValue(k).value)
        assertEquals(100, b.getValue("hp").value)
        // Gains still lift the evening: 1 l of water (+8) ends Stamina and Focus at 18.
        val w = bars(listOf(m + ("water.ml" to 1000.0)))
        assertEquals(18, w.getValue("stamina").value)
        assertEquals(18, w.getValue("focus").endOfDay)
    }

    @Test
    fun conditionIsPrimeSteadyOrRestDay() {
        fun cond(m: Map<String, Double>): VitalsB.Condition =
            TestB.buildWithCondition(mapOf(day to m), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 9, 0)).second
        assertEquals("prime", cond(mapOf("sleep.total_minutes" to 480.0, "sleep.deep_minutes" to 100.0, "sleep.rem_minutes" to 80.0)).state)
        assertEquals("steady", cond(mapOf("sleep.total_minutes" to 420.0)).state)
        // Under 6 h: HP, Stamina and Focus wake under 80, so a Rest Day, even with Mana boosted by deep + REM.
        val rest = cond(mapOf("sleep.total_minutes" to 340.0, "sleep.deep_minutes" to 70.0, "sleep.rem_minutes" to 60.0))
        assertEquals("rest", rest.state)
        assertEquals("Rest Day", rest.title)
        assertEquals("unknown", cond(emptyMap()).state)
        // A Rest Day never earns the Prime XP bonus.
        val g = TestB.build(mapOf(day to mapOf("sleep.total_minutes" to 340.0, "sleep.deep_minutes" to 70.0, "sleep.rem_minutes" to 60.0,
            "kb.swing.sets" to 3.0)), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 23, 59))
        assertFalse(g.buffs.any { it.name == "Prime Condition" })
    }

    @Test
    fun restDayDrainsMoreGentlyAndNoSleepStartsAt70() {
        // 5 h sleep: Stamina wakes at 67 and only drains 57, so it still ends at 10 rather than hitting 0 by evening.
        val b = bars(listOf(mapOf("sleep.total_minutes" to 300.0)), elapsed = 19.0 / 24)
        assertEquals(-56.7, b.getValue("stamina").tickPerDay!!, 0.05)
        assertTrue(b.getValue("stamina").value > 10)
        // Missing sleep is not a bad night: bars start at 70 (Mana 52), not from yesterday's near-empty evening.
        val n = bars(listOf(mapOf("sleep.total_minutes" to 450.0), emptyMap()), elapsed = 7.0 / 24)
        assertEquals(70.0, n.getValue("focus").startOfDay, 0.0)
        assertEquals(52.0, n.getValue("mana").startOfDay, 0.0)
    }

    @Test
    fun jsonCarriesConditionAndPace() {
        val (g, c) = TestB.buildWithCondition(mapOf(day to example), day, gameStart = day, now = LocalDateTime.of(2026, 11, 22, 15, 30))
        val json = TestB.toJson(g, c)
        assertEquals("steady", json.getJSONObject("condition").getString("state"))
        assertEquals(10.0, json.getJSONObject("pace").getDouble("end_level"), 0.0)
    }
}

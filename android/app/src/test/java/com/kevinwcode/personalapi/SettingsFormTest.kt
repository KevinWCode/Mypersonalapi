package com.kevinwcode.personalapi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SettingsFormTest {
    private fun apply(p: Profile, json: String) = SettingsForm.apply(p, JSONObject(json))

    @Test
    fun anEmptyChangeKeepsKevinsSetUp() {
        assertEquals(Profile.KEVIN, apply(Profile.KEVIN, "{}"))
    }

    @Test
    fun oneChangeLeavesEverythingElse() {
        val p = apply(Profile.KEVIN, """{"kcal_target": 2100}""")
        assertEquals(2100.0, p.kcalTarget, 0.0)
        assertEquals(Profile.KEVIN.copy(kcalTarget = 2100.0), p)
    }

    @Test
    fun targetsOutOfRangeAreRefused() {
        listOf("""{"kcal_target": 500}""", """{"protein_target": 1000}""", """{"water_target": 0.1}""", """{"social_cut": 7}""").forEach {
            try { apply(Profile.KEVIN, it); fail(it) } catch (e: SettingsForm.Bad) { }
        }
    }

    @Test
    fun renamedGoalKeepsItsMetricAndHistory() {
        val p = apply(Profile.KEVIN, """{"goal": {"name": "Longest catch", "unit": "yd", "target": 25}}""")
        assertEquals(Profile.CATCH_METRIC, p.goal!!.metric)
        assertEquals(25.0, p.goal!!.target!!, 0.0)
        assertNull(apply(Profile.KEVIN, """{"goal": null}""").goal)
        assertEquals(Profile.GOAL_METRIC, apply(Profile.NEW_TESTER, """{"goal": {"name": "Plank hold", "unit": "seconds", "target": null}}""").goal!!.metric)
    }

    @Test
    fun exercisesKeepTheirKeys() {
        val p = apply(Profile.KEVIN, """{"exercises": ["Clean & press", "Swing", "Snatch"]}""")
        assertEquals(listOf("clean_press", "swing", "snatch"), p.exercises.map { it.key })
    }

    @Test
    fun sessionsAndOptionalParts() {
        val p = apply(Profile.NEW_TESTER, """{"training": "Gym", "sessions": ["Monday", "Thursday"], "off": ["money", "nonsense"]}""")
        assertEquals("Gym", p.training)
        assertEquals(listOf("Monday", "Thursday"), p.sessions)
        assertEquals(setOf("money"), p.off)
        assertEquals(Profile.DEFAULT_SESSIONS, apply(p, """{"sessions": []}""").sessions)
    }

    @Test
    fun theOldFixedSocialRuleCantBePickedAgain() {
        assertNull(apply(Profile.KEVIN, """{"social_cut": null}""").socialCut)
        assertEquals(10, apply(Profile.KEVIN, """{"social_cut": 10}""").socialCut)
        try { apply(Profile.NEW_TESTER, """{"social_cut": null}"""); fail() } catch (e: SettingsForm.Bad) { assertTrue(true) }
    }
}

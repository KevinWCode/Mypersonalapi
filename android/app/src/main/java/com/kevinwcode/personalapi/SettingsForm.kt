package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject

/**
 * The Settings tab on the dashboard saves "Your activities" one change at a time: the page sends only what changed
 * (e.g. {"kcal_target": 2100}) and this applies it to the saved [Profile]. Anything not sent stays as it was, so an
 * existing set-up (Kevin's) keeps every choice it already had. Kept apart from Android so it can be unit tested.
 */
object SettingsForm {
    class Bad(message: String) : Exception(message)

    fun apply(old: Profile, change: JSONObject): Profile {
        var p = old
        if (change.has("training")) p = p.copy(training = change.optString("training", "").trim().take(40))
        if (change.has("sessions")) {
            val list = strings(change.optJSONArray("sessions")).map { it.take(30) }.distinct().take(7)
            p = p.copy(sessions = list.ifEmpty { Profile.DEFAULT_SESSIONS })
        }
        if (change.has("kcal_target")) p = p.copy(kcalTarget = number(change, "kcal_target", 800.0, 6000.0, "a calorie target from 800 to 6000"))
        if (change.has("protein_target")) p = p.copy(proteinTarget = number(change, "protein_target", 20.0, 400.0, "a protein target from 20 to 400 g"))
        if (change.has("water_target")) p = p.copy(waterTarget = number(change, "water_target", 0.5, 8.0, "a water target from 0.5 to 8 litres"))
        if (change.has("social_cut")) {
            val cut = if (change.isNull("social_cut")) null else change.optInt("social_cut", -1)
            if (cut == null && old.socialCut != null) throw Bad("The fixed 30 minutes can't be picked again")
            if (cut != null && cut !in Profile.SOCIAL_CUTS) throw Bad("Pick one of the social media targets")
            p = p.copy(socialCut = cut)
        }
        if (change.has("off")) {
            val known = Profile.ITEMS.map { it.key }.toSet()
            p = p.copy(off = strings(change.optJSONArray("off")).filter { it in known }.toSet())
        }
        if (change.has("goal")) {
            val g = change.optJSONObject("goal")
            val name = g?.optString("name", "")?.trim().orEmpty().take(60)
            p = p.copy(goal = if (name.isEmpty()) null else Profile.Goal(
                name, g!!.optString("unit", "").trim().take(20),
                if (g.isNull("target")) null else g.optDouble("target").takeIf { !it.isNaN() && it > 0 },
                // Keep logging to the same metric, so a renamed goal keeps its history.
                old.goal?.metric ?: Profile.GOAL_METRIC,
            ))
        }
        if (change.has("exercises")) p = p.withExerciseNames(strings(change.optJSONArray("exercises")).map { it.take(40) }.take(12))
        return p
    }

    private fun number(o: JSONObject, key: String, min: Double, max: Double, what: String): Double {
        val v = o.optDouble(key)
        if (v.isNaN() || v < min || v > max) throw Bad("Enter $what")
        return v
    }

    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.optString(it, "").trim() }.filter { it.isNotEmpty() }
}

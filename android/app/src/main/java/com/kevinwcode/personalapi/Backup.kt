package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * The backup file written by "Back up phone data to a file", and reading it back. Version 1 (app 0.21 and earlier)
 * holds the observations only; version 2 adds the meals list. Both restore.
 */
object Backup {
    const val FORMAT = "personal-api-backup"
    const val VERSION = 2

    data class Row(val date: String, val metric: String, val source: String, val value: Double, val unit: String, val updatedAt: String)
    data class MealRow(val id: String, val meal: Food.Meal, val addedAt: String)
    data class Contents(val rows: List<Row>, val meals: List<MealRow>)

    fun write(rows: List<Row>, meals: List<MealRow>, exportedAt: String): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("exported_at", exportedAt)
        .put("observations", JSONArray().apply {
            rows.forEach {
                put(JSONObject().put("date", it.date).put("metric", it.metric).put("source", it.source)
                    .put("value", it.value).put("unit", it.unit).put("updated_at", it.updatedAt))
            }
        })
        .put("meals", JSONArray().apply {
            meals.forEach {
                val m = it.meal
                put(JSONObject().put("id", it.id).put("date", m.date.toString()).put("meal", m.meal).put("kcal", m.kcal)
                    .put("protein_g", m.proteinG).put("carbs_g", m.carbsG).put("fat_g", m.fatG).put("sugar_g", m.sugarG)
                    .put("description", m.description).put("added_at", it.addedAt))
            }
        })
        .toString(1)

    /** Throws IllegalArgumentException with a plain message when the file isn't a backup from this app. */
    fun read(text: String): Contents {
        val doc = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("That file isn't a Personal API backup")
        }
        require(doc.optString("format") == FORMAT) { "That file isn't a Personal API backup" }
        require(doc.optInt("version", 1) <= VERSION) { "That backup is from a newer app version" }
        val obs = doc.getJSONArray("observations")
        val rows = (0 until obs.length()).map {
            val o = obs.getJSONObject(it)
            LocalDate.parse(o.getString("date")) // reject a malformed date before anything is written
            Row(o.getString("date"), o.getString("metric"), o.getString("source"), o.getDouble("value"),
                o.getString("unit"), o.optString("updated_at", ""))
        }
        val mealsJson = doc.optJSONArray("meals") ?: JSONArray()
        val meals = (0 until mealsJson.length()).map {
            val m = mealsJson.getJSONObject(it)
            MealRow(
                m.getString("id"),
                Food.Meal(LocalDate.parse(m.getString("date")), m.getString("meal"), m.getDouble("kcal"), m.getDouble("protein_g"),
                    m.getDouble("carbs_g"), m.getDouble("fat_g"), m.getDouble("sugar_g"), m.getString("description")),
                m.optString("added_at", ""),
            )
        }
        return Contents(rows, meals)
    }
}

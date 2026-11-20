package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate

/**
 * Meals pasted into the app, in the same one-line format your food AI already writes, for example
 * {"date":"2026-11-20","meal":"lunch","kcal":640,"protein_g":42,"carbs_g":55,"fat_g":20,"sugar_g":12,"description":"chicken wrap"}
 * Same rules as the server's app/food.py: meal is breakfast, lunch, dinner or snack; "calories", "sugar" and
 * "sugars_g" are accepted too; a meal without a date goes on today.
 */
object Food {
    val MEALS = listOf("breakfast", "lunch", "dinner", "snack")

    data class Meal(
        val date: LocalDate, val meal: String, val kcal: Double, val proteinG: Double = 0.0, val carbsG: Double = 0.0,
        val fatG: Double = 0.0, val sugarG: Double = 0.0, val description: String = "",
    )

    class BadMeal(message: String) : Exception(message)

    /** One JSON object, a list of them, or one object per line. Throws [BadMeal] with a readable reason. */
    fun parse(text: String, today: LocalDate): List<Meal> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw BadMeal("Paste a meal first")
        // Reads every value in the text in turn, so one per line (or pretty-printed ones back to back) all count.
        val objects = try {
            val tokens = JSONTokener(trimmed)
            val out = mutableListOf<JSONObject>()
            while (tokens.nextClean() != 0.toChar()) {
                tokens.back()
                when (val v = tokens.nextValue()) {
                    is JSONObject -> out += v
                    is JSONArray -> for (i in 0 until v.length()) out += v.getJSONObject(i)
                    else -> throw JSONException("expected a { ... } meal")
                }
            }
            out
        } catch (e: JSONException) {
            throw BadMeal("That isn't the meal format: ${e.message}")
        }
        if (objects.isEmpty()) throw BadMeal("Paste a meal first")
        return objects.map { meal(it, today) }
    }

    private fun meal(o: JSONObject, today: LocalDate): Meal {
        fun num(vararg names: String, max: Double): Double {
            val name = names.firstOrNull { o.has(it) && !o.isNull(it) } ?: return 0.0
            val v = o.optDouble(name, Double.NaN)
            if (v.isNaN() || v < 0 || v > max) throw BadMeal("$name must be a number from 0 to ${max.toInt()}")
            return v
        }
        val mealName = o.optString("meal", "").trim().lowercase().removeSuffix("s")
        if (mealName !in MEALS) throw BadMeal("meal must be breakfast, lunch, dinner or snack")
        if (!(o.has("kcal") && !o.isNull("kcal")) && !(o.has("calories") && !o.isNull("calories"))) throw BadMeal("kcal is missing")
        val date = if (o.has("date") && !o.isNull("date")) {
            try {
                LocalDate.parse(o.getString("date"))
            } catch (e: Exception) {
                throw BadMeal("date must look like 2026-11-20")
            }
        } else today
        var description = if (o.has("description") && !o.isNull("description")) o.get("description").toString() else ""
        if (description.isEmpty()) {
            o.optJSONArray("items")?.let { items -> description = (0 until items.length()).joinToString(", ") { items.get(it).toString() } }
        }
        return Meal(
            date = date, meal = mealName,
            kcal = num("kcal", "calories", max = 5000.0),
            proteinG = num("protein_g", max = 1000.0),
            carbsG = num("carbs_g", max = 1000.0),
            fatG = num("fat_g", max = 1000.0),
            sugarG = num("sugar_g", "sugars_g", "sugar", "sugars", max = 1000.0),
            description = description.take(300),
        )
    }

    /** A day's food.* totals, the same metrics the server writes. */
    fun totals(day: LocalDate, meals: List<Meal>): List<Observation> {
        val m = meals.filter { it.date == day }
        return listOf(
            Observation(day, "food.kcal", m.sumOf { it.kcal }, "kcal"),
            Observation(day, "food.protein_g", m.sumOf { it.proteinG }, "g"),
            Observation(day, "food.carbs_g", m.sumOf { it.carbsG }, "g"),
            Observation(day, "food.fat_g", m.sumOf { it.fatG }, "g"),
            Observation(day, "food.sugar_g", m.sumOf { it.sugarG }, "g"),
        ) + MEALS.map { meal -> Observation(day, "food.$meal.kcal", m.filter { it.meal == meal }.sumOf { it.kcal }, "kcal") }
    }

    fun line(m: Meal): String =
        "${m.meal.replaceFirstChar { it.uppercase() }}: ${m.kcal.toInt()} kcal, ${m.proteinG.toInt()} g protein, " +
            "${m.sugarG.toInt()} g sugar" + if (m.description.isNotEmpty()) " (${m.description})" else ""
}

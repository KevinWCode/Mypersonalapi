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

    /** One button of the meal matrix: a meal at a calorie tier, with its most likely macros. */
    data class Preset(
        val meal: String, val tier: String, val kcal: Double, val proteinG: Double, val carbsG: Double, val fatG: Double,
        val sugarG: Double,
    ) {
        val label get() = "$tier ${kcal.toInt()}"
        fun toMeal(day: LocalDate) = Meal(
            date = day, meal = meal, kcal = kcal, proteinG = proteinG, carbsG = carbsG, fatG = fatG, sugarG = sugarG,
            description = "${tier.lowercase()} ${meal} (estimate)",
        )
    }

    /**
     * The 12-cell meal matrix from Kevin's doc "Most Likely Macronutrient Profiles Across the 12 Meal-Tier Calorie
     * Matrix": UK-modelled protein, available carbs and fat for each meal at a Light, Standard and Heavy calorie tier.
     * Sugar is total sugars (what food labels and the food AI's sugar_g count, and what the HP rule uses) from Kevin's
     * matrix JSON v1.2.0; it also lists free sugars, which aren't used.
     */
    val PRESETS = listOf(
        Preset("breakfast", "Light", 350.0, 15.0, 48.0, 10.0, 14.0),
        Preset("breakfast", "Standard", 550.0, 21.0, 64.0, 22.0, 20.0),
        Preset("breakfast", "Heavy", 850.0, 33.0, 74.0, 45.0, 22.0),
        Preset("lunch", "Light", 450.0, 25.0, 49.0, 16.0, 13.0),
        Preset("lunch", "Standard", 700.0, 28.0, 82.0, 27.0, 20.0),
        Preset("lunch", "Heavy", 1050.0, 36.0, 108.0, 51.0, 28.0),
        Preset("dinner", "Light", 500.0, 36.0, 46.0, 17.0, 9.0),
        Preset("dinner", "Standard", 800.0, 40.0, 88.0, 30.0, 17.0),
        Preset("dinner", "Heavy", 1200.0, 44.0, 122.0, 58.0, 30.0),
        Preset("snack", "Light", 150.0, 6.0, 18.0, 6.0, 10.0),
        Preset("snack", "Standard", 300.0, 7.0, 37.0, 13.0, 21.0),
        Preset("snack", "Heavy", 500.0, 9.0, 60.0, 24.0, 34.0),
    )

    fun line(m: Meal): String =
        "${m.meal.replaceFirstChar { it.uppercase() }}: ${m.kcal.toInt()} kcal, ${m.proteinG.toInt()} g protein, " +
            "${m.sugarG.toInt()} g sugar" + if (m.description.isNotEmpty()) " (${m.description})" else ""
}

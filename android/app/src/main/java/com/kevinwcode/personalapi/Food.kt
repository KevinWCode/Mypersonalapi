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

    /** One choice of the two-tap meal tiles: a meal at a quality, with its most likely calories, macros and sugar. */
    data class Preset(
        val meal: String, val tier: String, val kcal: Double, val proteinG: Double, val carbsG: Double, val fatG: Double,
        val sugarG: Double, val hint: String = "",
    ) {
        val label get() = "$tier ${kcal.toInt()}"
        fun toMeal(day: LocalDate) = Meal(
            date = day, meal = meal, kcal = kcal, proteinG = proteinG, carbsG = carbsG, fatG = fatG, sugarG = sugarG,
            description = "${tier.lowercase()} ${meal} (estimate)",
        )
    }

    /** The four qualities a meal tile opens into, in the order they show. */
    val QUALITIES = listOf("Green", "Amber", "Red", "Protein-rich")

    private val HINTS = mapOf(
        "Green" to "Whole foods, veg, little sugar",
        "Amber" to "A normal mixed meal",
        "Red" to "Takeaway, fried or sweet",
        "Protein-rich" to "Lean meat, fish, eggs, a shake",
    )

    /**
     * Rough defaults for each meal at each quality, so a two-tap meal still moves the calorie, protein and sugar
     * rules (sugar is total sugars, which is what the HP rule counts). Amber is the old Standard tier of Kevin's
     * meal matrix; Green is lighter and low in sugar, Red is the heavy, sugary end, and Protein-rich is a
     * moderate-calorie meal built around protein. Snacks come from Kevin's snack buckets JSON v1.0; its sugar is
     * total sugars like everywhere else (its free sugars figure isn't used).
     */
    val PRESETS = listOf(
        Preset("breakfast", "Green", 400.0, 20.0, 50.0, 12.0, 8.0),
        Preset("breakfast", "Amber", 550.0, 21.0, 64.0, 22.0, 20.0),
        Preset("breakfast", "Red", 850.0, 25.0, 95.0, 42.0, 38.0),
        Preset("breakfast", "Protein-rich", 500.0, 38.0, 40.0, 20.0, 8.0),
        Preset("lunch", "Green", 500.0, 30.0, 55.0, 16.0, 8.0),
        Preset("lunch", "Amber", 700.0, 28.0, 82.0, 27.0, 20.0),
        Preset("lunch", "Red", 1050.0, 34.0, 110.0, 52.0, 32.0),
        Preset("lunch", "Protein-rich", 650.0, 48.0, 55.0, 26.0, 8.0),
        Preset("dinner", "Green", 550.0, 36.0, 55.0, 18.0, 8.0),
        Preset("dinner", "Amber", 800.0, 40.0, 88.0, 30.0, 17.0),
        Preset("dinner", "Red", 1200.0, 42.0, 125.0, 58.0, 30.0),
        Preset("dinner", "Protein-rich", 750.0, 55.0, 60.0, 30.0, 9.0),
        // Snacks: Kevin's snack buckets v1.0 (one standard UK snack portion), with its examples as hints.
        Preset("snack", "Green", 85.0, 2.0, 16.0, 1.0, 9.0, "Apple, banana, rice cakes, veg sticks"),
        Preset("snack", "Amber", 165.0, 5.0, 18.0, 8.0, 6.0, "Almonds, oatcakes, baked crisps, cereal bar"),
        Preset("snack", "Red", 245.0, 3.0, 26.0, 14.0, 17.0, "Chocolate bar, crisps, biscuits, pastry"),
        Preset("snack", "Protein-rich", 160.0, 16.0, 12.0, 4.0, 3.0, "Skyr, protein bar, 2 eggs, jerky"),
    ).map { if (it.hint.isEmpty()) it.copy(hint = HINTS.getValue(it.tier)) else it }

    fun line(m: Meal): String =
        "${m.meal.replaceFirstChar { it.uppercase() }}: ${m.kcal.toInt()} kcal, ${m.proteinG.toInt()} g protein, " +
            "${m.sugarG.toInt()} g sugar" + if (m.description.isNotEmpty()) " (${m.description})" else ""
}

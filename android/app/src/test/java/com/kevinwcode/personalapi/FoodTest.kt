package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class FoodTest {
    private val today = LocalDate.of(2026, 11, 20)

    @Test
    fun parsesTheFoodAiLine() {
        val m = Food.parse(
            """{"date":"2026-11-19","meal":"Lunch","kcal":640,"protein_g":42,"carbs_g":55,"fat_g":20,"sugar_g":12,"description":"chicken wrap"}""",
            today,
        ).single()
        assertEquals(Food.Meal(LocalDate.of(2026, 11, 19), "lunch", 640.0, 42.0, 55.0, 20.0, 12.0, "chicken wrap"), m)
    }

    @Test
    fun acceptsAliasesListsAndOnePerLine() {
        val lines = Food.parse(
            "{\"meal\":\"snacks\",\"calories\":200,\"sugars_g\":18,\"items\":[\"apple\",\"bar\"]}\n{\"meal\":\"dinner\",\"kcal\":700,\"sugar\":5}",
            today,
        )
        assertEquals(listOf("snack", "dinner"), lines.map { it.meal })
        assertEquals(today, lines[0].date)
        assertEquals(200.0, lines[0].kcal, 0.0)
        assertEquals(18.0, lines[0].sugarG, 0.0)
        assertEquals("apple, bar", lines[0].description)
        assertEquals(5.0, lines[1].sugarG, 0.0)
        assertEquals(2, Food.parse("""[{"meal":"breakfast","kcal":300},{"meal":"lunch","kcal":500}]""", today).size)
    }

    @Test
    fun rejectsBadMeals() {
        for (bad in listOf("", "hello", """{"meal":"brunch","kcal":300}""", """{"meal":"lunch"}""",
            """{"meal":"lunch","kcal":-5}""", """{"meal":"lunch","kcal":300,"date":"20/11/2026"}""")) {
            assertThrows(Food.BadMeal::class.java) { Food.parse(bad, today) }
        }
    }

    @Test
    fun totalsIncludeSugarAndPerMeal() {
        val meals = Food.parse(
            """[{"meal":"lunch","kcal":600,"protein_g":40,"sugar_g":10},{"meal":"lunch","kcal":100,"sugar_g":15},
               {"meal":"dinner","kcal":800,"protein_g":50},{"date":"2026-11-19","meal":"snack","kcal":999}]""",
            today,
        )
        val t = Food.totals(today, meals).associate { it.metric to it.value }
        assertEquals(1500.0, t.getValue("food.kcal"), 0.0)
        assertEquals(90.0, t.getValue("food.protein_g"), 0.0)
        assertEquals(25.0, t.getValue("food.sugar_g"), 0.0)
        assertEquals(700.0, t.getValue("food.lunch.kcal"), 0.0)
        assertEquals(0.0, t.getValue("food.snack.kcal"), 0.0)
    }

    @Test
    fun mealTilesLogTheirQualityDefaults() {
        assertEquals(16, Food.PRESETS.size)
        for (meal in Food.MEALS) assertEquals(Food.QUALITIES, Food.PRESETS.filter { it.meal == meal }.map { it.tier })
        val redSnack = Food.PRESETS.single { it.meal == "snack" && it.tier == "Red" }.toMeal(today)
        assertEquals(Food.Meal(today, "snack", 500.0, 6.0, 62.0, 25.0, 40.0, "red snack (estimate)"), redSnack)
        val t = Food.totals(today, Food.PRESETS.filter { it.tier == "Amber" }.map { it.toMeal(today) }).associate { it.metric to it.value }
        assertEquals(2350.0, t.getValue("food.kcal"), 0.0)
        assertEquals(78.0, t.getValue("food.sugar_g"), 0.0)
    }

    @Test
    fun qualitiesRankAsTheyRead() {
        for (meal in Food.MEALS) {
            val q = Food.PRESETS.filter { it.meal == meal }.associateBy { it.tier }
            // Green is the lightest and least sugary, Red the heaviest and most sugary, Protein-rich the most protein.
            assert(q.getValue("Green").kcal < q.getValue("Amber").kcal && q.getValue("Amber").kcal < q.getValue("Red").kcal)
            assert(q.getValue("Green").sugarG < q.getValue("Red").sugarG)
            assert(q.values.maxByOrNull { it.proteinG }!!.tier == "Protein-rich")
            // The macros roughly add up to the calories (4/4/9 kcal per g), within 15%.
            for (p in q.values) {
                val fromMacros = 4 * p.proteinG + 4 * p.carbsG + 9 * p.fatG
                assert(Math.abs(fromMacros - p.kcal) / p.kcal < 0.15) { "${p.meal} ${p.tier}: $fromMacros vs ${p.kcal}" }
            }
        }
    }
}

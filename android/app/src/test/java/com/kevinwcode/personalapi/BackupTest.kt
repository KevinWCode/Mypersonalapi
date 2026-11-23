package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    @Test
    fun readsTheOldAppsBackup() {
        // The shape app 0.21 writes: observations only, no meals.
        val v1 = """{"format":"personal-api-backup","version":1,"exported_at":"2026-11-22T20:00:00Z","observations":[
            {"date":"2026-10-23","metric":"health.steps","source":"health_connect","value":9000,"unit":"count","updated_at":"2026-10-23T21:00:00Z"},
            {"date":"2026-11-20","metric":"perk.con.1.claimed","source":"manual","value":1,"unit":"count","updated_at":"2026-11-20T08:00:00Z"}]}"""
        val b = Backup.read(v1)
        assertEquals(2, b.rows.size)
        assertEquals(Backup.Row("2026-10-23", "health.steps", "health_connect", 9000.0, "count", "2026-10-23T21:00:00Z"), b.rows[0])
        assertEquals(emptyList<Backup.MealRow>(), b.meals)
    }

    @Test
    fun roundTripsRowsAndMeals() {
        val rows = listOf(Backup.Row("2026-11-01", "sleep.total_minutes", "health_connect", 455.0, "minutes", "t1"))
        val meals = listOf(Backup.MealRow("m1", Food.Meal(LocalDate.parse("2026-11-01"), "lunch", 600.0, 40.0, 50.0, 20.0, 8.0, "wrap"), "t2"))
        val back = Backup.read(Backup.write(rows, meals, "now"))
        assertEquals(rows, back.rows)
        assertEquals(meals, back.meals)
    }

    @Test
    fun rejectsOtherFiles() {
        assertThrows(IllegalArgumentException::class.java) { Backup.read("not json") }
        assertThrows(IllegalArgumentException::class.java) { Backup.read("""{"format":"something-else","observations":[]}""") }
        assertThrows(IllegalArgumentException::class.java) { Backup.read("""{"format":"personal-api-backup","version":9,"observations":[]}""") }
    }
}

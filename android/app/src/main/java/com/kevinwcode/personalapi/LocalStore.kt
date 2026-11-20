package com.kevinwcode.personalapi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The app's own database, kept only on this phone. Same layout as the server's observations table: one row per
 * day, metric and source, and saving a day again overwrites it instead of adding a copy.
 */
class LocalStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "personal.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE observations (
                date TEXT NOT NULL,
                metric TEXT NOT NULL,
                source TEXT NOT NULL,
                value REAL NOT NULL,
                unit TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                PRIMARY KEY (date, metric, source)
            )"""
        )
        createMeals(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createMeals(db)
    }

    // Version 2: meals pasted into the app. The day's food.* totals are worked out from these rows.
    private fun createMeals(db: SQLiteDatabase) = db.execSQL(
        """CREATE TABLE meals (
            id TEXT PRIMARY KEY,
            date TEXT NOT NULL,
            meal TEXT NOT NULL,
            kcal REAL NOT NULL,
            protein_g REAL NOT NULL,
            carbs_g REAL NOT NULL,
            fat_g REAL NOT NULL,
            sugar_g REAL NOT NULL,
            description TEXT NOT NULL,
            added_at TEXT NOT NULL
        )"""
    )

    /** Stores meals and refreshes those days' food totals. Returns the new meals' ids. */
    fun addMeals(meals: List<Food.Meal>): List<String> {
        val now = Instant.now().toString()
        val db = writableDatabase
        val ids = meals.map { UUID.randomUUID().toString() }
        for ((m, id) in meals.zip(ids)) {
            db.insert("meals", null, ContentValues().apply {
                put("id", id)
                put("date", m.date.toString())
                put("meal", m.meal)
                put("kcal", m.kcal)
                put("protein_g", m.proteinG)
                put("carbs_g", m.carbsG)
                put("fat_g", m.fatG)
                put("sugar_g", m.sugarG)
                put("description", m.description)
                put("added_at", now)
            })
        }
        meals.map { it.date }.toSet().forEach { refreshFood(it) }
        return ids
    }

    /** Returns false when there's no such meal. */
    fun removeMeal(id: String): Boolean {
        val date = readableDatabase.rawQuery("SELECT date FROM meals WHERE id = ?", arrayOf(id))
            .use { c -> if (c.moveToFirst()) LocalDate.parse(c.getString(0)) else null } ?: return false
        writableDatabase.delete("meals", "id = ?", arrayOf(id))
        refreshFood(date)
        return true
    }

    /** Most recent save time per source, like the server's /status. */
    fun lastUpdates(): Map<String, String> =
        readableDatabase.rawQuery("SELECT source, MAX(updated_at) FROM observations GROUP BY source", null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
        }

    /** One day's meals with their ids, in the order they were added. */
    fun meals(day: LocalDate): List<Pair<String, Food.Meal>> =
        readableDatabase.rawQuery(
            "SELECT id, meal, kcal, protein_g, carbs_g, fat_g, sugar_g, description FROM meals WHERE date = ? ORDER BY added_at",
            arrayOf(day.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    c.getString(0) to Food.Meal(day, c.getString(1), c.getDouble(2), c.getDouble(3), c.getDouble(4),
                        c.getDouble(5), c.getDouble(6), c.getString(7))
                )
            }
        }

    /** The food totals saved for a day, to send to the PC. */
    fun foodTotals(day: LocalDate): List<Observation> = Food.totals(day, meals(day).map { it.second })

    private fun refreshFood(day: LocalDate) = save(FOOD_SOURCE, foodTotals(day))

    /**
     * Saves the observations from one source. Like the server, a re-sent day and metric replaces what was stored.
     */
    fun save(source: String, observations: List<Observation>): Int {
        val db = writableDatabase
        val now = Instant.now().toString()
        db.beginTransaction()
        try {
            for (o in observations) {
                db.insertWithOnConflict("observations", null, ContentValues().apply {
                    put("date", o.date.toString())
                    put("metric", o.metric)
                    put("source", source)
                    put("value", o.value)
                    put("unit", o.unit)
                    put("updated_at", now)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return observations.size
    }

    /**
     * Every day's metrics, ready for the game. A day copied from the PC uses the PC's totals (they already include
     * what the phone sent, plus dashboard entries); any other day is built from the phone's own rows.
     */
    fun allDays(): Map<LocalDate, Map<String, Double>> {
        val rows = mutableListOf<Triple<String, String, Pair<String, Double>>>()
        readableDatabase.rawQuery("SELECT date, source, metric, value FROM observations", null).use { c ->
            while (c.moveToNext()) rows += Triple(c.getString(0), c.getString(1), c.getString(2) to c.getDouble(3))
        }
        return DayMetrics.mergeDays(rows, PC_SOURCE)
    }

    /** Replaces the PC copy for start..end with [days]. */
    fun replacePcDays(start: LocalDate, end: LocalDate, days: Map<LocalDate, Map<String, Double>>) {
        val db = writableDatabase
        val now = Instant.now().toString()
        db.beginTransaction()
        try {
            db.delete("observations", "source = ? AND date BETWEEN ? AND ?", arrayOf(PC_SOURCE, start.toString(), end.toString()))
            for ((date, metrics) in days) for ((metric, value) in metrics) {
                db.insertWithOnConflict("observations", null, ContentValues().apply {
                    put("date", date.toString())
                    put("metric", metric)
                    put("source", PC_SOURCE)
                    put("value", value)
                    put("unit", "")
                    put("updated_at", now)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** How many days have any data, and the first of them. */
    fun span(): Pair<Int, String?> =
        readableDatabase.rawQuery("SELECT COUNT(DISTINCT date), MIN(date) FROM observations", null)
            .use { c -> c.moveToFirst(); c.getInt(0) to c.getString(1) }

    /** Writes every row as JSON, for a backup the user keeps wherever they like. */
    fun exportJson(out: OutputStream): Int {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            "SELECT date, metric, source, value, unit, updated_at FROM observations ORDER BY date, metric", null
        ).use { c ->
            while (c.moveToNext()) rows.put(
                JSONObject()
                    .put("date", c.getString(0))
                    .put("metric", c.getString(1))
                    .put("source", c.getString(2))
                    .put("value", c.getDouble(3))
                    .put("unit", c.getString(4))
                    .put("updated_at", c.getString(5))
            )
        }
        val doc = JSONObject()
            .put("format", "personal-api-backup")
            .put("version", 1)
            .put("exported_at", Instant.now().toString())
            .put("observations", rows)
        out.write(doc.toString(1).toByteArray())
        return rows.length()
    }

    companion object {
        const val PC_SOURCE = "pc"
        const val FOOD_SOURCE = "food"

        @Volatile private var instance: LocalStore? = null
        fun get(context: Context): LocalStore =
            instance ?: synchronized(this) { instance ?: LocalStore(context).also { instance = it } }
    }
}

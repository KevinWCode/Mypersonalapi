package com.kevinwcode.personalapi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The app's own database, kept only on this phone. Same layout as the server's observations table: one row per
 * day, metric and source, and saving a day again overwrites it instead of adding a copy.
 */
class LocalStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "personal.db", null, 3) {

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
        createTrading(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createMeals(db)
        if (oldVersion < 3) createTrading(db)
    }

    // Version 3: Alpaca fills and open positions, read by the phone itself.
    private fun createTrading(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE trades (
                id TEXT PRIMARY KEY,
                filled_at TEXT NOT NULL,
                symbol TEXT NOT NULL,
                side TEXT NOT NULL,
                qty REAL NOT NULL,
                price REAL NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE positions (
                symbol TEXT PRIMARY KEY,
                qty REAL NOT NULL,
                market_value REAL NOT NULL,
                unrealized_pl REAL NOT NULL,
                updated_at TEXT NOT NULL
            )"""
        )
    }

    /** Saves new fills (a fill seen again is overwritten) and replaces the positions, which are a snapshot. */
    fun saveTrading(fills: List<Fill>, positions: List<Position>) {
        val db = writableDatabase
        val now = Instant.now().toString()
        db.beginTransaction()
        try {
            for (f in fills) db.insertWithOnConflict("trades", null, ContentValues().apply {
                put("id", f.id)
                put("filled_at", f.filledAt)
                put("symbol", f.symbol)
                put("side", f.side)
                put("qty", f.qty)
                put("price", f.price)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.delete("positions", null, null)
            for (p in positions) db.insertWithOnConflict("positions", null, ContentValues().apply {
                put("symbol", p.symbol)
                put("qty", p.qty)
                put("market_value", p.marketValue)
                put("unrealized_pl", p.unrealizedPl)
                put("updated_at", now)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** The latest fills and the biggest positions, for the dashboard and widget. */
    fun trading(): Trading {
        val fills = readableDatabase.rawQuery(
            "SELECT id, filled_at, symbol, side, qty, price FROM trades ORDER BY filled_at DESC LIMIT 200", null
        ).use { c ->
            buildList { while (c.moveToNext()) add(Fill(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getDouble(4), c.getDouble(5))) }
        }
        val positions = readableDatabase.rawQuery("SELECT symbol, qty, market_value, unrealized_pl FROM positions", null).use { c ->
            buildList { while (c.moveToNext()) add(Position(c.getString(0), c.getDouble(1), c.getDouble(2), c.getDouble(3))) }
        }
        return Alpaca.trading(fills, positions)
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
        return DayMetrics.mergeDays(rows, PC_SOURCE, FOOD_SOURCE)
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

    /** Writes every row and meal as JSON ([Backup]), for a backup the user keeps wherever they like. */
    fun exportJson(out: OutputStream): Int {
        val rows = readableDatabase.rawQuery(
            "SELECT date, metric, source, value, unit, updated_at FROM observations ORDER BY date, metric", null
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Backup.Row(c.getString(0), c.getString(1), c.getString(2), c.getDouble(3),
                    c.getString(4), c.getString(5)))
            }
        }
        val meals = readableDatabase.rawQuery(
            "SELECT id, date, meal, kcal, protein_g, carbs_g, fat_g, sugar_g, description, added_at FROM meals ORDER BY date, added_at",
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Backup.MealRow(c.getString(0),
                    Food.Meal(LocalDate.parse(c.getString(1)), c.getString(2), c.getDouble(3), c.getDouble(4), c.getDouble(5),
                        c.getDouble(6), c.getDouble(7), c.getString(8)),
                    c.getString(9)))
            }
        }
        out.write(Backup.write(rows, meals, Instant.now().toString()).toByteArray())
        return rows.size
    }

    /**
     * Restores a backup file. Rows in the backup replace the same day, metric and source here; anything else on this
     * phone is kept. Returns how many entries were restored. Nothing is written if the file can't be read.
     */
    fun importJson(input: InputStream): Int {
        val backup = Backup.read(input.bufferedReader().readText())
        val now = Instant.now().toString()
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (r in backup.rows) db.insertWithOnConflict("observations", null, ContentValues().apply {
                put("date", r.date)
                put("metric", r.metric)
                put("source", r.source)
                put("value", r.value)
                put("unit", r.unit)
                put("updated_at", r.updatedAt.ifEmpty { now })
            }, SQLiteDatabase.CONFLICT_REPLACE)
            for (m in backup.meals) db.insertWithOnConflict("meals", null, ContentValues().apply {
                put("id", m.id)
                put("date", m.meal.date.toString())
                put("meal", m.meal.meal)
                put("kcal", m.meal.kcal)
                put("protein_g", m.meal.proteinG)
                put("carbs_g", m.meal.carbsG)
                put("fat_g", m.meal.fatG)
                put("sugar_g", m.meal.sugarG)
                put("description", m.meal.description)
                put("added_at", m.addedAt.ifEmpty { now })
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return backup.rows.size
    }

    companion object {
        const val PC_SOURCE = "pc"
        const val FOOD_SOURCE = "food"

        @Volatile private var instance: LocalStore? = null
        fun get(context: Context): LocalStore =
            instance ?: synchronized(this) { instance ?: LocalStore(context).also { instance = it } }
    }
}

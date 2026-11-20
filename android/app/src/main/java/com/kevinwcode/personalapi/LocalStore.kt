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

/**
 * The app's own database, kept only on this phone. Same layout as the server's observations table: one row per
 * day, metric and source, and saving a day again overwrites it instead of adding a copy.
 */
class LocalStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "personal.db", null, 1) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

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

    /** All rows for one day, as metric and value. */
    fun day(date: LocalDate): List<Pair<String, Double>> =
        readableDatabase.rawQuery("SELECT metric, value FROM observations WHERE date = ?", arrayOf(date.toString()))
            .use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getDouble(1)) } }

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
        @Volatile private var instance: LocalStore? = null
        fun get(context: Context): LocalStore =
            instance ?: synchronized(this) { instance ?: LocalStore(context).also { instance = it } }
    }
}

package com.kevinwcode.personalapi

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Copies day totals from the PC server's GET /days into the phone's database, while the PC still has things the
 * phone doesn't (history from before the phone kept its own data, and what's logged on the dashboard). Goes away
 * once the server is switched off.
 */
object PcHistory {
    fun fetch(settings: Settings, start: LocalDate, end: LocalDate): Map<LocalDate, Map<String, Double>> {
        val conn = URL("${settings.serverUrl}/days?start=$start&end=$end").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            val code = conn.responseCode
            if (code != 200) throw IllegalStateException("Server said $code")
            val body = JSONObject(conn.inputStream.bufferedReader().readText())
            val days = body.getJSONArray("days")
            return (0 until days.length()).associate { i ->
                val d = days.getJSONObject(i)
                val metrics = d.getJSONObject("metrics")
                LocalDate.parse(d.getString("date")) to metrics.keys().asSequence().associateWith { metrics.getDouble(it) }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Copies [start] to [end] from the PC and returns how many days came back. */
    fun copy(settings: Settings, store: LocalStore, start: LocalDate, end: LocalDate): Int {
        val days = fetch(settings, start, end)
        store.replacePcDays(start, end, days)
        return days.size
    }
}

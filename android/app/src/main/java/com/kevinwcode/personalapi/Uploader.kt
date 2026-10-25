package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Sends observations to the server's POST /ingest. Re-sending a day overwrites it on the server. */
object Uploader {
    fun send(settings: Settings, source: String, observations: List<Observation>): Int {
        if (observations.isEmpty()) return 0
        require(settings.token.isNotEmpty()) { "Set the upload token first" }
        val body = JSONObject()
            .put("source", source)
            .put("observations", JSONArray(observations.map {
                JSONObject()
                    .put("date", it.date.toString())
                    .put("metric", it.metric)
                    .put("value", it.value)
                    .put("unit", it.unit)
            }))
        val conn = URL("${settings.serverUrl}/ingest").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${settings.token}")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                throw IllegalStateException("Server said $code $err")
            }
            return observations.size
        } finally {
            conn.disconnect()
        }
    }
}

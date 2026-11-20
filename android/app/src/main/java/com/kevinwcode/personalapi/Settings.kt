package com.kevinwcode.personalapi

import android.content.Context

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", DEFAULT_SERVER)!!
        set(v) = prefs.edit().putString("server_url", v.trim().trimEnd('/')).apply()

    var token: String
        get() = prefs.getString("token", "")!!
        set(v) = prefs.edit().putString("token", v.trim()).apply()

    /** Off by default for a phone with no token; on for Kevin's phone, which already has one. */
    var serverEnabled: Boolean
        get() = prefs.getBoolean("server_enabled", token.isNotEmpty())
        set(v) = prefs.edit().putBoolean("server_enabled", v).apply()

    var lastSync: String
        get() = prefs.getString("last_sync", "never")!!
        set(v) = prefs.edit().putString("last_sync", v).apply()

    companion object {
        // Kevin's PC on Tailscale
        const val DEFAULT_SERVER = "http://100.80.206.11:8000"
    }
}

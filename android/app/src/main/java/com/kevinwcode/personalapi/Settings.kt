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

    // Keys for GitHub and Alpaca live in their own file, which is left out of Android's cloud backup.
    private val keys = context.getSharedPreferences(KEYS_FILE, Context.MODE_PRIVATE)

    /** A GitHub token that can read your contributions (the GITHUB_TOKEN from the PC's .env works). Empty = off. */
    var githubToken: String
        get() = keys.getString("github_token", "")!!
        set(v) = keys.edit().putString("github_token", v.trim()).apply()

    /** Alpaca key id and secret pairs, as typed: "KEY SECRET KEY2 SECRET2". Empty = off. */
    var alpacaKeys: String
        get() = keys.getString("alpaca_keys", "")!!
        set(v) = keys.edit().putString("alpaca_keys", v.trim()).apply()

    val alpacaAccounts: List<Alpaca.Account> get() = Alpaca.parseAccounts(alpacaKeys)

    /** Off = paper accounts (the default, like the server), on = the live account. */
    var alpacaLive: Boolean
        get() = keys.getBoolean("alpaca_live", false)
        set(v) = keys.edit().putBoolean("alpaca_live", v).apply()

    val alpacaUrl: String get() = if (alpacaLive) Alpaca.LIVE_URL else Alpaca.PAPER_URL

    companion object {
        const val KEYS_FILE = "keys"

        // Kevin's PC on Tailscale
        const val DEFAULT_SERVER = "http://100.80.206.11:8000"
    }
}

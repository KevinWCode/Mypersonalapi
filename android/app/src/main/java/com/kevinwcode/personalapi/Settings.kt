package com.kevinwcode.personalapi

import android.content.Context
import java.time.LocalDate

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", "")!!
        set(v) = prefs.edit().putString("server_url", v.trim().trimEnd('/')).apply()

    var token: String
        get() = prefs.getString("token", "")!!
        set(v) = prefs.edit().putString("token", v.trim()).apply()

    /** Off by default for a phone with no token; on for Kevin's phone, which already has one. */
    var serverEnabled: Boolean
        get() = prefs.getBoolean("server_enabled", token.isNotEmpty())
        set(v) = prefs.edit().putBoolean("server_enabled", v).apply()

    /**
     * The day this phone's game starts: XP, levels and vital bars count from here. Set once, on the first launch:
     * the default start if the phone already holds days from before it (Kevin's copied history), else the first
     * day this phone has data, or today.
     */
    fun gameStart(firstDataDay: String?): LocalDate {
        prefs.getString("game_start", null)?.let { return LocalDate.parse(it) }
        val first = firstDataDay?.let(LocalDate::parse)
        val start = when {
            first == null -> LocalDate.now()
            first <= Game.DEFAULT_GAME_START -> Game.DEFAULT_GAME_START
            else -> first
        }
        prefs.edit().putString("game_start", start.toString()).apply()
        return start
    }

    /** Forgets the game start, so the next read works it out again from the data (used after a restore). */
    fun resetGameStart() = prefs.edit().remove("game_start").apply()

    /** True until the first-run setup screen has been shown once. */
    var setupPending: Boolean
        get() = !prefs.getBoolean("setup_shown", false)
        set(v) = prefs.edit().putBoolean("setup_shown", !v).apply()

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

    }
}

package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * What this person actually does and tracks, picked on the first-run setup screen and changed in Settings.
 * The game only scores what is switched on: a switched-off activity leaves Daily quests and the day's XP total,
 * and its data is ignored by the vital bars, so not doing it never counts as a miss.
 *
 * [ALL] (kettlebell, everything on) is how the app worked before this setting existed, so an existing install keeps
 * scoring exactly as it did.
 */
data class Profile(val strength: String = "kettlebell", val off: Set<String> = emptySet()) {

    /** One thing a person can switch off: the XP lines it owns and the day metrics it feeds. */
    data class Item(val key: String, val label: String, val about: String, val lines: List<String>, val metrics: List<String>)

    fun isOn(item: String) = item !in off

    val kettlebell get() = strength == "kettlebell"

    /** Name of the strength line and the "done" tile, e.g. "Gym session". */
    val strengthLabel: String get() = STRENGTH_TYPES[strength]?.second ?: "Strength session"

    /** Whether the game scores this XP line ("sleep", "kettlebell", "social"...). */
    fun lineOn(key: String): Boolean {
        if (key == "kettlebell") return strength != "none"
        return ITEMS.none { it.key in off && key in it.lines }
    }

    /** A day's metrics without the switched-off ones, so neither XP nor the vital bars see them. */
    fun strip(m: Map<String, Double>): Map<String, Double> {
        if (this == ALL) return m
        val dropped = ITEMS.filter { it.key in off }.flatMap { it.metrics } +
            if (strength == "none") STRENGTH_METRICS else emptyList()
        val out = m.filterKeys { k -> dropped.none { k == it || (it.endsWith(".") && k.startsWith(it)) } }
        // Social media off: no "calm day" bonus or streak either, since nothing is being measured.
        return if ("social" in off && "screen.total_minutes" in m) out + (SOCIAL_OFF to 1.0) else out
    }

    fun stripDays(days: Map<LocalDate, Map<String, Double>>): Map<LocalDate, Map<String, Double>> =
        if (this == ALL) days else days.mapValues { strip(it.value) }

    fun toJson(): JSONObject = JSONObject()
        .put("strength", strength).put("strength_label", strengthLabel)
        .put("off", JSONArray(off.sorted()))

    /** Saved form: "strength|item,item". */
    fun encode(): String = strength + "|" + off.sorted().joinToString(",")

    companion object {
        val ALL = Profile()

        /** A new tester starts with the parts only Kevin uses switched off, and picks a strength type themselves. */
        val NEW_TESTER = Profile(strength = "none", off = setOf("catch", "github", "money"))

        /** Key to (choice shown in setup, name of its XP line and Workout done tile). */
        val STRENGTH_TYPES: Map<String, Pair<String, String>> = linkedMapOf(
            "kettlebell" to ("Kettlebells" to "Kettlebell"),
            "gym" to ("Gym weights" to "Gym session"),
            "bodyweight" to ("Bodyweight (push-ups, pull-ups...)" to "Bodyweight session"),
            "cardio" to ("Running, cycling or other cardio" to "Cardio session"),
            "sport" to ("A sport or class" to "Training session"),
            "none" to ("None for now" to "Strength session"),
        )

        /** A strength session done by hand, for anyone not logging kettlebell moves. */
        const val STRENGTH_DONE = "strength.done"
        val STRENGTH_METRICS = listOf("kb.", STRENGTH_DONE)

        const val SOCIAL_OFF = "profile.social_off"

        val ITEMS = listOf(
            Item("food", "Food and sugar", "Meal tiles, calories, protein and the sugar rules", emptyList(), listOf("food.")),
            Item("water", "Water", "+250 ml taps and their Stamina and Focus boost", emptyList(), listOf("water.")),
            Item("coffee", "Coffee", "Coffee taps, caffeine and their Focus and HP effects", emptyList(), listOf("coffee.")),
            Item("reading", "Reading", "Reading time (WIS)", listOf("reading"), listOf("reading.")),
            Item("study", "Study", "Study time (INT)", listOf("study"), listOf("study.")),
            Item("journal", "Journal", "Journal entries (WIS)", listOf("journal"), listOf("journal.")),
            Item("social", "Social media", "TikTok, Instagram, Facebook and Reddit minutes (PER)", listOf("social"),
                listOf("screen.social_media_minutes", "screen.tiktok_minutes", "screen.instagram_minutes",
                    "screen.facebook_minutes", "screen.reddit_minutes")),
            Item("youtube", "YouTube", "YouTube minutes (PER)", listOf("youtube"), listOf("screen.youtube_minutes")),
            Item("connect", "Calls and messages", "Time on calls and messaging apps (CHA)", listOf("connect"),
                listOf("screen.communication_minutes")),
            Item("github", "GitHub commits", "Needs a GitHub token in Settings (INT)", listOf("github"), listOf("github.")),
            Item("catch", "Flag football catch drill", "Longest reliable catch, logged in Settings (STR)", listOf("catch"),
                listOf("football.")),
            Item("money", "Money tab", "Alpaca trading accounts", emptyList(), emptyList()),
        )

        fun decode(saved: String?): Profile? {
            if (saved.isNullOrEmpty()) return null
            val strength = saved.substringBefore("|").takeIf { it in STRENGTH_TYPES } ?: "none"
            val known = ITEMS.map { it.key }.toSet()
            val off = saved.substringAfter("|", "").split(",").filter { it in known }.toSet()
            return Profile(strength, off)
        }
    }
}

package com.kevinwcode.personalapi

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Each person's own activities, from the "Your activities" setup (first launch, then Settings):
 *
 *  - their training (e.g. "Kettlebell", "Spin class"), its weekly sessions (e.g. "Monday", "Tuesday", "Friday" or
 *    "Session 1".."Session 4") and its exercises (e.g. "Swing", "Clean"). The STR training line, the Workout done tile
 *    and the "<training> this week" tracker (T12) all follow these. Blank training = no training line.
 *  - something they'd like to improve, with a unit and a target (Kevin's is the longest reliable catch, 20 yd).
 *    It replaces the catch drill line and chart (T14). Blank = no line.
 *  - optional parts they can switch off: GitHub commits and the Money tab. Everything else is tracked for everyone.
 *
 * A switched-off or blank part leaves Daily quests and the day's max XP, so not doing it never counts as a miss.
 * [KEVIN] is how the app worked before this setting existed, so an existing install keeps scoring as it did.
 */
data class Profile(
    val training: String = "",
    val sessions: List<String> = DEFAULT_SESSIONS,
    val exercises: List<Exercise> = emptyList(),
    val goal: Goal? = null,
    val off: Set<String> = emptySet(),
    /** Daily calorie target: the Fuel card's goal, and (+150) the most the HP calorie rule allows. */
    val kcalTarget: Double = DEFAULT_KCAL,
    /** Daily protein target in grams: the Fuel card goal, and (x 0.9) what the HP protein rule needs. */
    val proteinTarget: Double = DEFAULT_PROTEIN,
    /** Daily water target in litres, for the Fuel card. */
    val waterTarget: Double = DEFAULT_WATER,
) {
    /** One exercise of the training. [key] names its metrics (kb.<key>.done, kb.<key>.sets), so it never changes. */
    data class Exercise(val key: String, val name: String)

    /** Something to improve: logged as the day's best ([metric]), higher is better. */
    data class Goal(val name: String, val unit: String, val target: Double?, val metric: String = GOAL_METRIC)

    data class Item(val key: String, val label: String, val about: String, val lines: List<String>, val metrics: List<String>)

    fun isOn(item: String) = item !in off

    val hasTraining get() = training.isNotBlank()

    /** The rows of the weekly tracker and what Workout done ticks. With no exercises listed, one row for the training. */
    val trackedExercises: List<Exercise>
        get() = exercises.ifEmpty { if (hasTraining) listOf(Exercise(slug(training), training)) else emptyList() }

    /** Whether the game scores this XP line ("sleep", "kettlebell", "catch"...). */
    fun lineOn(key: String): Boolean = when (key) {
        "kettlebell" -> hasTraining
        "catch" -> goal != null
        else -> ITEMS.none { it.key in off && key in it.lines }
    }

    /** A day's metrics without the switched-off ones, so neither XP nor the vital bars see them. */
    fun strip(m: Map<String, Double>): Map<String, Double> {
        val dropped = ITEMS.filter { it.key in off }.flatMap { it.metrics }
        if (dropped.isEmpty()) return m
        return m.filterKeys { k -> dropped.none { k == it || (it.endsWith(".") && k.startsWith(it)) } }
    }

    fun stripDays(days: Map<LocalDate, Map<String, Double>>): Map<LocalDate, Map<String, Double>> =
        if (off.isEmpty()) days else days.mapValues { strip(it.value) }

    fun toJson(): JSONObject = JSONObject()
        .put("training", training)
        .put("sessions", JSONArray(sessions))
        .put("exercises", JSONArray(exercises.map { JSONObject().put("key", it.key).put("name", it.name) }))
        .put("tracked", JSONArray(trackedExercises.map { JSONObject().put("key", it.key).put("name", it.name) }))
        .put("goal", goal?.let {
            JSONObject().put("name", it.name).put("unit", it.unit).put("target", it.target ?: JSONObject.NULL).put("metric", it.metric)
        } ?: JSONObject.NULL)
        .put("off", JSONArray(off.sorted()))
        .put("kcal_target", kcalTarget)
        .put("protein_target", proteinTarget)
        .put("water_target", waterTarget)

    fun encode(): String = toJson().toString()

    /**
     * New exercise names typed in Settings, keeping the key of any exercise that keeps its name (so its history stays)
     * and giving new ones a key of their own.
     */
    fun withExerciseNames(names: List<String>): Profile {
        val used = mutableSetOf<String>()
        val list = names.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.map { name ->
            val kept = exercises.firstOrNull { it.name.equals(name, ignoreCase = true) }?.key
            // Keys are letters and _ only (what the dashboard may log), so a clash gets _b, _c...
            val base = slug(name).take(27)
            val key = kept ?: (listOf(base) + ('b'..'z').map { "${base}_$it" })
                .first { it !in used && exercises.none { e -> e.key == it && !e.name.equals(name, ignoreCase = true) } }
            used += key
            Exercise(key, name)
        }
        return copy(exercises = list)
    }

    companion object {
        const val DEFAULT_KCAL = 2000.0
        const val DEFAULT_PROTEIN = 150.0
        const val DEFAULT_WATER = 3.0
        val DEFAULT_SESSIONS = listOf("Session 1", "Session 2", "Session 3")
        const val GOAL_METRIC = "improve.best"
        const val CATCH_METRIC = "football.max_catch_yards"

        /** Kevin's set-up, and what an install from before this setting existed gets. */
        val KEVIN = Profile(
            training = "Kettlebell",
            sessions = listOf("Monday", "Tuesday", "Friday"),
            exercises = listOf(Exercise("swing", "Swing"), Exercise("clean", "Clean"), Exercise("clean_press", "Clean & press")),
            goal = Goal("Longest reliable catch", "yd", 20.0, CATCH_METRIC),
        )

        /** A new tester picks their own training and goal; GitHub and Money start off. */
        val NEW_TESTER = Profile(off = setOf("github", "money"))

        /** Examples shown under "Something you'd like to improve". */
        const val GOAL_EXAMPLES = "Longest reliable catch (yd), push-ups in one set, plank hold (seconds), " +
            "heaviest deadlift (kg), longest run (km), pages read in one sitting"

        /** The only parts that can be switched off for now. */
        val ITEMS = listOf(
            Item("github", "GitHub commits", "Needs a GitHub token in Settings (INT)", listOf("github"), listOf("github.")),
            Item("money", "Money tab", "Alpaca trading accounts", emptyList(), emptyList()),
        )

        /** "Clean & press" to "clean_press": what the dashboard may log (letters and _, up to 30). */
        fun slug(name: String): String =
            name.lowercase().replace(Regex("[^a-z]+"), "_").trim('_').take(30).trimEnd('_').ifEmpty { "exercise" }

        fun splitList(text: String): List<String> = text.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }

        fun decode(saved: String?): Profile? {
            if (saved.isNullOrEmpty()) return null
            return try {
                val o = JSONObject(saved)
                fun strings(name: String) = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                val ex = o.optJSONArray("exercises")?.let { a ->
                    (0 until a.length()).map { a.getJSONObject(it) }.map { Exercise(it.getString("key"), it.getString("name")) }
                } ?: emptyList()
                val g = o.optJSONObject("goal")?.let {
                    Goal(it.getString("name"), it.optString("unit", ""), if (it.isNull("target")) null else it.optDouble("target"),
                        it.optString("metric", GOAL_METRIC))
                }
                val known = ITEMS.map { it.key }.toSet()
                Profile(o.optString("training", ""), strings("sessions").ifEmpty { DEFAULT_SESSIONS }, ex, g,
                    strings("off").filter { it in known }.toSet(), o.optDouble("kcal_target", DEFAULT_KCAL),
                    o.optDouble("protein_target", DEFAULT_PROTEIN), o.optDouble("water_target", DEFAULT_WATER))
            } catch (e: Exception) {
                null
            }
        }
    }
}

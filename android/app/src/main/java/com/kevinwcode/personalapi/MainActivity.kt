package com.kevinwcode.personalapi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : AppCompatActivity() {
    private lateinit var settings: Settings
    private lateinit var manual: ManualLog
    private lateinit var status: TextView
    private lateinit var root: LinearLayout

    private val requestHealth =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }

    // Lets the user pick where the backup file goes (Downloads, Google Drive, ...). The app never sends it anywhere itself.
    private val saveBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)!!.use { LocalStore.get(this@MainActivity).exportJson(it) }
                }
            }
            toast(result.fold({ "Backed up $it entries" }, { "Backup failed: ${it.message}" }))
        }
    }

    // Restores a backup file (from this app or the 0.21 app). The game start is worked out again, so restored history counts.
    private val restoreBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val n = contentResolver.openInputStream(uri)!!.use { LocalStore.get(this@MainActivity).importJson(it) }
                    settings.resetGameStart()
                    n
                }
            }
            toast(result.fold({ "Restored $it entries" }, { "Restore failed: ${it.message}" }))
            if (result.isSuccess) refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        manual = ManualLog(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 48, 48, 48) }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        // Android 15 draws apps under the status and navigation bars, so pad the content clear of them
        // (and of the keyboard while typing).
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        root.addView(TextView(this).apply {
            text = "Settings"
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        })

        status = TextView(this).apply {
            textSize = 18f
            setLineSpacing(0f, 1.2f)
            setPadding(0, 0, 0, 24)
        }.also { root.addView(it) }

        root.addView(TextView(this).apply {
            text = "The dashboard is the app's main screen. Food, quick log, kettlebell ticks and perk claims are logged there."
            setPadding(0, 0, 0, 8)
        })
        if (intent.getBooleanExtra(EXTRA_WELCOME, false)) root.addView(TextView(this).apply {
            text = "Welcome! To get started:\n" +
                "1. Set up your activities below: your training, its sessions and exercises, and something you would like to improve.\n" +
                "2. Allow Health Connect access (sleep, steps, heart rate and workouts from your watch or phone).\n" +
                "3. Allow usage access, so screen time and social media minutes can be counted.\n" +
                "4. Keep syncing in the background.\n" +
                "GitHub, Alpaca and the PC server are optional. Everything stays on this phone."
            textSize = 16f
            setLineSpacing(0f, 1.2f)
            setPadding(0, 16, 0, 8)
        })
        button("Back to the dashboard") { finish() }

        activities()

        heading("Sync and backup")
        button("Sync now") { syncNow() }
        button("Back up phone data to a file") { saveBackup.launch("personal-api-backup-${LocalDate.now()}.json") }
        button("Restore from a backup file") { restoreBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }

        heading("GitHub and Alpaca")
        root.addView(TextView(this).apply {
            text = "Read straight from GitHub and Alpaca on each sync. The keys stay on this phone and are left out of cloud backups. " +
                "Alpaca is read only: the app can't place trades."
        })
        val github = field("GitHub token (GITHUB_TOKEN from the PC's .env)", settings.githubToken, secret = true)
        val alpaca = field("Alpaca keys: KEY_ID SECRET, then the next account's pair", settings.alpacaKeys, secret = true)
        val live = CheckBox(this).apply {
            text = "Alpaca live account (off = paper accounts)"
            isChecked = settings.alpacaLive
        }.also { root.addView(it) }
        button("Save keys and sync") {
            val typed = alpaca.text.toString()
            val words = typed.split(Regex("[\\s,:;]+")).count { it.isNotEmpty() }
            if (words % 2 != 0) return@button toast("Alpaca keys come in pairs: a key id, then its secret")
            settings.githubToken = github.text.toString()
            settings.alpacaKeys = typed
            settings.alpacaLive = live.isChecked
            toast("Saved: GitHub ${if (settings.githubToken.isEmpty()) "off" else "on"}, ${settings.alpacaAccounts.size} Alpaca account(s)")
            syncNow()
        }

        heading("PC server (optional)")
        button("Open PC dashboard") {
            if (settings.serverUrl.isEmpty()) toast("Enter the server address below first")
            else startActivity(OpenDashboardActivity.browserIntent(settings))
        }

        root.addView(CheckBox(this).apply {
            text = "Also send my data to my PC server"
            isChecked = settings.serverEnabled
            setOnCheckedChangeListener { _, on ->
                settings.serverEnabled = on
                // Without the PC, recent days go back to the phone's own data instead of the last copy from the PC.
                if (!on) lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val today = LocalDate.now()
                        LocalStore.get(this@MainActivity).replacePcDays(today.minusDays(2), today, emptyMap())
                    }
                    refresh()
                }
                refresh()
            }
        })
        button("Copy my history from the PC") { copyHistory() }
        val url = field("Server address, e.g. http://100.x.x.x:8000", settings.serverUrl)
        val token = field("Upload token (INGEST_TOKEN from .env)", settings.token, secret = true)
        button("Save") {
            settings.serverUrl = url.text.toString()
            settings.token = token.text.toString()
            toast("Saved")
        }

        heading("Permissions")
        button("Allow Health Connect access") {
            if (Health.available(this)) requestHealth.launch(Health.PERMISSIONS)
            else toast("Health Connect isn't available on this phone")
        }
        button("Allow usage access (screen time)") {
            startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        // Samsung pauses background work for apps it is saving battery on, so the automatic syncs would go missing.
        button("Keep syncing in the background") {
            if (backgroundAllowed()) toast("Already allowed")
            else startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }

        heading("Manual entries")
        button("Undo last quick log tap") {
            if (manual.undoLast()) afterLog("Undone") else toast("Nothing to undo")
        }
        button("Undo today's manual entries") {
            val zeroed = manual.clearToday()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    LocalStore.get(this@MainActivity).save("manual", zeroed)
                    SyncWorker.uploadToServer(settings, listOf("manual" to zeroed))
                }
                refresh()
            }
        }

        // Logging a set and a best: only for the exercises and goal picked under Your activities.
        val profile = settings.profile
        if (profile.trackedExercises.isNotEmpty()) {
            heading("Log a ${profile.training.ifBlank { "training" }} set")
            val names = profile.trackedExercises
            val exercise = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names.map { it.name })
            }.also { root.addView(it) }
            val reps = field("Reps", "", number = true)
            val weight = field("Weight (lb)", "", number = true)
            button("Add set") {
                val r = reps.text.toString().toIntOrNull()
                val w = weight.text.toString().toDoubleOrNull()
                if (r == null || w == null) return@button toast("Enter reps and weight")
                manual.addKettlebellSet(names[exercise.selectedItemPosition].key, r, w)
                reps.text.clear()
                afterLog("Set added")
            }
        }
        profile.goal?.let { goal ->
            heading(goal.name)
            val best = field("Today's best" + if (goal.unit.isNotBlank()) " (${goal.unit})" else "", "", number = true)
            button("Save today's best") {
                val v = best.text.toString().toDoubleOrNull() ?: return@button toast("Enter a number")
                manual.setBest(goal.metric, v, goal.unit.ifBlank { "count" })
                best.text.clear()
                afterLog("${goal.name} saved")
            }
        }

        SyncWorker.schedule(this)
    }

    /**
     * "Your activities": the person's own training, its sessions and exercises, something they'd like to improve,
     * and the optional parts. Saved with the button; the screen then rebuilds to match, and the dashboard and the
     * game read it the next time they load.
     */
    private fun activities() {
        val p = settings.profile
        heading("Your activities")
        root.addView(TextView(this).apply {
            text = "Set these up to match what you actually do. They name your Daily quests and your weekly tracker on " +
                "the Today tab. Leave a box empty to leave that part out, so it never counts against you."
        })
        subheading("Your training")
        val training = field("What you train, e.g. Kettlebells, Gym, Spin class, Running", p.training, plain = true)
        val sessions = field("Sessions each week, e.g. Monday, Tuesday, Friday or Session 1, Session 2", p.sessions.joinToString(", "), plain = true)
        val exercises = field("Exercises, e.g. Swing, Clean, Clean & press (optional)", p.exercises.joinToString(", ") { it.name }, plain = true)
        root.addView(TextView(this).apply {
            text = "Workouts from your watch count either way. Workout done on Daily quests ticks all your exercises for today."
        })

        subheading("Daily targets")
        root.addView(TextView(this).apply { text = "Calories (kcal), protein (g) and water (litres) a day." })
        val kcal = field("Calories a day, e.g. 2000", p.kcalTarget.toInt().toString(), number = true)
        val protein = field("Protein a day in grams, e.g. 150", p.proteinTarget.toInt().toString(), number = true)
        val water = field("Water a day in litres, e.g. 3", p.waterTarget.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }, number = true)

        subheading("Social media target")
        root.addView(TextView(this).apply {
            text = "Your target is your daily average over the last 90 days, worked out each Monday. Stay under it for " +
                "100 XP. Pick a cut to aim lower: the same % comes back as bonus XP on days you make it. Small cuts add up, " +
                "because next week's target follows your new average."
        })
        val cutChoices = listOfNotNull(
            if (p.socialCut == null) null to "Fixed 30 minutes (the old rule)" else null,
            0 to "Contain: don't go up",
            5 to "Trim 5% (+5% XP on days under it)",
            10 to "Cut 10% (+10% XP on days under it)",
            15 to "Cut 15% (+15% XP on days under it)",
        )
        val cutGroup = RadioGroup(this).also { root.addView(it) }
        cutChoices.forEach { (cut, label) ->
            cutGroup.addView(RadioButton(this).apply {
                id = View.generateViewId()
                tag = cut ?: -1
                text = label
                isChecked = p.socialCut == cut
            })
        }

        subheading("Something you'd like to improve")
        root.addView(TextView(this).apply { text = "For example: ${Profile.GOAL_EXAMPLES}. Log your best each time, higher is better." })
        val goalName = field("What you're improving", p.goal?.name ?: "", plain = true)
        val goalUnit = field("Unit, e.g. yd, kg, reps, seconds", p.goal?.unit ?: "", plain = true)
        val goalTarget = field("Target (optional)", p.goal?.target?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "", number = true)

        subheading("Optional")
        val boxes = Profile.ITEMS.map { item ->
            item.key to CheckBox(this).apply {
                text = "${item.label}\n${item.about}"
                isChecked = p.isOn(item.key)
            }.also { root.addView(it) }
        }
        button("Save my activities") {
            val kcalTarget = kcal.text.toString().toDoubleOrNull()
            if (kcalTarget == null || kcalTarget < 800 || kcalTarget > 6000) return@button toast("Enter a calorie target from 800 to 6000")
            val proteinTarget = protein.text.toString().toDoubleOrNull()
            if (proteinTarget == null || proteinTarget < 20 || proteinTarget > 400) return@button toast("Enter a protein target from 20 to 400 g")
            val waterTarget = water.text.toString().toDoubleOrNull()
            if (waterTarget == null || waterTarget < 0.5 || waterTarget > 8) return@button toast("Enter a water target from 0.5 to 8 litres")
            val name = goalName.text.toString().trim()
            val goal = if (name.isEmpty()) null else Profile.Goal(
                name, goalUnit.text.toString().trim(), goalTarget.text.toString().toDoubleOrNull(),
                // Keep logging to the same metric, so a renamed goal keeps its history.
                p.goal?.metric ?: Profile.GOAL_METRIC,
            )
            settings.profile = p.copy(
                training = training.text.toString().trim(),
                sessions = Profile.splitList(sessions.text.toString()).ifEmpty { Profile.DEFAULT_SESSIONS },
                goal = goal,
                off = boxes.filter { !it.second.isChecked }.map { it.first }.toSet(),
                kcalTarget = kcalTarget,
                proteinTarget = proteinTarget,
                waterTarget = waterTarget,
                socialCut = (cutGroup.findViewById<RadioButton>(cutGroup.checkedRadioButtonId)?.tag as? Int)
                    ?.takeIf { it >= 0 } ?: p.socialCut.takeIf { cutGroup.checkedRadioButtonId == -1 },
            ).withExerciseNames(Profile.splitList(exercises.text.toString()))
            toast("Saved your activities")
            recreate()
        }
    }

    private fun subheading(text: String) = root.addView(TextView(this).apply {
        this.text = text
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 24, 0, 0)
    })

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun afterLog(message: String) {
        toast(message)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val today = LocalDate.now()
                LocalStore.get(this@MainActivity).save("manual", manual.day(today))
            }
            syncNow()
        }
    }

    private fun syncNow() {
        status.text = "Syncing…"
        lifecycleScope.launch {
            settings.lastSync = withContext(Dispatchers.IO) {
                runCatching { "just now: ${SyncWorker.syncAll(this@MainActivity)}" }
                    .getOrElse { "just now: failed, ${it.message}" }
            }
            refresh()
            HomeWidget.refresh(this@MainActivity)
        }
    }

    private fun refresh() {
        lifecycleScope.launch {
            val span = withContext(Dispatchers.IO) { LocalStore.get(this@MainActivity).span() }
            val health = if (Health.available(this@MainActivity)) {
                val granted = HealthConnectClient.getOrCreate(this@MainActivity).permissionController.getGrantedPermissions()
                "${granted.intersect(Health.PERMISSIONS).size}/${Health.PERMISSIONS.size} allowed"
            } else "not available"
            status.text = buildString {
                appendLine("Health Connect: $health")
                appendLine("Screen time access: ${if (ScreenTime.hasAccess(this@MainActivity)) "allowed" else "not allowed"}")
                appendLine("Background sync: ${if (backgroundAllowed()) "every 15 min, ${SyncHours.text}" else "may be paused, tap Keep syncing in the background"}")
                appendLine("Today's manual entries: ${manual.today().size}")
                appendLine(if (span.first == 0) "Stored on this phone: nothing yet" else "Stored on this phone: ${span.first} days since ${span.second}")
                appendLine("GitHub: ${if (settings.githubToken.isEmpty()) "no token saved" else "on"}")
                appendLine("Alpaca: ${settings.alpacaAccounts.size.let { if (it == 0) "no keys saved" else "$it account(s), ${if (settings.alpacaLive) "live" else "paper"}" }}")
                appendLine("PC upload: ${if (settings.serverEnabled) "on" else "off, data stays on this phone"}")
                append("Last sync: ${settings.lastSync.replace(", ", "\n  ")}")
            }
        }
    }

    /** One-time copy of every day since the game started, so the phone's level matches the PC's. */
    private fun copyHistory() {
        toast("Copying from the PC…")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { PcHistory.copy(settings, LocalStore.get(this@MainActivity), Game.DEFAULT_GAME_START.minusDays(30), LocalDate.now()) }
            }
            toast(result.fold({ "Copied $it days from the PC" }, { "Couldn't reach the PC: ${it.message}" }))
            refresh()
        }
    }

    private fun backgroundAllowed() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun heading(text: String) = root.addView(TextView(this).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 48, 0, 8)
    })

    private fun field(hint: String, value: String, secret: Boolean = false, number: Boolean = false, plain: Boolean = false) =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = when {
                secret -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                number -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                plain -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            }
        }.also { root.addView(it) }

    private fun button(text: String, onClick: (View) -> Unit) =
        root.addView(Button(this).apply { this.text = text; setOnClickListener(onClick) })

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_WELCOME = "welcome"

    }
}

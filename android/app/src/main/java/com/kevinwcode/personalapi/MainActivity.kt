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
    private lateinit var todayCard: TextView
    private lateinit var characterCard: TextView
    private lateinit var perkButtons: LinearLayout
    private lateinit var mealList: LinearLayout
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
            text = "Personal API"
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        })

        status = TextView(this).apply {
            textSize = 18f
            setLineSpacing(0f, 1.2f)
            setPadding(0, 0, 0, 24)
        }.also { root.addView(it) }

        heading("Character")
        characterCard = TextView(this).apply {
            textSize = 16f
            setLineSpacing(0f, 1.2f)
        }.also { root.addView(it) }

        perkButtons = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }.also { root.addView(it) }

        heading("Quick log")
        for (q in ManualLog.QUICK) button(q.label) { logged(q.done, *q.entries.toTypedArray()) }
        button("Undo last tap") {
            if (manual.undoLast()) afterLog("Undone") else toast("Nothing to undo")
        }

        heading("Food")
        val mealBox = EditText(this).apply {
            hint = "Paste the meal line from your food AI"
            minLines = 2
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }.also { root.addView(it) }
        button("Add meal") {
            val meals = try {
                Food.parse(mealBox.text.toString(), LocalDate.now())
            } catch (e: Food.BadMeal) {
                return@button toast(e.message ?: "That isn't the meal format")
            }
            mealBox.text.clear()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { LocalStore.get(this@MainActivity).addMeals(meals) }
                afterLog("Added ${meals.size} meal${if (meals.size == 1) "" else "s"}")
            }
        }
        mealList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }.also { root.addView(it) }

        heading("Today on this phone")
        todayCard = TextView(this).apply {
            textSize = 16f
            setLineSpacing(0f, 1.2f)
        }.also { root.addView(it) }
        button("Open dashboard") { startActivity(Intent(this, DashboardActivity::class.java)) }
        root.addView(TextView(this).apply {
            text = "Tip: for today's numbers on your home screen, long-press an empty spot, tap Widgets, then Personal API."
            setPadding(0, 8, 0, 0)
        })
        button("Back up phone data to a file") { saveBackup.launch("personal-api-backup-${LocalDate.now()}.json") }

        heading("PC server (optional)")
        button("Open PC dashboard") { startActivity(OpenDashboardActivity.browserIntent(settings)) }

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
        val url = field("Server address", settings.serverUrl)
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

        heading("Kettlebell set")
        val move = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, MOVES.keys.toList())
        }.also { root.addView(it) }
        val reps = field("Reps", "", number = true)
        val weight = field("Weight (lb)", "", number = true)
        button("Add set") {
            val r = reps.text.toString().toIntOrNull()
            val w = weight.text.toString().toDoubleOrNull()
            if (r == null || w == null) return@button toast("Enter reps and weight")
            manual.addKettlebellSet(MOVES.getValue(move.selectedItem as String), r, w)
            reps.text.clear()
            afterLog("Set added")
        }

        heading("Flag football")
        val catch = field("Longest reliable catch today (yards)", "", number = true)
        button("Save catch distance") {
            val y = catch.text.toString().toDoubleOrNull() ?: return@button toast("Enter yards")
            manual.setCatchDistance(y)
            catch.text.clear()
            afterLog("Catch distance saved")
        }

        heading("Sync")
        button("Sync now") { syncNow() }
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

        SyncWorker.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** A quick log tap: saves it on the phone straight away, then syncs (which also sends it to the PC, if on). */
    private fun logged(message: String, vararg entries: ManualLog.Entry) {
        manual.log(LocalDate.now(), *entries)
        afterLog(message)
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
            val today = LocalDate.now()
            val store = LocalStore.get(this@MainActivity)
            val (game, lines, span) = withContext(Dispatchers.IO) {
                val days = store.allDays()
                Triple(Game.build(days, today), DayMetrics.summaryLines(days[today] ?: emptyMap()), store.span())
            }
            val meals = withContext(Dispatchers.IO) { store.meals(today) }
            mealList.removeAllViews()
            if (meals.isEmpty()) mealList.addView(TextView(this@MainActivity).apply { text = "No meals added in the app today" })
            for ((id, meal) in meals) {
                mealList.addView(TextView(this@MainActivity).apply { text = Food.line(meal); setPadding(0, 16, 0, 0) })
                mealList.addView(Button(this@MainActivity).apply {
                    text = "Remove this meal"
                    setOnClickListener {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { store.removeMeal(id) }
                            afterLog("Meal removed")
                        }
                    }
                })
            }
            perkButtons.removeAllViews()
            for (g in game.groups) for ((tier, perk) in g.earnedPerks) {
                if (tier in g.claimed) continue
                perkButtons.addView(Button(this@MainActivity).apply {
                    text = "Claim ${perk.token} (${g.ability} ${Perks.TIERS.getValue(tier).first})"
                    setOnClickListener { logged("Claimed ${perk.token}", ManualLog.perkClaim(g.key, tier)) }
                })
            }
            characterCard.text = (CharacterText.lines(game) + listOf("") + CharacterText.todayLines(game)).joinToString("\n").trim()
            todayCard.text = buildString {
                if (lines.isEmpty()) appendLine("Nothing saved yet today. Tap Sync now.")
                lines.forEach { appendLine(it) }
                append(
                    if (span.first == 0) "Stored on this phone: nothing yet"
                    else "Stored on this phone: ${span.first} days since ${span.second}"
                )
            }
            val health = if (Health.available(this@MainActivity)) {
                val granted = HealthConnectClient.getOrCreate(this@MainActivity).permissionController.getGrantedPermissions()
                "${granted.intersect(Health.PERMISSIONS).size}/${Health.PERMISSIONS.size} allowed"
            } else "not available"
            status.text = buildString {
                appendLine("Health Connect: $health")
                appendLine("Screen time access: ${if (ScreenTime.hasAccess(this@MainActivity)) "allowed" else "not allowed"}")
                appendLine("Background sync: ${if (backgroundAllowed()) "every 15 min, ${SyncHours.text}" else "may be paused, tap Keep syncing in the background"}")
                appendLine("Today's manual entries: ${manual.today().size}")
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

    private fun field(hint: String, value: String, secret: Boolean = false, number: Boolean = false) =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = when {
                secret -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                number -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            }
        }.also { root.addView(it) }

    private fun button(text: String, onClick: (View) -> Unit) =
        root.addView(Button(this).apply { this.text = text; setOnClickListener(onClick) })

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        val MOVES = linkedMapOf(
            "Swing" to "swing",
            "Clean" to "clean",
            "Clean & press" to "clean_press",
            "Ballistic row" to "ballistic_row",
        )
    }
}

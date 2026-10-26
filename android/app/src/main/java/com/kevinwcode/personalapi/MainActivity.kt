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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var settings: Settings
    private lateinit var manual: ManualLog
    private lateinit var status: TextView
    private lateinit var root: LinearLayout

    private val requestHealth =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }

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

        button("Open dashboard") { startActivity(OpenDashboardActivity.browserIntent(settings)) }
        root.addView(TextView(this).apply {
            text = "Tip: for today's numbers on your home screen, long-press an empty spot, tap Widgets, then Personal API."
            setPadding(0, 8, 0, 0)
        })

        heading("Connection")
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
            syncNow()
        }

        heading("Flag football")
        val catch = field("Longest reliable catch today (yards)", "", number = true)
        button("Save catch distance") {
            val y = catch.text.toString().toDoubleOrNull() ?: return@button toast("Enter yards")
            manual.setCatchDistance(y)
            catch.text.clear()
            syncNow()
        }

        heading("Sync")
        button("Sync now") { syncNow() }
        button("Undo today's manual entries") {
            val zeroed = manual.clearToday()
            lifecycleScope.launch {
                runCatching { withContext(Dispatchers.IO) { Uploader.send(settings, "manual", zeroed) } }
                refresh()
            }
        }

        SyncWorker.schedule(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
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
            val health = if (Health.available(this@MainActivity)) {
                val granted = HealthConnectClient.getOrCreate(this@MainActivity).permissionController.getGrantedPermissions()
                "${granted.intersect(Health.PERMISSIONS).size}/${Health.PERMISSIONS.size} allowed"
            } else "not available"
            status.text = buildString {
                appendLine("Health Connect: $health")
                appendLine("Screen time access: ${if (ScreenTime.hasAccess(this@MainActivity)) "allowed" else "not allowed"}")
                appendLine("Background sync: ${if (backgroundAllowed()) "every 15 min" else "may be paused, tap Keep syncing in the background"}")
                appendLine("Today's manual entries: ${manual.today().size}")
                append("Last sync: ${settings.lastSync.replace(", ", "\n  ")}")
            }
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

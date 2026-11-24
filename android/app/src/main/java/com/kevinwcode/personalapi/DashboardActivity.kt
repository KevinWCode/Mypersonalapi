package com.kevinwcode.personalapi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * The app's main screen: the dashboard, from the phone's own data. It's the same page the PC server serves; the page's
 * requests to the server are answered on the phone by [PhoneApi], so it needs no server. The page's settings cog
 * opens its Settings tab, which reads and saves the app's settings through [App], in the same style and theme as the
 * rest of the page. [MainActivity] only forwards here.
 */
class DashboardActivity : ComponentActivity() {
    private lateinit var web: WebView
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var resumed = false
    /** First launch: the page opens on Settings with the get-started steps. */
    @Volatile private var welcome = false
    /** Apps that wrote to Health Connect lately, for the "Main health app" choice. Read once, in the background. */
    @Volatile private var healthSources: Set<String> = emptySet()

    private val requestHealth =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            loadHealthSources()
            tell("Health Connect: ${it.intersect(Health.PERMISSIONS).size}/${Health.PERMISSIONS.size} allowed")
        }

    // Lets the person pick where the backup file goes (Downloads, Google Drive, ...). The app never sends it anywhere itself.
    private val saveBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { contentResolver.openOutputStream(uri)!!.use { LocalStore.get(this@DashboardActivity).exportJson(it) } }
            }
            tell(result.fold({ "Backed up $it entries" }, { "Backup failed: ${it.message}" }))
        }
    }

    // Restores a backup file (from this app or the 0.21 app). The game start is worked out again, so restored history counts.
    private val restoreBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val n = contentResolver.openInputStream(uri)!!.use { LocalStore.get(this@DashboardActivity).importJson(it) }
                    Settings(this@DashboardActivity).resetGameStart()
                    n
                }
            }
            tell(result.fold({ "Restored $it entries" }, { "Restore failed: ${it.message}" }))
            if (result.isSuccess) reloadPage()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(applicationContext), "PhoneApi")
        web.addJavascriptInterface(App(), "PhoneApp")
        val page = assets.open("dashboard.html").bufferedReader().use { it.readText() }
        // Runs before the page's own script: sends its fetch() calls to the phone, and turns logging on.
        val html = page.replaceFirst("<head>", "<head><script>$SHIM</script>")
        web.loadDataWithBaseURL(BASE_URL, html, "text/html", "utf-8", null)
        val frame = FrameLayout(this).apply { addView(web) }
        setContentView(frame)
        // Android 15 draws apps under the status and navigation bars, so keep the page clear of them.
        ViewCompat.setOnApplyWindowInsetsListener(frame) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        // The dashboard is the first screen now, so it starts the background sync (the settings screen did before).
        SyncWorker.schedule(this)
        if (intent.getBooleanExtra(EXTRA_SETTINGS, false)) openSettingsTab()
        loadHealthSources()
        // First launch on a new phone: fix the game's start day and show the setup steps (permissions, keys).
        val settings = Settings(this)
        if (settings.setupPending) {
            settings.setupPending = false
            scope.launch {
                val fresh = withContext(Dispatchers.IO) {
                    val span = LocalStore.get(this@DashboardActivity).span()
                    settings.gameStart(span.second)
                    span.first == 0
                }
                // A brand new install starts with Kevin-only parts off; the setup screen asks for the rest.
                if (fresh && !settings.profileChosen) settings.profile = Profile.NEW_TESTER
                if (fresh || !ScreenTime.hasAccess(this@DashboardActivity)) {
                    welcome = true
                    openSettingsTab()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SETTINGS, false)) openSettingsTab()
    }

    /** Shows the page's Settings tab (if the page is still loading, it asks [App.takeWelcome] / reads the hash itself). */
    private fun openSettingsTab() {
        openSettingsOnLoad = true
        runOnUiThread { web.evaluateJavascript("window.openSettings && window.openSettings()", null) }
    }
    @Volatile private var openSettingsOnLoad = false

    /** A short message for the page to show (and the page then re-reads the settings). */
    private fun tell(message: String) =
        runOnUiThread { web.evaluateJavascript("window.appEvent && window.appEvent(${JSONObject.quote(message)})", null) }

    private fun loadHealthSources() {
        if (!Health.available(this)) return
        scope.launch {
            healthSources = withContext(Dispatchers.IO) {
                withTimeoutOrNull(10_000) { runCatching { Health.recentSources(this@DashboardActivity) }.getOrNull() }
            } ?: healthSources
        }
    }

    private fun backgroundAllowed() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    override fun onResume() {
        super.onResume()
        // Back from the settings screen (or the phone was away): show the latest numbers.
        if (resumed) reloadPage()
        resumed = true
    }

    override fun onDestroy() {
        scope.cancel()
        web.destroy()
        super.onDestroy()
    }

    private fun reloadPage() = web.evaluateJavascript("window.dashboardReload && window.dashboardReload()", null)

    /** What the page's Settings tab calls. Runs on a WebView background thread. */
    inner class App {
        private val context get() = this@DashboardActivity
        private val settings get() = Settings(context)

        @JavascriptInterface
        fun openSettings() = openSettingsTab()

        /** True once, when the page should open on Settings (first launch, or the app's Settings shortcut). */
        @JavascriptInterface
        fun takeWelcome(): String {
            val answer = when {
                welcome -> "welcome"
                openSettingsOnLoad -> "settings"
                else -> ""
            }
            openSettingsOnLoad = false
            return answer
        }

        @JavascriptInterface
        fun syncNow() {
            scope.launch {
                val s = settings
                s.lastSync = withContext(Dispatchers.IO) {
                    runCatching { "just now: ${SyncWorker.syncAll(context)}" }.getOrElse { "just now: failed, ${it.message}" }
                }
                HomeWidget.refresh(context)
                reloadPage()
            }
        }

        @JavascriptInterface
        fun status(): String = statusText(context)

        /** Everything the Settings tab shows, in one go. */
        @JavascriptInterface
        fun state(): String {
            val s = settings
            val store = LocalStore.get(context)
            val span = store.span()
            val health = JSONObject().put("available", Health.available(context)).put("total", Health.PERMISSIONS.size)
            if (Health.available(context)) {
                val granted = runBlocking {
                    withTimeoutOrNull(3_000) {
                        runCatching { HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions() }.getOrNull()
                    }
                } ?: emptySet()
                health.put("granted", granted.intersect(Health.PERMISSIONS).size)
            } else health.put("granted", 0)
            val sources = (healthSources + listOfNotNull(s.healthSource.ifEmpty { null })).sortedBy { HealthSources.name(it) }
            return JSONObject()
                .put("welcome", welcome)
                .put("profile", s.profile.toJson())
                .put("health", health)
                .put("usage", ScreenTime.hasAccess(context))
                .put("background", backgroundAllowed())
                .put("sync_hours", SyncHours.text)
                .put("last_sync", s.lastSync)
                .put("stored_days", span.first)
                .put("stored_since", span.second ?: JSONObject.NULL)
                .put("manual_today", ManualLog(context).today().size)
                .put("health_source", s.healthSource)
                .put("health_sources", JSONArray(sources.map { JSONObject().put("id", it).put("name", HealthSources.name(it)) }))
                .put("github", s.githubToken.isNotEmpty())
                .put("alpaca_accounts", s.alpacaAccounts.size)
                .put("alpaca_live", s.alpacaLive)
                .put("server", JSONObject().put("enabled", s.serverEnabled).put("url", s.serverUrl).put("has_token", s.token.isNotEmpty()))
                .toString()
        }

        /** Saves a change to "Your activities" (see [SettingsForm]). Answers "" when saved, else what's wrong. */
        @JavascriptInterface
        fun saveProfile(change: String): String = try {
            val s = settings
            s.profile = SettingsForm.apply(s.profile, JSONObject(change))
            welcome = false
            ""
        } catch (e: SettingsForm.Bad) {
            e.message ?: "Couldn't save that"
        } catch (e: Exception) {
            "Couldn't save that"
        }

        @JavascriptInterface
        fun saveGithub(token: String): String {
            settings.githubToken = token
            syncNow()
            return if (token.isBlank()) "GitHub token removed" else "GitHub token saved"
        }

        @JavascriptInterface
        fun saveAlpaca(keys: String, live: Boolean): String {
            val words = keys.split(Regex("[\\s,:;]+")).count { it.isNotEmpty() }
            if (words % 2 != 0) return "Alpaca keys come in pairs: a key id, then its secret"
            val s = settings
            s.alpacaKeys = keys
            s.alpacaLive = live
            syncNow()
            return "Saved ${s.alpacaAccounts.size} Alpaca account(s)"
        }

        @JavascriptInterface
        fun setAlpacaLive(live: Boolean) { settings.alpacaLive = live }

        @JavascriptInterface
        fun setHealthSource(id: String) { settings.healthSource = id }

        @JavascriptInterface
        fun allowHealth() = runOnUiThread {
            if (Health.available(context)) requestHealth.launch(Health.PERMISSIONS)
            else tell("Health Connect isn't available on this phone")
        }

        @JavascriptInterface
        fun allowUsage() = runOnUiThread { startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)) }

        // Samsung pauses background work for apps it is saving battery on, so the automatic syncs would go missing.
        @JavascriptInterface
        fun allowBackground() = runOnUiThread {
            if (backgroundAllowed()) tell("Background sync is already allowed")
            else startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }

        @JavascriptInterface
        fun backup() = runOnUiThread { saveBackup.launch("personal-api-backup-${LocalDate.now()}.json") }

        @JavascriptInterface
        fun restore() = runOnUiThread { restoreBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }

        /** Zeroes today's quick log and app entries (not meals or watch data). */
        @JavascriptInterface
        fun clearToday() {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val zeroed = ManualLog(context).clearToday()
                    LocalStore.get(context).save("manual", zeroed)
                    SyncWorker.uploadToServer(settings, listOf("manual" to zeroed))
                }
                tell("Cleared today's manual entries")
                reloadPage()
            }
        }

        /** A set of one of your exercises: reps and weight (lb). Answers "" when saved. */
        @JavascriptInterface
        fun addSet(key: String, reps: String, weight: String): String {
            val r = reps.trim().toIntOrNull()
            val w = weight.trim().toDoubleOrNull()
            if (r == null || r <= 0 || w == null || w < 0) return "Enter reps and weight"
            if (settings.profile.trackedExercises.none { it.key == key }) return "Pick one of your exercises"
            val manual = ManualLog(context)
            manual.addKettlebellSet(key, r, w)
            afterLog(manual)
            return ""
        }

        /** Today's best on the thing you're improving. Answers "" when saved. */
        @JavascriptInterface
        fun saveBest(value: String): String {
            val v = value.trim().toDoubleOrNull() ?: return "Enter a number"
            val goal = settings.profile.goal ?: return "Set up something to improve first"
            val manual = ManualLog(context)
            manual.setBest(goal.metric, v, goal.unit.ifBlank { "count" })
            afterLog(manual)
            return ""
        }

        private fun afterLog(manual: ManualLog) {
            LocalStore.get(context).save("manual", manual.day(LocalDate.now()))
            HomeWidget.refreshFromPhone(context)
            syncSoon(context)
        }

        /** The PC server (Kevin's old set-up). A blank token keeps the saved one. */
        @JavascriptInterface
        fun saveServer(url: String, token: String): String {
            val s = settings
            s.serverUrl = url
            if (token.isNotBlank()) s.token = token
            return "Saved the PC server"
        }

        @JavascriptInterface
        fun setServerEnabled(on: Boolean) {
            settings.serverEnabled = on
            // Without the PC, recent days go back to the phone's own data instead of the last copy from the PC.
            if (!on) scope.launch {
                withContext(Dispatchers.IO) {
                    val today = LocalDate.now()
                    LocalStore.get(context).replacePcDays(today.minusDays(2), today, emptyMap())
                }
                reloadPage()
            }
        }

        /** One-time copy of every day since the game started, so the phone's level matches the PC's. */
        @JavascriptInterface
        fun copyHistory() {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { PcHistory.copy(settings, LocalStore.get(context), Game.DEFAULT_GAME_START.minusDays(30), LocalDate.now()) }
                }
                tell(result.fold({ "Copied $it days from the PC" }, { "Couldn't reach the PC: ${it.message}" }))
                if (result.isSuccess) reloadPage()
            }
        }

        @JavascriptInterface
        fun openPcDashboard() = runOnUiThread {
            val s = settings
            if (s.serverUrl.isEmpty()) tell("Enter the server address first")
            else startActivity(OpenDashboardActivity.browserIntent(s))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) return web.goBack()
        // On the Settings tab, Back goes back to the dashboard.
        web.evaluateJavascript("window.goBack ? window.goBack() : false") { handled ->
            if (handled != "true") super@DashboardActivity.onBackPressed()
        }
    }

    /** What the page calls instead of the network. Runs on a WebView background thread. */
    class Bridge(private val context: Context) {
        private val api = PhoneApi(StoreData(context))

        @JavascriptInterface
        fun call(method: String, url: String, body: String?): String {
            val answer = api.handle(method, url, body)
            if (method != "GET" && answer.status == 200) {
                HomeWidget.refreshFromPhone(context)
                syncSoon(context)
            }
            return JSONObject().put("status", answer.status).put("body", answer.body).toString()
        }
    }

    /** The phone's database and manual log, for [PhoneApi]. */
    class StoreData(context: Context) : PhoneApi.Data {
        private val store = LocalStore.get(context)
        private val manual = ManualLog(context)
        private val settings = Settings(context)

        override fun days() = store.allDays()
        override fun meals(day: LocalDate) = store.meals(day)
        override fun addMeals(meals: List<Food.Meal>) = store.addMeals(meals)
        override fun removeMeal(id: String) = store.removeMeal(id)
        override fun lastUpdates() = store.lastUpdates()
        override fun trading() = store.trading()
        override fun gameStart() = settings.gameStart(store.span().second)
        override fun profile() = settings.profile

        override fun log(day: LocalDate, entries: List<ManualLog.Entry>): Map<String, Double> {
            manual.log(day, *entries.toTypedArray())
            store.save("manual", manual.day(day))
            return entries.associate { it.metric to manual.value(it.metric, day) }
        }
    }

    companion object {
        /** Opens the page on its Settings tab (the app's Settings shortcut and Health Connect's privacy link). */
        const val EXTRA_SETTINGS = "settings"

        /** Short status for the Phone app card: what's switched on and the last sync. */
        fun statusText(context: Context): String {
            val settings = Settings(context)
            return listOf(
                "Screen time access: ${if (ScreenTime.hasAccess(context)) "allowed" else "not allowed, open app settings"}",
                "GitHub: ${if (settings.githubToken.isEmpty()) "no token saved" else "on"}",
                "Alpaca: ${settings.alpacaAccounts.size.let { if (it == 0) "no keys saved" else "$it account(s), ${if (settings.alpacaLive) "live" else "paper"}" }}",
                "PC upload: ${if (settings.serverEnabled) "on" else "off"}",
                "Last sync: ${settings.lastSync}",
            ).joinToString("\n")
        }

        // Any https origin works; nothing is ever loaded from it. It gives the page its own storage.
        private const val BASE_URL = "https://phone.personal-api/dashboard"

        private val SHIM = """
            (function () {
              try { localStorage.setItem('token', 'phone'); } catch (e) {}
              window.fetch = function (url, opts) {
                opts = opts || {};
                return new Promise(function (resolve, reject) {
                  try {
                    var r = JSON.parse(PhoneApi.call((opts.method || 'GET').toUpperCase(), String(url), opts.body || null));
                    resolve(new Response(JSON.stringify(r.body), { status: r.status, headers: { 'Content-Type': 'application/json' } }));
                  } catch (e) { reject(e); }
                });
              };
            })();
        """.trimIndent()

        /** Sends a dashboard entry to the PC soon, while the PC upload is on. */
        fun syncSoon(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync-now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().build(),
            )
        }
    }
}

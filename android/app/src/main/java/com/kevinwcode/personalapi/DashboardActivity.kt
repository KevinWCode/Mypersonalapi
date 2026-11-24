package com.kevinwcode.personalapi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

/**
 * The app's main screen: the dashboard, from the phone's own data. It's the same page the PC server serves; the page's
 * requests to the server are answered on the phone by [PhoneApi], so it needs no server. The page's settings cog
 * has a Phone app card that syncs and opens the settings screen ([MainActivity]) through [App].
 */
class DashboardActivity : Activity() {
    private lateinit var web: WebView
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var resumed = false

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
                    startActivity(Intent(this@DashboardActivity, MainActivity::class.java).putExtra(MainActivity.EXTRA_WELCOME, true))
                }
            }
        }
    }

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

    /** What the page's Phone app card calls. Runs on a WebView background thread. */
    inner class App {
        @JavascriptInterface
        fun openSettings() = runOnUiThread { startActivity(Intent(this@DashboardActivity, MainActivity::class.java)) }

        @JavascriptInterface
        fun syncNow() {
            scope.launch {
                val settings = Settings(this@DashboardActivity)
                settings.lastSync = withContext(Dispatchers.IO) {
                    runCatching { "just now: ${SyncWorker.syncAll(this@DashboardActivity)}" }.getOrElse { "just now: failed, ${it.message}" }
                }
                HomeWidget.refresh(this@DashboardActivity)
                reloadPage()
            }
        }

        @JavascriptInterface
        fun status(): String = statusText(this@DashboardActivity)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
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

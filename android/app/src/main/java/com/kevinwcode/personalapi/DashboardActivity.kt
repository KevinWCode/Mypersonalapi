package com.kevinwcode.personalapi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.time.LocalDate

/**
 * The dashboard, inside the app, from the phone's own data. It's the same page the PC server serves; the page's
 * requests to the server are answered on the phone by [PhoneApi], so it needs no server.
 */
class DashboardActivity : Activity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(applicationContext), "PhoneApi")
        val page = assets.open("dashboard.html").bufferedReader().use { it.readText() }
        // Runs before the page's own script: sends its fetch() calls to the phone, and turns logging on.
        val html = page.replaceFirst("<head>", "<head><script>$SHIM</script>")
        web.loadDataWithBaseURL(BASE_URL, html, "text/html", "utf-8", null)
        setContentView(web)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
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

        override fun days() = store.allDays()
        override fun meals(day: LocalDate) = store.meals(day)
        override fun addMeals(meals: List<Food.Meal>) = store.addMeals(meals)
        override fun removeMeal(id: String) = store.removeMeal(id)
        override fun lastUpdates() = store.lastUpdates()

        override fun log(day: LocalDate, entries: List<ManualLog.Entry>): Map<String, Double> {
            manual.log(day, *entries.toTypedArray())
            store.save("manual", manual.day(day))
            return entries.associate { it.metric to manual.value(it.metric, day) }
        }
    }

    companion object {
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

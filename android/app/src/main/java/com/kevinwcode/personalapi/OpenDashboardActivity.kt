package com.kevinwcode.personalapi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/** Opens the dashboard in the browser when you tap the home screen widget, without showing the app first. */
class OpenDashboardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(browserIntent(Settings(this)))
        finish()
    }

    companion object {
        /**
         * The dashboard with the upload token after the #. Browsers never send that part to the server; the dashboard
         * keeps the token so its Quick log buttons can save, then clears it from the address bar.
         */
        fun browserIntent(settings: Settings): Intent {
            val token = settings.token
            val url = "${settings.serverUrl}/dashboard" + if (token.isEmpty()) "" else "#token=${Uri.encode(token)}"
            return Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
    }
}

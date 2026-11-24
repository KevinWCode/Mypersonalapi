package com.kevinwcode.personalapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Settings live on the dashboard's own Settings tab now (same style and theme as the rest of the app). This stays
 * only because Health Connect opens it for "privacy policy" / permission usage, and it forwards there.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, DashboardActivity::class.java).putExtra(DashboardActivity.EXTRA_SETTINGS, true))
        finish()
    }
}

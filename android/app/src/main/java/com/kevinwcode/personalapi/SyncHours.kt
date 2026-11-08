package com.kevinwcode.personalapi

import java.time.LocalTime

/**
 * The background sync only runs from 7:00 until 22:00, to save battery overnight. Nothing is lost: each sync sends the
 * last 3 days again, so the first sync in the morning catches up. Sync now in the app works at any time.
 */
object SyncHours {
    const val FROM = 7
    const val UNTIL = 22

    fun allows(t: LocalTime): Boolean = t.hour in FROM until UNTIL

    val text = "%02d:00 to %02d:00".format(FROM, UNTIL)
}

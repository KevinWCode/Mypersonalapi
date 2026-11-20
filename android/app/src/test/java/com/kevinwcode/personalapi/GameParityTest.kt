package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The phone's game must score exactly like the server's app/game.py and app/vitals.py.
 * Generated: a random 52-day fixture of raw rows, with the expected numbers from the Python code.
 */
class GameParityTest {
    private val rows: List<Triple<String, String, Double>> = listOf(
        Triple("2026-10-17", "sleep.total_minutes", 405.0),
        Triple("2026-10-17", "sleep.deep_minutes", 90.0),
        Triple("2026-10-17", "sleep.rem_minutes", 56.0),
        Triple("2026-10-17", "health.resting_heart_rate", 60.0),
        Triple("2026-10-17", "health.hrv_rmssd", 48.0),
        Triple("2026-10-17", "health.steps", 9813.0),
        Triple("2026-10-17", "workout.sessions", 1.0),
        Triple("2026-10-17", "workout.minutes", 37.0),
        Triple("2026-10-17", "workout.counted_sessions", 1.0),
        Triple("2026-10-17", "kb.swing.sets", 5.0),
        Triple("2026-10-17", "journal.entries", 1.0),
        Triple("2026-10-17", "water.ml", 1500.0),
        Triple("2026-10-17", "coffee.cups", 1.0),
        Triple("2026-10-17", "reading.minutes", 30.0),
        Triple("2026-10-17", "study.minutes", 20.0),
        Triple("2026-10-17", "screen.app.com.android.chrome", 78.0),
        Triple("2026-10-17", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-17", "screen.app.com.instagram.android", 9.0),
        Triple("2026-10-17", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-10-18", "sleep.total_minutes", 430.0),
        Triple("2026-10-18", "sleep.deep_minutes", 103.0),
        Triple("2026-10-18", "sleep.rem_minutes", 118.0),
        Triple("2026-10-18", "health.resting_heart_rate", 57.0),
        Triple("2026-10-18", "workout.sessions", 1.0),
        Triple("2026-10-18", "workout.minutes", 60.0),
        Triple("2026-10-18", "workout.counted_sessions", 1.0),
        Triple("2026-10-18", "kb.clean.done", 1.0),
        Triple("2026-10-18", "cal.abc2def.done", 1.0),
        Triple("2026-10-18", "email.unread", 13.0),
        Triple("2026-10-18", "journal.entries", 2.0),
        Triple("2026-10-18", "water.ml", 1500.0),
        Triple("2026-10-18", "coffee.cups", 3.0),
        Triple("2026-10-18", "reading.minutes", 45.0),
        Triple("2026-10-18", "study.minutes", 60.0),
        Triple("2026-10-18", "screen.app.com.android.chrome", 79.0),
        Triple("2026-10-18", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-10-18", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-10-19", "sleep.total_minutes", 450.0),
        Triple("2026-10-19", "sleep.deep_minutes", 97.0),
        Triple("2026-10-19", "sleep.rem_minutes", 86.0),
        Triple("2026-10-19", "health.resting_heart_rate", 66.0),
        Triple("2026-10-19", "health.steps", 9064.0),
        Triple("2026-10-19", "workout.sessions", 1.0),
        Triple("2026-10-19", "workout.minutes", 41.0),
        Triple("2026-10-19", "workout.counted_sessions", 1.0),
        Triple("2026-10-19", "kb.clean.done", 1.0),
        Triple("2026-10-19", "football.max_catch_yards", 16.0),
        Triple("2026-10-19", "cal.abc0def.done", 1.0),
        Triple("2026-10-19", "cal.abc1def.done", 1.0),
        Triple("2026-10-19", "water.ml", 1500.0),
        Triple("2026-10-19", "study.minutes", 20.0),
        Triple("2026-10-19", "screen.app.com.android.chrome", 27.0),
        Triple("2026-10-19", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-19", "screen.app.com.instagram.android", 27.0),
        Triple("2026-10-19", "screen.app.com.google.android.youtube", 45.0),
        Triple("2026-10-19", "trading.pl_day", -6.48),
        Triple("2026-10-19", "trading.equity", 1547.7),
        Triple("2026-10-20", "sleep.total_minutes", 405.0),
        Triple("2026-10-20", "sleep.deep_minutes", 105.0),
        Triple("2026-10-20", "sleep.rem_minutes", 56.0),
        Triple("2026-10-20", "health.resting_heart_rate", 65.0),
        Triple("2026-10-20", "health.steps", 14572.0),
        Triple("2026-10-20", "kb.swing.sets", 4.0),
        Triple("2026-10-20", "kb.clean.done", 1.0),
        Triple("2026-10-20", "github.commits", 2.0),
        Triple("2026-10-20", "journal.entries", 1.0),
        Triple("2026-10-20", "water.ml", 500.0),
        Triple("2026-10-20", "coffee.cups", 2.0),
        Triple("2026-10-20", "study.minutes", 20.0),
        Triple("2026-10-20", "screen.app.com.android.chrome", 24.0),
        Triple("2026-10-20", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-10-20", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-10-21", "sleep.total_minutes", 520.0),
        Triple("2026-10-21", "sleep.deep_minutes", 79.0),
        Triple("2026-10-21", "sleep.rem_minutes", 60.0),
        Triple("2026-10-21", "health.resting_heart_rate", 63.0),
        Triple("2026-10-21", "health.steps", 15079.0),
        Triple("2026-10-21", "cal.abc0def.done", 1.0),
        Triple("2026-10-21", "github.commits", 5.0),
        Triple("2026-10-21", "email.unread", 20.0),
        Triple("2026-10-21", "coffee.cups", 2.0),
        Triple("2026-10-21", "study.minutes", 20.0),
        Triple("2026-10-21", "screen.app.com.android.chrome", 104.0),
        Triple("2026-10-21", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-21", "screen.app.com.instagram.android", 9.0),
        Triple("2026-10-21", "trading.pl_day", -21.86),
        Triple("2026-10-21", "trading.equity", 1607.06),
        Triple("2026-10-22", "sleep.total_minutes", 300.0),
        Triple("2026-10-22", "sleep.deep_minutes", 43.0),
        Triple("2026-10-22", "sleep.rem_minutes", 85.0),
        Triple("2026-10-22", "health.resting_heart_rate", 55.0),
        Triple("2026-10-22", "workout.sessions", 2.0),
        Triple("2026-10-22", "workout.minutes", 33.0),
        Triple("2026-10-22", "workout.counted_sessions", 1.0),
        Triple("2026-10-22", "kb.swing.sets", 4.0),
        Triple("2026-10-22", "kb.clean.done", 1.0),
        Triple("2026-10-22", "football.max_catch_yards", 19.0),
        Triple("2026-10-22", "cal.abc1def.done", 1.0),
        Triple("2026-10-22", "github.commits", 6.0),
        Triple("2026-10-22", "email.unread", 12.0),
        Triple("2026-10-22", "coffee.cups", 1.0),
        Triple("2026-10-22", "screen.app.com.android.chrome", 97.0),
        Triple("2026-10-22", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-10-22", "screen.app.com.reddit.frontpage", 7.0),
        Triple("2026-10-22", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-10-22", "trading.pl_day", 7.26),
        Triple("2026-10-22", "trading.equity", 1586.14),
        Triple("2026-10-23", "sleep.total_minutes", 520.0),
        Triple("2026-10-23", "sleep.deep_minutes", 84.0),
        Triple("2026-10-23", "sleep.rem_minutes", 69.0),
        Triple("2026-10-23", "health.resting_heart_rate", 54.0),
        Triple("2026-10-23", "health.hrv_rmssd", 55.0),
        Triple("2026-10-23", "github.commits", 3.0),
        Triple("2026-10-23", "email.unread", 16.0),
        Triple("2026-10-23", "journal.entries", 2.0),
        Triple("2026-10-23", "water.ml", 1500.0),
        Triple("2026-10-23", "reading.minutes", 45.0),
        Triple("2026-10-23", "screen.app.com.android.chrome", 109.0),
        Triple("2026-10-23", "screen.app.com.instagram.android", 9.0),
        Triple("2026-10-23", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-10-23", "trading.pl_day", 22.12),
        Triple("2026-10-23", "trading.equity", 1643.42),
        Triple("2026-10-24", "sleep.total_minutes", 405.0),
        Triple("2026-10-24", "sleep.deep_minutes", 62.0),
        Triple("2026-10-24", "sleep.rem_minutes", 68.0),
        Triple("2026-10-24", "health.resting_heart_rate", 63.0),
        Triple("2026-10-24", "health.hrv_rmssd", 31.0),
        Triple("2026-10-24", "health.steps", 9992.0),
        Triple("2026-10-24", "github.commits", 3.0),
        Triple("2026-10-24", "email.unread", 3.0),
        Triple("2026-10-24", "journal.entries", 1.0),
        Triple("2026-10-24", "screen.app.com.android.chrome", 70.0),
        Triple("2026-10-24", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-10-24", "screen.app.com.instagram.android", 27.0),
        Triple("2026-10-24", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-10-25", "sleep.total_minutes", 450.0),
        Triple("2026-10-25", "sleep.deep_minutes", 65.0),
        Triple("2026-10-25", "sleep.rem_minutes", 107.0),
        Triple("2026-10-25", "health.resting_heart_rate", 53.0),
        Triple("2026-10-25", "health.steps", 12496.0),
        Triple("2026-10-25", "workout.sessions", 1.0),
        Triple("2026-10-25", "workout.minutes", 23.0),
        Triple("2026-10-25", "football.max_catch_yards", 21.0),
        Triple("2026-10-25", "cal.abc0def.done", 1.0),
        Triple("2026-10-25", "email.unread", 3.0),
        Triple("2026-10-25", "journal.entries", 2.0),
        Triple("2026-10-25", "water.ml", 500.0),
        Triple("2026-10-25", "coffee.cups", 2.0),
        Triple("2026-10-25", "screen.app.com.android.chrome", 50.0),
        Triple("2026-10-25", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-25", "screen.app.com.instagram.android", 18.0),
        Triple("2026-10-25", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-10-26", "sleep.total_minutes", 450.0),
        Triple("2026-10-26", "sleep.deep_minutes", 105.0),
        Triple("2026-10-26", "sleep.rem_minutes", 58.0),
        Triple("2026-10-26", "health.resting_heart_rate", 66.0),
        Triple("2026-10-26", "workout.sessions", 2.0),
        Triple("2026-10-26", "workout.minutes", 27.0),
        Triple("2026-10-26", "workout.counted_sessions", 1.0),
        Triple("2026-10-26", "football.max_catch_yards", 16.0),
        Triple("2026-10-26", "cal.abc0def.done", 1.0),
        Triple("2026-10-26", "journal.entries", 2.0),
        Triple("2026-10-26", "water.ml", 500.0),
        Triple("2026-10-26", "coffee.cups", 1.0),
        Triple("2026-10-26", "reading.minutes", 15.0),
        Triple("2026-10-26", "screen.app.com.android.chrome", 15.0),
        Triple("2026-10-26", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-10-26", "screen.app.com.instagram.android", 3.0),
        Triple("2026-10-26", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-10-26", "trading.pl_day", 33.23),
        Triple("2026-10-26", "trading.equity", 1648.68),
        Triple("2026-10-27", "sleep.total_minutes", 430.0),
        Triple("2026-10-27", "sleep.deep_minutes", 54.0),
        Triple("2026-10-27", "sleep.rem_minutes", 70.0),
        Triple("2026-10-27", "health.resting_heart_rate", 54.0),
        Triple("2026-10-27", "health.hrv_rmssd", 39.0),
        Triple("2026-10-27", "health.steps", 10201.0),
        Triple("2026-10-27", "kb.swing.sets", 5.0),
        Triple("2026-10-27", "email.unread", 0.0),
        Triple("2026-10-27", "water.ml", 500.0),
        Triple("2026-10-27", "coffee.cups", 1.0),
        Triple("2026-10-27", "study.minutes", 120.0),
        Triple("2026-10-27", "screen.app.com.android.chrome", 68.0),
        Triple("2026-10-27", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-10-27", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-10-27", "trading.pl_day", 26.58),
        Triple("2026-10-27", "trading.equity", 1682.69),
        Triple("2026-10-28", "sleep.total_minutes", 495.0),
        Triple("2026-10-28", "sleep.deep_minutes", 84.0),
        Triple("2026-10-28", "sleep.rem_minutes", 56.0),
        Triple("2026-10-28", "health.resting_heart_rate", 52.0),
        Triple("2026-10-28", "health.hrv_rmssd", 53.0),
        Triple("2026-10-28", "health.steps", 8557.0),
        Triple("2026-10-28", "workout.sessions", 1.0),
        Triple("2026-10-28", "workout.minutes", 52.0),
        Triple("2026-10-28", "github.commits", 2.0),
        Triple("2026-10-28", "email.unread", 14.0),
        Triple("2026-10-28", "journal.entries", 2.0),
        Triple("2026-10-28", "study.minutes", 60.0),
        Triple("2026-10-28", "screen.app.com.android.chrome", 28.0),
        Triple("2026-10-28", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-10-28", "screen.app.com.instagram.android", 18.0),
        Triple("2026-10-28", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-10-28", "trading.pl_day", -32.73),
        Triple("2026-10-28", "trading.equity", 1726.82),
        Triple("2026-10-29", "sleep.total_minutes", 300.0),
        Triple("2026-10-29", "sleep.deep_minutes", 90.0),
        Triple("2026-10-29", "sleep.rem_minutes", 52.0),
        Triple("2026-10-29", "health.resting_heart_rate", 62.0),
        Triple("2026-10-29", "health.hrv_rmssd", 48.0),
        Triple("2026-10-29", "kb.swing.sets", 5.0),
        Triple("2026-10-29", "cal.abc0def.done", 1.0),
        Triple("2026-10-29", "github.commits", 6.0),
        Triple("2026-10-29", "reading.minutes", 15.0),
        Triple("2026-10-29", "screen.app.com.android.chrome", 119.0),
        Triple("2026-10-29", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-29", "screen.app.com.reddit.frontpage", 3.0),
        Triple("2026-10-29", "screen.app.com.google.android.youtube", 45.0),
        Triple("2026-10-30", "sleep.total_minutes", 520.0),
        Triple("2026-10-30", "sleep.deep_minutes", 46.0),
        Triple("2026-10-30", "sleep.rem_minutes", 52.0),
        Triple("2026-10-30", "health.resting_heart_rate", 62.0),
        Triple("2026-10-30", "health.hrv_rmssd", 38.0),
        Triple("2026-10-30", "health.steps", 14569.0),
        Triple("2026-10-30", "workout.sessions", 1.0),
        Triple("2026-10-30", "workout.minutes", 52.0),
        Triple("2026-10-30", "workout.counted_sessions", 1.0),
        Triple("2026-10-30", "cal.abc0def.done", 1.0),
        Triple("2026-10-30", "github.commits", 6.0),
        Triple("2026-10-30", "journal.entries", 2.0),
        Triple("2026-10-30", "water.ml", 750.0),
        Triple("2026-10-30", "screen.app.com.android.chrome", 23.0),
        Triple("2026-10-30", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-30", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-10-30", "trading.pl_day", -1.14),
        Triple("2026-10-30", "trading.equity", 1789.0),
        Triple("2026-10-31", "sleep.total_minutes", 430.0),
        Triple("2026-10-31", "sleep.deep_minutes", 102.0),
        Triple("2026-10-31", "sleep.rem_minutes", 87.0),
        Triple("2026-10-31", "health.resting_heart_rate", 56.0),
        Triple("2026-10-31", "health.steps", 3441.0),
        Triple("2026-10-31", "football.max_catch_yards", 17.0),
        Triple("2026-10-31", "github.commits", 1.0),
        Triple("2026-10-31", "water.ml", 750.0),
        Triple("2026-10-31", "coffee.cups", 1.0),
        Triple("2026-10-31", "reading.minutes", 15.0),
        Triple("2026-10-31", "study.minutes", 120.0),
        Triple("2026-10-31", "screen.app.com.android.chrome", 51.0),
        Triple("2026-10-31", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-01", "sleep.total_minutes", 520.0),
        Triple("2026-11-01", "sleep.deep_minutes", 90.0),
        Triple("2026-11-01", "sleep.rem_minutes", 53.0),
        Triple("2026-11-01", "health.resting_heart_rate", 59.0),
        Triple("2026-11-01", "health.steps", 13414.0),
        Triple("2026-11-01", "workout.sessions", 2.0),
        Triple("2026-11-01", "workout.minutes", 34.0),
        Triple("2026-11-01", "workout.counted_sessions", 1.0),
        Triple("2026-11-01", "kb.clean.done", 1.0),
        Triple("2026-11-01", "cal.abc0def.done", 1.0),
        Triple("2026-11-01", "email.unread", 11.0),
        Triple("2026-11-01", "journal.entries", 2.0),
        Triple("2026-11-01", "coffee.cups", 2.0),
        Triple("2026-11-01", "screen.app.com.android.chrome", 40.0),
        Triple("2026-11-01", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-01", "screen.app.com.instagram.android", 9.0),
        Triple("2026-11-01", "screen.app.com.reddit.frontpage", 10.0),
        Triple("2026-11-01", "screen.app.com.google.android.youtube", 45.0),
        Triple("2026-11-02", "sleep.total_minutes", 470.0),
        Triple("2026-11-02", "sleep.deep_minutes", 94.0),
        Triple("2026-11-02", "sleep.rem_minutes", 53.0),
        Triple("2026-11-02", "health.resting_heart_rate", 62.0),
        Triple("2026-11-02", "health.steps", 10579.0),
        Triple("2026-11-02", "kb.clean.done", 1.0),
        Triple("2026-11-02", "cal.abc1def.done", 1.0),
        Triple("2026-11-02", "github.commits", 5.0),
        Triple("2026-11-02", "email.unread", 15.0),
        Triple("2026-11-02", "journal.entries", 2.0),
        Triple("2026-11-02", "water.ml", 750.0),
        Triple("2026-11-02", "coffee.cups", 1.0),
        Triple("2026-11-02", "reading.minutes", 90.0),
        Triple("2026-11-02", "study.minutes", 120.0),
        Triple("2026-11-02", "screen.app.com.android.chrome", 31.0),
        Triple("2026-11-02", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-02", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-02", "trading.pl_day", 3.82),
        Triple("2026-11-02", "trading.equity", 1497.63),
        Triple("2026-11-03", "sleep.total_minutes", 360.0),
        Triple("2026-11-03", "sleep.deep_minutes", 80.0),
        Triple("2026-11-03", "sleep.rem_minutes", 80.0),
        Triple("2026-11-03", "health.resting_heart_rate", 64.0),
        Triple("2026-11-03", "health.steps", 13782.0),
        Triple("2026-11-03", "kb.swing.sets", 5.0),
        Triple("2026-11-03", "github.commits", 5.0),
        Triple("2026-11-03", "journal.entries", 1.0),
        Triple("2026-11-03", "water.ml", 500.0),
        Triple("2026-11-03", "coffee.cups", 3.0),
        Triple("2026-11-03", "screen.app.com.android.chrome", 116.0),
        Triple("2026-11-03", "screen.app.com.instagram.android", 27.0),
        Triple("2026-11-03", "trading.pl_day", 6.97),
        Triple("2026-11-03", "trading.equity", 1400.07),
        Triple("2026-11-04", "sleep.total_minutes", 520.0),
        Triple("2026-11-04", "sleep.deep_minutes", 97.0),
        Triple("2026-11-04", "sleep.rem_minutes", 81.0),
        Triple("2026-11-04", "health.resting_heart_rate", 55.0),
        Triple("2026-11-04", "health.hrv_rmssd", 46.0),
        Triple("2026-11-04", "workout.sessions", 2.0),
        Triple("2026-11-04", "workout.minutes", 15.0),
        Triple("2026-11-04", "workout.counted_sessions", 1.0),
        Triple("2026-11-04", "kb.swing.sets", 2.0),
        Triple("2026-11-04", "water.ml", 750.0),
        Triple("2026-11-04", "coffee.cups", 3.0),
        Triple("2026-11-04", "reading.minutes", 45.0),
        Triple("2026-11-04", "study.minutes", 120.0),
        Triple("2026-11-04", "screen.app.com.android.chrome", 73.0),
        Triple("2026-11-04", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-04", "screen.app.com.instagram.android", 18.0),
        Triple("2026-11-04", "trading.pl_day", -21.22),
        Triple("2026-11-04", "trading.equity", 1498.82),
        Triple("2026-11-05", "health.resting_heart_rate", 56.0),
        Triple("2026-11-05", "health.hrv_rmssd", 36.0),
        Triple("2026-11-05", "health.steps", 12550.0),
        Triple("2026-11-05", "kb.swing.sets", 2.0),
        Triple("2026-11-05", "cal.abc0def.done", 1.0),
        Triple("2026-11-05", "journal.entries", 2.0),
        Triple("2026-11-05", "water.ml", 750.0),
        Triple("2026-11-05", "study.minutes", 20.0),
        Triple("2026-11-05", "screen.app.com.android.chrome", 109.0),
        Triple("2026-11-05", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-05", "screen.app.com.instagram.android", 18.0),
        Triple("2026-11-05", "screen.app.com.reddit.frontpage", 21.0),
        Triple("2026-11-05", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-05", "trading.pl_day", -1.2),
        Triple("2026-11-05", "trading.equity", 1764.16),
        Triple("2026-11-06", "sleep.total_minutes", 405.0),
        Triple("2026-11-06", "sleep.deep_minutes", 90.0),
        Triple("2026-11-06", "sleep.rem_minutes", 56.0),
        Triple("2026-11-06", "health.resting_heart_rate", 61.0),
        Triple("2026-11-06", "health.hrv_rmssd", 31.0),
        Triple("2026-11-06", "health.steps", 4516.0),
        Triple("2026-11-06", "workout.sessions", 2.0),
        Triple("2026-11-06", "workout.minutes", 56.0),
        Triple("2026-11-06", "workout.counted_sessions", 1.0),
        Triple("2026-11-06", "kb.swing.sets", 2.0),
        Triple("2026-11-06", "football.max_catch_yards", 18.0),
        Triple("2026-11-06", "cal.abc0def.done", 1.0),
        Triple("2026-11-06", "cal.abc2def.done", 1.0),
        Triple("2026-11-06", "github.commits", 3.0),
        Triple("2026-11-06", "journal.entries", 1.0),
        Triple("2026-11-06", "water.ml", 750.0),
        Triple("2026-11-06", "coffee.cups", 1.0),
        Triple("2026-11-06", "screen.app.com.android.chrome", 103.0),
        Triple("2026-11-06", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-06", "trading.pl_day", 3.32),
        Triple("2026-11-06", "trading.equity", 1578.54),
        Triple("2026-11-07", "sleep.total_minutes", 520.0),
        Triple("2026-11-07", "sleep.deep_minutes", 43.0),
        Triple("2026-11-07", "sleep.rem_minutes", 102.0),
        Triple("2026-11-07", "health.resting_heart_rate", 62.0),
        Triple("2026-11-07", "health.steps", 2071.0),
        Triple("2026-11-07", "workout.sessions", 1.0),
        Triple("2026-11-07", "workout.minutes", 26.0),
        Triple("2026-11-07", "workout.counted_sessions", 1.0),
        Triple("2026-11-07", "kb.swing.sets", 5.0),
        Triple("2026-11-07", "cal.abc0def.done", 1.0),
        Triple("2026-11-07", "github.commits", 6.0),
        Triple("2026-11-07", "reading.minutes", 30.0),
        Triple("2026-11-07", "study.minutes", 120.0),
        Triple("2026-11-08", "health.resting_heart_rate", 56.0),
        Triple("2026-11-08", "health.steps", 3674.0),
        Triple("2026-11-08", "kb.swing.sets", 3.0),
        Triple("2026-11-08", "cal.abc0def.done", 1.0),
        Triple("2026-11-08", "cal.abc2def.done", 1.0),
        Triple("2026-11-08", "journal.entries", 1.0),
        Triple("2026-11-08", "water.ml", 250.0),
        Triple("2026-11-08", "screen.app.com.android.chrome", 59.0),
        Triple("2026-11-08", "screen.app.com.reddit.frontpage", 4.0),
        Triple("2026-11-08", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-09", "sleep.total_minutes", 520.0),
        Triple("2026-11-09", "sleep.deep_minutes", 62.0),
        Triple("2026-11-09", "sleep.rem_minutes", 79.0),
        Triple("2026-11-09", "health.resting_heart_rate", 59.0),
        Triple("2026-11-09", "health.steps", 13754.0),
        Triple("2026-11-09", "cal.abc0def.done", 1.0),
        Triple("2026-11-09", "github.commits", 6.0),
        Triple("2026-11-09", "email.unread", 14.0),
        Triple("2026-11-09", "journal.entries", 1.0),
        Triple("2026-11-09", "water.ml", 750.0),
        Triple("2026-11-09", "study.minutes", 60.0),
        Triple("2026-11-09", "screen.app.com.android.chrome", 36.0),
        Triple("2026-11-09", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-09", "screen.app.com.reddit.frontpage", 16.0),
        Triple("2026-11-09", "trading.pl_day", -2.02),
        Triple("2026-11-09", "trading.equity", 1727.64),
        Triple("2026-11-10", "sleep.total_minutes", 470.0),
        Triple("2026-11-10", "sleep.deep_minutes", 45.0),
        Triple("2026-11-10", "sleep.rem_minutes", 87.0),
        Triple("2026-11-10", "health.resting_heart_rate", 52.0),
        Triple("2026-11-10", "health.hrv_rmssd", 56.0),
        Triple("2026-11-10", "health.steps", 2730.0),
        Triple("2026-11-10", "workout.sessions", 1.0),
        Triple("2026-11-10", "workout.minutes", 38.0),
        Triple("2026-11-10", "football.max_catch_yards", 20.0),
        Triple("2026-11-10", "cal.abc1def.done", 1.0),
        Triple("2026-11-10", "cal.abc2def.done", 1.0),
        Triple("2026-11-10", "cal.abc3def.done", 1.0),
        Triple("2026-11-10", "github.commits", 3.0),
        Triple("2026-11-10", "email.unread", 23.0),
        Triple("2026-11-10", "water.ml", 250.0),
        Triple("2026-11-10", "study.minutes", 120.0),
        Triple("2026-11-10", "screen.app.com.android.chrome", 31.0),
        Triple("2026-11-10", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-10", "screen.app.com.reddit.frontpage", 5.0),
        Triple("2026-11-10", "trading.pl_day", -27.64),
        Triple("2026-11-10", "trading.equity", 1613.6),
        Triple("2026-11-11", "sleep.total_minutes", 495.0),
        Triple("2026-11-11", "sleep.deep_minutes", 74.0),
        Triple("2026-11-11", "sleep.rem_minutes", 102.0),
        Triple("2026-11-11", "health.steps", 2341.0),
        Triple("2026-11-11", "workout.sessions", 2.0),
        Triple("2026-11-11", "workout.minutes", 36.0),
        Triple("2026-11-11", "workout.counted_sessions", 1.0),
        Triple("2026-11-11", "cal.abc0def.done", 1.0),
        Triple("2026-11-11", "github.commits", 1.0),
        Triple("2026-11-11", "journal.entries", 1.0),
        Triple("2026-11-11", "coffee.cups", 2.0),
        Triple("2026-11-11", "study.minutes", 20.0),
        Triple("2026-11-11", "screen.app.com.android.chrome", 23.0),
        Triple("2026-11-11", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-11", "screen.app.com.instagram.android", 18.0),
        Triple("2026-11-11", "screen.app.com.google.android.youtube", 45.0),
        Triple("2026-11-11", "trading.pl_day", 1.69),
        Triple("2026-11-11", "trading.equity", 1770.2),
        Triple("2026-11-12", "sleep.total_minutes", 520.0),
        Triple("2026-11-12", "sleep.deep_minutes", 65.0),
        Triple("2026-11-12", "sleep.rem_minutes", 88.0),
        Triple("2026-11-12", "health.resting_heart_rate", 52.0),
        Triple("2026-11-12", "health.steps", 2374.0),
        Triple("2026-11-12", "kb.clean.done", 1.0),
        Triple("2026-11-12", "water.ml", 1500.0),
        Triple("2026-11-12", "coffee.cups", 1.0),
        Triple("2026-11-12", "reading.minutes", 30.0),
        Triple("2026-11-12", "study.minutes", 20.0),
        Triple("2026-11-12", "screen.app.com.android.chrome", 97.0),
        Triple("2026-11-12", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-12", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-12", "trading.pl_day", 4.0),
        Triple("2026-11-12", "trading.equity", 1650.82),
        Triple("2026-11-13", "sleep.total_minutes", 495.0),
        Triple("2026-11-13", "sleep.deep_minutes", 79.0),
        Triple("2026-11-13", "sleep.rem_minutes", 81.0),
        Triple("2026-11-13", "health.resting_heart_rate", 62.0),
        Triple("2026-11-13", "health.steps", 4428.0),
        Triple("2026-11-13", "workout.sessions", 2.0),
        Triple("2026-11-13", "workout.minutes", 39.0),
        Triple("2026-11-13", "workout.counted_sessions", 1.0),
        Triple("2026-11-13", "github.commits", 1.0),
        Triple("2026-11-13", "email.unread", 13.0),
        Triple("2026-11-13", "journal.entries", 2.0),
        Triple("2026-11-13", "water.ml", 250.0),
        Triple("2026-11-13", "coffee.cups", 1.0),
        Triple("2026-11-13", "reading.minutes", 45.0),
        Triple("2026-11-13", "screen.app.com.android.chrome", 11.0),
        Triple("2026-11-13", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-13", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-11-14", "sleep.total_minutes", 360.0),
        Triple("2026-11-14", "sleep.deep_minutes", 64.0),
        Triple("2026-11-14", "sleep.rem_minutes", 66.0),
        Triple("2026-11-14", "kb.swing.sets", 2.0),
        Triple("2026-11-14", "kb.clean.done", 1.0),
        Triple("2026-11-14", "cal.abc0def.done", 1.0),
        Triple("2026-11-14", "github.commits", 4.0),
        Triple("2026-11-14", "email.unread", 16.0),
        Triple("2026-11-14", "water.ml", 750.0),
        Triple("2026-11-14", "reading.minutes", 45.0),
        Triple("2026-11-14", "study.minutes", 20.0),
        Triple("2026-11-14", "screen.app.com.android.chrome", 86.0),
        Triple("2026-11-14", "screen.app.com.google.android.youtube", 45.0),
        Triple("2026-11-15", "sleep.total_minutes", 300.0),
        Triple("2026-11-15", "sleep.deep_minutes", 86.0),
        Triple("2026-11-15", "sleep.rem_minutes", 107.0),
        Triple("2026-11-15", "health.resting_heart_rate", 61.0),
        Triple("2026-11-15", "health.steps", 5629.0),
        Triple("2026-11-15", "cal.abc0def.done", 1.0),
        Triple("2026-11-15", "cal.abc1def.done", 1.0),
        Triple("2026-11-15", "github.commits", 3.0),
        Triple("2026-11-15", "journal.entries", 1.0),
        Triple("2026-11-15", "reading.minutes", 45.0),
        Triple("2026-11-15", "study.minutes", 120.0),
        Triple("2026-11-16", "sleep.total_minutes", 300.0),
        Triple("2026-11-16", "sleep.deep_minutes", 44.0),
        Triple("2026-11-16", "sleep.rem_minutes", 78.0),
        Triple("2026-11-16", "health.resting_heart_rate", 61.0),
        Triple("2026-11-16", "health.steps", 7465.0),
        Triple("2026-11-16", "kb.swing.sets", 2.0),
        Triple("2026-11-16", "football.max_catch_yards", 10.0),
        Triple("2026-11-16", "journal.entries", 1.0),
        Triple("2026-11-16", "water.ml", 750.0),
        Triple("2026-11-16", "coffee.cups", 1.0),
        Triple("2026-11-16", "reading.minutes", 90.0),
        Triple("2026-11-16", "study.minutes", 20.0),
        Triple("2026-11-16", "trading.pl_day", -28.05),
        Triple("2026-11-16", "trading.equity", 1438.32),
        Triple("2026-11-17", "sleep.total_minutes", 450.0),
        Triple("2026-11-17", "sleep.deep_minutes", 91.0),
        Triple("2026-11-17", "sleep.rem_minutes", 83.0),
        Triple("2026-11-17", "health.hrv_rmssd", 56.0),
        Triple("2026-11-17", "health.steps", 7239.0),
        Triple("2026-11-17", "cal.abc0def.done", 1.0),
        Triple("2026-11-17", "cal.abc2def.done", 1.0),
        Triple("2026-11-17", "journal.entries", 1.0),
        Triple("2026-11-17", "coffee.cups", 3.0),
        Triple("2026-11-17", "screen.app.com.android.chrome", 30.0),
        Triple("2026-11-17", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-17", "trading.pl_day", -15.25),
        Triple("2026-11-17", "trading.equity", 1520.11),
        Triple("2026-11-18", "sleep.total_minutes", 520.0),
        Triple("2026-11-18", "sleep.deep_minutes", 108.0),
        Triple("2026-11-18", "sleep.rem_minutes", 50.0),
        Triple("2026-11-18", "health.resting_heart_rate", 58.0),
        Triple("2026-11-18", "health.steps", 13652.0),
        Triple("2026-11-18", "kb.swing.sets", 1.0),
        Triple("2026-11-18", "github.commits", 6.0),
        Triple("2026-11-18", "water.ml", 250.0),
        Triple("2026-11-18", "coffee.cups", 3.0),
        Triple("2026-11-18", "screen.app.com.android.chrome", 38.0),
        Triple("2026-11-18", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-18", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-18", "screen.app.com.reddit.frontpage", 30.0),
        Triple("2026-11-18", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-19", "sleep.total_minutes", 470.0),
        Triple("2026-11-19", "sleep.deep_minutes", 64.0),
        Triple("2026-11-19", "sleep.rem_minutes", 99.0),
        Triple("2026-11-19", "health.resting_heart_rate", 55.0),
        Triple("2026-11-19", "health.steps", 12851.0),
        Triple("2026-11-19", "cal.abc2def.done", 1.0),
        Triple("2026-11-19", "journal.entries", 1.0),
        Triple("2026-11-19", "water.ml", 500.0),
        Triple("2026-11-19", "coffee.cups", 1.0),
        Triple("2026-11-19", "reading.minutes", 30.0),
        Triple("2026-11-19", "study.minutes", 20.0),
        Triple("2026-11-19", "screen.app.com.android.chrome", 8.0),
        Triple("2026-11-19", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-19", "screen.app.com.reddit.frontpage", 4.0),
        Triple("2026-11-19", "trading.pl_day", 7.24),
        Triple("2026-11-19", "trading.equity", 1545.36),
        Triple("2026-11-20", "sleep.total_minutes", 360.0),
        Triple("2026-11-20", "sleep.deep_minutes", 89.0),
        Triple("2026-11-20", "sleep.rem_minutes", 63.0),
        Triple("2026-11-20", "health.resting_heart_rate", 55.0),
        Triple("2026-11-20", "health.hrv_rmssd", 31.0),
        Triple("2026-11-20", "kb.clean.done", 1.0),
        Triple("2026-11-20", "cal.abc0def.done", 1.0),
        Triple("2026-11-20", "cal.abc1def.done", 1.0),
        Triple("2026-11-20", "journal.entries", 2.0),
        Triple("2026-11-20", "water.ml", 250.0),
        Triple("2026-11-20", "coffee.cups", 2.0),
        Triple("2026-11-20", "reading.minutes", 45.0),
        Triple("2026-11-20", "screen.app.com.android.chrome", 82.0),
        Triple("2026-11-20", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-20", "screen.app.com.reddit.frontpage", 15.0),
        Triple("2026-11-20", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-11-20", "trading.pl_day", -12.26),
        Triple("2026-11-20", "trading.equity", 1681.86),
        Triple("2026-11-21", "sleep.total_minutes", 430.0),
        Triple("2026-11-21", "sleep.deep_minutes", 51.0),
        Triple("2026-11-21", "sleep.rem_minutes", 86.0),
        Triple("2026-11-21", "health.resting_heart_rate", 52.0),
        Triple("2026-11-21", "health.steps", 13796.0),
        Triple("2026-11-21", "kb.swing.sets", 4.0),
        Triple("2026-11-21", "kb.clean.done", 1.0),
        Triple("2026-11-21", "journal.entries", 1.0),
        Triple("2026-11-21", "water.ml", 500.0),
        Triple("2026-11-21", "reading.minutes", 30.0),
        Triple("2026-11-21", "study.minutes", 120.0),
        Triple("2026-11-21", "screen.app.com.android.chrome", 67.0),
        Triple("2026-11-21", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-21", "screen.app.com.reddit.frontpage", 12.0),
        Triple("2026-11-21", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-22", "health.steps", 12081.0),
        Triple("2026-11-22", "workout.sessions", 1.0),
        Triple("2026-11-22", "workout.minutes", 29.0),
        Triple("2026-11-22", "workout.counted_sessions", 1.0),
        Triple("2026-11-22", "github.commits", 5.0),
        Triple("2026-11-22", "coffee.cups", 2.0),
        Triple("2026-11-22", "screen.app.com.android.chrome", 75.0),
        Triple("2026-11-22", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-22", "screen.app.com.instagram.android", 18.0),
        Triple("2026-11-22", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-23", "sleep.total_minutes", 430.0),
        Triple("2026-11-23", "sleep.deep_minutes", 104.0),
        Triple("2026-11-23", "sleep.rem_minutes", 74.0),
        Triple("2026-11-23", "health.resting_heart_rate", 64.0),
        Triple("2026-11-23", "health.steps", 4032.0),
        Triple("2026-11-23", "cal.abc0def.done", 1.0),
        Triple("2026-11-23", "cal.abc1def.done", 1.0),
        Triple("2026-11-23", "github.commits", 6.0),
        Triple("2026-11-23", "journal.entries", 1.0),
        Triple("2026-11-23", "water.ml", 500.0),
        Triple("2026-11-23", "screen.app.com.android.chrome", 30.0),
        Triple("2026-11-23", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-11-23", "screen.app.com.instagram.android", 27.0),
        Triple("2026-11-23", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-23", "trading.pl_day", 0.04),
        Triple("2026-11-23", "trading.equity", 1652.95),
        Triple("2026-11-24", "sleep.total_minutes", 405.0),
        Triple("2026-11-24", "sleep.deep_minutes", 72.0),
        Triple("2026-11-24", "sleep.rem_minutes", 101.0),
        Triple("2026-11-24", "health.resting_heart_rate", 55.0),
        Triple("2026-11-24", "health.steps", 10904.0),
        Triple("2026-11-24", "cal.abc3def.done", 1.0),
        Triple("2026-11-24", "github.commits", 6.0),
        Triple("2026-11-24", "water.ml", 1500.0),
        Triple("2026-11-24", "study.minutes", 60.0),
        Triple("2026-11-24", "screen.app.com.android.chrome", 7.0),
        Triple("2026-11-24", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-24", "trading.pl_day", -13.76),
        Triple("2026-11-24", "trading.equity", 1404.25),
        Triple("2026-11-25", "sleep.total_minutes", 360.0),
        Triple("2026-11-25", "sleep.deep_minutes", 44.0),
        Triple("2026-11-25", "sleep.rem_minutes", 82.0),
        Triple("2026-11-25", "health.resting_heart_rate", 54.0),
        Triple("2026-11-25", "workout.sessions", 2.0),
        Triple("2026-11-25", "workout.minutes", 16.0),
        Triple("2026-11-25", "journal.entries", 2.0),
        Triple("2026-11-25", "coffee.cups", 3.0),
        Triple("2026-11-25", "reading.minutes", 15.0),
        Triple("2026-11-25", "screen.app.com.android.chrome", 86.0),
        Triple("2026-11-25", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-25", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-25", "screen.app.com.reddit.frontpage", 15.0),
        Triple("2026-11-25", "trading.pl_day", 13.99),
        Triple("2026-11-25", "trading.equity", 1632.07),
        Triple("2026-11-26", "sleep.total_minutes", 450.0),
        Triple("2026-11-26", "sleep.deep_minutes", 91.0),
        Triple("2026-11-26", "sleep.rem_minutes", 117.0),
        Triple("2026-11-26", "workout.sessions", 1.0),
        Triple("2026-11-26", "workout.minutes", 18.0),
        Triple("2026-11-26", "kb.swing.sets", 2.0),
        Triple("2026-11-26", "github.commits", 6.0),
        Triple("2026-11-26", "coffee.cups", 2.0),
        Triple("2026-11-26", "screen.app.com.android.chrome", 65.0),
        Triple("2026-11-26", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-26", "screen.app.com.instagram.android", 27.0),
        Triple("2026-11-26", "trading.pl_day", -1.47),
        Triple("2026-11-26", "trading.equity", 1722.17),
        Triple("2026-11-27", "sleep.total_minutes", 470.0),
        Triple("2026-11-27", "sleep.deep_minutes", 71.0),
        Triple("2026-11-27", "sleep.rem_minutes", 88.0),
        Triple("2026-11-27", "health.resting_heart_rate", 59.0),
        Triple("2026-11-27", "health.steps", 12301.0),
        Triple("2026-11-27", "kb.swing.sets", 3.0),
        Triple("2026-11-27", "football.max_catch_yards", 19.0),
        Triple("2026-11-27", "cal.abc1def.done", 1.0),
        Triple("2026-11-27", "journal.entries", 1.0),
        Triple("2026-11-27", "water.ml", 250.0),
        Triple("2026-11-27", "reading.minutes", 15.0),
        Triple("2026-11-27", "screen.app.com.android.chrome", 28.0),
        Triple("2026-11-27", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-27", "screen.app.com.reddit.frontpage", 14.0),
        Triple("2026-11-27", "trading.pl_day", 31.29),
        Triple("2026-11-27", "trading.equity", 1643.35),
        Triple("2026-11-28", "sleep.total_minutes", 450.0),
        Triple("2026-11-28", "sleep.deep_minutes", 65.0),
        Triple("2026-11-28", "sleep.rem_minutes", 113.0),
        Triple("2026-11-28", "health.resting_heart_rate", 60.0),
        Triple("2026-11-28", "health.hrv_rmssd", 56.0),
        Triple("2026-11-28", "health.steps", 15961.0),
        Triple("2026-11-28", "workout.sessions", 1.0),
        Triple("2026-11-28", "workout.minutes", 26.0),
        Triple("2026-11-28", "workout.counted_sessions", 1.0),
        Triple("2026-11-28", "cal.abc0def.done", 1.0),
        Triple("2026-11-28", "cal.abc1def.done", 1.0),
        Triple("2026-11-28", "cal.abc2def.done", 1.0),
        Triple("2026-11-28", "github.commits", 5.0),
        Triple("2026-11-28", "water.ml", 750.0),
        Triple("2026-11-28", "coffee.cups", 3.0),
        Triple("2026-11-28", "study.minutes", 60.0),
        Triple("2026-11-28", "screen.app.com.android.chrome", 58.0),
        Triple("2026-11-28", "screen.app.com.reddit.frontpage", 13.0),
        Triple("2026-11-28", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-11-29", "sleep.total_minutes", 300.0),
        Triple("2026-11-29", "sleep.deep_minutes", 82.0),
        Triple("2026-11-29", "sleep.rem_minutes", 62.0),
        Triple("2026-11-29", "health.resting_heart_rate", 59.0),
        Triple("2026-11-29", "health.steps", 4995.0),
        Triple("2026-11-29", "cal.abc0def.done", 1.0),
        Triple("2026-11-29", "journal.entries", 2.0),
        Triple("2026-11-29", "water.ml", 250.0),
        Triple("2026-11-29", "reading.minutes", 90.0),
        Triple("2026-11-29", "screen.app.com.android.chrome", 39.0),
        Triple("2026-11-29", "screen.app.com.instagram.android", 9.0),
        Triple("2026-11-30", "sleep.total_minutes", 470.0),
        Triple("2026-11-30", "sleep.deep_minutes", 78.0),
        Triple("2026-11-30", "sleep.rem_minutes", 66.0),
        Triple("2026-11-30", "health.resting_heart_rate", 62.0),
        Triple("2026-11-30", "health.hrv_rmssd", 31.0),
        Triple("2026-11-30", "health.steps", 10581.0),
        Triple("2026-11-30", "kb.clean.done", 1.0),
        Triple("2026-11-30", "github.commits", 4.0),
        Triple("2026-11-30", "screen.app.com.android.chrome", 94.0),
        Triple("2026-11-30", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-30", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-30", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-30", "trading.pl_day", -25.5),
        Triple("2026-11-30", "trading.equity", 1414.79),
        Triple("2026-12-01", "sleep.total_minutes", 300.0),
        Triple("2026-12-01", "sleep.deep_minutes", 87.0),
        Triple("2026-12-01", "sleep.rem_minutes", 67.0),
        Triple("2026-12-01", "health.resting_heart_rate", 60.0),
        Triple("2026-12-01", "health.steps", 4527.0),
        Triple("2026-12-01", "workout.sessions", 2.0),
        Triple("2026-12-01", "workout.minutes", 11.0),
        Triple("2026-12-01", "workout.counted_sessions", 1.0),
        Triple("2026-12-01", "football.max_catch_yards", 19.0),
        Triple("2026-12-01", "cal.abc0def.done", 1.0),
        Triple("2026-12-01", "cal.abc1def.done", 1.0),
        Triple("2026-12-01", "journal.entries", 2.0),
        Triple("2026-12-01", "water.ml", 500.0),
        Triple("2026-12-01", "coffee.cups", 2.0),
        Triple("2026-12-01", "study.minutes", 60.0),
        Triple("2026-12-01", "screen.app.com.android.chrome", 24.0),
        Triple("2026-12-01", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-12-01", "screen.app.com.instagram.android", 3.0),
        Triple("2026-12-01", "trading.pl_day", -30.29),
        Triple("2026-12-01", "trading.equity", 1588.93),
        Triple("2026-12-02", "sleep.total_minutes", 430.0),
        Triple("2026-12-02", "sleep.deep_minutes", 97.0),
        Triple("2026-12-02", "sleep.rem_minutes", 73.0),
        Triple("2026-12-02", "health.steps", 12884.0),
        Triple("2026-12-02", "kb.clean.done", 1.0),
        Triple("2026-12-02", "cal.abc0def.done", 1.0),
        Triple("2026-12-02", "cal.abc2def.done", 1.0),
        Triple("2026-12-02", "journal.entries", 1.0),
        Triple("2026-12-02", "study.minutes", 60.0),
        Triple("2026-12-02", "screen.app.com.android.chrome", 26.0),
        Triple("2026-12-02", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-12-02", "trading.pl_day", -2.42),
        Triple("2026-12-02", "trading.equity", 1466.59),
        Triple("2026-12-03", "health.resting_heart_rate", 62.0),
        Triple("2026-12-03", "health.hrv_rmssd", 55.0),
        Triple("2026-12-03", "health.steps", 7819.0),
        Triple("2026-12-03", "cal.abc0def.done", 1.0),
        Triple("2026-12-03", "cal.abc1def.done", 1.0),
        Triple("2026-12-03", "water.ml", 250.0),
        Triple("2026-12-03", "study.minutes", 60.0),
        Triple("2026-12-03", "trading.pl_day", -9.01),
        Triple("2026-12-03", "trading.equity", 1550.48),
        Triple("2026-12-04", "sleep.total_minutes", 430.0),
        Triple("2026-12-04", "sleep.deep_minutes", 97.0),
        Triple("2026-12-04", "sleep.rem_minutes", 86.0),
        Triple("2026-12-04", "health.resting_heart_rate", 57.0),
        Triple("2026-12-04", "health.hrv_rmssd", 43.0),
        Triple("2026-12-04", "health.steps", 14869.0),
        Triple("2026-12-04", "football.max_catch_yards", 19.0),
        Triple("2026-12-04", "cal.abc0def.done", 1.0),
        Triple("2026-12-04", "coffee.cups", 1.0),
        Triple("2026-12-04", "screen.app.com.android.chrome", 105.0),
        Triple("2026-12-04", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-12-04", "screen.app.com.instagram.android", 3.0),
        Triple("2026-12-04", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-12-05", "sleep.total_minutes", 300.0),
        Triple("2026-12-05", "sleep.deep_minutes", 89.0),
        Triple("2026-12-05", "sleep.rem_minutes", 108.0),
        Triple("2026-12-05", "health.resting_heart_rate", 60.0),
        Triple("2026-12-05", "health.steps", 5315.0),
        Triple("2026-12-05", "workout.sessions", 2.0),
        Triple("2026-12-05", "workout.minutes", 66.0),
        Triple("2026-12-05", "kb.swing.sets", 5.0),
        Triple("2026-12-05", "football.max_catch_yards", 13.0),
        Triple("2026-12-05", "github.commits", 6.0),
        Triple("2026-12-05", "email.unread", 18.0),
        Triple("2026-12-05", "journal.entries", 2.0),
        Triple("2026-12-05", "reading.minutes", 90.0),
        Triple("2026-12-05", "study.minutes", 20.0),
        Triple("2026-12-05", "screen.app.com.android.chrome", 64.0),
        Triple("2026-12-05", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-12-05", "screen.app.com.instagram.android", 3.0),
        Triple("2026-12-05", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-12-06", "sleep.total_minutes", 430.0),
        Triple("2026-12-06", "sleep.deep_minutes", 102.0),
        Triple("2026-12-06", "sleep.rem_minutes", 77.0),
        Triple("2026-12-06", "health.resting_heart_rate", 64.0),
        Triple("2026-12-06", "health.hrv_rmssd", 33.0),
        Triple("2026-12-06", "github.commits", 2.0),
        Triple("2026-12-06", "journal.entries", 1.0),
        Triple("2026-12-06", "water.ml", 1500.0),
        Triple("2026-12-06", "coffee.cups", 1.0),
        Triple("2026-12-07", "sleep.total_minutes", 360.0),
        Triple("2026-12-07", "sleep.deep_minutes", 72.0),
        Triple("2026-12-07", "sleep.rem_minutes", 90.0),
        Triple("2026-12-07", "health.resting_heart_rate", 62.0),
        Triple("2026-12-07", "health.hrv_rmssd", 59.0),
        Triple("2026-12-07", "health.steps", 7940.0),
        Triple("2026-12-07", "workout.sessions", 1.0),
        Triple("2026-12-07", "workout.minutes", 33.0),
        Triple("2026-12-07", "football.max_catch_yards", 14.0),
        Triple("2026-12-07", "cal.abc0def.done", 1.0),
        Triple("2026-12-07", "github.commits", 1.0),
        Triple("2026-12-07", "email.unread", 16.0),
        Triple("2026-12-07", "reading.minutes", 30.0),
        Triple("2026-12-07", "study.minutes", 20.0),
        Triple("2026-12-07", "trading.pl_day", -16.1),
        Triple("2026-12-07", "trading.equity", 1636.59),
        Triple("2026-11-12", "perk.wis.1.claimed", 1.0),
        Triple("2026-11-17", "perk.con.1.claimed", 1.0),
        Triple("2026-11-22", "perk.per.2.claimed", 1.0),
        Triple("2026-11-25", "perk.wis.4.claimed", 1.0),
        Triple("2026-12-02", "perk.str.1.claimed", 1.0),
    )

    private val days: Map<LocalDate, Map<String, Double>> = rows.groupBy { LocalDate.parse(it.first) }
        .mapValues { (_, rs) -> DayMetrics.build(rs.map { it.second to it.third }) }

    private fun at(day: String, share: Double): Game.State {
        val d = LocalDate.parse(day)
        val seconds = Math.round(share * 86400).toLong()
        // share 1.0 is the end of the day, which todayElapsed reads as a past day.
        val now = if (share >= 1.0) d.plusDays(1).atStartOfDay() else d.atStartOfDay().plusSeconds(seconds)
        return Game.build(days, d, now = now)
    }

    @Test
    fun case0() {
        val g = at("2026-11-02", 1.0)
        assertEquals(5453, g.totalXp)
        assertEquals(8, g.level.level)
        assertEquals(693, g.level.levelXp)
        assertEquals(11, g.dayNumber)
        assertEquals(605, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("Keeper of the Mind", g.title)
        assertEquals(496, g.avgDailyXp14d)
        assertEquals(790, g.daysToMaxLevel)
        assertEquals(listOf(605, 290, 649, 605, 390, 572, 310, 621, 195, 611, 605), g.history.map { it.xp })
        assertEquals(listOf("sleep=75", "heart=0", "steps=100", "workout=0", "kettlebell=75", "catch=0", "calendar=50", "github=100", "email=0", "journal=100", "youtube=0", "social=0", "stocks=50"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Well-Rested=55"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=725/10", "heart=345/8", "steps=700/11", "workout=495/7", "kettlebell=300/8", "catch=150/6", "calendar=300/5", "github=525/9", "email=150/6", "journal=775/12", "youtube=350/11", "social=0/1", "stocks=300/8"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=8/0", "con=11/1", "int=9/0", "wis=12/1", "cha=8/0", "per=12/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=100/100", "stamina=1/1", "mana=3/3", "focus=40/40"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=2", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case1() {
        val g = at("2026-11-12", 1.0)
        assertEquals(10869, g.totalXp)
        assertEquals(13, g.level.level)
        assertEquals(909, g.level.levelXp)
        assertEquals(21, g.dayNumber)
        assertEquals(451, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("Keeper of the Mind", g.title)
        assertEquals(532, g.avgDailyXp14d)
        assertEquals(726, g.daysToMaxLevel)
        assertEquals(listOf(605, 290, 649, 605, 390, 572, 310, 621, 195, 611, 605, 600, 413, 390, 770, 435, 420, 677, 671, 589, 451), g.history.map { it.xp })
        assertEquals(listOf("sleep=75", "heart=40", "steps=20", "workout=0", "kettlebell=75", "catch=0", "calendar=0", "github=0", "email=0", "journal=0", "youtube=50", "social=100", "stocks=50"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Unrestricted Media Pass=0", "Well-Rested=41"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1325/15", "heart=695/13", "steps=1150/16", "workout=990/11", "kettlebell=825/15", "catch=250/9", "calendar=850/10", "github=1000/14", "email=150/6", "journal=1250/16", "youtube=700/18", "social=450/8", "stocks=600/12"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=15/1", "con=16/1", "int=14/1", "wis=18/1", "cha=12/1", "per=16/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=101/101", "stamina=16/16", "mana=0/0", "focus=40/40"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=1", "well_rested=4", "committed=3"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case2() {
        val g = at("2026-11-17", 1.0)
        assertEquals(12882, g.totalXp)
        assertEquals(15, g.level.level)
        assertEquals(422, g.level.levelXp)
        assertEquals(26, g.dayNumber)
        assertEquals(473, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("Keeper of the Mind", g.title)
        assertEquals(488, g.avgDailyXp14d)
        assertEquals(788, g.daysToMaxLevel)
        assertEquals(listOf(605, 290, 649, 605, 390, 572, 310, 621, 195, 611, 605, 600, 413, 390, 770, 435, 420, 677, 671, 589, 451, 445, 425, 350, 320, 473), g.history.map { it.xp })
        assertEquals(listOf("sleep=125", "heart=35", "steps=70", "workout=0", "kettlebell=0", "catch=0", "calendar=100", "github=0", "email=0", "journal=75", "youtube=50", "social=-50", "stocks=25"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Well-Rested=43"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1675/17", "heart=730/14", "steps=1380/17", "workout=1105/12", "kettlebell=975/17", "catch=300/10", "calendar=1100/12", "github=1200/16", "email=150/6", "journal=1575/19", "youtube=850/20", "social=500/9", "stocks=625/13"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=17/1", "con=17/1", "int=16/1", "wis=20/1", "cha=13/1", "per=19/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=87/87", "stamina=25/25", "mana=38/38", "focus=15/15"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=1", "committed=0"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case3() {
        val g = at("2026-11-22", 1.0)
        assertEquals(14728, g.totalXp)
        assertEquals(16, g.level.level)
        assertEquals(928, g.level.levelXp)
        assertEquals(31, g.dayNumber)
        assertEquals(275, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("Keeper of the Mind", g.title)
        assertEquals(446, g.avgDailyXp14d)
        assertEquals(857, g.daysToMaxLevel)
        assertEquals(listOf(290, 649, 605, 390, 572, 310, 621, 195, 611, 605, 600, 413, 390, 770, 435, 420, 677, 671, 589, 451, 445, 425, 350, 320, 473, 484, 457, 390, 240, 275), g.history.map { it.xp })
        assertEquals(listOf("sleep=0", "heart=0", "steps=100", "workout=75", "kettlebell=0", "catch=0", "calendar=0", "github=100", "email=0", "journal=0", "youtube=0", "social=0", "stocks=0"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1875/18", "heart=890/16", "steps=1780/20", "workout=1180/12", "kettlebell=1200/19", "catch=300/10", "calendar=1250/13", "github=1400/18", "email=150/6", "journal=1825/21", "youtube=950/21", "social=400/8", "stocks=725/14"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=19/1", "con=20/1", "int=18/1", "wis=21/1", "cha=14/1", "per=21/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=85/85", "stamina=0/0", "mana=0/0", "focus=0/0"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case4() {
        val g = at("2026-11-25", 1.0)
        assertEquals(15923, g.totalXp)
        assertEquals(17, g.level.level)
        assertEquals(723, g.level.levelXp)
        assertEquals(34, g.dayNumber)
        assertEquals(390, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("Keeper of the Mind", g.title)
        assertEquals(393, g.avgDailyXp14d)
        assertEquals(970, g.daysToMaxLevel)
        assertEquals(listOf(390, 572, 310, 621, 195, 611, 605, 600, 413, 390, 770, 435, 420, 677, 671, 589, 451, 445, 425, 350, 320, 473, 484, 457, 390, 240, 275, 415, 390, 390), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=40", "steps=0", "workout=75", "kettlebell=0", "catch=0", "calendar=0", "github=0", "email=0", "journal=100", "youtube=50", "social=0", "stocks=75"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=2025/19", "heart=970/17", "steps=1920/21", "workout=1255/13", "kettlebell=1200/19", "catch=300/10", "calendar=1400/14", "github=1600/19", "email=150/6", "journal=2000/22", "youtube=1050/23", "social=400/8", "stocks=850/15"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=19/1", "con=21/1", "int=19/1", "wis=23/1", "cha=15/1", "per=22/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=75/75", "stamina=0/0", "mana=3/3", "focus=6/6"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=3"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case5() {
        val g = at("2026-11-26", 1.0)
        assertEquals(16418, g.totalXp)
        assertEquals(17, g.level.level)
        assertEquals(1218, g.level.levelXp)
        assertEquals(35, g.dayNumber)
        assertEquals(495, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("Keeper of the Mind", g.title)
        assertEquals(396, g.avgDailyXp14d)
        assertEquals(961, g.daysToMaxLevel)
        assertEquals(listOf(572, 310, 621, 195, 611, 605, 600, 413, 390, 770, 435, 420, 677, 671, 589, 451, 445, 425, 350, 320, 473, 484, 457, 390, 240, 275, 415, 390, 390, 495), g.history.map { it.xp })
        assertEquals(listOf("sleep=125", "heart=0", "steps=0", "workout=75", "kettlebell=75", "catch=0", "calendar=0", "github=100", "email=0", "journal=0", "youtube=50", "social=0", "stocks=25"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Well-Rested=45"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=2150/20", "heart=970/17", "steps=1920/21", "workout=1330/13", "kettlebell=1275/20", "catch=300/10", "calendar=1400/14", "github=1700/20", "email=150/6", "journal=2000/22", "youtube=1100/23", "social=400/8", "stocks=875/16"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=20/1", "con=21/1", "int=20/1", "wis=23/1", "cha=16/1", "per=22/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=100/100", "stamina=0/0", "mana=0/0", "focus=32/32"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=1", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case6() {
        val g = at("2026-11-23", 1.0)
        assertEquals(15143, g.totalXp)
        assertEquals(16, g.level.level)
        assertEquals(1343, g.level.levelXp)
        assertEquals(32, g.dayNumber)
        assertEquals(415, g.todayXp)
        assertEquals("Seer", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(428, g.avgDailyXp14d)
        assertEquals(894, g.daysToMaxLevel)
        assertEquals(listOf(649, 605, 390, 572, 310, 621, 195, 611, 605, 600, 413, 390, 770, 435, 420, 677, 671, 589, 451, 445, 425, 350, 320, 473, 484, 457, 390, 240, 275, 415), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=0", "steps=40", "workout=0", "kettlebell=0", "catch=0", "calendar=100", "github=100", "email=0", "journal=75", "youtube=0", "social=0", "stocks=50"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1925/19", "heart=890/16", "steps=1820/21", "workout=1180/12", "kettlebell=1200/19", "catch=300/10", "calendar=1350/13", "github=1500/18", "email=150/6", "journal=1900/21", "youtube=950/21", "social=400/8", "stocks=775/14"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=19/1", "con=21/1", "int=18/1", "wis=21/1", "cha=14/1", "per=21/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=100/100", "stamina=26/26", "mana=1/1", "focus=21/21"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=2"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case7() {
        val g = at("2026-12-07", 0.6)
        assertEquals(21732, g.totalXp)
        assertEquals(21, g.level.level)
        assertEquals(332, g.level.levelXp)
        assertEquals(46, g.dayNumber)
        assertEquals(420, g.todayXp)
        assertEquals("Druid", g.characterClass.name)
        assertEquals("High Archon of Focus", g.title)
        assertEquals(471, g.avgDailyXp14d)
        assertEquals(798, g.daysToMaxLevel)
        assertEquals(listOf(420, 677, 671, 589, 451, 445, 425, 350, 320, 473, 484, 457, 390, 240, 275, 415, 390, 390, 495, 621, 853, 430, 385, 530, 500, 230, 440, 730, 175, 420), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=35", "steps=70", "workout=115", "kettlebell=0", "catch=50", "calendar=50", "github=25", "email=0", "journal=0", "youtube=0", "social=0", "stocks=25"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=2775/23", "heart=1315/20", "steps=2690/26", "workout=1710/15", "kettlebell=1575/23", "catch=550/15", "calendar=2050/17", "github=2075/23", "email=150/6", "journal=2525/25", "youtube=1450/28", "social=850/13", "stocks=1000/17"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=23/1", "con=26/1", "int=23/1", "wis=28/2", "cha=17/1", "per=25/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=93/98", "stamina=32/0", "mana=60/28", "focus=54/21"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=3"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case8() {
        val g = at("2026-12-07", 1.0)
        assertEquals(21732, g.totalXp)
        assertEquals(21, g.level.level)
        assertEquals(332, g.level.levelXp)
        assertEquals(46, g.dayNumber)
        assertEquals(420, g.todayXp)
        assertEquals("Druid", g.characterClass.name)
        assertEquals("High Archon of Focus", g.title)
        assertEquals(471, g.avgDailyXp14d)
        assertEquals(798, g.daysToMaxLevel)
        assertEquals(listOf(420, 677, 671, 589, 451, 445, 425, 350, 320, 473, 484, 457, 390, 240, 275, 415, 390, 390, 495, 621, 853, 430, 385, 530, 500, 230, 440, 730, 175, 420), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=35", "steps=70", "workout=115", "kettlebell=0", "catch=50", "calendar=50", "github=25", "email=0", "journal=0", "youtube=0", "social=0", "stocks=25"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=2775/23", "heart=1315/20", "steps=2690/26", "workout=1710/15", "kettlebell=1575/23", "catch=550/15", "calendar=2050/17", "github=2075/23", "email=150/6", "journal=2525/25", "youtube=1450/28", "social=850/13", "stocks=1000/17"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=23/1", "con=26/1", "int=23/1", "wis=28/2", "cha=17/1", "per=25/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=98/98", "stamina=0/0", "mana=28/28", "focus=21/21"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=3"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case9() {
        val g = at("2026-10-21", 1.0)
        assertEquals(0, g.totalXp)
        assertEquals(1, g.level.level)
        assertEquals(0, g.level.levelXp)
        assertEquals(0, g.dayNumber)
        assertEquals(0, g.todayXp)
        assertEquals("Adventurer", g.characterClass.name)
        assertEquals(null, g.title)
        assertEquals(null, g.avgDailyXp14d)
        assertEquals(null, g.daysToMaxLevel)
        assertEquals(emptyList<String>(), g.history.map { it.xp })
        assertEquals(emptyList<String>(), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=0/1", "heart=0/1", "steps=0/1", "workout=0/1", "kettlebell=0/1", "catch=0/1", "calendar=0/1", "github=0/1", "email=0/1", "journal=0/1", "youtube=0/1", "social=0/1", "stocks=0/1"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=1/0", "con=1/0", "int=1/0", "wis=1/0", "cha=1/0", "per=1/0"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=70/82", "stamina=70/10", "mana=70/10", "focus=70/10"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=0"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun levelCurveMatchesTheServer() {
        assertEquals(397210, Game.XP_TO_MAX)
        assertEquals(1200, Game.DAILY_MAX)
    }
}

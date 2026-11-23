package com.kevinwcode.personalapi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The phone's game must score exactly like the server's app/game.py and app/vitals.py.
 * Generated: a random 52-day fixture of raw rows, with the expected numbers from the Python code.
 */
internal val PARITY_ROWS: List<Triple<String, String, Double>> = listOf(
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
        Triple("2026-10-17", "screen.app.com.whatsapp", 12.0),
        Triple("2026-10-18", "sleep.total_minutes", 470.0),
        Triple("2026-10-18", "sleep.deep_minutes", 99.0),
        Triple("2026-10-18", "sleep.rem_minutes", 108.0),
        Triple("2026-10-18", "health.resting_heart_rate", 55.0),
        Triple("2026-10-18", "health.steps", 5499.0),
        Triple("2026-10-18", "workout.sessions", 2.0),
        Triple("2026-10-18", "workout.minutes", 43.0),
        Triple("2026-10-18", "workout.counted_sessions", 1.0),
        Triple("2026-10-18", "kb.swing.sets", 4.0),
        Triple("2026-10-18", "email.unread", 10.0),
        Triple("2026-10-18", "journal.entries", 2.0),
        Triple("2026-10-18", "water.ml", 250.0),
        Triple("2026-10-18", "screen.app.com.android.chrome", 93.0),
        Triple("2026-10-18", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-10-19", "sleep.total_minutes", 300.0),
        Triple("2026-10-19", "sleep.deep_minutes", 79.0),
        Triple("2026-10-19", "sleep.rem_minutes", 107.0),
        Triple("2026-10-19", "health.resting_heart_rate", 58.0),
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
        Triple("2026-10-19", "screen.app.com.whatsapp", 40.0),
        Triple("2026-10-19", "trading.pl_day", -14.51),
        Triple("2026-10-19", "trading.equity", 1450.2),
        Triple("2026-10-20", "sleep.total_minutes", 300.0),
        Triple("2026-10-20", "sleep.deep_minutes", 98.0),
        Triple("2026-10-20", "sleep.rem_minutes", 100.0),
        Triple("2026-10-20", "health.resting_heart_rate", 58.0),
        Triple("2026-10-20", "health.hrv_rmssd", 50.0),
        Triple("2026-10-20", "health.steps", 4622.0),
        Triple("2026-10-20", "workout.sessions", 1.0),
        Triple("2026-10-20", "workout.minutes", 38.0),
        Triple("2026-10-20", "workout.counted_sessions", 1.0),
        Triple("2026-10-20", "kb.swing.sets", 1.0),
        Triple("2026-10-20", "kb.clean.done", 1.0),
        Triple("2026-10-20", "cal.abc0def.done", 1.0),
        Triple("2026-10-20", "cal.abc1def.done", 1.0),
        Triple("2026-10-20", "cal.abc2def.done", 1.0),
        Triple("2026-10-20", "email.unread", 8.0),
        Triple("2026-10-20", "coffee.cups", 1.0),
        Triple("2026-10-20", "screen.app.com.android.chrome", 66.0),
        Triple("2026-10-20", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-10-20", "screen.app.com.instagram.android", 18.0),
        Triple("2026-10-20", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-10-20", "screen.app.com.whatsapp", 12.0),
        Triple("2026-10-20", "trading.pl_day", -28.27),
        Triple("2026-10-20", "trading.equity", 1617.27),
        Triple("2026-10-21", "sleep.total_minutes", 450.0),
        Triple("2026-10-21", "sleep.deep_minutes", 51.0),
        Triple("2026-10-21", "sleep.rem_minutes", 83.0),
        Triple("2026-10-21", "health.resting_heart_rate", 66.0),
        Triple("2026-10-21", "health.hrv_rmssd", 54.0),
        Triple("2026-10-21", "health.steps", 10373.0),
        Triple("2026-10-21", "kb.swing.sets", 2.0),
        Triple("2026-10-21", "journal.entries", 2.0),
        Triple("2026-10-21", "water.ml", 250.0),
        Triple("2026-10-21", "screen.app.com.android.chrome", 82.0),
        Triple("2026-10-21", "screen.app.com.instagram.android", 18.0),
        Triple("2026-10-21", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-10-21", "screen.app.com.whatsapp", 40.0),
        Triple("2026-10-21", "screen.app.com.snapchat.android", 4.0),
        Triple("2026-10-21", "trading.pl_day", 38.82),
        Triple("2026-10-21", "trading.equity", 1644.1),
        Triple("2026-10-22", "sleep.total_minutes", 470.0),
        Triple("2026-10-22", "sleep.deep_minutes", 50.0),
        Triple("2026-10-22", "sleep.rem_minutes", 65.0),
        Triple("2026-10-22", "health.steps", 9332.0),
        Triple("2026-10-22", "football.max_catch_yards", 21.0),
        Triple("2026-10-22", "cal.abc0def.done", 1.0),
        Triple("2026-10-22", "cal.abc2def.done", 1.0),
        Triple("2026-10-22", "github.commits", 2.0),
        Triple("2026-10-22", "email.unread", 18.0),
        Triple("2026-10-22", "coffee.cups", 3.0),
        Triple("2026-10-22", "screen.app.com.android.chrome", 75.0),
        Triple("2026-10-22", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-10-22", "screen.app.com.instagram.android", 3.0),
        Triple("2026-10-22", "screen.app.com.whatsapp", 12.0),
        Triple("2026-10-22", "trading.pl_day", -19.85),
        Triple("2026-10-22", "trading.equity", 1517.19),
        Triple("2026-10-23", "sleep.total_minutes", 470.0),
        Triple("2026-10-23", "sleep.deep_minutes", 73.0),
        Triple("2026-10-23", "sleep.rem_minutes", 119.0),
        Triple("2026-10-23", "health.resting_heart_rate", 54.0),
        Triple("2026-10-23", "health.hrv_rmssd", 53.0),
        Triple("2026-10-23", "health.steps", 9006.0),
        Triple("2026-10-23", "cal.abc0def.done", 1.0),
        Triple("2026-10-23", "cal.abc1def.done", 1.0),
        Triple("2026-10-23", "water.ml", 500.0),
        Triple("2026-10-23", "coffee.cups", 3.0),
        Triple("2026-10-23", "reading.minutes", 15.0),
        Triple("2026-10-23", "study.minutes", 120.0),
        Triple("2026-10-23", "screen.app.com.android.chrome", 66.0),
        Triple("2026-10-23", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-10-23", "screen.app.com.reddit.frontpage", 8.0),
        Triple("2026-10-23", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-10-23", "screen.app.com.whatsapp", 5.0),
        Triple("2026-10-24", "sleep.total_minutes", 430.0),
        Triple("2026-10-24", "sleep.deep_minutes", 75.0),
        Triple("2026-10-24", "sleep.rem_minutes", 107.0),
        Triple("2026-10-24", "health.resting_heart_rate", 64.0),
        Triple("2026-10-24", "cal.abc1def.done", 1.0),
        Triple("2026-10-24", "cal.abc2def.done", 1.0),
        Triple("2026-10-24", "cal.abc3def.done", 1.0),
        Triple("2026-10-24", "github.commits", 3.0),
        Triple("2026-10-24", "email.unread", 7.0),
        Triple("2026-10-24", "journal.entries", 1.0),
        Triple("2026-10-24", "study.minutes", 120.0),
        Triple("2026-10-24", "screen.app.com.android.chrome", 51.0),
        Triple("2026-10-24", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-10-24", "screen.app.com.reddit.frontpage", 5.0),
        Triple("2026-10-24", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-10-24", "screen.app.com.whatsapp", 12.0),
        Triple("2026-10-24", "screen.app.com.snapchat.android", 10.0),
        Triple("2026-10-25", "health.resting_heart_rate", 58.0),
        Triple("2026-10-25", "health.hrv_rmssd", 40.0),
        Triple("2026-10-25", "health.steps", 7495.0),
        Triple("2026-10-25", "workout.sessions", 2.0),
        Triple("2026-10-25", "workout.minutes", 38.0),
        Triple("2026-10-25", "kb.swing.sets", 5.0),
        Triple("2026-10-25", "github.commits", 2.0),
        Triple("2026-10-25", "journal.entries", 2.0),
        Triple("2026-10-25", "water.ml", 500.0),
        Triple("2026-10-25", "coffee.cups", 1.0),
        Triple("2026-10-25", "screen.app.com.android.chrome", 38.0),
        Triple("2026-10-25", "screen.app.com.zhiliaoapp.musically", 50.0),
        Triple("2026-10-25", "screen.app.com.whatsapp", 5.0),
        Triple("2026-10-26", "sleep.total_minutes", 360.0),
        Triple("2026-10-26", "sleep.deep_minutes", 74.0),
        Triple("2026-10-26", "sleep.rem_minutes", 52.0),
        Triple("2026-10-26", "health.resting_heart_rate", 64.0),
        Triple("2026-10-26", "health.hrv_rmssd", 49.0),
        Triple("2026-10-26", "health.steps", 2591.0),
        Triple("2026-10-26", "workout.sessions", 1.0),
        Triple("2026-10-26", "workout.minutes", 39.0),
        Triple("2026-10-26", "workout.counted_sessions", 1.0),
        Triple("2026-10-26", "cal.abc0def.done", 1.0),
        Triple("2026-10-26", "cal.abc2def.done", 1.0),
        Triple("2026-10-26", "cal.abc3def.done", 1.0),
        Triple("2026-10-26", "github.commits", 1.0),
        Triple("2026-10-26", "email.unread", 9.0),
        Triple("2026-10-26", "water.ml", 500.0),
        Triple("2026-10-26", "coffee.cups", 3.0),
        Triple("2026-10-26", "study.minutes", 20.0),
        Triple("2026-10-26", "trading.pl_day", -38.53),
        Triple("2026-10-26", "trading.equity", 1602.26),
        Triple("2026-10-27", "health.resting_heart_rate", 55.0),
        Triple("2026-10-27", "health.steps", 14917.0),
        Triple("2026-10-27", "cal.abc0def.done", 1.0),
        Triple("2026-10-27", "cal.abc1def.done", 1.0),
        Triple("2026-10-27", "cal.abc2def.done", 1.0),
        Triple("2026-10-27", "cal.abc3def.done", 1.0),
        Triple("2026-10-27", "journal.entries", 2.0),
        Triple("2026-10-27", "reading.minutes", 45.0),
        Triple("2026-10-27", "screen.app.com.android.chrome", 90.0),
        Triple("2026-10-27", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-10-27", "screen.app.com.snapchat.android", 4.0),
        Triple("2026-10-27", "trading.pl_day", -4.33),
        Triple("2026-10-27", "trading.equity", 1505.3),
        Triple("2026-10-28", "health.steps", 15957.0),
        Triple("2026-10-28", "workout.sessions", 2.0),
        Triple("2026-10-28", "workout.minutes", 21.0),
        Triple("2026-10-28", "workout.counted_sessions", 1.0),
        Triple("2026-10-28", "kb.swing.sets", 4.0),
        Triple("2026-10-28", "github.commits", 3.0),
        Triple("2026-10-28", "journal.entries", 1.0),
        Triple("2026-10-28", "water.ml", 750.0),
        Triple("2026-10-28", "coffee.cups", 1.0),
        Triple("2026-10-28", "reading.minutes", 30.0),
        Triple("2026-10-28", "screen.app.com.android.chrome", 117.0),
        Triple("2026-10-28", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-10-28", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-10-28", "screen.app.com.snapchat.android", 20.0),
        Triple("2026-10-28", "trading.pl_day", 10.19),
        Triple("2026-10-28", "trading.equity", 1693.54),
        Triple("2026-10-29", "sleep.total_minutes", 405.0),
        Triple("2026-10-29", "sleep.deep_minutes", 107.0),
        Triple("2026-10-29", "sleep.rem_minutes", 114.0),
        Triple("2026-10-29", "health.resting_heart_rate", 65.0),
        Triple("2026-10-29", "health.steps", 11069.0),
        Triple("2026-10-29", "github.commits", 2.0),
        Triple("2026-10-29", "water.ml", 1500.0),
        Triple("2026-10-29", "coffee.cups", 3.0),
        Triple("2026-10-29", "reading.minutes", 30.0),
        Triple("2026-10-29", "screen.app.com.android.chrome", 107.0),
        Triple("2026-10-29", "screen.app.com.zhiliaoapp.musically", 50.0),
        Triple("2026-10-29", "screen.app.com.reddit.frontpage", 18.0),
        Triple("2026-10-29", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-10-29", "screen.app.com.whatsapp", 5.0),
        Triple("2026-10-29", "trading.pl_day", 20.52),
        Triple("2026-10-29", "trading.equity", 1492.29),
        Triple("2026-10-30", "sleep.total_minutes", 520.0),
        Triple("2026-10-30", "sleep.deep_minutes", 103.0),
        Triple("2026-10-30", "sleep.rem_minutes", 98.0),
        Triple("2026-10-30", "health.resting_heart_rate", 66.0),
        Triple("2026-10-30", "health.steps", 11608.0),
        Triple("2026-10-30", "kb.swing.sets", 5.0),
        Triple("2026-10-30", "kb.clean.done", 1.0),
        Triple("2026-10-30", "cal.abc1def.done", 1.0),
        Triple("2026-10-30", "github.commits", 4.0),
        Triple("2026-10-30", "email.unread", 21.0),
        Triple("2026-10-30", "journal.entries", 1.0),
        Triple("2026-10-30", "coffee.cups", 3.0),
        Triple("2026-10-30", "reading.minutes", 90.0),
        Triple("2026-10-30", "screen.app.com.android.chrome", 119.0),
        Triple("2026-10-30", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-10-30", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-10-30", "screen.app.com.whatsapp", 40.0),
        Triple("2026-10-30", "trading.pl_day", 33.32),
        Triple("2026-10-30", "trading.equity", 1772.21),
        Triple("2026-10-31", "sleep.total_minutes", 360.0),
        Triple("2026-10-31", "sleep.deep_minutes", 58.0),
        Triple("2026-10-31", "sleep.rem_minutes", 117.0),
        Triple("2026-10-31", "health.resting_heart_rate", 57.0),
        Triple("2026-10-31", "health.hrv_rmssd", 56.0),
        Triple("2026-10-31", "health.steps", 6080.0),
        Triple("2026-10-31", "cal.abc0def.done", 1.0),
        Triple("2026-10-31", "cal.abc1def.done", 1.0),
        Triple("2026-10-31", "water.ml", 1500.0),
        Triple("2026-10-31", "coffee.cups", 2.0),
        Triple("2026-10-31", "reading.minutes", 45.0),
        Triple("2026-10-31", "screen.app.com.android.chrome", 30.0),
        Triple("2026-10-31", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-10-31", "screen.app.com.instagram.android", 18.0),
        Triple("2026-10-31", "screen.app.com.reddit.frontpage", 14.0),
        Triple("2026-10-31", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-01", "sleep.total_minutes", 300.0),
        Triple("2026-11-01", "sleep.deep_minutes", 75.0),
        Triple("2026-11-01", "sleep.rem_minutes", 63.0),
        Triple("2026-11-01", "health.resting_heart_rate", 62.0),
        Triple("2026-11-01", "health.hrv_rmssd", 59.0),
        Triple("2026-11-01", "health.steps", 5853.0),
        Triple("2026-11-01", "workout.sessions", 2.0),
        Triple("2026-11-01", "workout.minutes", 22.0),
        Triple("2026-11-01", "football.max_catch_yards", 22.0),
        Triple("2026-11-01", "email.unread", 23.0),
        Triple("2026-11-01", "journal.entries", 1.0),
        Triple("2026-11-01", "coffee.cups", 1.0),
        Triple("2026-11-01", "screen.app.com.android.chrome", 58.0),
        Triple("2026-11-01", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-01", "screen.app.com.instagram.android", 18.0),
        Triple("2026-11-01", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-01", "screen.app.com.whatsapp", 40.0),
        Triple("2026-11-01", "screen.app.com.snapchat.android", 20.0),
        Triple("2026-11-02", "sleep.total_minutes", 430.0),
        Triple("2026-11-02", "sleep.deep_minutes", 104.0),
        Triple("2026-11-02", "sleep.rem_minutes", 113.0),
        Triple("2026-11-02", "health.resting_heart_rate", 59.0),
        Triple("2026-11-02", "workout.sessions", 1.0),
        Triple("2026-11-02", "workout.minutes", 45.0),
        Triple("2026-11-02", "workout.counted_sessions", 1.0),
        Triple("2026-11-02", "kb.swing.sets", 3.0),
        Triple("2026-11-02", "cal.abc0def.done", 1.0),
        Triple("2026-11-02", "journal.entries", 2.0),
        Triple("2026-11-02", "coffee.cups", 2.0),
        Triple("2026-11-02", "reading.minutes", 15.0),
        Triple("2026-11-02", "screen.app.com.android.chrome", 51.0),
        Triple("2026-11-02", "screen.app.com.zhiliaoapp.musically", 50.0),
        Triple("2026-11-02", "screen.app.com.whatsapp", 12.0),
        Triple("2026-11-02", "trading.pl_day", -5.45),
        Triple("2026-11-02", "trading.equity", 1524.81),
        Triple("2026-11-03", "sleep.total_minutes", 300.0),
        Triple("2026-11-03", "sleep.deep_minutes", 56.0),
        Triple("2026-11-03", "sleep.rem_minutes", 54.0),
        Triple("2026-11-03", "health.resting_heart_rate", 64.0),
        Triple("2026-11-03", "health.steps", 11120.0),
        Triple("2026-11-03", "workout.sessions", 1.0),
        Triple("2026-11-03", "workout.minutes", 35.0),
        Triple("2026-11-03", "cal.abc0def.done", 1.0),
        Triple("2026-11-03", "cal.abc1def.done", 1.0),
        Triple("2026-11-03", "cal.abc2def.done", 1.0),
        Triple("2026-11-03", "screen.app.com.android.chrome", 10.0),
        Triple("2026-11-03", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-03", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-03", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-03", "trading.pl_day", 21.11),
        Triple("2026-11-03", "trading.equity", 1439.78),
        Triple("2026-11-04", "sleep.total_minutes", 430.0),
        Triple("2026-11-04", "sleep.deep_minutes", 89.0),
        Triple("2026-11-04", "sleep.rem_minutes", 83.0),
        Triple("2026-11-04", "health.resting_heart_rate", 61.0),
        Triple("2026-11-04", "health.hrv_rmssd", 47.0),
        Triple("2026-11-04", "health.steps", 9047.0),
        Triple("2026-11-04", "workout.sessions", 2.0),
        Triple("2026-11-04", "workout.minutes", 51.0),
        Triple("2026-11-04", "kb.swing.sets", 5.0),
        Triple("2026-11-04", "football.max_catch_yards", 16.0),
        Triple("2026-11-04", "cal.abc0def.done", 1.0),
        Triple("2026-11-04", "cal.abc1def.done", 1.0),
        Triple("2026-11-04", "journal.entries", 1.0),
        Triple("2026-11-04", "reading.minutes", 15.0),
        Triple("2026-11-04", "screen.app.com.android.chrome", 51.0),
        Triple("2026-11-04", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-04", "screen.app.com.instagram.android", 18.0),
        Triple("2026-11-04", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-04", "screen.app.com.whatsapp", 12.0),
        Triple("2026-11-04", "trading.pl_day", -21.54),
        Triple("2026-11-04", "trading.equity", 1488.58),
        Triple("2026-11-05", "sleep.total_minutes", 450.0),
        Triple("2026-11-05", "sleep.deep_minutes", 53.0),
        Triple("2026-11-05", "sleep.rem_minutes", 113.0),
        Triple("2026-11-05", "health.resting_heart_rate", 66.0),
        Triple("2026-11-05", "health.hrv_rmssd", 43.0),
        Triple("2026-11-05", "workout.sessions", 1.0),
        Triple("2026-11-05", "workout.minutes", 69.0),
        Triple("2026-11-05", "workout.counted_sessions", 1.0),
        Triple("2026-11-05", "kb.swing.sets", 5.0),
        Triple("2026-11-05", "kb.clean.done", 1.0),
        Triple("2026-11-05", "football.max_catch_yards", 10.0),
        Triple("2026-11-05", "cal.abc0def.done", 1.0),
        Triple("2026-11-05", "reading.minutes", 30.0),
        Triple("2026-11-05", "screen.app.com.android.chrome", 64.0),
        Triple("2026-11-05", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-05", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-05", "screen.app.com.snapchat.android", 10.0),
        Triple("2026-11-05", "trading.pl_day", -6.39),
        Triple("2026-11-05", "trading.equity", 1754.07),
        Triple("2026-11-06", "sleep.total_minutes", 430.0),
        Triple("2026-11-06", "sleep.deep_minutes", 88.0),
        Triple("2026-11-06", "sleep.rem_minutes", 95.0),
        Triple("2026-11-06", "health.resting_heart_rate", 56.0),
        Triple("2026-11-06", "health.steps", 2307.0),
        Triple("2026-11-06", "kb.swing.sets", 5.0),
        Triple("2026-11-06", "football.max_catch_yards", 15.0),
        Triple("2026-11-06", "cal.abc0def.done", 1.0),
        Triple("2026-11-06", "cal.abc1def.done", 1.0),
        Triple("2026-11-06", "email.unread", 1.0),
        Triple("2026-11-06", "journal.entries", 1.0),
        Triple("2026-11-06", "water.ml", 250.0),
        Triple("2026-11-06", "reading.minutes", 45.0),
        Triple("2026-11-06", "study.minutes", 120.0),
        Triple("2026-11-06", "screen.app.com.android.chrome", 100.0),
        Triple("2026-11-06", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-06", "screen.app.com.reddit.frontpage", 25.0),
        Triple("2026-11-07", "sleep.total_minutes", 430.0),
        Triple("2026-11-07", "sleep.deep_minutes", 53.0),
        Triple("2026-11-07", "sleep.rem_minutes", 110.0),
        Triple("2026-11-07", "health.resting_heart_rate", 59.0),
        Triple("2026-11-07", "health.steps", 5613.0),
        Triple("2026-11-07", "kb.clean.done", 1.0),
        Triple("2026-11-07", "reading.minutes", 45.0),
        Triple("2026-11-07", "screen.app.com.android.chrome", 81.0),
        Triple("2026-11-07", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-07", "screen.app.com.instagram.android", 9.0),
        Triple("2026-11-07", "screen.app.com.reddit.frontpage", 4.0),
        Triple("2026-11-07", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-07", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-07", "screen.app.com.snapchat.android", 10.0),
        Triple("2026-11-08", "sleep.total_minutes", 360.0),
        Triple("2026-11-08", "sleep.deep_minutes", 73.0),
        Triple("2026-11-08", "sleep.rem_minutes", 60.0),
        Triple("2026-11-08", "health.resting_heart_rate", 58.0),
        Triple("2026-11-08", "health.steps", 8823.0),
        Triple("2026-11-08", "workout.sessions", 1.0),
        Triple("2026-11-08", "workout.minutes", 36.0),
        Triple("2026-11-08", "workout.counted_sessions", 1.0),
        Triple("2026-11-08", "email.unread", 8.0),
        Triple("2026-11-08", "journal.entries", 2.0),
        Triple("2026-11-08", "water.ml", 750.0),
        Triple("2026-11-08", "coffee.cups", 1.0),
        Triple("2026-11-08", "reading.minutes", 30.0),
        Triple("2026-11-08", "study.minutes", 120.0),
        Triple("2026-11-08", "screen.app.com.android.chrome", 13.0),
        Triple("2026-11-08", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-08", "screen.app.com.reddit.frontpage", 27.0),
        Triple("2026-11-08", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-08", "screen.app.com.snapchat.android", 10.0),
        Triple("2026-11-09", "sleep.total_minutes", 430.0),
        Triple("2026-11-09", "sleep.deep_minutes", 97.0),
        Triple("2026-11-09", "sleep.rem_minutes", 97.0),
        Triple("2026-11-09", "health.resting_heart_rate", 56.0),
        Triple("2026-11-09", "health.hrv_rmssd", 31.0),
        Triple("2026-11-09", "health.steps", 15064.0),
        Triple("2026-11-09", "github.commits", 6.0),
        Triple("2026-11-09", "water.ml", 750.0),
        Triple("2026-11-09", "coffee.cups", 1.0),
        Triple("2026-11-09", "reading.minutes", 45.0),
        Triple("2026-11-09", "study.minutes", 120.0),
        Triple("2026-11-09", "screen.app.com.android.chrome", 31.0),
        Triple("2026-11-09", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-11-09", "screen.app.com.whatsapp", 12.0),
        Triple("2026-11-09", "screen.app.com.snapchat.android", 10.0),
        Triple("2026-11-09", "trading.pl_day", -34.94),
        Triple("2026-11-09", "trading.equity", 1440.56),
        Triple("2026-11-10", "sleep.total_minutes", 405.0),
        Triple("2026-11-10", "sleep.deep_minutes", 108.0),
        Triple("2026-11-10", "sleep.rem_minutes", 61.0),
        Triple("2026-11-10", "health.resting_heart_rate", 58.0),
        Triple("2026-11-10", "health.steps", 6141.0),
        Triple("2026-11-10", "kb.clean.done", 1.0),
        Triple("2026-11-10", "cal.abc0def.done", 1.0),
        Triple("2026-11-10", "cal.abc1def.done", 1.0),
        Triple("2026-11-10", "water.ml", 1500.0),
        Triple("2026-11-10", "coffee.cups", 1.0),
        Triple("2026-11-10", "study.minutes", 20.0),
        Triple("2026-11-10", "screen.app.com.android.chrome", 56.0),
        Triple("2026-11-10", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-10", "screen.app.com.instagram.android", 9.0),
        Triple("2026-11-10", "screen.app.com.reddit.frontpage", 3.0),
        Triple("2026-11-10", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-10", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-10", "trading.pl_day", -12.16),
        Triple("2026-11-10", "trading.equity", 1464.73),
        Triple("2026-11-11", "sleep.total_minutes", 360.0),
        Triple("2026-11-11", "sleep.deep_minutes", 53.0),
        Triple("2026-11-11", "sleep.rem_minutes", 99.0),
        Triple("2026-11-11", "health.resting_heart_rate", 64.0),
        Triple("2026-11-11", "health.steps", 6441.0),
        Triple("2026-11-11", "workout.sessions", 1.0),
        Triple("2026-11-11", "workout.minutes", 68.0),
        Triple("2026-11-11", "workout.counted_sessions", 1.0),
        Triple("2026-11-11", "kb.swing.sets", 4.0),
        Triple("2026-11-11", "kb.clean.done", 1.0),
        Triple("2026-11-11", "water.ml", 1500.0),
        Triple("2026-11-11", "coffee.cups", 1.0),
        Triple("2026-11-11", "reading.minutes", 30.0),
        Triple("2026-11-11", "study.minutes", 20.0),
        Triple("2026-11-11", "screen.app.com.android.chrome", 97.0),
        Triple("2026-11-11", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-11", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-11", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-11", "trading.pl_day", -20.06),
        Triple("2026-11-11", "trading.equity", 1555.68),
        Triple("2026-11-12", "sleep.total_minutes", 520.0),
        Triple("2026-11-12", "sleep.deep_minutes", 62.0),
        Triple("2026-11-12", "sleep.rem_minutes", 52.0),
        Triple("2026-11-12", "health.resting_heart_rate", 59.0),
        Triple("2026-11-12", "health.steps", 11634.0),
        Triple("2026-11-12", "kb.clean.done", 1.0),
        Triple("2026-11-12", "github.commits", 3.0),
        Triple("2026-11-12", "journal.entries", 2.0),
        Triple("2026-11-12", "water.ml", 250.0),
        Triple("2026-11-12", "coffee.cups", 1.0),
        Triple("2026-11-12", "reading.minutes", 45.0),
        Triple("2026-11-12", "screen.app.com.android.chrome", 11.0),
        Triple("2026-11-12", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-12", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-11-12", "trading.pl_day", -24.5),
        Triple("2026-11-12", "trading.equity", 1792.69),
        Triple("2026-11-13", "sleep.total_minutes", 405.0),
        Triple("2026-11-13", "sleep.deep_minutes", 68.0),
        Triple("2026-11-13", "sleep.rem_minutes", 58.0),
        Triple("2026-11-13", "health.resting_heart_rate", 61.0),
        Triple("2026-11-13", "health.steps", 11552.0),
        Triple("2026-11-13", "workout.sessions", 2.0),
        Triple("2026-11-13", "workout.minutes", 19.0),
        Triple("2026-11-13", "workout.counted_sessions", 1.0),
        Triple("2026-11-13", "cal.abc1def.done", 1.0),
        Triple("2026-11-13", "cal.abc2def.done", 1.0),
        Triple("2026-11-13", "cal.abc3def.done", 1.0),
        Triple("2026-11-13", "github.commits", 3.0),
        Triple("2026-11-13", "water.ml", 750.0),
        Triple("2026-11-13", "coffee.cups", 3.0),
        Triple("2026-11-13", "reading.minutes", 45.0),
        Triple("2026-11-13", "screen.app.com.android.chrome", 71.0),
        Triple("2026-11-13", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-11-13", "screen.app.com.instagram.android", 27.0),
        Triple("2026-11-13", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-13", "trading.pl_day", 21.17),
        Triple("2026-11-13", "trading.equity", 1576.91),
        Triple("2026-11-14", "sleep.total_minutes", 300.0),
        Triple("2026-11-14", "sleep.deep_minutes", 77.0),
        Triple("2026-11-14", "sleep.rem_minutes", 116.0),
        Triple("2026-11-14", "health.resting_heart_rate", 62.0),
        Triple("2026-11-14", "health.steps", 12374.0),
        Triple("2026-11-14", "football.max_catch_yards", 14.0),
        Triple("2026-11-14", "cal.abc1def.done", 1.0),
        Triple("2026-11-14", "cal.abc2def.done", 1.0),
        Triple("2026-11-14", "cal.abc3def.done", 1.0),
        Triple("2026-11-14", "github.commits", 5.0),
        Triple("2026-11-14", "journal.entries", 1.0),
        Triple("2026-11-14", "water.ml", 750.0),
        Triple("2026-11-14", "coffee.cups", 2.0),
        Triple("2026-11-14", "screen.app.com.android.chrome", 22.0),
        Triple("2026-11-14", "screen.app.com.zhiliaoapp.musically", 50.0),
        Triple("2026-11-14", "screen.app.com.reddit.frontpage", 2.0),
        Triple("2026-11-14", "screen.app.com.whatsapp", 12.0),
        Triple("2026-11-15", "sleep.total_minutes", 405.0),
        Triple("2026-11-15", "sleep.deep_minutes", 74.0),
        Triple("2026-11-15", "sleep.rem_minutes", 101.0),
        Triple("2026-11-15", "health.resting_heart_rate", 52.0),
        Triple("2026-11-15", "health.hrv_rmssd", 56.0),
        Triple("2026-11-15", "health.steps", 7239.0),
        Triple("2026-11-15", "cal.abc0def.done", 1.0),
        Triple("2026-11-15", "cal.abc2def.done", 1.0),
        Triple("2026-11-15", "journal.entries", 1.0),
        Triple("2026-11-15", "coffee.cups", 3.0),
        Triple("2026-11-15", "screen.app.com.android.chrome", 30.0),
        Triple("2026-11-15", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-15", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-15", "screen.app.com.snapchat.android", 20.0),
        Triple("2026-11-16", "sleep.total_minutes", 520.0),
        Triple("2026-11-16", "sleep.deep_minutes", 108.0),
        Triple("2026-11-16", "sleep.rem_minutes", 50.0),
        Triple("2026-11-16", "health.resting_heart_rate", 58.0),
        Triple("2026-11-16", "health.steps", 13652.0),
        Triple("2026-11-16", "kb.swing.sets", 1.0),
        Triple("2026-11-16", "github.commits", 6.0),
        Triple("2026-11-16", "water.ml", 250.0),
        Triple("2026-11-16", "coffee.cups", 3.0),
        Triple("2026-11-16", "screen.app.com.android.chrome", 38.0),
        Triple("2026-11-16", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-16", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-16", "screen.app.com.reddit.frontpage", 30.0),
        Triple("2026-11-16", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-16", "trading.pl_day", 30.41),
        Triple("2026-11-16", "trading.equity", 1531.42),
        Triple("2026-11-17", "sleep.total_minutes", 520.0),
        Triple("2026-11-17", "sleep.deep_minutes", 100.0),
        Triple("2026-11-17", "sleep.rem_minutes", 117.0),
        Triple("2026-11-17", "health.resting_heart_rate", 65.0),
        Triple("2026-11-17", "health.hrv_rmssd", 60.0),
        Triple("2026-11-17", "health.steps", 10844.0),
        Triple("2026-11-17", "cal.abc1def.done", 1.0),
        Triple("2026-11-17", "cal.abc2def.done", 1.0),
        Triple("2026-11-17", "cal.abc3def.done", 1.0),
        Triple("2026-11-17", "journal.entries", 1.0),
        Triple("2026-11-17", "water.ml", 500.0),
        Triple("2026-11-17", "screen.app.com.android.chrome", 13.0),
        Triple("2026-11-17", "screen.app.com.reddit.frontpage", 28.0),
        Triple("2026-11-17", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-11-17", "trading.pl_day", -9.29),
        Triple("2026-11-17", "trading.equity", 1498.63),
        Triple("2026-11-18", "sleep.total_minutes", 300.0),
        Triple("2026-11-18", "sleep.deep_minutes", 44.0),
        Triple("2026-11-18", "sleep.rem_minutes", 61.0),
        Triple("2026-11-18", "health.resting_heart_rate", 62.0),
        Triple("2026-11-18", "health.steps", 3673.0),
        Triple("2026-11-18", "workout.sessions", 1.0),
        Triple("2026-11-18", "workout.minutes", 28.0),
        Triple("2026-11-18", "workout.counted_sessions", 1.0),
        Triple("2026-11-18", "kb.clean.done", 1.0),
        Triple("2026-11-18", "cal.abc0def.done", 1.0),
        Triple("2026-11-18", "water.ml", 750.0),
        Triple("2026-11-18", "reading.minutes", 90.0),
        Triple("2026-11-18", "study.minutes", 120.0),
        Triple("2026-11-18", "screen.app.com.android.chrome", 49.0),
        Triple("2026-11-18", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-11-18", "screen.app.com.reddit.frontpage", 29.0),
        Triple("2026-11-18", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-18", "screen.app.com.snapchat.android", 4.0),
        Triple("2026-11-18", "trading.pl_day", -16.93),
        Triple("2026-11-18", "trading.equity", 1700.21),
        Triple("2026-11-19", "sleep.total_minutes", 470.0),
        Triple("2026-11-19", "sleep.deep_minutes", 102.0),
        Triple("2026-11-19", "sleep.rem_minutes", 62.0),
        Triple("2026-11-19", "health.resting_heart_rate", 64.0),
        Triple("2026-11-19", "cal.abc0def.done", 1.0),
        Triple("2026-11-19", "github.commits", 6.0),
        Triple("2026-11-19", "email.unread", 5.0),
        Triple("2026-11-19", "journal.entries", 1.0),
        Triple("2026-11-19", "water.ml", 250.0),
        Triple("2026-11-19", "reading.minutes", 90.0),
        Triple("2026-11-19", "screen.app.com.android.chrome", 100.0),
        Triple("2026-11-19", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-19", "screen.app.com.instagram.android", 9.0),
        Triple("2026-11-19", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-11-19", "screen.app.com.whatsapp", 40.0),
        Triple("2026-11-19", "trading.pl_day", 35.51),
        Triple("2026-11-19", "trading.equity", 1450.75),
        Triple("2026-11-20", "sleep.total_minutes", 300.0),
        Triple("2026-11-20", "sleep.deep_minutes", 84.0),
        Triple("2026-11-20", "sleep.rem_minutes", 91.0),
        Triple("2026-11-20", "health.resting_heart_rate", 65.0),
        Triple("2026-11-20", "health.steps", 13656.0),
        Triple("2026-11-20", "workout.sessions", 2.0),
        Triple("2026-11-20", "workout.minutes", 38.0),
        Triple("2026-11-20", "kb.swing.sets", 2.0),
        Triple("2026-11-20", "kb.clean.done", 1.0),
        Triple("2026-11-20", "github.commits", 6.0),
        Triple("2026-11-20", "screen.app.com.android.chrome", 35.0),
        Triple("2026-11-20", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-11-20", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-20", "screen.app.com.reddit.frontpage", 23.0),
        Triple("2026-11-20", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-20", "screen.app.com.whatsapp", 12.0),
        Triple("2026-11-20", "trading.pl_day", -5.21),
        Triple("2026-11-20", "trading.equity", 1478.48),
        Triple("2026-11-21", "sleep.total_minutes", 360.0),
        Triple("2026-11-21", "sleep.deep_minutes", 75.0),
        Triple("2026-11-21", "sleep.rem_minutes", 76.0),
        Triple("2026-11-21", "health.steps", 15497.0),
        Triple("2026-11-21", "cal.abc0def.done", 1.0),
        Triple("2026-11-21", "cal.abc1def.done", 1.0),
        Triple("2026-11-21", "github.commits", 2.0),
        Triple("2026-11-21", "journal.entries", 2.0),
        Triple("2026-11-21", "screen.app.com.android.chrome", 79.0),
        Triple("2026-11-21", "screen.app.com.google.android.youtube", 45.0),
        Triple("2026-11-21", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-22", "sleep.total_minutes", 495.0),
        Triple("2026-11-22", "sleep.deep_minutes", 60.0),
        Triple("2026-11-22", "sleep.rem_minutes", 82.0),
        Triple("2026-11-22", "health.resting_heart_rate", 59.0),
        Triple("2026-11-22", "health.steps", 8206.0),
        Triple("2026-11-22", "cal.abc1def.done", 1.0),
        Triple("2026-11-22", "github.commits", 1.0),
        Triple("2026-11-22", "email.unread", 17.0),
        Triple("2026-11-22", "journal.entries", 1.0),
        Triple("2026-11-22", "water.ml", 250.0),
        Triple("2026-11-22", "study.minutes", 60.0),
        Triple("2026-11-22", "screen.app.com.android.chrome", 86.0),
        Triple("2026-11-22", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-22", "screen.app.com.whatsapp", 12.0),
        Triple("2026-11-23", "sleep.total_minutes", 360.0),
        Triple("2026-11-23", "sleep.deep_minutes", 85.0),
        Triple("2026-11-23", "sleep.rem_minutes", 57.0),
        Triple("2026-11-23", "health.resting_heart_rate", 58.0),
        Triple("2026-11-23", "health.steps", 8358.0),
        Triple("2026-11-23", "github.commits", 6.0),
        Triple("2026-11-23", "screen.app.com.android.chrome", 104.0),
        Triple("2026-11-23", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-23", "screen.app.com.instagram.android", 9.0),
        Triple("2026-11-23", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-11-23", "screen.app.com.whatsapp", 25.0),
        Triple("2026-11-23", "trading.pl_day", 22.39),
        Triple("2026-11-23", "trading.equity", 1587.76),
        Triple("2026-11-24", "sleep.total_minutes", 430.0),
        Triple("2026-11-24", "sleep.deep_minutes", 74.0),
        Triple("2026-11-24", "sleep.rem_minutes", 98.0),
        Triple("2026-11-24", "health.resting_heart_rate", 58.0),
        Triple("2026-11-24", "health.steps", 14696.0),
        Triple("2026-11-24", "kb.swing.sets", 2.0),
        Triple("2026-11-24", "cal.abc0def.done", 1.0),
        Triple("2026-11-24", "github.commits", 3.0),
        Triple("2026-11-24", "journal.entries", 2.0),
        Triple("2026-11-24", "coffee.cups", 2.0),
        Triple("2026-11-24", "study.minutes", 20.0),
        Triple("2026-11-24", "screen.app.com.android.chrome", 14.0),
        Triple("2026-11-24", "screen.app.com.zhiliaoapp.musically", 22.0),
        Triple("2026-11-24", "screen.app.com.whatsapp", 40.0),
        Triple("2026-11-24", "trading.pl_day", 32.33),
        Triple("2026-11-24", "trading.equity", 1716.67),
        Triple("2026-11-25", "sleep.total_minutes", 360.0),
        Triple("2026-11-25", "sleep.deep_minutes", 110.0),
        Triple("2026-11-25", "sleep.rem_minutes", 88.0),
        Triple("2026-11-25", "health.resting_heart_rate", 63.0),
        Triple("2026-11-25", "health.hrv_rmssd", 32.0),
        Triple("2026-11-25", "health.steps", 8685.0),
        Triple("2026-11-25", "kb.swing.sets", 1.0),
        Triple("2026-11-25", "football.max_catch_yards", 12.0),
        Triple("2026-11-25", "cal.abc0def.done", 1.0),
        Triple("2026-11-25", "cal.abc1def.done", 1.0),
        Triple("2026-11-25", "cal.abc2def.done", 1.0),
        Triple("2026-11-25", "github.commits", 4.0),
        Triple("2026-11-25", "email.unread", 5.0),
        Triple("2026-11-25", "journal.entries", 1.0),
        Triple("2026-11-25", "water.ml", 750.0),
        Triple("2026-11-25", "coffee.cups", 3.0),
        Triple("2026-11-25", "study.minutes", 60.0),
        Triple("2026-11-25", "screen.app.com.android.chrome", 58.0),
        Triple("2026-11-25", "screen.app.com.reddit.frontpage", 13.0),
        Triple("2026-11-25", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-11-25", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-26", "sleep.total_minutes", 360.0),
        Triple("2026-11-26", "sleep.deep_minutes", 105.0),
        Triple("2026-11-26", "sleep.rem_minutes", 111.0),
        Triple("2026-11-26", "health.resting_heart_rate", 66.0),
        Triple("2026-11-26", "health.hrv_rmssd", 36.0),
        Triple("2026-11-26", "health.steps", 11744.0),
        Triple("2026-11-26", "workout.sessions", 1.0),
        Triple("2026-11-26", "workout.minutes", 65.0),
        Triple("2026-11-26", "kb.swing.sets", 5.0),
        Triple("2026-11-26", "cal.abc0def.done", 1.0),
        Triple("2026-11-26", "cal.abc1def.done", 1.0),
        Triple("2026-11-26", "journal.entries", 2.0),
        Triple("2026-11-26", "water.ml", 750.0),
        Triple("2026-11-26", "reading.minutes", 30.0),
        Triple("2026-11-26", "screen.app.com.android.chrome", 47.0),
        Triple("2026-11-26", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-26", "screen.app.com.instagram.android", 3.0),
        Triple("2026-11-26", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-11-26", "trading.pl_day", -15.97),
        Triple("2026-11-26", "trading.equity", 1402.48),
        Triple("2026-11-27", "sleep.total_minutes", 520.0),
        Triple("2026-11-27", "sleep.deep_minutes", 47.0),
        Triple("2026-11-27", "sleep.rem_minutes", 114.0),
        Triple("2026-11-27", "health.steps", 11769.0),
        Triple("2026-11-27", "football.max_catch_yards", 10.0),
        Triple("2026-11-27", "cal.abc1def.done", 1.0),
        Triple("2026-11-27", "journal.entries", 1.0),
        Triple("2026-11-27", "water.ml", 500.0),
        Triple("2026-11-27", "study.minutes", 60.0),
        Triple("2026-11-27", "screen.app.com.android.chrome", 9.0),
        Triple("2026-11-27", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-11-27", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-27", "trading.pl_day", -6.31),
        Triple("2026-11-27", "trading.equity", 1678.28),
        Triple("2026-11-28", "sleep.total_minutes", 360.0),
        Triple("2026-11-28", "sleep.deep_minutes", 41.0),
        Triple("2026-11-28", "sleep.rem_minutes", 99.0),
        Triple("2026-11-28", "health.resting_heart_rate", 62.0),
        Triple("2026-11-28", "health.steps", 8257.0),
        Triple("2026-11-28", "kb.swing.sets", 4.0),
        Triple("2026-11-28", "football.max_catch_yards", 10.0),
        Triple("2026-11-28", "cal.abc0def.done", 1.0),
        Triple("2026-11-28", "cal.abc2def.done", 1.0),
        Triple("2026-11-28", "email.unread", 3.0),
        Triple("2026-11-28", "journal.entries", 1.0),
        Triple("2026-11-28", "water.ml", 500.0),
        Triple("2026-11-28", "coffee.cups", 3.0),
        Triple("2026-11-28", "reading.minutes", 15.0),
        Triple("2026-11-28", "study.minutes", 120.0),
        Triple("2026-11-28", "screen.app.com.android.chrome", 114.0),
        Triple("2026-11-28", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-11-28", "screen.app.com.instagram.android", 27.0),
        Triple("2026-11-28", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-29", "sleep.total_minutes", 300.0),
        Triple("2026-11-29", "sleep.deep_minutes", 50.0),
        Triple("2026-11-29", "sleep.rem_minutes", 99.0),
        Triple("2026-11-29", "health.resting_heart_rate", 63.0),
        Triple("2026-11-29", "cal.abc0def.done", 1.0),
        Triple("2026-11-29", "cal.abc1def.done", 1.0),
        Triple("2026-11-29", "github.commits", 6.0),
        Triple("2026-11-29", "email.unread", 25.0),
        Triple("2026-11-29", "journal.entries", 2.0),
        Triple("2026-11-29", "coffee.cups", 2.0),
        Triple("2026-11-29", "screen.app.com.android.chrome", 12.0),
        Triple("2026-11-29", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-11-29", "screen.app.com.google.android.youtube", 90.0),
        Triple("2026-11-29", "screen.app.com.snapchat.android", 4.0),
        Triple("2026-11-30", "sleep.total_minutes", 450.0),
        Triple("2026-11-30", "sleep.deep_minutes", 94.0),
        Triple("2026-11-30", "sleep.rem_minutes", 81.0),
        Triple("2026-11-30", "health.resting_heart_rate", 62.0),
        Triple("2026-11-30", "health.steps", 5339.0),
        Triple("2026-11-30", "kb.swing.sets", 1.0),
        Triple("2026-11-30", "journal.entries", 1.0),
        Triple("2026-11-30", "study.minutes", 120.0),
        Triple("2026-11-30", "screen.app.com.android.chrome", 68.0),
        Triple("2026-11-30", "screen.app.com.zhiliaoapp.musically", 4.0),
        Triple("2026-11-30", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-11-30", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-30", "trading.pl_day", -23.47),
        Triple("2026-11-30", "trading.equity", 1501.89),
        Triple("2026-12-01", "sleep.total_minutes", 495.0),
        Triple("2026-12-01", "sleep.deep_minutes", 98.0),
        Triple("2026-12-01", "sleep.rem_minutes", 119.0),
        Triple("2026-12-01", "health.resting_heart_rate", 64.0),
        Triple("2026-12-01", "health.steps", 8023.0),
        Triple("2026-12-01", "cal.abc1def.done", 1.0),
        Triple("2026-12-01", "cal.abc2def.done", 1.0),
        Triple("2026-12-01", "github.commits", 6.0),
        Triple("2026-12-01", "email.unread", 18.0),
        Triple("2026-12-01", "journal.entries", 2.0),
        Triple("2026-12-01", "reading.minutes", 90.0),
        Triple("2026-12-01", "study.minutes", 20.0),
        Triple("2026-12-01", "screen.app.com.android.chrome", 64.0),
        Triple("2026-12-01", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-12-01", "screen.app.com.instagram.android", 3.0),
        Triple("2026-12-01", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-12-01", "screen.app.com.whatsapp", 12.0),
        Triple("2026-12-01", "trading.pl_day", -1.09),
        Triple("2026-12-01", "trading.equity", 1626.87),
        Triple("2026-12-02", "sleep.total_minutes", 450.0),
        Triple("2026-12-02", "sleep.deep_minutes", 94.0),
        Triple("2026-12-02", "sleep.rem_minutes", 62.0),
        Triple("2026-12-02", "health.steps", 3644.0),
        Triple("2026-12-02", "workout.sessions", 1.0),
        Triple("2026-12-02", "workout.minutes", 31.0),
        Triple("2026-12-02", "workout.counted_sessions", 1.0),
        Triple("2026-12-02", "kb.swing.sets", 1.0),
        Triple("2026-12-02", "kb.clean.done", 1.0),
        Triple("2026-12-02", "football.max_catch_yards", 15.0),
        Triple("2026-12-02", "cal.abc0def.done", 1.0),
        Triple("2026-12-02", "coffee.cups", 2.0),
        Triple("2026-12-02", "trading.pl_day", -8.55),
        Triple("2026-12-02", "trading.equity", 1579.34),
        Triple("2026-12-03", "sleep.total_minutes", 430.0),
        Triple("2026-12-03", "sleep.deep_minutes", 68.0),
        Triple("2026-12-03", "sleep.rem_minutes", 72.0),
        Triple("2026-12-03", "health.resting_heart_rate", 56.0),
        Triple("2026-12-03", "health.steps", 10557.0),
        Triple("2026-12-03", "kb.clean.done", 1.0),
        Triple("2026-12-03", "cal.abc0def.done", 1.0),
        Triple("2026-12-03", "cal.abc1def.done", 1.0),
        Triple("2026-12-03", "journal.entries", 2.0),
        Triple("2026-12-03", "coffee.cups", 2.0),
        Triple("2026-12-03", "reading.minutes", 90.0),
        Triple("2026-12-03", "study.minutes", 60.0),
        Triple("2026-12-03", "screen.app.com.android.chrome", 61.0),
        Triple("2026-12-03", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-12-03", "screen.app.com.google.android.youtube", 25.0),
        Triple("2026-12-03", "screen.app.com.snapchat.android", 4.0),
        Triple("2026-12-03", "trading.pl_day", 9.49),
        Triple("2026-12-03", "trading.equity", 1549.24),
        Triple("2026-12-04", "sleep.total_minutes", 520.0),
        Triple("2026-12-04", "sleep.deep_minutes", 52.0),
        Triple("2026-12-04", "sleep.rem_minutes", 99.0),
        Triple("2026-12-04", "health.resting_heart_rate", 62.0),
        Triple("2026-12-04", "health.hrv_rmssd", 40.0),
        Triple("2026-12-04", "health.steps", 5331.0),
        Triple("2026-12-04", "workout.sessions", 2.0),
        Triple("2026-12-04", "workout.minutes", 19.0),
        Triple("2026-12-04", "workout.counted_sessions", 1.0),
        Triple("2026-12-04", "kb.clean.done", 1.0),
        Triple("2026-12-04", "cal.abc0def.done", 1.0),
        Triple("2026-12-04", "github.commits", 4.0),
        Triple("2026-12-04", "journal.entries", 2.0),
        Triple("2026-12-04", "water.ml", 750.0),
        Triple("2026-12-04", "coffee.cups", 1.0),
        Triple("2026-12-04", "reading.minutes", 15.0),
        Triple("2026-12-04", "study.minutes", 60.0),
        Triple("2026-12-04", "screen.app.com.android.chrome", 70.0),
        Triple("2026-12-04", "screen.app.com.zhiliaoapp.musically", 75.0),
        Triple("2026-12-04", "screen.app.com.whatsapp", 12.0),
        Triple("2026-12-04", "trading.pl_day", -19.08),
        Triple("2026-12-04", "trading.equity", 1495.47),
        Triple("2026-12-05", "sleep.total_minutes", 495.0),
        Triple("2026-12-05", "sleep.deep_minutes", 77.0),
        Triple("2026-12-05", "sleep.rem_minutes", 103.0),
        Triple("2026-12-05", "health.hrv_rmssd", 53.0),
        Triple("2026-12-05", "workout.sessions", 1.0),
        Triple("2026-12-05", "workout.minutes", 38.0),
        Triple("2026-12-05", "kb.swing.sets", 2.0),
        Triple("2026-12-05", "cal.abc0def.done", 1.0),
        Triple("2026-12-05", "cal.abc1def.done", 1.0),
        Triple("2026-12-05", "cal.abc2def.done", 1.0),
        Triple("2026-12-05", "cal.abc3def.done", 1.0),
        Triple("2026-12-05", "github.commits", 2.0),
        Triple("2026-12-05", "email.unread", 5.0),
        Triple("2026-12-05", "journal.entries", 1.0),
        Triple("2026-12-05", "coffee.cups", 1.0),
        Triple("2026-12-05", "screen.app.com.android.chrome", 102.0),
        Triple("2026-12-05", "screen.app.com.zhiliaoapp.musically", 12.0),
        Triple("2026-12-05", "screen.app.com.instagram.android", 9.0),
        Triple("2026-12-05", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-12-06", "sleep.total_minutes", 300.0),
        Triple("2026-12-06", "sleep.deep_minutes", 106.0),
        Triple("2026-12-06", "sleep.rem_minutes", 94.0),
        Triple("2026-12-06", "health.resting_heart_rate", 65.0),
        Triple("2026-12-06", "workout.sessions", 2.0),
        Triple("2026-12-06", "workout.minutes", 68.0),
        Triple("2026-12-06", "kb.swing.sets", 3.0),
        Triple("2026-12-06", "cal.abc0def.done", 1.0),
        Triple("2026-12-06", "journal.entries", 2.0),
        Triple("2026-12-06", "coffee.cups", 2.0),
        Triple("2026-12-06", "screen.app.com.android.chrome", 46.0),
        Triple("2026-12-06", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-12-06", "screen.app.com.google.android.youtube", 10.0),
        Triple("2026-12-07", "sleep.total_minutes", 405.0),
        Triple("2026-12-07", "sleep.deep_minutes", 42.0),
        Triple("2026-12-07", "sleep.rem_minutes", 81.0),
        Triple("2026-12-07", "health.hrv_rmssd", 35.0),
        Triple("2026-12-07", "health.steps", 6610.0),
        Triple("2026-12-07", "workout.sessions", 1.0),
        Triple("2026-12-07", "workout.minutes", 11.0),
        Triple("2026-12-07", "workout.counted_sessions", 1.0),
        Triple("2026-12-07", "kb.clean.done", 1.0),
        Triple("2026-12-07", "football.max_catch_yards", 19.0),
        Triple("2026-12-07", "cal.abc0def.done", 1.0),
        Triple("2026-12-07", "cal.abc1def.done", 1.0),
        Triple("2026-12-07", "cal.abc2def.done", 1.0),
        Triple("2026-12-07", "cal.abc3def.done", 1.0),
        Triple("2026-12-07", "github.commits", 2.0),
        Triple("2026-12-07", "email.unread", 3.0),
        Triple("2026-12-07", "journal.entries", 2.0),
        Triple("2026-12-07", "water.ml", 250.0),
        Triple("2026-12-07", "coffee.cups", 1.0),
        Triple("2026-12-07", "study.minutes", 20.0),
        Triple("2026-12-07", "screen.app.com.android.chrome", 78.0),
        Triple("2026-12-07", "screen.app.com.zhiliaoapp.musically", 35.0),
        Triple("2026-12-07", "screen.app.com.instagram.android", 3.0),
        Triple("2026-12-07", "screen.app.com.google.android.youtube", 61.0),
        Triple("2026-12-07", "screen.app.com.whatsapp", 5.0),
        Triple("2026-11-12", "perk.per.1.claimed", 1.0),
        Triple("2026-11-17", "perk.con.1.claimed", 1.0),
        Triple("2026-11-22", "perk.wis.2.claimed", 1.0),
        Triple("2026-11-25", "perk.per.4.claimed", 1.0),
        Triple("2026-12-02", "perk.str.1.claimed", 1.0),
        Triple("2026-10-23", "food.sugar_g", 27.1),
        Triple("2026-10-26", "food.sugar_g", 33.6),
        Triple("2026-10-29", "food.sugar_g", 55.5),
        Triple("2026-11-01", "food.sugar_g", 27.9),
        Triple("2026-11-04", "food.sugar_g", 30.5),
        Triple("2026-11-07", "food.sugar_g", 35.2),
        Triple("2026-11-10", "food.sugar_g", 11.1),
        Triple("2026-11-13", "food.sugar_g", 30.7),
        Triple("2026-11-16", "food.sugar_g", 37.8),
        Triple("2026-11-19", "food.sugar_g", 47.6),
        Triple("2026-11-22", "food.sugar_g", 5.6),
        Triple("2026-11-25", "food.sugar_g", 18.2),
        Triple("2026-11-28", "food.sugar_g", 5.4),
        Triple("2026-12-01", "food.sugar_g", 48.6),
        Triple("2026-12-04", "food.sugar_g", 41.6),
        Triple("2026-12-07", "food.sugar_g", 2.5),
)

internal val PARITY_DAYS: Map<LocalDate, Map<String, Double>> = PARITY_ROWS.groupBy { LocalDate.parse(it.first) }
    .mapValues { (_, rs) -> DayMetrics.build(rs.map { it.second to it.third }) }

internal fun parityNow(day: LocalDate, share: Double): LocalDateTime =
    // share 1.0 is the end of the day, which todayElapsed reads as a past day.
    if (share >= 1.0) day.plusDays(1).atStartOfDay() else day.atStartOfDay().plusSeconds(Math.round(share * 86400))

class GameParityTest {
    private val days = PARITY_DAYS

    private fun at(day: String, share: Double): Game.State {
        val d = LocalDate.parse(day)
        return Game.build(days, d, now = parityNow(d, share))
    }

    @Test
    fun case0() {
        val g = at("2026-11-02", 1.0)
        assertEquals(5136, g.totalXp)
        assertEquals(8, g.level.level)
        assertEquals(376, g.level.levelXp)
        assertEquals(11, g.dayNumber)
        assertEquals(480, g.todayXp)
        assertEquals("Druid", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(467, g.avgDailyXp14d)
        assertEquals(840, g.daysToMaxLevel)
        assertEquals(listOf(621, 450, 500, 210, 465, 600, 200, 825, 310, 475, 480), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=40", "steps=0", "workout=115", "kettlebell=75", "catch=0", "study=0", "github=0", "reading=25", "journal=100", "connect=25", "youtube=50", "social=0"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=550/8", "heart=345/8", "steps=690/11", "workout=495/7", "kettlebell=300/8", "catch=50/3", "study=200/5", "github=375/7", "reading=375/9", "journal=600/10", "connect=375/7", "youtube=250/9", "social=400/8"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=8/0", "con=11/1", "int=7/0", "wis=10/1", "cha=7/0", "per=9/0"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=94/94", "stamina=0/0", "mana=7/7", "focus=30/30"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=0"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case1() {
        val g = at("2026-11-12", 1.0)
        assertEquals(10229, g.totalXp)
        assertEquals(13, g.level.level)
        assertEquals(269, g.level.levelXp)
        assertEquals(21, g.dayNumber)
        assertEquals(649, g.todayXp)
        assertEquals("Druid", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(513, g.avgDailyXp14d)
        assertEquals(754, g.daysToMaxLevel)
        assertEquals(listOf(621, 450, 500, 210, 465, 600, 200, 825, 310, 475, 480, 415, 595, 484, 535, 465, 560, 615, 275, 500, 649), g.history.map { it.xp })
        assertEquals(listOf("sleep=75", "heart=40", "steps=100", "workout=0", "kettlebell=75", "catch=0", "study=0", "github=75", "reading=75", "journal=100", "connect=0", "youtube=50", "social=0"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Unrestricted Media Pass=0", "Well-Rested=59"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1150/13", "heart=625/13", "steps=1350/17", "workout=1070/11", "kettlebell=825/15", "catch=200/8", "study=500/9", "github=550/9", "reading=850/15", "journal=950/14", "connect=675/11", "youtube=500/14", "social=750/12"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=15/1", "con=17/1", "int=9/0", "wis=15/1", "cha=11/1", "per=14/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=100/100", "stamina=6/6", "mana=2/2", "focus=40/40"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=1", "well_rested=1", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case2() {
        val g = at("2026-11-17", 1.0)
        assertEquals(12471, g.totalXp)
        assertEquals(15, g.level.level)
        assertEquals(11, g.level.levelXp)
        assertEquals(26, g.dayNumber)
        assertEquals(533, g.todayXp)
        assertEquals("Sentinel", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(494, g.avgDailyXp14d)
        assertEquals(778, g.daysToMaxLevel)
        assertEquals(listOf(621, 450, 500, 210, 465, 600, 200, 825, 310, 475, 480, 415, 595, 484, 535, 465, 560, 615, 275, 500, 649, 365, 490, 370, 484, 533), g.history.map { it.xp })
        assertEquals(listOf("sleep=125", "heart=35", "steps=100", "workout=0", "kettlebell=0", "catch=0", "study=0", "github=0", "reading=0", "journal=75", "connect=0", "youtube=50", "social=100"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Well-Rested=48"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1450/16", "heart=855/15", "steps=1820/21", "workout=1145/12", "kettlebell=900/16", "catch=250/9", "study=500/9", "github=825/12", "reading=925/16", "journal=1175/16", "connect=800/12", "youtube=700/18", "social=800/12"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=16/1", "con=21/1", "int=12/1", "wis=16/1", "cha=12/1", "per=18/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=105/105", "stamina=40/40", "mana=10/10", "focus=37/37"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=1", "well_rested=2", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case3() {
        val g = at("2026-11-22", 1.0)
        assertEquals(14916, g.totalXp)
        assertEquals(16, g.level.level)
        assertEquals(1116, g.level.levelXp)
        assertEquals(31, g.dayNumber)
        assertEquals(572, g.todayXp)
        assertEquals("Sentinel", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(480, g.avgDailyXp14d)
        assertEquals(796, g.daysToMaxLevel)
        assertEquals(listOf(450, 500, 210, 465, 600, 200, 825, 310, 475, 480, 415, 595, 484, 535, 465, 560, 615, 275, 500, 649, 365, 490, 370, 484, 533, 545, 413, 465, 450, 572), g.history.map { it.xp })
        assertEquals(listOf("sleep=75", "heart=40", "steps=80", "workout=0", "kettlebell=0", "catch=0", "study=50", "github=25", "reading=0", "journal=75", "connect=25", "youtube=50", "social=100"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(listOf("Well-Rested=52"), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1750/18", "heart=935/16", "steps=2130/23", "workout=1335/13", "kettlebell=1050/18", "catch=250/9", "study=650/11", "github=1100/15", "reading=1075/18", "journal=1425/18", "connect=1000/14", "youtube=850/20", "social=950/14"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=18/1", "con=23/1", "int=15/1", "wis=18/1", "cha=14/1", "per=20/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=101/101", "stamina=26/26", "mana=0/0", "focus=40/40"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=2", "well_rested=1", "committed=4"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case4() {
        val g = at("2026-11-25", 1.0)
        assertEquals(16506, g.totalXp)
        assertEquals(17, g.level.level)
        assertEquals(1306, g.level.levelXp)
        assertEquals(34, g.dayNumber)
        assertEquals(630, g.todayXp)
        assertEquals("Sentinel", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(495, g.avgDailyXp14d)
        assertEquals(770, g.daysToMaxLevel)
        assertEquals(listOf(465, 600, 200, 825, 310, 475, 480, 415, 595, 484, 535, 465, 560, 615, 275, 500, 649, 365, 490, 370, 484, 533, 545, 413, 465, 450, 572, 270, 690, 630), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=0", "steps=80", "workout=0", "kettlebell=75", "catch=50", "study=50", "github=100", "reading=0", "journal=75", "connect=0", "youtube=50", "social=100"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1900/19", "heart=1015/17", "steps=2390/25", "workout=1335/13", "kettlebell=1200/19", "catch=300/10", "study=700/11", "github=1375/17", "reading=1075/18", "journal=1600/19", "connect=1150/16", "youtube=950/21", "social=1100/15"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=19/1", "con=25/1", "int=17/1", "wis=19/1", "cha=16/1", "per=21/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=63/63", "stamina=0/0", "mana=10/10", "focus=40/40"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=2", "well_rested=0", "committed=7"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case5() {
        val g = at("2026-11-26", 1.0)
        assertEquals(16946, g.totalXp)
        assertEquals(18, g.level.level)
        assertEquals(286, g.level.levelXp)
        assertEquals(35, g.dayNumber)
        assertEquals(440, g.todayXp)
        assertEquals("Sentinel", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(480, g.avgDailyXp14d)
        assertEquals(793, g.daysToMaxLevel)
        assertEquals(listOf(600, 200, 825, 310, 475, 480, 415, 595, 484, 535, 465, 560, 615, 275, 500, 649, 365, 490, 370, 484, 533, 545, 413, 465, 450, 572, 270, 690, 630, 440), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=0", "steps=100", "workout=115", "kettlebell=75", "catch=0", "study=0", "github=0", "reading=50", "journal=100", "connect=0", "youtube=0", "social=-50"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1950/19", "heart=1015/17", "steps=2490/25", "workout=1450/14", "kettlebell=1275/20", "catch=300/10", "study=700/11", "github=1375/17", "reading=1125/18", "journal=1700/20", "connect=1150/16", "youtube=950/21", "social=1050/15"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=20/1", "con=25/1", "int=17/1", "wis=20/1", "cha=16/1", "per=21/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=77/77", "stamina=0/0", "mana=6/6", "focus=2/2"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=2", "well_rested=0", "committed=7"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case6() {
        val g = at("2026-11-23", 1.0)
        assertEquals(15186, g.totalXp)
        assertEquals(16, g.level.level)
        assertEquals(1386, g.level.levelXp)
        assertEquals(32, g.dayNumber)
        assertEquals(270, g.todayXp)
        assertEquals("Sentinel", g.characterClass.name)
        assertEquals("The Rested Initiate", g.title)
        assertEquals(456, g.avgDailyXp14d)
        assertEquals(838, g.daysToMaxLevel)
        assertEquals(listOf(500, 210, 465, 600, 200, 825, 310, 475, 480, 415, 595, 484, 535, 465, 560, 615, 275, 500, 649, 365, 490, 370, 484, 533, 545, 413, 465, 450, 572, 270), g.history.map { it.xp })
        assertEquals(listOf("sleep=50", "heart=40", "steps=80", "workout=0", "kettlebell=0", "catch=0", "study=0", "github=100", "reading=0", "journal=0", "connect=50", "youtube=0", "social=-50"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=1800/18", "heart=975/17", "steps=2210/23", "workout=1335/13", "kettlebell=1050/18", "catch=250/9", "study=650/11", "github=1200/16", "reading=1075/18", "journal=1425/18", "connect=1050/15", "youtube=850/20", "social=900/13"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=18/1", "con=23/1", "int=16/1", "wis=18/1", "cha=15/1", "per=20/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=78/78", "stamina=0/0", "mana=0/0", "focus=0/0"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=2", "well_rested=1", "committed=5"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case7() {
        val g = at("2026-12-07", 0.6)
        assertEquals(22662, g.totalXp)
        assertEquals(21, g.level.level)
        assertEquals(1262, g.level.levelXp)
        assertEquals(46, g.dayNumber)
        assertEquals(410, g.todayXp)
        assertEquals("Druid", g.characterClass.name)
        assertEquals("Unbroken Vigor", g.title)
        assertEquals(534, g.avgDailyXp14d)
        assertEquals(701, g.daysToMaxLevel)
        assertEquals(listOf(560, 615, 275, 500, 649, 365, 490, 370, 484, 533, 545, 413, 465, 450, 572, 270, 690, 630, 440, 440, 545, 200, 677, 721, 379, 590, 677, 687, 390, 410), g.history.map { it.xp })
        assertEquals(listOf("sleep=0", "heart=0", "steps=60", "workout=75", "kettlebell=75", "catch=50", "study=0", "github=50", "reading=0", "journal=100", "connect=0", "youtube=0", "social=0"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=2700/23", "heart=1210/19", "steps=3040/28", "workout=1945/17", "kettlebell=1875/25", "catch=500/14", "study=1050/15", "github=1775/20", "reading=1325/20", "journal=2600/26", "connect=1200/16", "youtube=1350/26", "social=1350/17"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=25/1", "con=28/2", "int=20/1", "wis=26/1", "cha=16/1", "per=26/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=86/90", "stamina=20/0", "mana=12/0", "focus=58/25"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
    }

    @Test
    fun case8() {
        val g = at("2026-12-07", 1.0)
        assertEquals(22662, g.totalXp)
        assertEquals(21, g.level.level)
        assertEquals(1262, g.level.levelXp)
        assertEquals(46, g.dayNumber)
        assertEquals(410, g.todayXp)
        assertEquals("Druid", g.characterClass.name)
        assertEquals("Unbroken Vigor", g.title)
        assertEquals(534, g.avgDailyXp14d)
        assertEquals(701, g.daysToMaxLevel)
        assertEquals(listOf(560, 615, 275, 500, 649, 365, 490, 370, 484, 533, 545, 413, 465, 450, 572, 270, 690, 630, 440, 440, 545, 200, 677, 721, 379, 590, 677, 687, 390, 410), g.history.map { it.xp })
        assertEquals(listOf("sleep=0", "heart=0", "steps=60", "workout=75", "kettlebell=75", "catch=50", "study=0", "github=50", "reading=0", "journal=100", "connect=0", "youtube=0", "social=0"), g.lines.map { "${it.key}=${it.xp}" })
        assertEquals(emptyList<String>(), g.buffs.map { "${it.name}=${it.xp}" })
        assertEquals(listOf("sleep=2700/23", "heart=1210/19", "steps=3040/28", "workout=1945/17", "kettlebell=1875/25", "catch=500/14", "study=1050/15", "github=1775/20", "reading=1325/20", "journal=2600/26", "connect=1200/16", "youtube=1350/26", "social=1350/17"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
        assertEquals(listOf("str=25/1", "con=28/2", "int=20/1", "wis=26/1", "cha=16/1", "per=26/1"), g.groups.map { "${it.key}=${it.level}/${it.tier}" })
        assertEquals(listOf("hp=90/90", "stamina=0/0", "mana=0/0", "focus=25/25"), g.vitals.map { "${it.key}=${it.value}/${it.endOfDay}" })
        assertEquals(listOf("social_ok=0", "well_rested=0", "committed=1"), g.streaks.map { "${it.key}=${it.days}" })
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
        assertEquals(listOf("sleep=0/1", "heart=0/1", "steps=0/1", "workout=0/1", "kettlebell=0/1", "catch=0/1", "study=0/1", "github=0/1", "reading=0/1", "journal=0/1", "connect=0/1", "youtube=0/1", "social=0/1"), g.stats.map { "${it.key}=${it.xp}/${it.level}" })
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

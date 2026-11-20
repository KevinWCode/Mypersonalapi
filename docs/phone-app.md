# Phone app

A small Android app that collects your stats, saves them in its own database on the phone, and (if you want) sends a copy to the PC server. Once it's set up, a background sync every 15 minutes (the most often Android allows) between 07:00 and 22:00 reads:

- **Screen time** per app, from Android's usage stats. The server adds TikTok, Instagram, Facebook and Reddit into one social media total, and tracks YouTube on its own.
- **Health data** from Health Connect: sleep and its stages, steps, heart rate, resting heart rate, HRV and workouts. Your Galaxy Watch gets there through Samsung Health.
- **Manual entries** you add in the app: kettlebell sets, and catch distance.

Each sync re-reads the last 3 days, so a missed sync fixes itself next time. No AI is involved: it's plain code on a timer.

## Data on the phone

Since version 0.10 every sync is saved on the phone first, in the app's own database (one row per day, metric and source, the same layout as the server's). It needs no network, server or account, and nothing leaves the phone unless the PC upload is on.

- **Today on this phone** shows today's sleep, steps, resting heart rate, workouts, social media, YouTube, phone use and manual entries from that database, and how many days are stored.
- **Back up phone data to a file** saves everything as one JSON file wherever you choose (Downloads, Google Drive...). Uninstalling the app deletes its database, so back up before you do.
- **Also send my data to my PC server** is on when an upload token is set, so the PC dashboard keeps working. Turn it off and the app runs on its own. A failed upload doesn't stop the phone saving; it's tried again next sync.

For now the home screen widget and the dashboard still read from the PC server. Moving the game, the dashboard and the widget onto the phone are the next steps.

## Install

1. On your phone, open the repo's **Releases** page (signed in to GitHub) and download `personal-api.apk` from **Phone app (latest)**.
2. Open the downloaded file. Android asks you to allow installs from your browser: allow it, go back, and install.
3. Updates install the same way, on top of the old version.

## One-time setup in the app

1. **Upload token (only if you use the PC server):** paste the `INGEST_TOKEN` value from `.env` on the server. To get it onto your phone, copy it on the PC and send it to yourself (for example, a Google Keep note), then delete the note. The server address is already filled in (`http://100.80.206.11:8000`). Tap **Save**.
2. **Allow Health Connect access:** tap the button and allow everything, including *Access data in the background*.
3. **Allow usage access:** tap the button, find **Personal API** in the list, and turn it on.
4. **Make Samsung Health share with Health Connect:** in Samsung Health, open Settings → Health Connect and allow it to write sleep, steps, heart rate and exercise.
5. **Keep it running:** tap **Keep syncing in the background** and choose **Allow**. Samsung otherwise tends to pause background syncs. The status line then says `Background sync: every 15 min, 07:00 to 22:00`. Overnight it rests to save battery; the first sync in the morning sends the last 3 days again, so nothing is lost. **Sync now** works at any time. (The same setting lives in Android Settings → Apps → Personal API → Battery → **Unrestricted**.)
6. Tap **Sync now**. The status line should show counts like `screen 25, health 14, manual 0, saved on phone, sent 39 to PC`.

Tailscale has to be switched on for syncs to reach the server. The phone saves its own copy either way.

## Open dashboard

**Open dashboard** opens the dashboard in your browser and passes it the upload token once, so its Quick log buttons, food box and kettlebell ticks can save. Bookmark the page afterwards; you only need the button again if the dashboard asks for the token.

## Home screen widget

Long-press an empty spot on the home screen, tap **Widgets**, find **Personal API** and drag **Personal API today** onto the screen. It's 4 x 4 by default and can be resized down to about 3 x 3; when it's shorter than 4 rows it switches to a compact layout (day names only under the bars, no small lines under the tiles, one line for the last trade). It shows:

- social media for each of the last 7 days as bars: green up to half the daily limit, amber up to the limit, red over it, with the limit as a dashed line (60 minutes; change `SOCIAL_LIMIT_MINUTES` in the server's `.env`). Under them: minutes per day, time reclaimed against last week, and YouTube per day
- calories and protein today, and last night's sleep
- steps per day, resting heart rate and workouts over the last 7 days
- study and reading hours over the last 7 days
- trading P/L over the last 7 days (green when up) and your last trade

It updates after every sync and when you tap the refresh arrow, and tapping anywhere else opens the dashboard. The time in its corner is when it last got numbers. If the server is off (the PC is shut down) it says **Offline** and keeps the last numbers, with the day they're from.

## Manual entries

- **Kettlebell set:** pick the move, enter reps and weight in lb, and tap **Add set**. Reps and sets add up for the day, and the heaviest weight is kept.
- **Catch distance:** enter today's longest reliable catch in yards. The highest value for the day is kept.
- **Undo today's manual entries** clears today's entries on the phone and on the server.

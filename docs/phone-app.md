# Phone app

A small Android app that sends your stats to the server. Once it's set up, a background sync every 15 minutes (the most often Android allows) reads:

- **Screen time** per app, from Android's usage stats. The server adds TikTok and Instagram into one social media total.
- **Health data** from Health Connect: sleep and its stages, steps, heart rate, resting heart rate, HRV and workouts. Your Galaxy Watch gets there through Samsung Health.
- **Manual entries** you add in the app: kettlebell sets, and catch distance.

Each sync re-sends the last 3 days, so a missed sync fixes itself next time. No AI is involved: it's plain code on a timer.

## Install

1. On your phone, open the repo's **Releases** page (signed in to GitHub) and download `personal-api.apk` from **Phone app (latest)**.
2. Open the downloaded file. Android asks you to allow installs from your browser: allow it, go back, and install.
3. Updates install the same way, on top of the old version.

## One-time setup in the app

1. **Upload token:** paste the `INGEST_TOKEN` value from `.env` on the server. To get it onto your phone, copy it on the PC and send it to yourself (for example, a Google Keep note), then delete the note. The server address is already filled in (`http://100.80.206.11:8000`). Tap **Save**.
2. **Allow Health Connect access:** tap the button and allow everything, including *Access data in the background*.
3. **Allow usage access:** tap the button, find **Personal API** in the list, and turn it on.
4. **Make Samsung Health share with Health Connect:** in Samsung Health, open Settings → Health Connect and allow it to write sleep, steps, heart rate and exercise.
5. **Keep it running:** tap **Keep syncing in the background** and choose **Allow**. Samsung otherwise tends to pause background syncs. The status line then says `Background sync: every 15 min`. (The same setting lives in Android Settings → Apps → Personal API → Battery → **Unrestricted**.)
6. Tap **Sync now**. The status line should show counts like `screen 25, health 14, manual 0`.

Tailscale has to be switched on for syncs to reach the server.

## Open dashboard

**Open dashboard** opens the dashboard in your browser and passes it the upload token once, so its Quick log buttons, food box and kettlebell ticks can save. Bookmark the page afterwards; you only need the button again if the dashboard asks for the token.

## Home screen widget

Long-press an empty spot on the home screen, tap **Widgets**, find **Personal API** and drag **Personal API today** onto the screen. It's 4 x 4 by default and can be resized. It shows:

- time reclaimed from social media this week (the dashboard's headline)
- calories and protein today, and last night's sleep
- steps per day, resting heart rate and workouts over the last 7 days
- study and reading hours over the last 7 days
- trading P/L over the last 7 days (green when up) and your last trade

It updates after every sync and when you tap the refresh arrow, and tapping anywhere else opens the dashboard. The time in its corner is when it last got numbers. If the server is off (the PC is shut down) it says **Offline** and keeps the last numbers, with the day they're from.

## Manual entries

- **Kettlebell set:** pick the move, enter reps and weight in lb, and tap **Add set**. Reps and sets add up for the day, and the heaviest weight is kept.
- **Catch distance:** enter today's longest reliable catch in yards. The highest value for the day is kept.
- **Undo today's manual entries** clears today's entries on the phone and on the server.

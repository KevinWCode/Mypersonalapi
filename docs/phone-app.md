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

## Character (game on the phone)

Since version 0.11 the app works out the game itself: level, tier, class, title, today's XP and buffs, the HP, Stamina, Mana and Focus bars, and streaks, from the phone's database. The rules are a port of the server's `app/game.py`, `app/vitals.py` and `app/perks.py`; `GameParityTest` checks the phone scores 52 days of random data exactly like the server (regenerate it with `tools/make_game_parity.py` after changing the server's rules).

While the PC server is still running:

- **Copy my history from the PC** (under PC server) copies every day since the game started (23 Oct 2026) so your level carries over. Do it once after installing.
- Each sync with the PC upload on also copies the PC's totals for the last 3 days, which include what you log on the dashboard (journal, water, food, kettlebell ticks, perk claims). A day copied from the PC uses the PC's totals; other days use the phone's own data.

Since version 0.13 the dashboard and the home screen widget run from the phone's data too (see below). Left for the server: GitHub commits and Alpaca trades, which still come from the PC until the app fetches them itself. Google Calendar is no longer part of the game (12 stats, 1,050 XP a day at most), on the phone or the server.

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

## Dashboard

**Open dashboard** (under Today on this phone) shows the dashboard inside the app, from the phone's own data, so it works with the PC off. It's the same page the PC server serves (bundled into the app when it's built); the page's requests are answered on the phone, with the same numbers the server would give (`DashboardParityTest` checks this). Its Quick log buttons, food box, kettlebell ticks and perk claims save on the phone, and go to the PC on the next sync while the upload is on. The Money tab shows P/L and equity from the stored days; recent trades and positions stay empty until the app reads Alpaca itself.

**Open PC dashboard** (under PC server) still opens the server's own dashboard in your browser, and passes it the upload token once so its logging works.

## Home screen widget

Long-press an empty spot on the home screen, tap **Widgets**, find **Personal API** and drag **Personal API today** onto the screen. It's 4 x 4 by default and can be resized down to about 3 x 3; when it's shorter than 4 rows it switches to a compact layout (day names only under the bars, no small lines under the tiles, one line for the last trade). It shows:

- social media for each of the last 7 days as bars: green up to half the daily limit, amber up to the limit, red over it, with the limit as a dashed line (60 minutes). Under them: minutes per day, time reclaimed against last week, and YouTube per day
- calories and protein today, and last night's sleep
- steps per day, resting heart rate and workouts over the last 7 days
- study and reading hours over the last 7 days
- trading P/L over the last 7 days (green when up) and your last trade

Since version 0.13 its numbers are worked out on the phone (the same sums as the server's `/summary`), so it keeps working with the PC off. Only the last trade still comes from the PC, while the upload is on. It updates after every sync, after logging and when you tap the refresh arrow, and tapping anywhere else opens the dashboard in the app. The time in its corner is when it last updated.

## Logging in the app

Since version 0.12 everything you used to log on the dashboard can be logged in the app. It saves on the phone straight away (and goes to the PC too while the upload is on).

- **Quick log:** +250 ml water, +1 coffee, +1 hour reading, +30 min study, Journal entry, and Workout done (ticks Swing, Clean and Clean & press for today). **Undo last tap** puts back what the last tap changed.
- **Food:** paste the line your food AI writes, for example `{"meal":"lunch","kcal":640,"protein_g":42,"carbs_g":55,"fat_g":20,"sugar_g":12,"description":"chicken wrap"}`, and tap **Add meal**. Several meals at once work too (one per line, or a list). A meal without a `date` goes on today. Each meal is listed under the box with **Remove this meal**. Sugar counts toward the game (1 HP per gram, and a short Stamina rush).
- **Perk claims:** when a perk token is earned and not claimed, a **Claim** button appears under the Character card.
- **Kettlebell set:** pick the move, enter reps and weight in lb, and tap **Add set**. Reps and sets add up for the day, and the heaviest weight is kept.
- **Catch distance:** enter today's longest reliable catch in yards. The highest value for the day is kept.
- **Undo today's manual entries** clears today's quick log, kettlebell and catch entries on the phone and on the server.

Log food in one place only while the PC is running: in the app, or the food AI's file on the PC, not both, or that meal counts twice. The same goes for the dashboard's Quick log buttons.

Turning off **Also send my data to my PC server** drops the PC's copied totals for the last 3 days, so the phone's own numbers show from then on.

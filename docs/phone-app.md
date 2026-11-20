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
- Each sync with the PC upload on also copies the PC's totals for the last 3 days, which include what you log on the dashboard (journal, water, food, kettlebell ticks, perk claims). A day copied from the PC uses the PC's totals, except food: from 20 Nov 2026 food comes only from meals logged in the app, so the food AI's files on the PC no longer count (earlier days keep the PC's food). Other days use the phone's own data.

Since version 0.13 the dashboard and the home screen widget run from the phone's data too (see below). Since version 0.16 the app also reads GitHub commits and Alpaca trades itself (see GitHub and Alpaca below), so nothing is left that only the PC can do. Google Calendar is no longer part of the game (12 stats, 1,050 XP a day at most), on the phone or the server.

## Install

1. On your phone, open the repo's **Releases** page (signed in to GitHub) and download `personal-api.apk` from **Phone app (latest)**.
2. Open the downloaded file. Android asks you to allow installs from your browser: allow it, go back, and install.
3. Updates install the same way, on top of the old version.

## One-time setup in the app

1. **Upload token (only if you use the PC server):** paste the `INGEST_TOKEN` value from `.env` on the server. To get it onto your phone, copy it on the PC and send it to yourself (for example, a Google Keep note), then delete the note. Enter the server's address too (for example `http://100.x.x.x:8000` on Tailscale). Tap **Save**.
2. **Allow Health Connect access:** tap the button and allow everything, including *Access data in the background*.
3. **Allow usage access:** tap the button, find **Personal API** in the list, and turn it on.
4. **Make Samsung Health share with Health Connect:** in Samsung Health, open Settings → Health Connect and allow it to write sleep, steps, heart rate and exercise.
5. **Keep it running:** tap **Keep syncing in the background** and choose **Allow**. Samsung otherwise tends to pause background syncs. The status line then says `Background sync: every 15 min, 07:00 to 22:00`. Overnight it rests to save battery; the first sync in the morning sends the last 3 days again, so nothing is lost. **Sync now** works at any time. (The same setting lives in Android Settings → Apps → Personal API → Battery → **Unrestricted**.)
6. Tap **Sync now**. The status line should show counts like `screen 25, health 14, manual 0, saved on phone, sent 39 to PC`.

Tailscale has to be switched on for syncs to reach the server. The phone saves its own copy either way.

## GitHub and Alpaca

Under **GitHub and Alpaca**, paste the keys once and tap **Save keys and sync**. Every sync then reads them straight from their websites, the same way the PC server does:

- **GitHub token:** the `GITHUB_TOKEN` value from `.env` on the PC (the username isn't needed). Gives the day's commit count for the GitHub commits XP line.
- **Alpaca keys:** `ALPACA_KEY_ID` then `ALPACA_SECRET_KEY` from `.env`, then `ALPACA_KEY_ID_2` and `ALPACA_SECRET_KEY_2` and so on for each trading bot, separated by spaces. The accounts add up as one, like on the PC. Gives equity, daily P/L, trades and positions. Only GET requests are made, so the app can't place or change a trade.
- **Alpaca live account:** leave it off for paper accounts (the PC's default). Tick it only if `.env` sets `ALPACA_BASE_URL` to `https://api.alpaca.markets`.

The keys stay on the phone in a file left out of Android's cloud backup. The status card says whether each one is on, and the last sync line shows `github …` and `alpaca …`, or the error if a key is wrong.

## Dashboard

Since version 0.17 the dashboard is the app's main screen: opening the app shows it. It runs inside the app, from the phone's own data, so it works with the PC off. It's the same page the PC server serves (bundled into the app when it's built); the page's requests are answered on the phone, with the same numbers the server would give (`DashboardParityTest` checks this). Its Quick log buttons, food box, kettlebell ticks and perk claims save on the phone, and go to the PC on the next sync while the upload is on. The Money tab shows P/L and equity from the stored days; recent trades and positions come from the Alpaca fills the phone has read.

**Open PC dashboard** (under PC server) still opens the server's own dashboard in your browser, and passes it the upload token once so its logging works.

## Home screen widget

Long-press an empty spot on the home screen, tap **Widgets**, find **Personal API** and drag **Personal API today** onto the screen. It's 4 x 4 by default and can be resized down to about 3 x 3; when it's shorter than 4 rows it switches to a compact layout (day names only under the bars, no small lines under the tiles, one line for the last trade). It shows:

- social media for each of the last 7 days as bars: green up to half the daily limit, amber up to the limit, red over it, with the limit as a dashed line (60 minutes). Under them: minutes per day, time reclaimed against last week, and YouTube per day
- calories and protein today, and last night's sleep
- steps per day, resting heart rate and workouts over the last 7 days
- study and reading hours over the last 7 days
- trading P/L over the last 7 days (green when up) and your last trade

Since version 0.13 its numbers are worked out on the phone (the same sums as the server's `/summary`), so it keeps working with the PC off. Since 0.16 the last trade comes from the phone's own Alpaca reads too. It updates after every sync, after logging and when you tap the refresh arrow, and tapping anywhere else opens the dashboard in the app. The time in its corner is when it last updated.

## On someone else's phone

Since version 0.18 nothing in the app points at Kevin's PC, so anyone with an Android phone can use it on their own:

- **Install:** the APK from the repo's Releases page (they need access to the repo, and must allow installs from that source). It's a debug build, signed with the CI's key.
- **First launch:** the app opens a setup screen listing what to allow: Health Connect, usage access and background sync. GitHub, Alpaca and the PC server are optional and start switched off.
- **Game start:** each phone's character starts on its own first day (the first day with data, or the install day), so a new player starts at level 1. A phone that already holds days from before 23 Oct 2026 (Kevin's copied history) keeps 23 Oct 2026.
- **Still Kevin's for now:** the daily goals (2,000 kcal, 150 g protein, 3 L water, 400 mg caffeine, 60 min social media, 20 yd catch), the kettlebell moves and the flag football catch are fixed in the app.

## Main screen and settings

Since version 0.17 the app opens on the dashboard, with four tabs: **Today**, **Food**, **Money** and **Character**. The cog at the top opens **Settings and info**, which has a **Phone app** card (what's switched on and the last sync) with **Sync now** and **Open app settings**.

- **Food tab:** the 12-cell meal matrix ("Most Likely Macronutrient Profiles Across the 12 Meal-Tier Calorie Matrix") as tiles. One tap logs that meal at its most likely macros: Breakfast Light 350 / Standard 550 / Heavy 850, Lunch 450 / 700 / 1,050, Dinner 500 / 800 / 1,200, Snack 150 / 300 / 500 kcal, each with the matrix's total sugars (v1.2.0: breakfast 14 / 20 / 22 g, lunch 13 / 20 / 28 g, dinner 9 / 17 / 30 g, snack 10 / 21 / 34 g), which count toward the sugar HP rule. Today's meals are listed below with an × to remove one, and each tap has an Undo. The tab only shows in the app (the PC server doesn't serve the matrix).
- **Today tab:** Quick log (water, coffee, reading, study, journal, workout done), Fuel today with the paste box for a food AI line, and the kettlebell ticks.
- **Character tab:** perk tokens are claimed here.
- **App settings** (from the cog): Sync now, Back up phone data to a file, GitHub and Alpaca keys, the optional PC server, permissions (Health Connect, usage access, background sync), and manual entries: undo the last quick log tap, undo today's manual entries, a kettlebell set (move, reps, weight in lb) and the day's longest reliable catch in yards. Back returns to the dashboard.

Log food in the app. The PC's food (its food AI inbox) is no longer read by the app.

Turning off **Also send my data to my PC server** drops the PC's copied totals for the last 3 days, so the phone's own numbers show from then on.

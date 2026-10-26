# Personal API

One endpoint that gathers Kevin's daily stats (sleep, workouts, GitHub commits, screen time) for dashboards.

AI helped write this code, but nothing here calls an AI at runtime. It's plain scheduled scripts and a small SQLite database.

## How it fits together

```
collectors (GitHub, ...) ──┐
phone app  ── POST /ingest ┼──> observations table (SQLite) ──> GET /days ──> /dashboard
```

- **`observations` table**: one row per day, metric and source. Re-running a collector overwrites that day instead of duplicating it.
- **`GET /dashboard`** (and `/`): the dashboard page. Plain HTML and JavaScript in `app/static/dashboard.html` that reads `/days`, `/trading` and `/status`, with 7, 30 and 90-day views. It refreshes every 5 minutes.
- **`GET /days?start=YYYY-MM-DD&end=YYYY-MM-DD`**: per-day metrics (the last 7 days by default).
- **`GET /trading`**: rolling P/L over 7 and 30 days, last stock bought and sold, the 5 most recent fills and the 5 biggest positions, from Alpaca.
- **`GET /status`**: when each source (phone, manual, github, alpaca, food, dashboard) last sent data.
- **`POST /log`**: the dashboard's Quick log buttons and kettlebell ticks. Each entry adds to the day's value (or sets it), with `Authorization: Bearer $INGEST_TOKEN`.
- **`GET /meals?date=`, `POST /meals`, `DELETE /meals/{id}`**: the day's meals. The food totals (`food.*`) are recomputed from these, so nothing is counted twice.
- **`POST /ingest`**: the phone app uploads screen time and quick manual entries here, with `Authorization: Bearer $INGEST_TOKEN`.
- **Social media total**: the phone sends minutes per Android app as `screen.app.<package>`. `/days` adds the apps listed in `app/metrics.py` into `screen.social_media_minutes`, currently TikTok plus Instagram.

## Metrics so far

| Metric | Source | Automatic? |
|---|---|---|
| `github.commits` | GitHub GraphQL, hourly | Yes |
| `screen.tiktok_minutes`, `screen.instagram_minutes`, `screen.social_media_minutes` | Phone app (Android usage stats) | Yes |
| `screen.total_minutes` | All apps the phone reports, added up. The dashboard also uses it to tell a day with no social media apart from a day the phone didn't sync. | Yes |
| `sleep.total_minutes`, `sleep.deep_minutes`, `sleep.rem_minutes`, `sleep.light_minutes`, `sleep.awake_minutes` | Galaxy Watch → Samsung Health → Health Connect → phone app. Sleep counts toward the day you wake up. | Yes |
| `health.steps`, `health.heart_rate_avg` (average across the whole day) | Health Connect via the phone app | Yes |
| `health.resting_heart_rate` | Samsung doesn't share a resting heart rate, so the app uses the lowest 5-minute average during sleep | Yes |
| `health.spo2_avg` | Blood oxygen from the watch, averaged per day | Yes |
| `health.hrv_rmssd` | Only from devices that share HRV with Health Connect. Samsung doesn't, so it stays empty with the Galaxy Watch. | Yes, if the device shares it |
| `workout.sessions`, `workout.minutes` | Workouts started on the watch | Yes |
| `kb.<move>.reps`, `kb.<move>.sets`, `kb.<move>.top_weight_lb` | Quick entry in the phone app | No, about 10 seconds per set |
| `football.max_catch_yards` | Quick entry in the phone app | No |
| `trading.equity`, `trading.pl_day` | Alpaca portfolio history, hourly (read-only) | Yes |
| `food.kcal`, `food.protein_g`, `food.carbs_g`, `food.fat_g`, `food.<meal>.kcal` | Meal files your food AI saves into the inbox folder, or lines pasted on the dashboard (see below) | Partly: the AI does the work, you take the photo |
| `water.ml`, `coffee.cups`, `reading.minutes`, `study.minutes`, `food.protein_g`, `kb.<move>.done` | Quick log buttons and kettlebell ticks on the dashboard | No, one tap each |

## Food

The server checks `data/inbox` (on the PC: `C:\Users\kevin\personal-api\data\inbox`) every minute. Each `.json` file there is one meal (or a list of meals). It's added to that day's totals and moved to `inbox/done`; a file that can't be read goes to `inbox/failed`. An AI that works out meals from photos and can save files on the PC only needs this instruction:

> After working out a meal from my photo, save a new file in C:\Users\kevin\personal-api\data\inbox named with the date, time and meal (for example 2026-10-26-1305-lunch.json), containing only:
> `{"date":"2026-10-26","meal":"lunch","kcal":640,"protein_g":42,"carbs_g":55,"fat_g":20,"description":"chicken wrap, apple"}`
> meal is breakfast, lunch, dinner or snack.

The same line can be pasted into the Fuel today card instead, for example from an AI app on the phone. A meal without a `date` goes on the day it's imported. The AI is only how the numbers get worked out; the server runs no AI.

## Run it

```
cp .env.example .env   # fill in GITHUB_TOKEN and INGEST_TOKEN
docker compose up -d --build
curl localhost:8000/days
```

Then open http://localhost:8000/dashboard (or the PC's Tailscale address from the phone).

Server setup for the spare computer: [docs/server-setup.md](docs/server-setup.md).

Phone app (in `android/`): [docs/phone-app.md](docs/phone-app.md). CI builds the APK on every PR, and every push to `main` publishes it as the `latest` release.

## Develop

```
python -m venv .venv && .venv/bin/pip install -r requirements-dev.txt
.venv/bin/python -m pytest
```

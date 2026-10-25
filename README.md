# Personal API

One endpoint that gathers Kevin's daily stats (sleep, workouts, GitHub commits, screen time) for dashboards.

AI helped write this code, but nothing here calls an AI at runtime. It's plain scheduled scripts and a small SQLite database.

## How it fits together

```
collectors (GitHub, ...) ──┐
phone app  ── POST /ingest ┼──> observations table (SQLite) ──> GET /days ──> dashboards
```

- **`observations` table**: one row per day, metric and source. Re-running a collector overwrites that day instead of duplicating it.
- **`GET /days?start=YYYY-MM-DD&end=YYYY-MM-DD`**: per-day metrics (the last 7 days by default).
- **`POST /ingest`**: the phone app uploads screen time and quick manual entries here, with `Authorization: Bearer $INGEST_TOKEN`.
- **Social media total**: the phone sends minutes per Android app as `screen.app.<package>`. `/days` adds the apps listed in `app/metrics.py` into `screen.social_media_minutes`, currently TikTok plus Instagram.

## Metrics so far

| Metric | Source | Automatic? |
|---|---|---|
| `github.commits` | GitHub GraphQL, hourly | Yes |
| `screen.tiktok_minutes`, `screen.instagram_minutes`, `screen.social_media_minutes` | Phone app (Android usage stats) | Yes |
| `sleep.total_minutes`, `sleep.deep_minutes`, `sleep.rem_minutes`, `sleep.light_minutes`, `sleep.awake_minutes` | Galaxy Watch → Samsung Health → Health Connect → phone app. Sleep counts toward the day you wake up. | Yes |
| `health.steps`, `health.heart_rate_avg`, `health.resting_heart_rate`, `health.hrv_rmssd` | Health Connect via the phone app | Yes |
| `workout.sessions`, `workout.minutes` | Workouts started on the watch | Yes |
| `kb.<move>.reps`, `kb.<move>.sets`, `kb.<move>.top_weight_lb` | Quick entry in the phone app | No, about 10 seconds per set |
| `football.max_catch_yards` | Quick entry in the phone app | No |

## Run it

```
cp .env.example .env   # fill in GITHUB_TOKEN and INGEST_TOKEN
docker compose up -d --build
curl localhost:8000/days
```

Server setup for the spare computer: [docs/server-setup.md](docs/server-setup.md).

Phone app (in `android/`): [docs/phone-app.md](docs/phone-app.md). CI builds the APK on every PR, and every push to `main` publishes it as the `latest` release.

## Develop

```
python -m venv .venv && .venv/bin/pip install -r requirements-dev.txt
.venv/bin/python -m pytest
```

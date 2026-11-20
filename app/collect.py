"""Runs every collector on a timer. Plain scheduled code: no AI is involved at runtime."""
import argparse
import logging
import time

from app import config, db, food
from app.collectors import alpaca, github

# Each collector takes the database connection and returns how many rows it stored.
COLLECTORS = {
    "github": lambda conn: db.upsert(conn, github.fetch()),
    "alpaca": alpaca.collect,
}

log = logging.getLogger("collect")
# The food inbox is checked far more often than the other sources, so a saved meal shows up within a minute.
INBOX_EVERY_SECONDS = 60


def run_once() -> None:
    conn = db.connect()
    for name, collect in COLLECTORS.items():
        try:
            n = collect(conn)
            log.info("%s: stored %d rows", name, n)
        except Exception:
            # One broken source shouldn't stop the others.
            log.exception("%s: failed", name)
    conn.close()


def import_food() -> None:
    conn = db.connect()
    try:
        n = food.import_inbox(conn)
        if n:
            log.info("food: added %d meals from the inbox", n)
    except Exception:
        log.exception("food: inbox import failed")
    finally:
        conn.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--loop", action="store_true",
        help="keep running: the food inbox every minute, the other sources every COLLECT_EVERY_MINUTES",
    )
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s %(message)s")
    next_full_run = 0.0
    while True:
        if time.monotonic() >= next_full_run:
            run_once()
            next_full_run = time.monotonic() + config.COLLECT_EVERY_MINUTES * 60
        import_food()
        if not args.loop:
            break
        time.sleep(INBOX_EVERY_SECONDS)


if __name__ == "__main__":
    main()

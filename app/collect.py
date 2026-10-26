"""Runs every collector on a timer. Plain scheduled code: no AI is involved at runtime."""
import argparse
import logging
import time

from app import config, db
from app.collectors import alpaca, github

# Each collector takes the database connection and returns how many rows it stored.
COLLECTORS = {
    "github": lambda conn: db.upsert(conn, github.fetch()),
    "alpaca": alpaca.collect,
}

log = logging.getLogger("collect")


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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--loop", action="store_true", help="keep running every COLLECT_EVERY_MINUTES")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(name)s %(message)s")
    while True:
        run_once()
        if not args.loop:
            break
        time.sleep(config.COLLECT_EVERY_MINUTES * 60)


if __name__ == "__main__":
    main()

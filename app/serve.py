"""Runs the API and the collector without Docker, for a PC where Docker can't run (python -m app.serve).
Restarts either one if it stops, and writes their output to data/logs."""
import os
import subprocess
import sys
import time
from pathlib import Path

from app.config import API_HOST, ROOT

DATA = ROOT / "data"
COMMANDS = {
    "api": [sys.executable, "-m", "uvicorn", "app.main:app", "--host", API_HOST, "--port", "8000"],
    "collector": [sys.executable, "-m", "app.collect", "--loop"],
}


def main() -> None:
    (DATA / "logs").mkdir(parents=True, exist_ok=True)
    # .env says /data/personal.db, which is the path inside Docker. Here the data folder sits next to the code.
    env = {**os.environ, "DB_PATH": str(DATA / "personal.db"), "FOOD_INBOX": str(DATA / "inbox")}
    running: dict[str, subprocess.Popen] = {}
    while True:
        for name, cmd in COMMANDS.items():
            proc = running.get(name)
            if proc is None or proc.poll() is not None:
                log = open(DATA / "logs" / f"{name}.log", "a", encoding="utf-8")
                running[name] = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
        time.sleep(10)


if __name__ == "__main__":
    main()

"""Runs the API and the collector without Docker, for a PC where Docker can't run (python -m app.serve).
Restarts either one if it stops, and writes their output to data/logs."""
import os
import subprocess
import sys
import time

from app.config import API_HOST, ROOT

DATA = ROOT / "data"
COMMANDS = {
    "api": [sys.executable, "-m", "uvicorn", "app.main:app", "--host", API_HOST, "--port", "8000"],
    "collector": [sys.executable, "-m", "app.collect", "--loop"],
}


def single_instance(path):
    """Locks path and returns the open file, or None if another copy of the server holds the lock.
    The lock goes away when the process ends, however it ends."""
    f = open(path, "a+")
    try:
        if os.name == "nt":
            import msvcrt
            f.seek(0)
            msvcrt.locking(f.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl
            fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        f.close()
        return None
    return f


def main() -> None:
    (DATA / "logs").mkdir(parents=True, exist_ok=True)
    # The scheduled task starts this every 10 minutes, so quietly do nothing while the server already runs.
    lock = single_instance(DATA / "serve.lock")
    if lock is None:
        return
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

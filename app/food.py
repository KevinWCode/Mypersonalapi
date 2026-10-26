"""Meals, from files an AI saves into the inbox folder or lines pasted on the dashboard.

Each meal is kept as a row, and a day's food.* totals are recomputed from those rows, so re-reading a file or
removing a meal never double-counts.
"""
import datetime as dt
import hashlib
import json
import logging
import shutil
import sqlite3
import time
import uuid
from pathlib import Path
from typing import Literal

from pydantic import BaseModel, Field, ValidationError, model_validator

from app import config, db

MEALS = ("breakfast", "lunch", "dinner", "snack")
# A file this new may still be being written, so it waits for the next pass.
SETTLE_SECONDS = 10
log = logging.getLogger("collect.food")


class Meal(BaseModel):
    date: dt.date | None = None
    meal: Literal["breakfast", "lunch", "dinner", "snack"]
    kcal: float = Field(ge=0, le=5000)
    protein_g: float = Field(default=0, ge=0, le=1000)
    carbs_g: float = Field(default=0, ge=0, le=1000)
    fat_g: float = Field(default=0, ge=0, le=1000)
    description: str = ""

    @model_validator(mode="before")
    @classmethod
    def _tidy(cls, data):
        """Accepts what an AI tends to write: "Lunch" or "snacks", "calories" for kcal, an items list, nulls."""
        if not isinstance(data, dict):
            return data
        data = {k: v for k, v in data.items() if v is not None}
        if "kcal" not in data and "calories" in data:
            data["kcal"] = data["calories"]
        if isinstance(data.get("meal"), str):
            data["meal"] = data["meal"].strip().lower().removesuffix("s")
        if not data.get("description") and isinstance(data.get("items"), list):
            data["description"] = ", ".join(map(str, data["items"]))
        data["description"] = str(data.get("description", ""))[:300]
        return data


def add(conn: sqlite3.Connection, meals: list[Meal], origin: str, ids: list[str] | None = None) -> list[str]:
    """Stores dated meals (an existing id is replaced) and refreshes those days' totals."""
    ids = ids or [uuid.uuid4().hex for _ in meals]
    now = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")
    conn.executemany(
        """INSERT OR REPLACE INTO meals (id, date, meal, kcal, protein_g, carbs_g, fat_g, description, origin, added_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        [(i, m.date.isoformat(), m.meal, m.kcal, m.protein_g, m.carbs_g, m.fat_g, m.description, origin, now)
         for i, m in zip(ids, meals)],
    )
    recompute(conn, {m.date.isoformat() for m in meals})
    return ids


def remove(conn: sqlite3.Connection, meal_id: str) -> bool:
    row = conn.execute("SELECT date FROM meals WHERE id = ?", (meal_id,)).fetchone()
    if row is None:
        return False
    conn.execute("DELETE FROM meals WHERE id = ?", (meal_id,))
    recompute(conn, {row["date"]})
    return True


def for_day(conn: sqlite3.Connection, day: str) -> list[dict]:
    return [dict(r) for r in conn.execute(
        "SELECT id, meal, kcal, protein_g, carbs_g, fat_g, description, origin FROM meals WHERE date = ? ORDER BY added_at",
        (day,),
    )]


def recompute(conn: sqlite3.Connection, days: set[str]) -> None:
    rows = []
    for day in days:
        kcal, protein, carbs, fat = conn.execute(
            "SELECT COALESCE(SUM(kcal), 0), COALESCE(SUM(protein_g), 0), COALESCE(SUM(carbs_g), 0), COALESCE(SUM(fat_g), 0) "
            "FROM meals WHERE date = ?",
            (day,),
        ).fetchone()
        by_meal = dict(conn.execute("SELECT meal, SUM(kcal) FROM meals WHERE date = ? GROUP BY meal", (day,)).fetchall())
        values = [("food.kcal", kcal, "kcal"), ("food.protein_g", protein, "g"), ("food.carbs_g", carbs, "g"),
                  ("food.fat_g", fat, "g")] + [(f"food.{m}.kcal", by_meal.get(m, 0), "kcal") for m in MEALS]
        rows += [{"date": day, "metric": k, "value": v, "unit": u, "source": "food"} for k, v, u in values]
    db.upsert(conn, rows)


def import_inbox(conn: sqlite3.Connection, inbox: Path | None = None, today: dt.date | None = None) -> int:
    """Adds every settled .json file in the inbox, then moves it to done/ (or failed/ if it can't be read).

    A file holds one meal object or a list of them. Meals without a date go on today. The meal ids come from the
    file's name and contents, so a file that couldn't be moved and is read again replaces itself.
    """
    inbox = Path(inbox or config.FOOD_INBOX)
    inbox.mkdir(parents=True, exist_ok=True)
    added = 0
    for path in sorted(inbox.glob("*.json")):
        if time.time() - path.stat().st_mtime < SETTLE_SECONDS:
            continue
        try:
            raw = path.read_bytes()
            data = json.loads(raw.decode("utf-8-sig"))
            meals = [Meal.model_validate(item) for item in (data if isinstance(data, list) else [data])]
        except (OSError, UnicodeDecodeError, json.JSONDecodeError, ValidationError) as e:
            log.warning("food: couldn't read %s, moved it to failed/: %s", path.name, str(e).splitlines()[0])
            _move(path, inbox / "failed")
            continue
        day = today or dt.date.today()
        meals = [m if m.date else m.model_copy(update={"date": day}) for m in meals]
        digest = hashlib.sha256(path.name.encode() + raw).hexdigest()[:16]
        add(conn, meals, origin=f"inbox:{path.name}", ids=[f"{digest}-{i}" for i in range(len(meals))])
        _move(path, inbox / "done")
        added += len(meals)
    return added


def _move(path: Path, folder: Path) -> None:
    folder.mkdir(exist_ok=True)
    target = folder / path.name
    if target.exists():
        target = folder / f"{path.stem}-{int(time.time())}{path.suffix}"
    shutil.move(str(path), str(target))

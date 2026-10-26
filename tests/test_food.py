import json
import os
import time

from fastapi.testclient import TestClient

from app import db, food
from app.main import app

client = TestClient(app)
AUTH = {"Authorization": "Bearer secret"}
DAY = "2026-10-26"


def totals(day=DAY):
    days = client.get("/days", params={"start": day, "end": day}).json()["days"]
    return {k: v for k, v in days[0]["metrics"].items() if k.startswith("food.")} if days else {}


def write(inbox, name, data, age=60):
    path = inbox / name
    path.write_text(data if isinstance(data, str) else json.dumps(data))
    then = time.time() - age
    os.utime(path, (then, then))
    return path


def test_inbox_adds_meals_and_files_them(tmp_path):
    inbox = tmp_path / "inbox"
    inbox.mkdir()
    write(inbox, "lunch.json", {"date": DAY, "meal": "Lunch", "calories": 640, "protein_g": 42, "carbs_g": 55, "fat_g": 20,
                                "items": ["chicken wrap", "apple"]})
    write(inbox, "snacks.json", [{"date": DAY, "meal": "snacks", "kcal": 100, "protein_g": None}, {"date": DAY, "meal": "snack", "kcal": 19}])
    write(inbox, "broken.json", "{not json")
    write(inbox, "still-writing.json", {"date": DAY, "meal": "dinner", "kcal": 900}, age=0)

    assert food.import_inbox(db.connect(), inbox) == 3
    t = totals()
    assert t["food.kcal"] == 759 and t["food.lunch.kcal"] == 640 and t["food.snack.kcal"] == 119
    assert t["food.protein_g"] == 42 and t["food.dinner.kcal"] == 0
    assert sorted(p.name for p in (inbox / "done").iterdir()) == ["lunch.json", "snacks.json"]
    assert [p.name for p in (inbox / "failed").iterdir()] == ["broken.json"]
    assert [p.name for p in inbox.glob("*.json")] == ["still-writing.json"]  # picked up on the next pass
    meals = client.get("/meals", params={"date": DAY}).json()["meals"]
    assert meals[0]["description"] == "chicken wrap, apple"


def test_reading_the_same_file_again_does_not_double_count(tmp_path):
    inbox = tmp_path / "inbox"
    inbox.mkdir()
    meal = {"date": DAY, "meal": "breakfast", "kcal": 452}
    write(inbox, "breakfast.json", meal)
    food.import_inbox(db.connect(), inbox)
    write(inbox, "breakfast.json", meal)  # e.g. it couldn't be moved the first time
    food.import_inbox(db.connect(), inbox)
    assert totals()["food.kcal"] == 452
    assert len(list((inbox / "done").iterdir())) == 2


def test_meal_without_date_goes_on_today(tmp_path):
    inbox = tmp_path / "inbox"
    inbox.mkdir()
    write(inbox, "x.json", {"meal": "dinner", "kcal": 800})
    food.import_inbox(db.connect(), inbox, today=food.dt.date(2026, 10, 25))
    assert totals("2026-10-25")["food.dinner.kcal"] == 800


def test_pasted_meals_add_up_and_can_be_removed():
    body = {"date": DAY, "meals": [{"meal": "lunch", "kcal": 640, "protein_g": 42}, {"meal": "snack", "kcal": 119}]}
    assert client.post("/meals", json=body).status_code == 401
    ids = client.post("/meals", json=body, headers=AUTH).json()["ids"]
    assert totals()["food.kcal"] == 759
    assert client.delete(f"/meals/{ids[0]}", headers=AUTH).json() == {"ok": True}
    assert totals()["food.kcal"] == 119 and totals()["food.lunch.kcal"] == 0
    assert client.delete(f"/meals/{ids[0]}", headers=AUTH).status_code == 404


def test_bad_meals_are_refused():
    for meal in [{"meal": "brunch", "kcal": 300}, {"meal": "lunch"}, {"meal": "lunch", "kcal": -5}]:
        assert client.post("/meals", json={"date": DAY, "meals": [meal]}, headers=AUTH).status_code == 422

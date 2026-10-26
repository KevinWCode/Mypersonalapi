import hmac
from datetime import date, timedelta
from pathlib import Path
from typing import Literal

from fastapi import Depends, FastAPI, Header, HTTPException, Query
from fastapi.responses import FileResponse, RedirectResponse
from pydantic import BaseModel, Field

from app import config, db, food
from app.collectors import alpaca
from app.metrics import build_days

app = FastAPI(title="Personal API")
DASHBOARD = Path(__file__).parent / "static" / "dashboard.html"


def get_conn():
    conn = db.connect()
    try:
        yield conn
    finally:
        conn.close()


class Observation(BaseModel):
    date: date
    metric: str = Field(min_length=1, max_length=200)
    value: float
    unit: str = Field(min_length=1, max_length=20)


class IngestBody(BaseModel):
    source: str = Field(min_length=1, max_length=50)
    observations: list[Observation] = Field(max_length=5000)


# What the dashboard's Quick log buttons and kettlebell ticks may write. Meals go through /meals instead.
LOG_METRICS = r"^(water\.ml|reading\.minutes|study\.minutes|coffee\.cups|food\.protein_g|kb\.[a-z_]{1,30}\.done)$"


class LogEntry(BaseModel):
    metric: str = Field(pattern=LOG_METRICS)
    value: float = Field(ge=-100000, le=100000)
    unit: str = Field(min_length=1, max_length=20)
    mode: Literal["add", "set"] = "add"


class LogBody(BaseModel):
    date: date
    entries: list[LogEntry] = Field(max_length=20)


class MealsBody(BaseModel):
    date: date
    meals: list[food.Meal] = Field(min_length=1, max_length=20)


def require_token(authorization: str):
    expected = f"Bearer {config.INGEST_TOKEN}"
    if not config.INGEST_TOKEN or not hmac.compare_digest(authorization, expected):
        raise HTTPException(401, "bad token")


@app.get("/", include_in_schema=False)
def root():
    return RedirectResponse("/dashboard")


@app.get("/dashboard", include_in_schema=False)
def dashboard():
    """The dashboard page. It's plain HTML and JavaScript that reads /days, /trading and /status."""
    return FileResponse(DASHBOARD)


@app.get("/health")
def health():
    return {"ok": True}


@app.get("/status")
def status(conn=Depends(get_conn)):
    """When each source last sent data, e.g. the phone's last sync."""
    return {"last_update": db.last_updates(conn)}


@app.get("/days")
def days(start: date | None = None, end: date | None = None, conn=Depends(get_conn)):
    end = end or date.today()
    start = start or end - timedelta(days=6)
    if start > end:
        raise HTTPException(400, "start must be on or before end")
    return {"days": build_days(db.fetch_range(conn, start.isoformat(), end.isoformat()))}


@app.get("/trading")
def trading(conn=Depends(get_conn)):
    return alpaca.summary(conn, date.today().isoformat())


@app.post("/ingest")
def ingest(body: IngestBody, authorization: str = Header(default=""), conn=Depends(get_conn)):
    """Where the phone app uploads screen time, health data and quick manual entries."""
    require_token(authorization)
    rows = [{**o.model_dump(), "date": o.date.isoformat(), "source": body.source} for o in body.observations]
    return {"stored": db.upsert(conn, rows)}


@app.post("/log")
def log(body: LogBody, authorization: str = Header(default=""), conn=Depends(get_conn)):
    """Quick log taps and kettlebell ticks from the dashboard. "add" entries add to the day's value instead of
    replacing it, so every tap counts. An empty list just checks the token."""
    require_token(authorization)
    entries = [e.model_dump() for e in body.entries]
    return {"values": db.add(conn, body.date.isoformat(), entries, source="dashboard")}


@app.get("/meals")
def list_meals(day: date = Query(alias="date"), conn=Depends(get_conn)):
    return {"meals": food.for_day(conn, day.isoformat())}


@app.post("/meals")
def add_meals(body: MealsBody, authorization: str = Header(default=""), conn=Depends(get_conn)):
    """Meals pasted on the dashboard. A meal without its own date goes on the body's date (the viewer's today)."""
    require_token(authorization)
    meals = [m if m.date else m.model_copy(update={"date": body.date}) for m in body.meals]
    return {"ids": food.add(conn, meals, origin="paste")}


@app.delete("/meals/{meal_id}")
def delete_meal(meal_id: str, authorization: str = Header(default=""), conn=Depends(get_conn)):
    require_token(authorization)
    if not food.remove(conn, meal_id):
        raise HTTPException(404, "no such meal")
    return {"ok": True}

import hmac
from datetime import date, timedelta
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.responses import FileResponse, RedirectResponse
from pydantic import BaseModel, Field

from app import config, db
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
    expected = f"Bearer {config.INGEST_TOKEN}"
    if not config.INGEST_TOKEN or not hmac.compare_digest(authorization, expected):
        raise HTTPException(401, "bad token")
    rows = [{**o.model_dump(), "date": o.date.isoformat(), "source": body.source} for o in body.observations]
    return {"stored": db.upsert(conn, rows)}

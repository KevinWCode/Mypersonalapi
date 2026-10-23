"""Counts commits per day from GitHub's contributions data (includes private repos when the token allows)."""
from datetime import date, datetime, time, timedelta, timezone

import httpx

from app import config

QUERY = """
query($login: String!, $from: DateTime!, $to: DateTime!) {
  user(login: $login) {
    contributionsCollection(from: $from, to: $to) {
      commitContributionsByRepository(maxRepositories: 100) {
        contributions(first: 100) {
          nodes { occurredAt commitCount }
        }
      }
    }
  }
}
"""


def fetch(days_back: int = 30, client: httpx.Client | None = None, today: date | None = None) -> list[dict]:
    if not (config.GITHUB_USER and config.GITHUB_TOKEN):
        raise RuntimeError("GITHUB_USER and GITHUB_TOKEN must be set")
    today = today or date.today()
    start = today - timedelta(days=days_back - 1)
    variables = {
        "login": config.GITHUB_USER,
        "from": datetime.combine(start, time.min, timezone.utc).isoformat(),
        "to": datetime.combine(today, time.max, timezone.utc).isoformat(),
    }
    client = client or httpx.Client(timeout=30)
    resp = client.post(
        "https://api.github.com/graphql",
        json={"query": QUERY, "variables": variables},
        headers={"Authorization": f"Bearer {config.GITHUB_TOKEN}"},
    )
    resp.raise_for_status()
    payload = resp.json()
    if payload.get("errors"):
        raise RuntimeError(payload["errors"])

    # Every day in the window gets a row, so days with no commits show as 0 rather than missing.
    counts = {(start + timedelta(days=i)).isoformat(): 0 for i in range(days_back)}
    repos = payload["data"]["user"]["contributionsCollection"]["commitContributionsByRepository"]
    for repo in repos:
        for node in repo["contributions"]["nodes"]:
            day = node["occurredAt"][:10]
            if day in counts:
                counts[day] += node["commitCount"]
    return [
        {"date": d, "metric": "github.commits", "value": n, "unit": "count", "source": "github"}
        for d, n in counts.items()
    ]

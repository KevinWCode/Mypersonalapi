from datetime import date

import httpx

from app.collectors import github


def fake_client(payload):
    def handler(request):
        assert request.headers["Authorization"] == "Bearer tok"
        return httpx.Response(200, json=payload)
    return httpx.Client(transport=httpx.MockTransport(handler))


def test_commits_summed_across_repos_with_zero_days():
    payload = {"data": {"user": {"contributionsCollection": {"commitContributionsByRepository": [
        {"contributions": {"nodes": [{"occurredAt": "2026-10-22T07:00:00Z", "commitCount": 3}]}},
        {"contributions": {"nodes": [{"occurredAt": "2026-10-22T07:00:00Z", "commitCount": 2},
                                     {"occurredAt": "2026-10-23T07:00:00Z", "commitCount": 1}]}},
    ]}}}}
    rows = github.fetch(days_back=3, client=fake_client(payload), today=date(2026, 10, 23))
    assert {r["date"]: r["value"] for r in rows} == {"2026-10-21": 0, "2026-10-22": 5, "2026-10-23": 1}
    assert all(r["metric"] == "github.commits" for r in rows)

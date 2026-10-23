import pytest

from app import config


@pytest.fixture(autouse=True)
def tmp_db(tmp_path, monkeypatch):
    monkeypatch.setattr(config, "DB_PATH", str(tmp_path / "test.db"))
    monkeypatch.setattr(config, "INGEST_TOKEN", "secret")
    monkeypatch.setattr(config, "GITHUB_USER", "someone")
    monkeypatch.setattr(config, "GITHUB_TOKEN", "tok")

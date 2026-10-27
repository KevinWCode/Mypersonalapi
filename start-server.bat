@echo off
rem Starts the Personal API without Docker: the API on port 8000 and the collector, in the background.
rem The first run sets up Python packages in .venv. Logs go to data\logs.
cd /d "%~dp0"
if not exist .venv\Scripts\pythonw.exe (
  python -m venv .venv || exit /b 1
  .venv\Scripts\python -m pip install -r requirements.txt || exit /b 1
)
start "" .venv\Scripts\pythonw.exe -m app.serve

@echo off
rem Makes a Task Scheduler task that starts the Personal API every 10 minutes unless it is already running,
rem so it comes back by itself if something stops it. Needs start-server.bat to have run once (it sets up .venv).
cd /d "%~dp0"
if not exist .venv\Scripts\pythonw.exe (
  echo Run start-server.bat once first, then run this again.
  pause
  exit /b 1
)
schtasks /create /f /tn "Personal API" /sc minute /mo 10 /tr "\"%~dp0.venv\Scripts\pythonw.exe\" \"%~dp0run-server.pyw\""
if errorlevel 1 (
  echo Could not create the task.
) else (
  echo Done. The Personal API now starts within 10 minutes whenever it is not running.
)
pause

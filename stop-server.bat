@echo off
rem Stops the Personal API started by start-server.bat.
cd /d "%~dp0"
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -like '%~dp0.venv\*' } | Invoke-CimMethod -MethodName Terminate | Out-Null"

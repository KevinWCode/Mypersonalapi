"""Starts the Personal API with no console window, for the scheduled task that install-autostart.bat makes.
Does nothing if the server is already running."""
import os
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
os.chdir(ROOT)
sys.path.insert(0, ROOT)

from app.serve import main  # noqa: E402

main()

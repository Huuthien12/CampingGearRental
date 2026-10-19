@echo off
rem Start the server first with Start-CampingGearRental.bat
start "" msedge "http://localhost:8080/login"
if errorlevel 1 start "" "http://localhost:8080/login"

@echo off
REM Launcher for LAN Device Manager (interactive menu)
REM Double-click this file to run the menu in a Windows cmd console.

:: Ensure console uses UTF-8 so Chinese characters display correctly
chcp 65001 >nul

:: Change to the script directory (repo root)
cd /d "%~dp0"

:: Run the Python menu script. Requires 'python' on PATH.
python "%~dp0python\lan_device_manager.py" %*

echo.
pause

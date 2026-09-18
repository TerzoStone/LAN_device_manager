@echo off
chcp 65001 > nul
cd /d "%~dp0"
if not exist "c\lan_device_manager.exe" (
    gcc -o "c\lan_device_manager.exe" "c\lan_device_manager.c"
    if errorlevel 1 (
        echo 编译失败，请确认已安装 GCC / MinGW。
        pause
        exit /b 1
    )
)
"c\lan_device_manager.exe"

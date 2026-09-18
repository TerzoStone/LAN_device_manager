@echo off
cd /d "%~dp0"
set JAVA_HOME=C:\Java\jdk-21.0.8+9
set PATH=%JAVA_HOME%\bin;%PATH%
mkdir out 2>nul
javac -d out java\src\LanDeviceViewer.java
if errorlevel 1 (
    echo Java 编译失败，请确认 JDK 已安装并配置正确。
    pause
    exit /b 1
)
java -cp out LanDeviceViewer

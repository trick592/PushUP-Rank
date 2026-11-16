@echo off
setlocal enabledelayedexpansion
title PushUp Rank - PC Installer & Launcher

echo ==================================================
echo   PushUp Rank - Windows PC / Android Installer
echo ==================================================
echo.

set "SCRIPT_DIR=%~dp0"
set "APK_PATH=%SCRIPT_DIR%PushUpRank-Mobile-Android.apk"
if not exist "%APK_PATH%" set "APK_PATH=%SCRIPT_DIR%..\.build-outputs\app-debug.apk"

if not exist "%APK_PATH%" (
  echo [ERROR] Could not find PushUpRank-Mobile-Android.apk.
  echo Download PushUpRank-PC-Installer.zip from GitHub Releases or place the APK next to this script.
  pause
  exit /b 1
)

where adb >nul 2>&1
if errorlevel 1 (
  echo [ERROR] ADB ^(Android Debug Bridge^) was not found in PATH.
  echo.
  echo To install and run PushUp Rank on Windows PC:
  echo   1. Install Android SDK Platform Tools or Android Studio / WSA / an Android Emulator.
  echo   2. Add the folder containing adb.exe to your PATH.
  echo   3. Start your Android Emulator ^(or enable WSA Developer Mode^) and re-run install-pc.bat.
  pause
  exit /b 1
)

rem Attempt to connect to Windows Subsystem for Android if no device is already listed
adb devices | findstr /R /C:"device$" >nul 2>&1
if errorlevel 1 (
  echo Trying Windows Subsystem for Android ^(127.0.0.1:58526^)...
  adb connect 127.0.0.1:58526 >nul 2>&1
)

adb devices | findstr /R /C:"device$" >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No active Android emulator, WSA instance, or USB device detected.
  echo Start an Android Emulator / WSA or connect your phone with USB Debugging enabled.
  pause
  exit /b 1
)

echo Installing PushUp Rank from "%APK_PATH%"...
adb install -r "%APK_PATH%"
if errorlevel 1 (
  echo [ERROR] Installation failed.
  pause
  exit /b 1
)

echo Launching PushUp Rank...
adb shell am start -n com.aistudio.pushuprank.xkqzmp/com.example.MainActivity
echo.
echo Done! PushUp Rank has been installed and launched.
pause

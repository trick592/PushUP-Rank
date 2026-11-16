@echo off
setlocal enabledelayedexpansion
title PushUp Rank - PC Installer & Launcher

echo ==================================================
echo   PushUp Rank - Windows PC / Android Installer
echo ==================================================
echo.

set "SCRIPT_DIR=%~dp0"
set "TARGET_APK="
for %%F in ("%SCRIPT_DIR%*.apk") do set "TARGET_APK=%%F"
if not defined TARGET_APK if exist "%SCRIPT_DIR%..\..\.build-outputs\app-debug.apk" (
  set "TARGET_APK=%SCRIPT_DIR%..\..\.build-outputs\app-debug.apk"
)

if not defined TARGET_APK (
  echo [ERROR] Nessun file .apk trovato nella cartella releases\pc\.
  echo Scarica l'APK da GitHub Releases o dal menu AI Studio e copialo qui accanto a install-pc.bat.
  pause
  exit /b 1
)

where adb >nul 2>&1
if errorlevel 1 (
  echo [ERROR] ADB ^(Android Debug Bridge^) non trovato nel PATH.
  echo Installa Android SDK Platform Tools, Android Studio Emulator o WSA e aggiungi adb.exe al PATH.
  pause
  exit /b 1
)

adb devices | findstr /R /C:"device$" >nul 2>&1
if errorlevel 1 (
  echo Tentativo di connessione a Windows Subsystem for Android ^(127.0.0.1:58526^)...
  adb connect 127.0.0.1:58526 >nul 2>&1
)

adb devices | findstr /R /C:"device$" >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Nessun emulatore Android, istanza WSA o smartphone USB rilevato.
  pause
  exit /b 1
)

echo Installazione di "%TARGET_APK%"...
adb install -r "%TARGET_APK%"
if errorlevel 1 (
  echo [ERROR] Installazione fallita.
  pause
  exit /b 1
)

echo Avvio di PushUp Rank...
adb shell am start -n com.aistudio.pushuprank.xkqzmp/com.example.MainActivity
echo Fatto! PushUp Rank installato e avviato.
pause

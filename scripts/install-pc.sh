#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK_PATH="${SCRIPT_DIR}/PushUpRank-Mobile-Android.apk"
if [[ ! -f "${APK_PATH}" ]]; then
  APK_PATH="${SCRIPT_DIR}/../.build-outputs/app-debug.apk"
fi

if [[ ! -f "${APK_PATH}" ]]; then
  echo "[ERROR] PushUpRank-Mobile-Android.apk not found."
  echo "Download PushUpRank-PC-Installer.zip from GitHub Releases or place the APK beside this script."
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "[ERROR] 'adb' (Android Debug Bridge) is not installed or not in PATH."
  echo "Install Android SDK Platform Tools (or Android Studio / Waydroid on Linux) and re-run."
  exit 1
fi

if ! adb devices | grep -qE $'\tdevice$'; then
  echo "[ERROR] No active Android emulator, Waydroid container, or USB device found."
  echo "Start an Android Emulator / Waydroid session or connect an Android phone with USB Debugging enabled."
  exit 1
fi

echo "Installing PushUp Rank from ${APK_PATH}..."
adb install -r "${APK_PATH}"

echo "Launching PushUp Rank..."
adb shell am start -n com.aistudio.pushuprank.xkqzmp/com.example.MainActivity
echo "Done! PushUp Rank installed and launched."

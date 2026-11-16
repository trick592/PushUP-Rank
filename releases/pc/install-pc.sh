#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_APK=""
for f in "${SCRIPT_DIR}"/*.apk; do
  if [[ -f "${f}" ]]; then
    TARGET_APK="${f}"
    break
  fi
done
if [[ -z "${TARGET_APK}" && -f "${SCRIPT_DIR}/../../.build-outputs/app-debug.apk" ]]; then
  TARGET_APK="${SCRIPT_DIR}/../../.build-outputs/app-debug.apk"
fi

if [[ -z "${TARGET_APK}" ]]; then
  echo "[ERROR] Nessun file .apk trovato in releases/pc/."
  echo "Scarica l'APK da GitHub Releases o dal menu AI Studio e posizionalo accanto a install-pc.sh."
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "[ERROR] 'adb' (Android Debug Bridge) non trovato nel PATH."
  echo "Installa Android SDK Platform Tools (o Android Studio / Waydroid su Linux) e riprova."
  exit 1
fi

if ! adb devices | grep -qE $'\tdevice$'; then
  echo "[ERROR] Nessun emulatore Android, Waydroid o smartphone USB rilevato."
  exit 1
fi

echo "Installazione di ${TARGET_APK}..."
adb install -r "${TARGET_APK}"

echo "Avvio di PushUp Rank..."
adb shell am start -n com.aistudio.pushuprank.xkqzmp/com.example.MainActivity
echo "Fatto! PushUp Rank installato e avviato."

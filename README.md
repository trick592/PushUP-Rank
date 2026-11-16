# PushUp Rank

Competitive Android fitness application powered by **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, **Google ML Kit Pose Detection** (100% on-device), and **Cloud Firestore**.

![PushUp Rank PC Banner](assets/pc/pushup_rank_pc.jpg)

---

## Install the App (Mobile & PC)

Because the compiled APK with on-device ML Kit Pose Detection models is ~107 MB (exceeding GitHub's 100 MB raw git file limit), installable binaries are published automatically to **GitHub Releases** via [`.github/workflows/release-assets.yml`](.github/workflows/release-assets.yml).

### Mobile (Android)
1. Open the repository's **Releases** tab (`Releases -> Latest`).
2. Download **`PushUpRank-Mobile-Android.apk`** directly onto your Android device.
3. Open the downloaded `.apk` file and allow installation from your browser/files app when prompted.

> **First-time GitHub push**: After pushing this repo to GitHub, open the **Actions** tab and run **Build & Publish Mobile and PC Release Assets** (or push to `main` / tag `v1.0.0`) to generate and attach the `latest` release assets automatically.

### PC (Windows / macOS / Linux)
PushUp Rank is a native Android application (requiring CameraX + ML Kit). To install and run it on PC:
1. Download **`PushUpRank-PC-Installer.zip`** from **Releases** (contains `PushUpRank-Mobile-Android.apk`, `install-pc.bat`, and `install-pc.sh`).
2. Extract the archive and start your preferred PC Android runtime:
   - **Windows**: Windows Subsystem for Android (WSA with Developer Mode), Android Studio Emulator, or any Android runtime with ADB enabled, then double-click **`install-pc.bat`**.
   - **macOS / Linux**: Android Studio Emulator or Waydroid (Linux), then run `chmod +x install-pc.sh && ./install-pc.sh`.
3. Alternatively, connect your Android phone via USB (with USB Debugging enabled) and run the same script from PC to install and launch it directly on your phone.

---

## Mobile Showcase Art

<p align="center">
  <img src="assets/mobile/pushup_rank_mobile.jpg" alt="PushUp Rank Mobile Asset" width="320" />
</p>

## Key Features

- **On-Device AI Pose Tracking**: Real-time elbow angle (< 90° bottom / > 160° lockout) and back alignment validation via ML Kit Pose Detection running entirely in RAM.
- **Competitive Ranks & Arena**: Progression tiers (Bronze to Master), live Cloud Firestore global/friends leaderboards with cadence anti-cheat, and 1v1 Arena Duels.
- **Progress Analytics & Daily Streak**: Interactive Compose Canvas workout progression chart (Line & Bar modes) and consecutive-day streak tracking.
- **PushCoins & Cosmetic Shop**: Unlockable themes, skeleton visual effects, avatar frames, titles, and consumable session boosts.
- **Play Policy Compliance**: Explicit RAM-only camera consent, first-launch medical fitness disclaimer, and full in-app account/data deletion.

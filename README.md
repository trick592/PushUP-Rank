# PushUp Rank

Applicazione Android competitiva basata su **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, **Google ML Kit Pose Detection** (100% on-device) e **Cloud Firestore**.

![PushUp Rank PC Banner](assets/pc/pushup_rank_pc.jpg)

---

## Installazione (Mobile & PC)

### Mobile (Android)
1. Genera o scarica l'APK dal menu impostazioni di AI Studio (oppure dalla scheda **Releases** del repository GitHub dopo averlo caricato).
2. Trasferisci o scarica il file `.apk` sul tuo smartphone Android e aprilo per completare l'installazione.

### PC (Windows / macOS / Linux)
PushUp Rank è un'app Android nativa (CameraX + ML Kit). Per installarla e avviarla su PC tramite **Windows Subsystem for Android (WSA)**, **Android Studio Emulator**, **Waydroid** o smartphone collegato via USB:
1. Metti il file `.apk` nella cartella `releases/pc/` (oppure usa quello esportato nella stessa cartella dello script).
2. Avvia lo script di installazione:
   - **Windows**: [`releases/pc/install-pc.bat`](releases/pc/install-pc.bat)
   - **macOS / Linux**: [`releases/pc/install-pc.sh`](releases/pc/install-pc.sh)

### Pubblicazione su GitHub Releases (Opzionale)
Per evitare blocchi durante il push automatico (`workflow` scope OAuth e limiti di dimensione binari):
- **Metodo rapido**: su GitHub apri **Releases → Create a new release**, crea un tag (es. `v1.0.0`) e allega l'APK esportato così sarà subito installabile da Mobile e PC.
- **Build automatica via GitHub Actions**: apri il file [`docs/release-assets.yml`](docs/release-assets.yml), copiane il contenuto e incollalo su GitHub creando il file `.github/workflows/release-assets.yml` direttamente dall'interfaccia web di GitHub.

---

## Showcase Mobile

<p align="center">
  <img src="assets/mobile/pushup_rank_mobile.jpg" alt="PushUp Rank Mobile Asset" width="320" />
</p>

## Funzionalità Principali

- **Rilevamento Flessioni AI On-Device**: calcolo angolo del gomito (< 90° discesa, > 160° risalita) e allineamento schiena con ML Kit Pose Detection elaborato esclusivamente in RAM.
- **Rank, Classifica & Arena 1v1**: livelli da Bronzo a Maestro, classifica globale e amici in tempo reale su Cloud Firestore con anti-cheat e duelli 1v1.
- **Grafico Progressi & Serie Giornaliera**: grafico interattivo Compose Canvas (Linee/Barre) e conteggio giorni consecutivi sul profilo.
- **PushCoin & Negozio In-App**: temi, effetti scheletro, cornici avatar, titoli e potenziamenti.
- **Conformità Google Play**: consenso esplicito fotocamera in RAM, disclaimer medico iniziale ed eliminazione completa account/dati.

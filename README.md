# PushUp Rank

Applicazione Android competitiva basata su **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, **Google ML Kit Pose Detection** (100% on-device) e **Cloud Firestore**.

<p align="center">
  <img src="assets/mobile/pushup_rank_mobile.jpg" alt="PushUp Rank Android" width="320" />
</p>

---

## Scarica e Installa su Telefono Android

Chiunque abbia un telefono Android può scaricare e installare l'app direttamente da GitHub Releases:

- **Download diretto APK (Android)**: [**Scarica `PushUpRank-Android.apk`**](../../releases/latest/download/PushUpRank-Android.apk)
- **Pagina Releases**: [Vedi tutte le versioni](../../releases/latest)

### Come installare da smartphone (3 passaggi)
1. Apri il link **Scarica `PushUpRank-Android.apk`** dal browser del tuo telefono Android.
2. Al termine del download, tocca il file `.apk` dalle notifiche o dall'app **File**.
3. Se richiesto da Android, attiva **"Consenti da questa origine"** e premi **Installa**.

---

## Funzionalità Principali

- **Rilevamento Flessioni AI On-Device**: calcolo angolo del gomito (< 90° discesa, > 160° risalita) e allineamento schiena con ML Kit Pose Detection elaborato esclusivamente in RAM.
- **Rank, Classifica & Arena 1v1**: livelli da Bronzo a Maestro, classifica globale e amici in tempo reale su Cloud Firestore con anti-cheat e duelli 1v1.
- **Grafico Progressi & Serie Giornaliera**: grafico interattivo Compose Canvas (Linee/Barre) e conteggio giorni consecutivi sul profilo.
- **PushCoin & Negozio In-App**: temi, effetti scheletro, cornici avatar, titoli e potenziamenti.
- **Conformità Google Play**: consenso esplicito fotocamera in RAM, disclaimer medico iniziale ed eliminazione completa account/dati.

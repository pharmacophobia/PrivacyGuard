# 🛡️ PrivacyGuard — Android Telemetry Monitor & Policy Auditor

> **See exactly what your apps are secretly sending — and report policy-breaking apps in 1 tap.**

PrivacyGuard is a free, open-source Android security app that monitors network traffic, audits embedded SDKs, flags Google Play policy violations, and semi-automates takedown complaints. A calculator app requesting your microphone? An ad network harvesting fine GPS coordinates from a torrent client? PrivacyGuard catches it all and arms you with technical evidence to report them.

---

## 📥 Direct APK Download & Install

| Direct 1-Tap Download | Scan QR Code with Phone Camera to Install |
| :---: | :---: |
| [![Download APK](https://img.shields.io/badge/Download-PrivacyGuard.apk-00C853?style=for-the-badge&logo=android&logoColor=white)](https://github.com/pharmacophobia/PrivacyGuard/raw/main/PrivacyGuard.apk)<br><br>👉 **[Click here to download PrivacyGuard.apk (15 MB)](https://github.com/pharmacophobia/PrivacyGuard/raw/main/PrivacyGuard.apk)**<br><br>📦 Alternate: [Official GitHub Release v1.1.0](https://github.com/pharmacophobia/PrivacyGuard/releases/tag/v1.1.0) | <img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=https://github.com/pharmacophobia/PrivacyGuard/raw/main/PrivacyGuard.apk" width="180" height="180" alt="Scan to install APK" /><br>*(Point your phone camera to download)* |

---

## ✨ Features

- **📡 Real-Time Telemetry Detection** — Intercepts and classifies outbound network calls made by other apps using local `VpnService` sinkhole routing (zero root required).
- **⚖️ Google Play Policy Violation Engine** — Scans installed apps for policy breaches:
  - *Location Exfiltration via Ad SDKs* (InMobi, UnityAds, ironSource, AppLovin accessing fine/coarse GPS)
  - *Background Location Abuse* (monetization/ad networks tracking location in the background)
  - *Restricted Permissions Harvesting* (SMS & Call Log access in non-default apps)
  - *Deceptive Sensor Surveillance* (Microphone & Camera declared in basic utility tools)
  - *Undeclared Device Fingerprinting* (Data brokers correlating telephony state & hardware IDs)
- **🚀 1-Tap Prepare & Report Pipeline** — Automates the reporting workflow:
  - Compiles structured, factual policy violation briefs with package IDs, detected SDK signatures, and Google Play policy clauses.
  - Automatically copies the legal brief to your clipboard.
  - Deep-links directly to the target app's listing in the **Google Play Store** (ready to tap `⋮ -> Flag as inappropriate`) or opens Google's formal **Developer Policy Takedown Web Form**.
- **🔍 Static Privacy Scanner** — Analyses installed packages for embedded commercial tracking SDKs and anomalous permission matrices.
- **📊 Interactive Risk Dashboard** — Colour-coded risk ratings (Critical, High, Medium, Low) with filterable views for flagged policy breakers.
- **🔒 Local Sinkhole Server** — Blocks known telemetry and advertising endpoints locally on-device.

---

## 📱 Requirements

- Android 8.0 (API 26) or higher
- No root required

---

## 🚀 Build & Install

```bash
# Clone the repo
git clone https://github.com/pharmacophobia/PrivacyGuard.git
cd PrivacyGuard

# Build debug APK
./gradlew assembleDebug

# Install via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or download the latest APK from the [Releases](../../releases) page and sideload it.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.0.0
- **UI**: Jetpack Compose + Material 3 (Material You)
- **Architecture**: Clean Architecture / MVVM + StateFlow
- **Network Monitoring**: VpnService API (Local Sinkhole DNS & SNI inspection)
- **Static Auditing**: PackageManager heuristic analysis + Tracker Database
- **Reporting Engine**: 1-Tap Prepare & File Intent Dispatcher (`market://details` & Google Takedown portal)

---

## 📄 License

MIT License — free to use, modify, and distribute.

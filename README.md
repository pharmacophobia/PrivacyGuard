# 🛡️ PrivacyGuard — Android Telemetry Monitor

> **See exactly what your apps are secretly sending — in real time.**

PrivacyGuard is a free, open-source Android app that monitors network traffic and permissions to expose apps that are gathering telemetry you wouldn't expect. A calculator app requesting your microphone? A flashlight app phoning home with your location? PrivacyGuard catches it all.

---

## ✨ Features

- **📡 Real-Time Telemetry Detection** — Intercepts and classifies outbound network calls made by other apps
- **🔍 Static Privacy Scanner** — Analyses installed apps for suspicious permission combinations (e.g. mic + background internet on a utility app)
- **📊 Risk Dashboard** — Colour-coded risk scores for every installed app, updated live
- **🔒 Local Sinkhole Server** — Optionally block telemetry endpoints without root
- **📋 Detailed Reports** — Export a full privacy audit of your device

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

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + ViewModel
- **Network Monitoring**: VpnService API (no root)
- **Scanning**: Static permission analysis + heuristic rules

---

## 📄 License

MIT License — free to use, modify, and distribute.

# Aether (æther) — Living Miniature Diorama Weather

A minimalist weather application for Android built with Jetpack Compose, Kotlin coroutines, and Room local database persistence. Features living miniature diorama backgrounds responsive to geography and live conditions, dual-provider weather data (Open-Meteo & Tomorrow.io), hyper-local Indian AQI station fallback (CPCB via WAQI), 24-hour time scrubbing with tactile feedback, seamless in-app GitHub update downloads, and customizable ambiance modes.

---

## 📑 Table of Contents
1. [Overview & Highlights](#-overview--highlights)
2. [Latest Release: v1.2.0](#-latest-release-v120)
3. [Download & Installation](#-download--installation)
4. [Google Play Protect Troubleshooting & FAQ](#-google-play-protect-troubleshooting--faq)
5. [Key Features](#-key-features)
6. [Weather Data & AQI Providers](#-weather-data--aqi-providers)
7. [Architecture & Tech Stack](#-architecture--tech-stack)
8. [Privacy & Security](#-privacy--security)
9. [Android App Compliance & Quality Checklist](#-android-app-compliance--quality-checklist)

---

## 🌟 Overview & Highlights

- **Living Miniature Dioramas**: Procedural Vector & Compose Canvas rendering that illustrates live weather conditions, daylight cycles, terrain elevation, precipitation density, and lightning flashes.
- **Dual Weather Engine**: Switch seamlessly between Open-Meteo and Tomorrow.io with automatic fallback protection.
- **24-Hour Time Scrubber**: Scrub backwards or forwards across the day with multi-tier tactile haptic feedback.
- **Hyper-Local AQI Engine**: Real-time Air Quality Index integration with fallback to CPCB (Central Pollution Control Board) stations via WAQI.
- **In-App Updater**: Direct GitHub Release update detector, system notification triggers, and native background APK installer.
- **100% On-Device Privacy**: No accounts, no user profiling, and no server-side telemetry.

---

## 🚀 Latest Release: v1.2.0

### **Release Title:**
**`v1.2.0 — Enhanced Scrubber Haptics, In-App Privacy Policy & Update Polish`**

### **What's New in v1.2.0:**
* 🎯 **Universal Timeline Scrubber Haptics**: Completely overhauled the timeline scrubber vibration engine. Added Compose-level tactile clicks combined with an adaptive `EFFECT_CLICK` + hardware pulse fallback that works seamlessly across all Android OEMs (Samsung, Xiaomi, Pixel, OnePlus, Motorola).
* 🛡️ **In-App Privacy Policy Dialog**: Added a dedicated Privacy Policy section in Settings detailing our zero-tracking, ephemeral location policy and local-only Room database architecture.
* 📦 **In-App Self-Update Engine**: Integrated background GitHub release checks, notification shade alerts for new versions, and in-app `DownloadManager` package installer integration.
* ⚡ **Performance & Build Upgrades**: Bumped version code to 3 (v1.2.0) targeting Android 16 (API 36).

---

## 📥 Download & Installation

Download the latest release APK directly from GitHub:
👉 **[Download Latest APK](https://github.com/SpicychieF05/Aether/releases/latest)**

### How to Install:
1. Download `Aether-v1.2.0.apk` from the [Releases page](https://github.com/SpicychieF05/Aether/releases/latest).
2. Tap the downloaded `.apk` file.
3. If prompted by Android, toggle **"Allow from this source"** / **"Install unknown apps"** for your browser or file manager.
4. Tap **Install**.

* **Minimum Supported Android**: Android 7.0 (Nougat, API level 24) or higher.
* **Target Android Version**: Android 16 (API level 36).

---

## 🛡️ Google Play Protect Troubleshooting & FAQ

### **Q: Why does the installation get stuck or hang on "Scanning with Play Protect"?**
> **Answer:**
> When you install an APK downloaded from GitHub (outside the Google Play Store), Google Play Protect automatically performs a cloud verification. Because open-source release keys are not yet in Google's enterprise commercial database, Play Protect pauses to upload file hashes or bytecode metadata to Google's analysis servers.
>
> If your phone has a slow network connection, an active VPN, or private DNS blockers (e.g. AdGuard, NextDNS), the Play Protect connection can hang indefinitely waiting for a server response.

### **Q: How do I pass through or bypass the Play Protect scan?**
> **Solution 1 — Instant Bypass (Recommended):**
> 1. When the installer pauses or shows a prompt, look for **"More details"** or **"Install anyway (unsafe)"**.
> 2. If a popup asks *"Send app for scanning?"*, tap **"Don't send"**.
> 3. The package manager will complete installation in seconds.
>
> **Solution 2 — Temporarily Disable Scanning on Test Devices:**
> If you frequently test sideloaded development APKs:
> 1. Open the **Google Play Store** app.
> 2. Tap your **profile avatar** in the top-right corner.
> 3. Select **Play Protect** → tap the **Settings (gear)** icon in the upper-right.
> 4. Temporarily toggle off **"Scan apps with Play Protect"** or **"Improve harmful app detection"**.
> 5. Install Aether, then re-enable Play Protect.

---

## ⚡ Key Features

1. **Procedural Dioramas**: Zero heavy bitmap overhead; renders clouds, rain streaks, snow particles, wind turbulence, and lightning via dynamic GPU Canvas draw calls.
2. **Thunderstorm Haptics**: Generates dynamic rumble vibration effects during active thunderstorm weather.
3. **Multi-Pane Responsive UI**: Beautifully adapts across compact phones, foldables, and large tablet screens.
4. **Air Quality Badges**: Color-coded European/Indian AQI scale with dominant pollutant indicators (PM2.5, PM10, O3, NO2).
5. **Offline Mode**: Automatic 30-minute stale cache threshold with offline badges when traveling without internet.

---

## 🌐 Weather Data & AQI Providers

1. **Open-Meteo**: Free, unlimited rate limits, global high-resolution meteorological models.
2. **Tomorrow.io**: Hyper-local Timelines API with minute-by-minute precipitation tracking.
3. **WAQI (World Air Quality Index)**: Live station telemetry with automatic fallback to nearest CPCB monitors for accurate Indian AQI.

---

## 🏛️ Architecture & Tech Stack

* **UI Toolkit**: Jetpack Compose & Material Design 3 (M3)
* **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel)
* **Asynchronous Flow**: Kotlin Coroutines, StateFlow, SharedFlow
* **Local Persistence**: Room Database (SQLite) + SharedPreferences
* **Networking**: OkHttp, Retrofit, Moshi JSON Converter
* **Platform Security**: Android FileProvider, DownloadManager, System Notification Channels

---

## 🔒 Privacy & Security

* **Ephemeral Location Data**: GPS coordinates are accessed solely while using the app to fetch local weather. Coordinates are never logged, stored, or transmitted to analytics servers.
* **On-Device Storage**: Saved search history and favorite locations are stored 100% locally in your device's Room database.
* **Strict HTTPS**: All network traffic is encrypted via TLS (`https://`).

---

## 📋 Android App Compliance & Quality Checklist

### A. UI and Layout
- [x] **1. [CORE] Support different screen sizes and orientations**: Multi-pane layout for tablets/foldables (`MultiPaneAdaptiveLayout`) and single-column layout for compact phones (`CompactPhoneLayout`).
- [x] **2. [CORE] Handle edge-to-edge display, system bar insets and on-screen keyboard**: Enabled via `enableEdgeToEdge()` in `MainActivity` with bottom sheets handling IME insets.
- [x] **3. [CORE] Support dark mode and light mode with consistent theme**: Atmospheric theme optimized for diorama canvases and day/night transitions.
- [x] **4. [CORE] String resources & dimensions**: Semantic color schemes, typography, and unified formatters.
- [x] **5. [CORE] Clean code**: Zero placeholder text, lorem ipsum, or mock data.
- [x] **6. [CORE] Adaptive icon**: Custom adaptive launcher icon configured.
- [x] **7. [CORE] Version naming**: `versionCode = 3`, `versionName = "1.2.0"`, unique applicationId `com.aistudio.aether.wthrx`.

### B. Navigation and States
- [x] **8. [CORE] Consistent back-button and navigation behaviour**: Modal bottom sheets with swipe-to-dismiss and back-press handling.
- [x] **9. [IF] Empty states**: Handled for search queries with no results, empty search history, and initial state.
- [x] **10. [IF] Loading states**: Polished indicator with atmospheric backdrop during provider queries and GPS lock.
- [x] **11. [IF] Error states**: Explicit error banners with one-tap Retry actions and automatic provider fallback.
- [x] **12. [IF] Success feedback**: Instant feedback on saving default weather provider and unit preferences.
- [x] **13. [IF] Form validation**: Real-time debounced location search validation.

### C. Robustness & Persistence
- [x] **14. [CORE] Configuration changes**: Preserved across rotation and process death in `AetherViewModel` with Room cache.
- [x] **15. [IF] Network resilience**: Room database offline cache fallback with 30-minute stale indicator.
- [x] **16. [IF] Permissions**: Location permissions requested with graceful fallback to manual city search.
- [x] **17. [CORE] Threading**: Heavy I/O, network, and database operations safely isolated on `Dispatchers.IO`.
- [x] **18. [CORE] Least privilege**: Only minimal necessary permissions declared in `AndroidManifest.xml`.

---

Developed with ❤️ for weather enthusiasts.

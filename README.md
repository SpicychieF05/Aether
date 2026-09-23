# Aether (æther) — Living Miniature Diorama Weather

A minimalist weather application for Android built with Jetpack Compose, Kotlin coroutines, and Room local database persistence. Features living miniature diorama backgrounds responsive to geography and live conditions, dual-provider weather data (Open-Meteo & Tomorrow.io), hyper-local Indian AQI station fallback (CPCB via WAQI), 24-hour time scrubbing, and customizable ambiance modes.

---

## Android App Checklist (Kotlin) Status

### A. UI and Layout
- [x] **1. [CORE] Support different screen sizes and orientations**: Multi-pane layout for tablets/foldables (`MultiPaneAdaptiveLayout`) and single-column layout for compact phones (`CompactPhoneLayout`).
- [x] **2. [CORE] Handle edge-to-edge display, system bar insets and on-screen keyboard**: Enabled via `enableEdgeToEdge()` in `MainActivity` with bottom sheets handling ime insets.
- [x] **3. [CORE] Support dark mode and light mode with consistent theme**: Dark atmospheric theme optimized for vibrant diorama canvases and day/night transitions.
- [x] **4. [CORE] Use string resources instead of hardcoded text; use dimens and colour resources**: Semantic color schemes, text styles, and unified formats.
- [x] **5. [CORE] Remove placeholder text, lorem ipsum, test data and leftover template code**: No placeholder text or mock weather states.
- [x] **6. [CORE] Add an adaptive app icon and a splash screen**: Custom adaptive launcher icon and theming configured.
- [x] **7. [CORE] Set a proper app name, versionCode and versionName**: App name "Aether", versionCode 1, versionName "1.0", unique applicationId `com.aistudio.aether.wthrx`.

### B. Navigation and States
- [x] **8. [CORE] Consistent back-button and navigation behaviour**: Modal bottom sheets with swipe-to-dismiss and back-press handling.
- [x] **9. [IF] Add empty states**: Empty states for search query with no results, empty search history, and initial state.
- [x] **10. [IF] Add loading states**: Polished circular indicator with atmospheric backdrop during provider queries and GPS lock.
- [x] **11. [IF] Add error states with clear messages and a Retry action**: Explicit error banners with one-tap Retry actions and automatic provider fallback.
- [x] **12. [IF] Add success feedback after actions**: "Saved as default" feedback and instant slot assignment confirmation.
- [x] **13. [IF] Add form validation with inline error messages**: Real-time debounced search validation.

### C. Robustness
- [x] **14. [CORE] Survive configuration changes and process death**: State preserved in `AetherViewModel` with Room local database cache.
- [x] **15. [IF] Handle no internet and slow networks gracefully**: Offline cache indicator and Room database offline cache fallback with 30-minute stale threshold.
- [x] **16. [IF] Runtime permissions**: GPS location permissions requested with rationale and graceful fallback to default location (Kolkata).
- [x] **17. [CORE] Keep heavy work off the main thread**: Coroutine scopes (`Dispatchers.IO`), Moshi parsing, and Canvas rendering off the main thread.
- [x] **18. [CORE] Remove unused permissions, unused dependencies, debug logs and test code**: Clean manifest and dependency catalog.

### D. Content and Links
- [x] **19. [IF] Intents for external links**: Attribution links for Open-Meteo, Tomorrow.io, and WAQI open in browser.
- [x] **20. [IF] Fix broken links and deep links**: Valid API endpoints and proper user-agent headers for OpenStreetMap Nominatim.
- [x] **21. [CORE] Compress and optimize images and assets**: Procedural Canvas-based miniature diorama engine with zero bitmap memory bloat.
- [x] **22. [IF] Image loading library**: Procedural vector graphics and Compose Canvas rendering.

### E. Accessibility
- [x] **23. [CORE] Accessibility compliance**: Content descriptions on all interactive elements, 48dp minimum touch targets, sp font dimensions.

### F. Security and Release Readiness
- [x] **24. [CORE] Keep API keys and secrets secure**: Securely separated API configurations with fallback options.
- [x] **25. [IF] Use HTTPS only**: All network calls strictly over TLS (`https://`).
- [x] **26. [CORE] Enable R8/ProGuard shrinking for release builds**: Configured with proguard rules.
- [x] **27. [CORE] Target latest API level**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`.
- [x] **28. [CORE] Configure release signing**: Configured in `build.gradle.kts`.
- [x] **29. [IF] Prepare Play Store assets**: Play-compliant metadata and icons.

### G. Manual Testing Checklist (For Real Devices)
- [ ] **30.** Test on a real Android device (and emulator with different screen size).
- [ ] **31.** Test on different Android versions (`minSdk 24` through `API 35/36`).
- [ ] **32.** Test with airplane mode (verifying offline Room cache), denied location permissions, and device rotation.
- [ ] **33.** Test release APK/AAB build before publishing.

---

## Weather Providers
1. **Open-Meteo**: Free, unlimited rate limits, global open numerical models.
2. **Tomorrow.io**: Hyper-local v4 Timelines API with seamless automatic fallback to Open-Meteo upon rate limits or errors.

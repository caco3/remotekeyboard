# Release Notes — Remote Keyboard 0.1.0

## Overview

This is the first tagged release of the modernised Remote Keyboard fork. It brings the original app up to current Android standards with a Material Design 3 UI, vector adaptive icons, a Gradle-based build system, and GitHub Actions CI.

---

## What's New

### UI & Design
- **Material Design 3 (Material You)** — complete visual overhaul from the legacy Holo theme
- **Adaptive launcher icon** — a new vector PC-keyboard icon that adapts to the user's wallpaper and theme (Android 8+); includes 4 alternative style variants
- **Material3 vector menu icons** — all toolbar and menu icons replaced with scalable vector drawables
- **Version display** — current version name and git commit hash shown on the main screen
- **Removed legacy IME button strip** — the on-screen keyboard-selection / disconnect / send buttons were removed for a cleaner interface
- **Keyboard selection button** — moved to a prominent, easy-to-reach button on the main screen

### Build & Tooling
- **Gradle + AGP 8.x** — migrated from the original Ant build system to modern Gradle
- **GitHub Actions CI** — automated debug APK builds on every push
- **Release signing support** — optional release signing via GitHub Actions secrets
- **Target SDK 34 / Min SDK 21** — supports Android 5.0 through Android 14

### Networking
- **Plain telnet restored** — TLS/SSL socket support was attempted and found incompatible with standard telnet clients; the app now uses plain TCP/telnet on port 2323 as originally intended
- **Help URLs updated** — documentation links now point to the `caco3/RemoteKeyboard` repository

### Widget
- **Unified icon** — the home-screen widget now uses the same adaptive launcher icon as the app instead of an old, mismatched PNG
- **Legacy PNG assets cleaned up** — all obsolete bitmap icons have been removed

### Removed
- **TextFiction promotional link** — removed from the overflow menu; the app no longer suggests or links to an external companion app
- **Outdated README** — replaced with comprehensive, up-to-date documentation including screenshots and ADB deployment instructions

---

## Features (unchanged from original)

- Type from your PC keyboard into any Android text field over Wi-Fi (telnet / TCP)
- Text replacement / macro system with downloadable dictionaries
- Home screen widget to toggle the server
- F1–F12 key support with configurable quick-launch actions

---

## Requirements

- Android 5.0 (API 21) or newer
- PC and phone on the same Wi-Fi network
- A telnet client on the PC (e.g. `telnet`, `nc`, PuTTY)

---

## Installation

### Option 1 — Download from GitHub Actions
Open the [Actions tab](../../actions), click the latest successful workflow run, and download the `app-debug` artifact.

### Option 2 — Build from source
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Acknowledgements

This release is a fork of [Lepidos/remotekeyboard](https://github.com/Lepidos/remotekeyboard), itself a modernised fork of the original [onyxbits/remotekeyboard](https://github.com/onyxbits/remotekeyboard) by Patrick Matusz.

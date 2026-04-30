# Remote Keyboard

An Android input method that lets you type on your phone from your PC keyboard over Wi-Fi.

This is a fork of [Lepidos/remotekeyboard](https://github.com/Lepidos/remotekeyboard), which itself is a modernised fork of the original [onyxbits/remotekeyboard](https://github.com/onyxbits/remotekeyboard).

| | |
|---|---|
| ![Main screen](main-screen.jpg) | ![Keyboard selection](keyboard-selection.jpg) |

---

## What's new compared to the original

| Feature | [onyxbits original](https://github.com/onyxbits/remotekeyboard) | This fork |
|---|---|---|
| Client protocol | Plain-text Telnet (port 2323) | ✅ HTTPS + browser web client (port 4430) |
| Authentication | Optional password | ✅ Mandatory password enforced on first launch |
| Transport security | Unencrypted | ✅ TLS via Android KeyStore self-signed cert |
| PC client | Any telnet tool | ✅ Zero-install — any modern browser |
| Build system | Ant | Gradle (AGP 8.x) |
| Target SDK | Old | API 34 (Android 14) |
| Min SDK | — | API 21 (Android 5.0) |
| GitHub Actions CI | ❌ | ✅ Builds APK on every push |
| UI theme | Holo (old) | ✅ Material Design 3 (Material You) |
| Launcher icon | Old bitmap | ✅ Adaptive vector icon (keyboard) |

---

## Features

- Type from your PC keyboard into any Android text field over a secure HTTPS web client
- Zero install on the PC — just open `https://<phone-ip>:4430` in any browser
- HTTP on port 4431 auto-redirects to HTTPS, so forgetting `https://` still works
- Mandatory password (enforced on first launch) + session tokens
- Clickable on-screen toolbar in the web UI (Esc, Tab, Enter, Backspace, Delete, arrows, Home/End, PgUp/PgDn, Ctrl+A/C/V/X, F1–F12)
- Text replacement / macro system
- Home screen widget to toggle the server
- F1–F12 key support with configurable quick-launch actions

---

## 📖 User Guide

For installation instructions, setup steps, usage guide, keyboard shortcuts, and troubleshooting, see the **[User Guide](USER_GUIDE.md)**.

---

## Building

### Prerequisites

- **JDK 17** or newer (`java -version`)
- **Android SDK** with:
  - Build Tools 34.0.0
  - Platform `android-34`

#### Install Android SDK command-line tools (if needed)

```bash
# Download from https://developer.android.com/studio#command-tools
# then:
export ANDROID_HOME=~/android-sdk
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### Clone

```bash
git clone https://github.com/caco3/RemoteKeyboard.git
cd RemoteKeyboard
```

### Build debug APK

```bash
export ANDROID_HOME=~/android-sdk   # adjust to your SDK path
./gradlew assembleDebug
```

The APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Build release APK (unsigned)

```bash
./gradlew assembleRelease
```

The APK is written to:

```
app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Local deploy via ADB

With a device connected over USB (or Wi-Fi ADB), build and install in one step:

```bash
# Build + install debug build
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or use the Gradle shortcut which builds and installs in a single task:

```bash
./gradlew installDebug
```

### Check connected devices

```bash
adb devices
```

### Launch the app immediately after install

```bash
adb shell am start -n de.caco3.remotekeyboard/.MainActivity
```

### View live logcat output

```bash
adb logcat -s RemoteKeyboard
```

---

## License

Apache 2.0 — see [LICENSE](LICENSE)

Original work © onyxbits  
Modernisation © Lepidos  
Modernisation © caco3

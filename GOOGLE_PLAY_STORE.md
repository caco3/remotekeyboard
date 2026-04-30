# Google Play Store Publishing Materials

## App Name (30 characters max)

**Remote Keyboard**

*(Alternative: Remote Keyboard — PC to Phone)*

---

## Short Description (80 characters max)

Type on your Android phone from your PC keyboard over Wi-Fi via a secure web page.

---

## Full Description (4,000 characters max)

**Remote Keyboard** lets you type on your Android device using your PC's full-sized keyboard. Perfect for long chat messages, filling out forms, or any situation where tapping on a small screen is frustrating.

**How it works**
The app starts a tiny, secure web server on your phone. Open the web page from your PC browser, enter your password, and every keystroke is instantly forwarded to your Android device. It works in any app — messengers, browsers, email, notes, and more.

**Key features**
• Type from your PC into any Android text field in real time
• Encrypted HTTPS connection — your keystrokes stay private on the local network
• Password protection enforced — no one else on your Wi-Fi can connect
• Text replacement / macro system for frequently used phrases
• Home screen widget to quickly toggle the server
• F1–F12 key support with configurable quick-launch app actions
• Material Design 3 interface with adaptive icon

**Setup in 30 seconds**
1. Install the app and set your password
2. Enable "Remote Keyboard" in your phone's keyboard settings
3. Switch to Remote Keyboard in any text field
4. Open the shown URL in your PC browser and start typing

**Requirements**
• Android 5.0 or newer
• Phone and PC on the same Wi-Fi network
• Any modern web browser (Chrome, Firefox, Edge, Safari)

**Security & Privacy**
Remote Keyboard uses an encrypted HTTPS connection between your PC and phone. A password is mandatory on first launch. No data ever leaves your local network — there are no cloud servers, no accounts, and no tracking.

**Open Source**
Remote Keyboard is open source (Apache 2.0). The source code is available on GitHub. The original project was created by onyxbits; this is a modernised fork with up-to-date Android SDK, Material Design 3, and a secure web-based client.

---

## What's New / Release Notes (v0.2.0 — first Play Store release)

• Secure HTTPS web client (port 4430) — type from any browser, no extra app needed
• HTTP on port 4431 auto-redirects to HTTPS for convenience
• Mandatory password protection enforced on first launch
• Clickable on-screen toolbar for special keys (arrows, F-keys, Ctrl shortcuts, etc.)
• Welcome panel explains the "no local echo" behaviour and lists every shortcut
• Fully localised notifications and instructions (English + German)
• Modernised UI with Material Design 3 and adaptive launcher icon
• Targets Android 14 (API 34); supports Android 5.0+

Full details: see RELEASE_NOTES.md in the GitHub repository.

---

## Category

**Productivity**

*(Secondary: Tools)*

---

## Content Rating

**Everyone**

No violence, no user-generated content shared externally, no sensitive data collection.

---

## Privacy Policy (required for Play Store)

Below is a minimal privacy policy text you can publish as a GitHub Pages page (or any hosted page) and link in the Play Console:

---

### Privacy Policy for Remote Keyboard

**Last updated:** [Date]

**1. No data collection**
Remote Keyboard does not collect, transmit, or share any personal data. The app operates entirely on your local Wi-Fi network.

**2. Local network only**
All communication happens directly between your Android device and your PC on the same local network. There are no external servers, cloud services, or third-party analytics.

**3. Password storage**
Your password is stored locally on your device using Android's standard SharedPreferences. It is never transmitted outside your local network and is only used to authenticate connections from your PC.

**4. Permissions**
The app requests the minimum necessary permissions:
- **BIND_INPUT_METHOD**: to act as a keyboard
- **INTERNET**: to run the local web server
- **WAKE_LOCK**: to keep the screen on while typing
- **ACCESS_WIFI_STATE**: to display your phone's IP address

**5. Contact**
For questions about this privacy policy, please open an issue on our GitHub repository.

---

## Store Listing Assets Checklist

| Asset | Specification | Status |
|---|---|---|
| **App icon** | 512 × 512 px PNG | Reuse `preproduction/ic_launcher.svg` → export |
| **Feature graphic** | 1024 × 500 px JPG/PNG | **Needs creation** — recommend screenshot collage with PC + phone |
| **Phone screenshots** | Up to 8, 16:9 or similar | Need 2–3 new screenshots of main screen + browser client |
| **Tablet screenshots** | Optional, up to 8 | Same as phone if supported |
| **Promo video** | Optional, 30–120 sec | Not required for launch |
| **Wear OS / TV** | Not applicable | — |

---

## Suggested Tags / Keywords (for ASO)

remote keyboard, pc keyboard android, type on phone from computer, wifi keyboard, virtual keyboard, remote input, pc to phone typing, keyboard over wifi, android keyboard pc control, productivity keyboard

---

## Contact & Support Information (Play Console)

- **Website:** https://github.com/caco3/RemoteKeyboard
- **Email:** [your email] *(required for Play Console)*
- **Support:** GitHub Issues page

---

## Notes on Publishing

1. **Signing:** You need a release keystore. The project already supports environment-variable-based signing in `app/build.gradle`. Generate a dedicated Play Store keystore and keep it secure.
2. **AAB vs APK:** Google Play now requires App Bundles (AAB) for new apps. Build with `./gradlew bundleRelease` instead of `assembleRelease`.
3. **API 34 target:** The app targets API 34 (Android 14), which satisfies current Play Store requirements.
4. **App signing by Google Play:** Enable Play App Signing in the console. Upload your AAB; Google will manage the final signing.

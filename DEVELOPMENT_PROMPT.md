# Development Prompt: Replace Telnet Server with HTTPS Web Server

## Goal
Replace the existing unencrypted telnet server (`net.wimpi.telnetd`) with an embedded HTTPS web server that serves a single-page web client. The web client becomes the new PC-side typing interface. Reuse the existing password storage (`pref_password` in `SharedPreferences`). Enforce password setup on first app launch. Update all user-facing documentation.

---

## 1. Remove the Telnet Stack

- **Delete** the entire `src/net/wimpi/telnetd/` package and all its sub-packages (io, io/terminal, io/toolkit, net, shell).
- **Delete** `src/de/caco3/remotekeyboard/TelnetEditorShell.java`.
- **Delete** `src/de/caco3/remotekeyboard/DummyShell.java`.
- **Delete** the asset `assets/telnetd.properties`.
- **Delete** `assets/welcomescreen.txt` (or repurpose its ASCII art for the web client).
- Remove the `uses-library org.apache.http.legacy` entry from `AndroidManifest.xml`.
- Remove `compileOnly 'commons-logging:commons-logging:1.2'` from `app/build.gradle`.
- Update all references:
  - `RemoteKeyboardService.java` currently creates `TelnetD` on `onCreate()` and stops it on `onDestroy()`. Replace this with the new web server lifecycle.
  - Remove all `TelnetEditorShell.self` references in `RemoteKeyboardService.java` and `MainActivity.java`. The "show text to connected client" and "disconnect client" features will be replaced by web client equivalents (or removed if not applicable).

---

## 2. Add an Embedded HTTPS Web Server

- Choose a lightweight, embeddable Java HTTP server compatible with Android API 21+. **NanoHTTPD** (`org.nanohttpd:nanohttpd:2.3.1`) or its SSL fork is the recommended default. Add it to `app/build.gradle` dependencies.
- The server must listen on **port 4430** (configurable via resources/constants). Use a different port than the old 2323 to avoid conflicts and reflect the protocol change.
- **HTTPS only** — HTTP requests must redirect to HTTPS.
- Generate or bundle a self-signed certificate at build time or first run. The certificate can be a static BKS (BouncyCastle Keystore) file in `res/raw/`. The server loads it on startup.
  - Document for users that the browser will show a certificate warning on first visit — this is expected for a local self-signed cert.
- Serve three endpoints:
  - `GET /` — serves the web client HTML/JS/CSS (single-page application, inline everything for zero external dependencies).
  - `POST /api/auth` — accepts JSON `{"password":"..."}`. Compares against `SharedPreferences` key `pref_password` (same key used today by `TelnetEditorShell`). Returns a short-lived session token (e.g. JWT or simple random token) on success, `401` on failure.
  - `POST /api/keystroke` — accepts JSON `{"token":"...", "key":"...", "type":"text|special"}`. Validate token, then translate the key event into the same `TextInputAction` / `CtrlInputAction` pipeline that `TelnetEditorShell` used.
- The web client (`GET /`) must contain:
  - A password input field (shown if a password is set).
  - A large text input area (like a full-screen `<textarea>`) where typing on the PC forwards every keystroke to the phone via `POST /api/keystroke`.
  - Visual feedback: connection status, last-sent character, latency indicator.
  - Handle special keys (Enter, Backspace, Delete, Arrow keys, F1–F12) by mapping them to the existing `Decoder`/`CtrlInputAction` function codes.
- Keep the server lifecycle identical to the old telnet server: start in `RemoteKeyboardService.onCreate()`, stop in `RemoteKeyboardService.onDestroy()`.
- Update the notification text in `RemoteKeyboardService.updateNotification()` to show `https://<IP>:4430` instead of `Port 2323`.
- Update `app_quickinstuctions` string (and any translations) to describe the web client workflow instead of telnet.

---

## 3. Password Enforcement on First Launch

- In `MainActivity.onResume()` (or a new splash/onboarding activity), check `SharedPreferences` for `pref_password`.
- If the password is empty, **block the main UI** and display a non-cancellable dialog/fragment that forces the user to set a password before proceeding.
  - Reuse the existing `EditTextPreference` logic from `SettingsActivity` — the same `pref_password` key.
  - Minimum length: 4 characters.
- Only after a non-empty password is stored should the user reach the main screen.
- The Settings screen must still allow changing the password later, but never to empty (disable clearing).

---

## 4. Reuse Existing Input Pipeline

- The web server handler for `/api/keystroke` must feed keystrokes into the **same** `Decoder` → `TextInputAction` / `CtrlInputAction` → `ActionRunner` pipeline that `TelnetEditorShell.run()` currently uses.
- Do **not** rewrite the keyboard injection logic. Extract the action-running code from `TelnetEditorShell` into a reusable helper (e.g. `KeyboardActionDispatcher`) so both the old shell (if kept temporarily) and the new web handler can call it.
- Reuse `Decoder.java` as-is for mapping raw key strings to printable/function codes.
- Reuse the replacement/macro system (`replacements` HashMap loaded in `RemoteKeyboardService`).
- Reuse the quick-launcher F-key mappings (`CtrlInputAction.PREF_QUICKLAUNCHER.*`).

---

## 5. Web Client Requirements (Served by `GET /`)

- **Zero external dependencies** — inline CSS and JavaScript. No CDN requests.
- **Dark theme by default**, clean mobile-like UI (the user is typing on a PC but the client runs in a browser).
- **Password gate**: if the server responds that a password is required, show a centered login form. On success, hide the form and reveal the typing area.
- **Typing area**: a full-page `<textarea>` (or `contenteditable` div) that captures `keydown`/`keyup`. Each keystroke is sent as a WebSocket message or HTTP POST to `/api/keystroke`.
  - **Recommendation**: use WebSocket for lower latency. NanoWebSocketServer (NanoHTTPD extension) or simple repeated POSTs are both acceptable; document the choice.
- **Special key support**: map browser key events to the same codes the old telnet decoder produced:
  - Arrow keys, Backspace, Delete, Enter, Escape, Tab
  - Ctrl+C / Ctrl+V / Ctrl+X / Ctrl+A (map to copy/paste/select-all actions)
  - F1–F12 (sent as function codes to trigger quick-launch)
- **Connection status bar**: show phone IP, connection state, and a disconnect button.
- **Security note display**: remind the user that the connection is local-network only.

---

## 6. String & Resource Updates

- Update `app/src/main/res/values/strings.xml`:
  - Change `notification_waiting` from "Listening on %1$s Port 2323" to "Listening on https://%1$s:4430".
  - Change `app_quickinstuctions` from telnet instructions to browser instructions.
  - Change `err_noclient` to "No web client connected!" or equivalent.
  - Update `card_how_to_connect` if needed.
- Delete German translations that reference telnet (`app/src/main/res/values-de/strings.xml`) or update them to match.
- Remove `homepage` string if it pointed to the old GitHub help anchor; repurpose to `USER_GUIDE.md` anchor (see README changes below).

---

## 7. AndroidManifest & Permissions

- Add `android.permission.ACCESS_NETWORK_STATE` if needed by the new server.
- Keep `INTERNET`, `WAKE_LOCK`, `ACCESS_WIFI_STATE`, `BIND_INPUT_METHOD`.
- Ensure the manifest does not reference deleted activities or libraries.

---

## 8. Testing Checklist

- [ ] APK builds successfully (`./gradlew assembleDebug`).
- [ ] App launches and **forces password** before showing main screen.
- [ ] Web server starts when the input method service is created.
- [ ] Opening `https://<PHONE_IP>:4430/` in a PC browser loads the client page.
- [ ] Browser shows cert warning → user accepts → login form appears.
- [ ] Correct password → token received → typing area unlocked.
- [ ] Typing in the browser textarea inserts text into an Android text field.
- [ ] Backspace, Enter, Arrow keys, and F1–F12 work as before.
- [ ] Wrong password returns 401 and typing area stays locked.
- [ ] Password can be changed in Settings and the new password is enforced immediately.
- [ ] HTTP request to port 4430 redirects to HTTPS.
- [ ] Notification shows the correct HTTPS URL.
- [ ] Home screen widget still toggles the server on/off (if applicable; if the widget only toggled telnet, adapt it to pause/resume the web server).

---

## Summary of Reused Components

| Component | Reuse Strategy |
|---|---|
| Password storage (`pref_password`) | Read directly from `SharedPreferences`, same key |
| `Decoder.java` | Unchanged; map web key events to its codes |
| `TextInputAction` / `CtrlInputAction` | Unchanged; feed them from the web handler |
| `ActionRunner` | Unchanged; post to service handler |
| Replacement / macro DB | Unchanged; loaded by `RemoteKeyboardService` |
| Quick-launcher F-keys | Unchanged; settings screen untouched |
| Notification manager | Update text only |
| `RemoteKeyboardService` | Replace server lifecycle, keep everything else |

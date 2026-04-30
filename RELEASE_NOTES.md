# Release Notes

## 0.3.0 — IME awareness, UI polish and service refactor

### New
- **IME status card** on the main screen shows which keyboard is currently selected and highlights whether Remote Keyboard is active.
- **Live IME observer** refreshes the status card automatically when the user switches keyboards via the system picker.
- **Web client IME polling** — the browser UI now polls `/api/status` every 3 seconds and shows a warning banner when Remote Keyboard is not the active input method.
- **Color-coded status labels** — green for active, red for a different keyboard selected, improving visual feedback.
- **Favicon** on the web client using the app keyboard logo (inline SVG data URI).

### Changed
- **Web server extracted to `WebServerService`** — runs as a foreground service (`specialUse` type) so it survives when another keyboard is selected. The service is started from `MainActivity` on launch and displays a persistent notification.
- `RemoteKeyboardService` is now purely the input method; all HTTP/HTTPS serving lives in `WebServerService`.
- Updated connection instructions in EN and DE to the correct 6-step order (pick keyboard → open browser → accept cert → enter password → focus text field → type).

### Fixed
- **HTTP → HTTPS redirect** now works on the same port (4430) by peeking the first byte of every TCP connection (`0x16` for TLS handshake) instead of a separate redirect port.
- `Html.fromHtml()` properly renders styled IME status text escaped via XML entities in `strings.xml`.

### Build / CI
- GitHub Actions now names release APKs with the git tag (`RemoteKeyboard-v0.3.0.apk`) or short commit hash (`RemoteKeyboard-a1b2c3d.apk`) instead of a generic name.

---

## 0.2.0 — HTTPS web client (replaces Telnet)

**Breaking change.** The legacy unencrypted Telnet server (port 2323) has been completely removed and replaced by a secure HTTPS web client.

### New
- **HTTPS web server** on port `4430`, backed by a self-signed ECDSA certificate stored in the Android KeyStore.
- **Browser-based client** — no more telnet tool needed. Open `https://<phone-ip>:4430` in Chrome, Firefox, Edge or Safari.
- **Plain-HTTP → HTTPS redirect on the same port (4430)**: the server peeks the first byte of every connection (TLS handshakes start with `0x16`) and either runs the handshake or replies with a `301 Moved Permanently` to the `https://` URL. Forgetting `https://` in the browser still works.
- **Mandatory password** enforced on first launch; cannot be cleared afterwards.
- **Session tokens** issued by `POST /api/auth`; every keystroke is token-validated.
- **On-screen toolbar** in the web UI: `Esc`, `Tab`, `Enter`, `⌫`, `Del`, arrow keys, `Home`/`End`/`PgUp`/`PgDn`, `Ctrl+A/C/V/X`, `F1`–`F12`.
- **Welcome panel** in the web UI explaining that keystrokes appear only on the Android device (no local echo), with a full keyboard-shortcut reference.
- **Help link** in the web UI header pointing to the online User Guide.
- **Buy me a coffee** card on the web UI for optional project support.

### Changed
- Notification now shows `https://<IP>:4430` instead of the old `Port 2323`.
- Main-screen instructions updated (localised EN + DE) and now render with real HTML formatting (bold, line breaks).
- Help menu links to `USER_GUIDE.md` on GitHub.
- German translation (`values-de/strings.xml`) fully updated — no more telnet references.

### Fixed
- TLS handshake (`PR_END_OF_FILE_ERROR`) caused by the Android KeyStore key disallowing Conscrypt's `NONEwithECDSA` signing callback. The key is now generated with both `DIGEST_SHA256` and `DIGEST_NONE`, and a new alias (`remotekeyboard_tls_v2`) forces regeneration of any previously broken key.
- `<br/>` rendering in the instructions card — escaped in `strings.xml` and parsed via `Html.fromHtml()`.

### Removed
- `net.wimpi.telnetd` library and all its sub-packages.
- `TelnetEditorShell`, `DummyShell`.
- `assets/telnetd.properties`, `assets/welcomescreen.txt`.
- `uses-library org.apache.http.legacy` in the manifest and `compileOnly commons-logging` in Gradle.
- Legacy Eclipse/ADT artefacts at the repo root: `/src`, `/res`, `/assets`, `.classpath`, `.project`, `custom_rules.xml`, `project.properties`, `proguard-project.txt`.

### Security
- Local traffic is now encrypted end-to-end between the PC browser and the phone.
- Self-signed certificate generation is deferred to first server start and stored in the hardware-backed Android KeyStore.

---

## 0.1.0 — 2025-10-27

Initial fork and rebrand from `de.onyxbits.remotekeyboard` → `de.caco3.remotekeyboard`. Ant build replaced with Gradle (AGP 8.x), Material 3 theme, adaptive launcher icon, GitHub Actions CI. Underlying Telnet protocol unchanged.

# Amnos Architecture

## Layered design

1. `ui`
   Compose surfaces including the browser shell and Security Cockpit.
2. `ui/screens/browser/BrowserViewModel.kt`
   Orchestrates navigation, address-bar state, policy toggles, and diagnostic exposure.
3. `core/session/SessionManager.kt`
   Owns tab creation, proxy lifecycle, volatile storage, and GHOST wipe behavior.
4. `core/network/NetworkSecurityManager.kt`
   Applies HTTPS-only rules, sanitizes URLs, classifies requests, strips stateful headers, and builds proxied responses.
5. `core/fingerprint`
   Generates deterministic session/tab silos and injects the document-start spoofing script.
6. `core/webview`
   Hosts the hardened `SecureWebView`, the privacy WebView client, and the Chrome client bridge.

## Navigation flow

1. User input enters `BrowserViewModel.navigate`.
2. `NavigationResolver` classifies the input as either direct URL or search using the Amnos Search Dog heuristic.
3. The transformed target is sanitized by `UrlSanitizer` before any `WebView` load.
4. `SessionManager.loadUrl` applies final HTTPS-only normalization and request headers.
5. `PrivacyWebViewClient` commits the address bar only after top-level navigation is visibly committed.
6. Failures are logged to the in-memory diagnostics stream without smearing transient URLs into the address bar.

## Storage and wipe model

- WebView cookies are disabled.
- `Set-Cookie` response headers are stripped before responses reach WebView.
- Volatile downloads are stored under `cacheDir/volatile_downloads`.
- Session startup and teardown both purge cookies, `WebStorage`, form data, auth data, and volatile downloads.
- **Task Removal Cloaking** - Uses `finishAffinity()` and `onTaskRemoved` to purge the app from "Recents".
- The kill switch and background wipe both rebuild the active session ID, tab silos, and trigger a **Cryptographic Kill** of all session-scoped keys.
- **Forensic RAM Scrambling** - Sensative buffers are noise-saturated before zeroing.

## Diagnostics model

- **Security Controller** owns RAM-only request logs, active connections, status banners, and internal logs.
- **Threat Detection HUD** - Real-time Accessibility Service monitoring and Sandbox Mode (Paranoid/Balanced) enforcement.
- `AmnosLog` routes subsystem events into `SecurityController.logInternal` when a session exists and falls back safely otherwise.
- Navigation trace tags follow the sequence `[Nav:Navigate]`, `[Nav:Transform]`, `[Nav:Sanitize]`, `[Nav:Load]`, `[Nav:Commit]`, and `[Nav:Failure]`.

## Release safeguards

- `android:usesCleartextTraffic="false"`
- release signing no longer falls back to the debug keystore
- remote debugging and relaxed diagnostics only work in debug builds
- user CAs are accepted only inside `<debug-overrides>`

## Honest boundary

Amnos hardens Android WebView aggressively, but the browser engine, OS networking stack, and WebView implementation still set the outer security boundary. It should be treated as a privacy-hardened browser, not a full anonymity platform.

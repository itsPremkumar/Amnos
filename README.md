# Amnos

**[Project Status]** Active | **[Type]** Hardened Android Browser | **[Focus]** Privacy, Security, Local-Only

## 🔍 SEO & Project Metadata

| Attribute | Details |
| :--- | :--- |
| **Official Name** | Amnos |
| **Category** | Privacy-focused Web Browser |
| **Platform** | Android (API 34+) |
| **Key Features** | Aggressive Tracker Blocking, WebRTC Isolation, Fingerprint Mitigation |
| **Tech Stack** | Kotlin, WebView, JavaScript Injection |

---

Amnos is a privacy-focused Android browser built on top of `WebView` and hardened as far as the platform realistically allows. It is designed for local-only browsing sessions, zero persistent footprint, aggressive tracker blocking, and reduced browser fingerprint entropy.

Amnos does **not** provide Tor-class anonymity. It is a hardened WebView browser, not an anonymity network client.

## Current privacy model

Amnos now includes:

- HTTPS-only navigation and cleartext denial
- loopback proxy support for broader DoH coverage via `ProxyController` where supported
- DoH-backed request proxying for intercepted traffic
- WebRTC shutdown via document-start API replacement and fake peer connection objects
- WebSocket blocking by default, with JS telemetry for attempted socket use
- document-start fingerprint overrides for UA, screen, timezone, language, hardware, fonts, canvas, audio, and timing
- RAM-only request inspection and active-connection visibility
- strict first-party isolation that can rebuild the tab silo when top-level site identity changes
- disabled cookies, storage, service workers, file chooser access, and persistent downloads
- volatile internal-only download storage that is wiped with the session

## Important limitation

Amnos provides strong privacy protections, but **not full network anonymity**.

Known WebView constraints include:

- Chromium internals are still underneath the browser engine
- not every network path is as controllable as in a custom browser engine
- encrypted WebSocket payloads inside HTTPS tunnels cannot be deeply inspected without TLS interception
- real anonymity against network observers still requires a separate anonymity layer such as Tor or a trusted VPN

## Architecture highlights

- [FingerprintManager.kt](app/src/main/kotlin/com/privacy/browser/core/fingerprint/FingerprintManager.kt)
  deterministic per-session and per-tab identity generation
- [FingerprintObfuscator.js](app/src/main/assets/FingerprintObfuscator.js)
  document-start API overrides, timing mitigation, WebRTC shutdown, WebSocket telemetry
- [NetworkSecurityManager.kt](app/src/main/kotlin/com/privacy/browser/core/network/NetworkSecurityManager.kt)
  HTTPS-only policy, request classification, header hardening, site-key logic
- [LoopbackProxyServer.kt](app/src/main/kotlin/com/privacy/browser/core/network/LoopbackProxyServer.kt)
  local CONNECT proxy for broader DoH-backed resolution
- [SessionManager.kt](app/src/main/kotlin/com/privacy/browser/core/session/SessionManager.kt)
  policy ownership, loopback proxy activation, tab recreation, wipe behavior
- [SecurityDashboard.kt](app/src/main/kotlin/com/privacy/browser/ui/components/SecurityDashboard.kt)
  live status, counters, toggles, fingerprint mode, and visibility into active connections

More detail lives in:

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [SECURITY_AUDIT.md](SECURITY_AUDIT.md)
- [VALIDATION.md](VALIDATION.md)

## Build

Requirements:

- Android Studio Giraffe or newer
- JDK 17
- Android SDK for API 34

Commands:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Validation status

Validated from this machine on April 10, 2026:

- `./gradlew testDebugUnitTest` passed

Not completed from this machine:

- physical device validation
- emulator-based leak testing
- on-device WebRTC, DNS, and compatibility sweeps

See [VALIDATION.md](VALIDATION.md) for the exact manual test plan and current gaps.

## Security reporting

See [SECURITY.md](SECURITY.md).

# Amnos

Amnos is a privacy-first hardened Android browser built on `WebView` for local-only sessions and zero durable browsing state. It prioritizes privacy over compatibility: no persistent cookies, no cache retention, aggressive tracker blocking, HTTPS-only upgrades, strict first-party isolation, and deterministic fingerprint silos scoped to the active session.

Amnos is not Tor, not a VPN, and not a custom browser engine. It reduces exposure inside Android WebView constraints; it does not provide network-layer anonymity against every observer.

## Production posture

- Cleartext traffic disabled in the manifest and network security config
- User-installed CAs trusted only through `debug-overrides`
- Release builds no longer fall back to debug signing
- Release APK generation is stable by default on normal developer machines; optional shrinking can be enabled with `-Pamnos.release.minify=true` on higher-memory builders
- Remote WebView debugging and relaxed diagnostics locked behind debug-build guards
- Navigation now routes through a tested resolver that applies the Search Dog heuristic before load
- Address bar updates commit on successful top-level navigation instead of transient load state
- `Set-Cookie` headers are stripped and WebView cookies are disabled
- Session startup and teardown both purge WebView storage to limit crash-leftover persistence
- Diagnostic logging is centralized through `AmnosLog` and surfaced in the in-app dashboard

## Core modules

- `core/network`
  Request classification, URL sanitization, navigation resolution, HTTPS-only enforcement, and proxy-backed fetch logic.
- `core/session`
  Tab lifecycle, GHOST wipe flow, volatile downloads, internal diagnostics, and loopback proxy orchestration.
- `core/webview`
  Hardened `WebView` settings plus the privacy-aware WebView and Chrome clients.
- `core/fingerprint`
  Deterministic device-profile generation and document-start API spoofing.
- `ui`
  Compose browser shell plus the Security Cockpit / diagnostics surface.

## Validation commands

Windows:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleRelease
```

Project verification script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify_build.ps1
```

## Documentation map

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [VALIDATION.md](VALIDATION.md)
- [docs/PRODUCTION_READINESS.md](docs/PRODUCTION_READINESS.md)
- [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md)

## Honest limits

- Some sites will break by design because Amnos disables cookies, strips tracking state, and blocks invasive browser capabilities.
- Real leak validation still requires device or emulator testing against live WebRTC, DNS, and fingerprint test sites.
- WebView and Android system behavior still define the ultimate trust boundary.

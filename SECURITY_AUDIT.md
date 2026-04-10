# Amnos Security Audit

Audit date: April 10, 2026
Project: Amnos Android privacy browser
Scope: `app/` source, manifest, Gradle/build wiring, WebView/network/session/privacy layers

## Executive summary

The original codebase had the right privacy-oriented package layout, but several protections were incomplete or inaccurately represented. The largest gaps were that DNS-over-HTTPS was not connected to WebView traffic, Referer handling did not truly strip headers, WebRTC protection relied mostly on permission denial, cleartext traffic was not denied at the manifest/network-security layer, and the in-app kill switch attempted to recreate a session after immediately killing the process.

The codebase has now been refactored into a stricter policy-driven browser core with:

- HTTPS-only enforcement at both manifest and request-filter layers
- document-start fingerprint/API override injection
- disabled cookies, storage, service workers, and `X-Requested-With`
- proxied GET/HEAD request fetching through a DoH-backed OkHttp client
- third-party/tracker/script blocking with a live RAM-only request inspector
- per-tab identity profiles with reset-on-refresh support
- background/session wipe handling that can either purge in-process or terminate the app
- regenerated Gradle wrapper and passing JVM tests

## High-risk findings in the original code

1. DoH was implemented but not used by WebView.
   `DnsManager` built an OkHttp DoH client, but WebView traffic still used Chromium's default network stack.

2. Referer stripping was incorrect.
   `NetworkSecurityManager` replaced cross-site Referer values with `https://<domain>/` instead of removing them.

3. WebRTC protection was overstated.
   Permission denial alone does not disable `RTCPeerConnection` or WebRTC fingerprint surfaces.

4. Request interception was mostly declarative, not enforceable.
   Tracker blocking and HTTPS-only logic blocked some URLs, but did not give Amnos control over most request headers, DNS resolution, or response policies.

5. WebView fingerprint leakage remained exposed.
   `X-Requested-With`, service workers, local/session storage surfaces, and several Web APIs were still available.

6. Kill-switch flow was broken.
   `BrowserViewModel.killSwitch()` called `killAll()` and then attempted to reinitialize a session, but `killAll()` killed the process first.

7. Manifest/network configuration was incomplete.
   Cleartext traffic was not denied by manifest or network security config.

8. Repository build hygiene was incomplete.
   `proguard-rules.pro` and `gradle-wrapper.jar` were missing, so release configuration and wrapper-driven builds were not reliable.

## Implemented remediations

### Manifest and platform

- Added `android:usesCleartextTraffic="false"` in [AndroidManifest.xml](/C:/one/browser/app/src/main/AndroidManifest.xml)
- Added [network_security_config.xml](/C:/one/browser/app/src/main/res/xml/network_security_config.xml) to deny cleartext traffic
- Kept `FLAG_SECURE` and `allowBackup="false"`
- Regenerated the Gradle wrapper so `./gradlew` works again

### WebView hardening

- Centralized runtime policy in [PrivacyPolicy.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/security/PrivacyPolicy.kt)
- Hardened [SecureWebView.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/webview/SecureWebView.kt) with:
  - disabled cookies and third-party cookies
  - disabled DOM storage/database/file/content access
  - disabled `X-Requested-With` origin allowlist
  - disabled or blocked service workers
  - document-start script installation
- Extended [PrivacyWebChromeClient.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/webview/PrivacyWebChromeClient.kt) to deny permissions, geolocation, popups, and file chooser access

### Network and request filtering

- Replaced best-effort logic in [NetworkSecurityManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/NetworkSecurityManager.kt) with:
  - navigation URL upgrade/sanitization
  - per-request classification
  - third-party blocking
  - third-party script blocking
  - HTTPS-only blocking
  - local network/IP literal blocking
  - proxied GET/HEAD fetches through a DoH-backed OkHttp client
  - response hardening headers including `Referrer-Policy`, `Permissions-Policy`, `Cache-Control`, and CSP in restricted mode
- Reworked [DnsManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/DnsManager.kt) to expose IPv4-filtered DoH lookups
- Rewrote [UrlSanitizer.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/UrlSanitizer.kt) using OkHttp `HttpUrl` so it is deterministic and unit-testable

### Fingerprinting and Web API protection

- Rebuilt [FingerprintManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/fingerprint/FingerprintManager.kt) for deterministic per-session/per-tab Android-like device profiles
- Reworked [ScriptInjector.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/fingerprint/ScriptInjector.kt) to carry both profile and policy flags
- Replaced [FingerprintObfuscator.js](/C:/one/browser/app/src/main/assets/FingerprintObfuscator.js) with document-start protection for:
  - UA/platform/language/timezone/screen spoofing
  - storage API suppression
  - WebRTC blocking
  - WebSocket blocking
  - service worker blocking
  - sensors, camera, mic, location, battery, clipboard restrictions
  - canvas/audio noise
  - font restriction
  - prefetch/preconnect suppression
  - eval/function/WebAssembly blocking in restricted mode

### Session and storage isolation

- Refactored [SessionManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/SessionManager.kt) to:
  - own the active privacy policy
  - generate per-tab identities
  - support reset-on-refresh by recreating the tab
  - separate purge-only wipes from process termination
  - maintain a session timeout callback
- Expanded [StorageController.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/StorageController.kt) with actual volatile download storage and wipe support
- Updated [ClipboardSentinel.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/security/ClipboardSentinel.kt) to use `ClipData`
- Expanded [SecurityController.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/SecurityController.kt) into a richer RAM-only request log with blocked/passthrough/allowed dispositions

### UI and transparency

- Updated [BrowserViewModel.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/ui/screens/browser/BrowserViewModel.kt) so the UI controls real policy state
- Updated [SecurityDashboard.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/ui/components/SecurityDashboard.kt) with:
  - JS mode selection
  - HTTPS-only toggle
  - third-party blocking toggle
  - inline script shield toggle
  - WebSocket shield toggle
  - WebGL toggle
  - identity-reset-on-refresh toggle
  - richer request inspector metadata

## Validation completed

Executed locally on April 10, 2026:

- `./gradlew testDebugUnitTest`

Result:

- build passed
- JVM tests passed

Added tests:

- [UrlSanitizerTest.kt](/C:/one/browser/app/src/test/kotlin/com/privacy/browser/core/network/UrlSanitizerTest.kt)
- [FingerprintManagerTest.kt](/C:/one/browser/app/src/test/kotlin/com/privacy/browser/core/fingerprint/FingerprintManagerTest.kt)

## Residual limitations and honest scope boundaries

These are important Android WebView limitations, not hidden failures:

1. DoH is not universally enforceable for every Chromium-internal network path.
   Amnos now forces DoH for proxied GET/HEAD requests and ephemeral downloads. Non-proxied methods such as POST still rely on WebView's native stack after policy checks.

2. Per-tab isolation is policy-based rather than process-isolated.
   Cookies, DOM storage, service workers, and persistent storage are disabled, but separate renderer/network processes per tab are not something standard Android WebView exposes to apps.

3. Restricted-mode CSP and third-party blocking will break some sites by design.
   This is an intentional privacy/security tradeoff.

4. Full on-device leak validation was not executed in this environment.
   No live device/emulator tests against real websites, WebRTC leak pages, or persistence-forensics checks were run here.

5. Some protections are best-effort JavaScript/API shims.
   They raise the cost of fingerprinting and API abuse, but WebView is still Chromium underneath and cannot match Tor Browser's anti-fingerprinting guarantees.

## Recommended next validation steps

- Run instrumented tests on a physical Android 13/14 device
- Validate WebRTC leak pages with JS `FULL`, `RESTRICTED`, and `DISABLED` modes
- Verify no cookies/storage persist after:
  - tab close
  - refresh identity reset
  - background/terminate
- Test compatibility on:
  - simple static sites
  - script-heavy SPAs
  - login flows using POST
  - pages with service workers
- Add instrumentation around third-party blocking and request rewriting

## Overall status

Amnos is materially closer to a production-grade privacy-first WebView browser after this refactor, but it should be described honestly as a hardened local privacy browser with known Android WebView platform limits, not as a Tor-class anonymity browser.

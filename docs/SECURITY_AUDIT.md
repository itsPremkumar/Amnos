# Amnos Security Audit

Audit date: April 10, 2026
Project: Amnos Android privacy browser
Scope: Android source, WebView hardening, session lifecycle, fingerprinting defenses, local transport/privacy controls, validation artifacts

## Executive summary

Amnos now goes beyond a standard hardened WebView browser by combining:

- manifest-level cleartext denial
- request filtering and tracker blocking
- loopback proxy transport hardening with DoH-backed DNS resolution
- document-start WebRTC/WebSocket suppression and telemetry
- normalized fingerprint protection levels
- strict first-party isolation for top-level site transitions
- internal-only ephemeral downloads and wipe-on-session-end behavior

The browser is materially stronger than the earlier baseline, but it still remains bound by Android WebView platform limits. It should be presented as a hardened privacy browser, not a full anonymity browser.

## Major improvements in this round

### 1. WebRTC leak mitigation

Implemented in [FingerprintObfuscator.js](app/src/main/assets/FingerprintObfuscator.js):

- `RTCPeerConnection` replaced with a fake implementation when blocking is enabled
- `RTCSessionDescription` and `RTCIceCandidate` replaced with inert shims
- ICE gathering short-circuited
- `RTCDataChannel` creation blocked
- `getUserMedia()` remains blocked
- WebRTC attempts are reported to the native security controller through the WebMessage bridge

Impact:

- page JavaScript no longer gets a working WebRTC peer connection object
- STUN candidate gathering is prevented at the JavaScript API layer
- real on-device leak validation is still required to confirm Chromium does not emit any network activity outside the replaced API path

### 2. Stronger DNS privacy

Implemented in:

- [DnsManager.kt](app/src/main/kotlin/com.amnos.browser/core/network/DnsManager.kt)
- [LoopbackProxyServer.kt](app/src/main/kotlin/com.amnos.browser/core/network/LoopbackProxyServer.kt)
- [SessionManager.kt](app/src/main/kotlin/com.amnos.browser/core/session/SessionManager.kt)

What changed:

- a local loopback CONNECT proxy now resolves upstream hosts through DoH before opening tunnels
- `ProxyController` is used when supported by the installed WebView
- active proxy status and DoH coverage are surfaced to the dashboard
- IPv6 fallback remains disabled in the DoH resolver path

Impact:

- non-intercepted WebView traffic has a stronger chance of staying within the app-controlled DoH path
- fallback to system DNS is reduced when proxy override is active

Remaining limit:

- WebView proxy override support depends on the WebView implementation and platform support
- internal Chromium behaviors may still escape full proxy control in edge cases

### 3. WebSocket and background channel control

Implemented in:

- [FingerprintObfuscator.js](app/src/main/assets/FingerprintObfuscator.js)
- [SecurityController.kt](app/src/main/kotlin/com.amnos.browser/core/session/SecurityController.kt)

What changed:

- WebSocket is wrapped at document start
- blocked attempts are logged in RAM only
- allowed connections can emit open/close/error telemetry
- the dashboard now shows WebSocket status and event counts

Remaining limit:

- encrypted `wss://` traffic inside a CONNECT tunnel cannot be deep-inspected without TLS interception
- Amnos therefore blocks at the JavaScript API boundary instead of trying to MITM traffic

### 4. Advanced fingerprint resistance

Implemented in:

- [FingerprintManager.kt](app/src/main/kotlin/com.amnos.browser/core/fingerprint/FingerprintManager.kt)
- [ScriptInjector.kt](app/src/main/kotlin/com.amnos.browser/core/fingerprint/ScriptInjector.kt)
- [FingerprintObfuscator.js](app/src/main/assets/FingerprintObfuscator.js)

What changed:

- added fingerprint protection levels: `BALANCED` and `STRICT`
- strict mode normalizes timezone and hardware values more aggressively
- `performance.now()`, `Date.now()`, and `requestAnimationFrame()` are quantized and jittered
- `measureText()` is rounded in strict mode
- font detection is further reduced by disabling `FontFace` in strict mode and forcing system-style fonts

### 5. First-party isolation

Implemented in:

- [NetworkSecurityManager.kt](app/src/main/kotlin/com.amnos.browser/core/network/NetworkSecurityManager.kt)
- [SessionManager.kt](app/src/main/kotlin/com.amnos.browser/core/session/SessionManager.kt)
- [BrowserViewModel.kt](app/src/main/kotlin/com.amnos.browser/ui/screens/browser/BrowserViewModel.kt)

What changed:

- site keys are derived from top-level URLs
- when strict first-party isolation is enabled, a top-level cross-site navigation recreates the tab silo
- current top-level site identity is tracked on the tab instance

Impact:

- Amnos now reduces cross-site continuity further than “disable storage” alone

### 6. Download and file leak protection

Implemented in [StorageController.kt](app/src/main/kotlin/com.amnos.browser/core/session/StorageController.kt):

- downloads stay in the app’s internal cache area
- external app opening is not triggered by the browser
- volatile downloads are wiped on session destruction

### 7. Dashboard and transparency

Updated [SecurityDashboard.kt](app/src/main/kotlin/com.amnos.browser/ui/components/SecurityDashboard.kt) to show:

- proxy status
- DoH status
- WebRTC status and event count
- WebSocket status and event count
- active connection count
- fingerprint protection level
- explicit warning that Amnos is not a full anonymity browser

## Current residual limitations

1. Full anonymity is still out of scope.
   Amnos does not hide the user’s public IP the way Tor Browser does.

2. Loopback proxy control is bounded by WebView support.
   If `ProxyController` is unsupported or partially honored by the installed WebView, some internal Chromium paths may still avoid the proxy.

3. WebRTC shutdown is strongest at the JavaScript boundary.
   The fake peer-connection layer is intentionally aggressive, but final confirmation must come from device-side leak testing.

4. Deep encrypted tunnel inspection is intentionally not performed.
   Amnos does not MITM TLS traffic, so `wss://` inspection is limited to API-level controls and telemetry.

5. First-party isolation is site-key based.
   It meaningfully improves separation, but it is not equivalent to Tor Browser’s origin isolation model.

## Validation completed from this machine

### Automated tests 
Executed successfully on April 10, 2026:

- `./gradlew testDebugUnitTest`

Unit coverage now includes:

- [FingerprintManagerTest.kt](app/src/test/kotlin/com.amnos.browser/core/fingerprint/FingerprintManagerTest.kt)
- [UrlSanitizerTest.kt](app/src/test/kotlin/com.amnos.browser/core/network/UrlSanitizerTest.kt)
- [NetworkSecurityManagerTest.kt](app/src/test/kotlin/com.amnos.browser/core/network/NetworkSecurityManagerTest.kt)

## Validation not completed from this machine

Not completed because no Android device or emulator was attached:

- WebRTC leak-site verification
- DNS leak-site verification
- live fingerprint comparison across sessions on real devices
- login-heavy/media-heavy/manual compatibility sweeps

See [VALIDATION.md](VALIDATION.md) for the exact manual and adb-based validation plan.

## Conclusion

Amnos is now a more serious privacy-focused Android WebView browser with a stronger transport story, better first-party isolation, and more transparent runtime status. The project should still be documented and marketed honestly as a hardened WebView browser with explicit WebView-era limitations.

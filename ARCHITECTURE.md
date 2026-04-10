# Amnos Architecture

## Core layout

- `core/fingerprint`
  - [FingerprintManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/fingerprint/FingerprintManager.kt)
  - [ScriptInjector.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/fingerprint/ScriptInjector.kt)
  - [FingerprintObfuscator.js](/C:/one/browser/app/src/main/assets/FingerprintObfuscator.js)
- `core/network`
  - [DnsManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/DnsManager.kt)
  - [NetworkSecurityManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/NetworkSecurityManager.kt)
  - [UrlSanitizer.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/UrlSanitizer.kt)
  - [RequestDecision.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/network/RequestDecision.kt)
- `core/webview`
  - [SecureWebView.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/webview/SecureWebView.kt)
  - [PrivacyWebViewClient.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/webview/PrivacyWebViewClient.kt)
  - [PrivacyWebChromeClient.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/webview/PrivacyWebChromeClient.kt)
- `core/session`
  - [SessionManager.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/SessionManager.kt)
  - [SecurityController.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/SecurityController.kt)
  - [StorageController.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/StorageController.kt)
  - [TabInstance.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/session/TabInstance.kt)
- `core/security`
  - [PrivacyPolicy.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/security/PrivacyPolicy.kt)
  - [PermissionSentinel.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/security/PermissionSentinel.kt)
  - [ClipboardSentinel.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/security/ClipboardSentinel.kt)
- `core/adblock`
  - [AdBlocker.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/core/adblock/AdBlocker.kt)
- `ui`
  - [BrowserViewModel.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/ui/screens/browser/BrowserViewModel.kt)
  - [SecurityDashboard.kt](/C:/one/browser/app/src/main/kotlin/com/privacy/browser/ui/components/SecurityDashboard.kt)
  - Compose screens under `ui/screens/browser`

## Request flow

1. User navigation enters `BrowserViewModel`.
2. `SessionManager` sanitizes the URL and loads it into the active `SecureWebView`.
3. `PrivacyWebViewClient` evaluates each request through `NetworkSecurityManager`.
4. Requests are:
   - blocked immediately
   - proxied through the DoH-backed OkHttp client for GET/HEAD
   - or passed through when WebView-native handling is still required
5. `SecurityController` records the event in a RAM-only inspector log.
6. Document-start injection applies the tab-specific fingerprint and API overrides.

## Session model

- One app session has a session UUID.
- Each tab gets its own tab UUID and device profile.
- Refresh can recreate the current tab to rotate identity.
- Backgrounding or timeout can purge the session.
- Cookies, DOM storage, service workers, and downloads are treated as volatile.

## Threat-model notes

- This architecture is built to minimize local traces, block trackers, and reduce browser fingerprint entropy.
- It does not provide network anonymity comparable to Tor.
- Android WebView imposes hard limits on per-tab process isolation and universal transport interception.

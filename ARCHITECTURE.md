# Amnos Architecture

## Core modules

- `core/fingerprint`
  - [FingerprintManager.kt](app/src/main/kotlin/com/privacy/browser/core/fingerprint/FingerprintManager.kt)
  - [ScriptInjector.kt](app/src/main/kotlin/com/privacy/browser/core/fingerprint/ScriptInjector.kt)
  - [FingerprintObfuscator.js](app/src/main/assets/FingerprintObfuscator.js)
- `core/network`
  - [DnsManager.kt](app/src/main/kotlin/com/privacy/browser/core/network/DnsManager.kt)
  - [NetworkSecurityManager.kt](app/src/main/kotlin/com/privacy/browser/core/network/NetworkSecurityManager.kt)
  - [LoopbackProxyServer.kt](app/src/main/kotlin/com/privacy/browser/core/network/LoopbackProxyServer.kt)
  - [UrlSanitizer.kt](app/src/main/kotlin/com/privacy/browser/core/network/UrlSanitizer.kt)
  - [RequestDecision.kt](app/src/main/kotlin/com/privacy/browser/core/network/RequestDecision.kt)
- `core/webview`
  - [SecureWebView.kt](app/src/main/kotlin/com/privacy/browser/core/webview/SecureWebView.kt)
  - [PrivacyWebViewClient.kt](app/src/main/kotlin/com/privacy/browser/core/webview/PrivacyWebViewClient.kt)
  - [PrivacyWebChromeClient.kt](app/src/main/kotlin/com/privacy/browser/core/webview/PrivacyWebChromeClient.kt)
- `core/session`
  - [SessionManager.kt](app/src/main/kotlin/com/privacy/browser/core/session/SessionManager.kt)
  - [SecurityController.kt](app/src/main/kotlin/com/privacy/browser/core/session/SecurityController.kt)
  - [StorageController.kt](app/src/main/kotlin/com/privacy/browser/core/session/StorageController.kt)
  - [TabInstance.kt](app/src/main/kotlin/com/privacy/browser/core/session/TabInstance.kt)
- `core/security`
  - [PrivacyPolicy.kt](app/src/main/kotlin/com/privacy/browser/core/security/PrivacyPolicy.kt)
  - [PermissionSentinel.kt](app/src/main/kotlin/com/privacy/browser/core/security/PermissionSentinel.kt)
  - [ClipboardSentinel.kt](app/src/main/kotlin/com/privacy/browser/core/security/ClipboardSentinel.kt)
- `core/adblock`
  - [AdBlocker.kt](app/src/main/kotlin/com/privacy/browser/core/adblock/AdBlocker.kt)

## Runtime flow

1. `MainActivity` creates a fresh WebView data-directory suffix and instantiates `SessionManager`.
2. `SessionManager` applies the active `PrivacyPolicy`, starts the loopback proxy if supported, and creates the current tab.
3. `SecureWebView` hardens settings, installs the document-start script, and registers the WebMessage telemetry bridge.
4. `PrivacyWebViewClient` filters navigations and request loads through `NetworkSecurityManager`.
5. `LoopbackProxyServer` handles CONNECT tunnels for broader DoH-backed hostname resolution when proxy override is active.
6. `SecurityController` tracks RAM-only request events, active connections, WebRTC/WebSocket state, and dashboard status strings.

## Privacy policy model

[PrivacyPolicy.kt](app/src/main/kotlin/com/privacy/browser/core/security/PrivacyPolicy.kt) is the central switchboard for:

- JavaScript mode
- WebGL mode
- fingerprint protection level
- WebRTC/WebSocket blocking
- first-party isolation
- loopback proxy usage
- timing and script-hardening behavior

## Transport model

Amnos now has two network-control layers:

1. Intercepted request proxying in `NetworkSecurityManager`
   Used for intercepted GET/HEAD traffic where the app can return its own `WebResourceResponse`.

2. Loopback CONNECT proxy in `LoopbackProxyServer`
   Used with `ProxyController` to extend DoH-backed resolution to more of WebView’s native traffic.

This is the strongest practical transport model available without building a custom browser engine or TLS-intercepting proxy.

## Fingerprinting model

The fingerprint layer combines:

- deterministic Android-like device profiles
- strict and balanced protection levels
- document-start JS overrides
- canvas and audio perturbation
- font restriction
- timing quantization and jitter

## Isolation model

- cookies and persistent storage are disabled globally
- each tab gets its own identity profile
- refresh can rotate identity
- strict first-party isolation recreates the tab silo on cross-site top-level navigation
- session teardown wipes clipboard, downloads, logs, WebView storage, and WebView databases

## Honest boundary

Amnos is optimized for privacy within Android WebView. It is not a substitute for Tor Browser or a full custom browser engine.

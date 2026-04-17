# Amnos v3.1 Architecture

Amnos is organized as a four-layer privacy container. Each layer is meant to degrade independently so that a single failure does not collapse the full session boundary.

## 1. Network Layer

This layer sanitizes navigation, applies request policy, and proxies eligible traffic through the loopback stack.

- Entry points: `NetworkSecurityManager`, `SecurityRuleEngine`, `NetworkFetcher`, `LoopbackProxyServer`
- Responsibilities: URL normalization, HTTPS upgrade, request classification, tracker filtering, response header hardening, domain allowlisting in strict modes
- Stability notes: proxy shutdown must never crash active sessions; request fallbacks should fail closed for dangerous schemes and fail soft for transient transport errors

## 2. RAM Layer

This layer keeps volatile session state in memory and coordinates teardown when tabs or whole sessions are destroyed.

- Entry points: `SessionManager`, `SuperWipeEngine`, `SecureWebView`, `ClipboardVault`
- Responsibilities: tab lifecycle, ephemeral key rotation, WebView teardown, in-memory clipboard handling, session timeout and wipe orchestration
- Stability notes: wipes are main-thread coordinated and debounced; WebView teardown is idempotent and contains failing cleanup steps instead of propagating them

## 3. Stealth Layer

This layer controls how the app shell presents itself and how UI state transitions avoid exposing stale browser state.

- Entry points: `MainActivity`, `BrowserViewModel`, `BrowserScreen`, `CamouflageManager`
- Responsibilities: screen routing, decoy visibility, launcher alias switching, checklist and browsing state transitions
- Stability notes: UI state changes should flow through the ViewModel so repeated transitions do not leave stale tab or address-bar state behind

## 4. OS Layer

This layer integrates with Android lifecycle, process, and platform surfaces where privacy regressions usually show up first.

- Entry points: `MainActivity`, `ClipboardSentinel`, `GhostService`, `PrivacyWebViewClient`
- Responsibilities: screenshot blocking, memory-pressure handling, clipboard suppression, render-process crash containment, crash-handler cleanup
- Stability notes: render-process exits must be handled without app crashes, and clipboard operations should be skipped when the process is not foregrounded

## Typical Session Flow

1. `MainActivity` sets the WebView data suffix before session services initialize.
2. `SessionManager` creates a `SecureWebView` and applies runtime policy plus the injected compatibility script.
3. `PrivacyWebViewClient` and `PrivacyWebChromeClient` mediate navigation, progress, permission, and render-process events.
4. `BrowserViewModel` owns view-state transitions for home, checklist, and browsing surfaces.
5. `SuperWipeEngine` tears down tabs, storage, keys, and proxy state when a session is reset.

## Regression Targets

- WebView teardown must remain safe when the view is still attached.
- Repeated `Privacy Checklist -> Home -> Browsing` transitions must not corrupt UI state.
- Media compatibility exceptions for trusted player hosts must stay narrow and documented.
- Config values in `.env`, `.env.example`, `app/build.gradle`, and `PrivacyPolicy.kt` must evolve together.

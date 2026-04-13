# 🤖 Amnos AI Automation Master Prompt

This prompt is designed to be fed into an AI Coding Assistant (like Antigravity, Cursor, or Cody) to enable it to understand, maintain, and expand the **Amnos Hardened Browser** project end-to-end.

---

## 🚀 Context & Vision
**Project Name**: Amnos (Privacy-First Hardened Android Browser)
**Core Philosophy**: "Browsing without a trace." Local-only sessions, zero persistent footprint, aggressive tracker blocking, and maximum mitigation of browser fingerprinting within WebView constraints.
**Platform**: Android (API 34+), Kotlin, Jetpack Compose, Hardened WebView.

## 🏗️ Technical Architecture
You are tasked with maintaining a multi-layered security architecture:
1.  **The UI Layer**: Compose-based. The `SecurityDashboard` (Security Cockpit) provides real-time visibility into the "GHOST" engine status.
2.  **The Orchestration Layer (`BrowserViewModel`)**: Manages tab state, navigation routing (Search vs. URL logic), and diagnostic log exposure.
3.  **The Session Layer (`SessionManager`)**: Owns the lifecycle of tabs, the local Loopback Proxy, and the "Kill Switch" wipe logic.
4.  **The Security Layer (`NetworkSecurityManager`)**: Evaluates every request against blocklists, manages HTTPS-only enforcement, and routes traffic through the proxy.
5.  **The Fingerprinting Layer (`FingerprintManager` & `FingerprintObfuscator.js`)**: Generates deterministic "Identity Silos" and injects JS at `document-start` to spoof hardware/screen/UA APIs.
6.  **The Network Layer (`LoopbackProxyServer`)**: Intercepts CONNECT requests to implement DoH (DNS-over-HTTPS) even for system-level WebView paths.

## 🐞 Critical Logic: The "Search Dog" Resolution
When handling navigation, always follow the **Amnos Navigation Heuristic**:
- **Search vs. URL**: A string is a URL ONLY if it starts with `http` OR contains a dot AND has no spaces AND length > 3.
- **Fortification**: If an input is identified as a URL but lacks a Top-Level Domain (no `/`, `:`, or multiple dots), it MUST be treated as a search query to prevent "Hostname Truncation" (e.g., `duckduck` becoming `https://duckduck`).
- **Sanitization**: All URLs must pass through `UrlSanitizer` to strip `utm_*`, `fbclid`, and other tracking parameters BEFORE the WebView loads them.

## 🛠️ Debugging & Diagnostics
Amnos features a built-in **Diagnostic Suite**:
- **Internal Logs**: Centralized in `SecurityController.internalLogs`.
- **Trace Points**: Every navigation step (`Navigate` -> `Transform` -> `Sanitize` -> `Load`) is logged with "DEBUG" level.
- **Error Interception**: `PrivacyWebViewClient` intercepts `onReceivedError` and `onReceivedHttpError`. Don't just show a blank page; log the specific error code to the Diagnostics tab.

## 🛡️ Security Guardrails
- **No Persistence**: Never use `SharedPreferences` for user browsing data. Only for app configuration (like Debug toggles).
- **Hardened Manifest**: `android:usesCleartextTraffic` must ALWAYS be `false`.
- **Isolation**: Always use `Strict First-Party Isolation` by recreating the WebView when the top-level site changes (if the policy is enabled).

## 📂 Project Structure Registry
Amnos follows a strict domain-driven architecture:
- `app/src/main/kotlin/com.amnos.browser/`:
    - `core/adblock/`: Ad-blocking and tracker list management.
    - `core/fingerprint/`: JS injection and identity silo generation.
    - `core/network/`: Sanitization, classification, and loopback proxy logic.
    - `core/security/`: Privacy policies, root detection, and sensitive API sentinels.
    - `core/session/`: Tab lifecycle, storage control, and central logging.
    - `core/webview/`: Hardened WebView, customized Clients (Chrome/WebView).
    - `ui/`: Compose-based architecture (Screens, ViewModels, Themes).
- `app/src/main/assets/`: Static JS obfuscators and tracker databases.
- `scripts/`: Production verification and build automation tools.
- `docs/`: Technical guides, diagnostics, and AI automation prompts.

## 🕵️ Root Cause Analysis (RCA): The "Search Dog" Bug
The "duckduck" truncation bug was caused by a race condition between URL formation and WebView error reporting:
1.  **Truncation**: If an input like `duckduck` was misinterpreted as a URL, it became `https://duckduck`. Because `toHttpUrlOrNull()` fails on such malformed hostnames, the sanitizer returned the raw truncated string.
2.  **State Smearing**: During failed navigations, the WebView would synchronously report a state change to `https://duckduck` before the error page loaded, overwriting the user's intent in the address bar.
3.  **The Fix**: Implemented a "Fortification Heuristic" in `BrowserViewModel` that treats any URL lacking a valid Top-Level Domain (TLD) or standard structure as a search query, regardless of scheme-like prefixes.

## ⚡ Concurrent State & Error Management
Errors in Amnos often occur simultaneously (e.g., a Tracker is blocked WHILE a navigation times out).
- **Logging Rule**: All errors must be logged to `SecurityController.logInternal` with a timestamp and tag. 
- **Synchronicity**: UI updates to the address bar must be decoupled from internal loading states to prevent "address bar flickering" during complex redirection chains.
- **Trace Consistency**: When debugging simultaneous errors, always correlate logs via the `SessionID` found in the **LOGS** Dashboard.

## 📝 Maintenance & AI Rules
1.  **Privacy First**: If a feature trade-off is required, always prioritize privacy over convenience.
2.  **Zero Persistence**: No history, no cache, no cookies. If a site requires persistent cookies, it is incompatible with Amnos by design.
3.  **Audit-Ready Code**: Use descriptive tag names in `logInternal` (e.g., `[Security:Warp]` or `[Net:Interception]`) to make manual log audits fast.
4.  **Verification**: Always run `./gradlew lintDebug` to catch privacy-leaking API calls (like plain `Log` instead of `logInternal`).

---
**Final Directive**: Treat Amnos as a high-stakes security tool. Every line of code must be written with the assumption that the user's anonymity is at risk.

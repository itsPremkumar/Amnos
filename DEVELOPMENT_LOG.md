# 📜 Amnos: Development & Security Audit Log

This log tracks the engineering evolution of the **Amnos** project, from a basic WebView prototype to a hardened, modular privacy engine.

---

## 🚀 Phase 1: Prototype (The Foundation)
*   **Initial Build**: Basic Kotlin Android app with a full-screen WebView.
*   **Privacy 1.0**: Disabled standard `domStorage` and `databaseEnabled`.
*   **Hardening**: Set `cacheMode` to `LOAD_NO_CACHE`.

## 🏗️ Phase 2: Modularization (Identity Synthesis)
*   **The Problem**: WebViews often leak device inconsistencies (e.g., a Windows User-Agent on an Android screen resolution).
*   **The Solution**: Created the **`FingerprintManager`**. This module synthesizes a "Coherent Identity" where all JS-exposed properties (User-Agent, Platform, Screen, Hardware) match a single, realistic profile.
*   **Execution**: Implemented `ScriptInjector` to force-inject the identity before the page loads.

## 🛡️ Phase 3: Elite Hardening (Tactical Defense)
*   **Network Guard**: Integrated **DNS-over-HTTPS (DoH)** via Cloudflare.
*   **Anti-Fingerprint v2**: Added **Canvas Noise**, **Audio Jitter**, and **WebGL Masking**.
*   **Protocol Lockdown**: Created `NetworkSecurityManager` to block **WebSockets** and strip **Referer** headers.
*   **Storage Guard**: Implemented `StorageController` for **Ephemeral Downloads** and **Clipboard Wiping**.

## 🎨 Phase 4: Branding (The Amnos Identity)
*   **The Shift**: Moved from generic "Privacy Browser" to **Amnos**.
*   **Amnos Philosophy**: Naming based on "Amnesia." All sessions are volatile; all data is temporary.
*   **Logo Design**: Designed the **Chrome Portal**—a liquid mercury ring with a negative-space "A" representing the "Void" of the user's identity.

## 🏁 Phase 5: Production Readiness
*   **Build Hardening**: Enabled **R8/ProGuard** for code obfuscation.
*   **UX Upgrade**: Created the **Security Cockpit** with the **Live Request Inspector** for total user transparency.
*   **Manifest Guard**: Disabled `android:allowBackup` to prevent cloud data leakage.

---

### 🛡️ Security Audit Note
As of **v1.0.0**, Amnos has been audited for:
1.  **Zero Persistence**: No data remains in `/data/data/` or `/sdcard/` after a "Deep Wipe."
2.  **Identity Coherence**: No detectable mismatch between User-Agent and Platform properties.
3.  **Network Stealth**: No unencrypted DNS leakage. No Referer leakage to third parties.

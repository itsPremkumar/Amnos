# 🛡️ Privacy Browser: God-Tier Modular Security Engine

A high-performance, **RAM-Only** Android web browser designed for absolute anonymity. This browser features a **Modular Fingerprint Management System** that ensures all browser-exposed properties match a single, realistic "Device Identity" for maximum deniability.

---

## 🔒 Coherent Security Shield

### 1. 🧬 Modular Fingerprint Management (Identity Coherence)
Unlike standard browsers that randomize properties, our engine uses a **FingerprintManager** to generate a single, coherent "Device Identity" (personality).
*   **Property Consistency**: If your User-Agent is Windows, your `navigator.platform` is `Win32`, and your hardware/screen specs are automatically aligned.
*   **Hardware Spoofing**: Overrides WebGL `getParameter` (GPU Vendor/Renderer) to return fake profiles (e.g., Intel, Apple, NVIDIA).
*   **Silent Perms**: Overrides `navigator.geolocation` so sites receive a clean "Permission Denied" without ever triggering a system-level popup.

### 2. 🧠 Architecture v2: Real-Time Tactical Defense
*   **Security Cockpit**: A real-time dashboard to toggle **JavaScript**, **WebGL**, and **Hardware Acceleration** on the fly.
*   **Live Tracker Intelligence**: A Top-Bar indicator showing exactly how many trackers are being killed in real-time.
*   **Incognito PIN Lock**: Secures your active RAM session with a 4-digit PIN (Default: `1111`) to prevent physical snooping.

### 3. 🛡️ Network & Protocol Hardening
*   **DNS-over-HTTPS (DoH)**: All lookups are encrypted via **Cloudflare (1.1.1.1)**, making your browsing invisible to your ISP.
*   **Strict HTTPS-Only Mode**: Force-blocks all insecure `http://` traffic.
*   **URL Tracking Sanitizer**: Automatically scrubs tracking IDs (UTM, FBCLID, GCLID) from links before they are processed.

### 4. 🧠 RAM-Only "Amnesia" Architecture
*   **Disposable Session Silos**: Every launch creates a unique, randomized Session UUID silo.
*   **Dead Man's Switch (Idle Wipe)**: Automatically performs a total forensic memory scrub and kills the process if the app is minimized.
*   **Forensics-Proof**: Zero persistent data remains on the physical device.

---

## 📁 Modular Project Structure

- **`core/fingerprint`**: Coherent Identity generation (`FingerprintManager`, `ScriptInjector`).
- **`core/webview`**: Hardened browser engine (`SecureWebView`, `PrivacyWebViewClient`).
- **`core/network`**: Encrypted DNS-over-HTTPS and Tracking Sanitization.
- **`ui/`**: Professional, state-driven Material 3 navigation and Security Cockpit.

## 🏗️ Build & Requirements
- **Android API 28+**
- **Giraffe / Hedgehog+**

## 📜 License
Released under the MIT License. A 100% free, zero-cost professional privacy tool.

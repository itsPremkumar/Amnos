# 🛡️ Privacy Browser: God-Tier Security Engine

A high-performance, **RAM-Only** Android web browser designed for absolute anonymity. This is a **God-Tier** implementation featuring enterprise-grade security protocols used by investigative journalists and security researchers.

---

## 🔒 Built-In Security Shield

### 1. 🧠 RAM-Only Volatile Architecture (Total Amnesia)
*   **Disposable Session Silos**: Every launch creates a unique, randomized Session UUID silo. Your browsing path is 100% isolated and never linked.
*   **Zero-Disk Footprint**: Hardware-locked settings prevent site data (LocalStorage, Cookies, Databases) from ever touching your physical storage chip.
*   **Dead Man's Switch (Idle Wipe)**: Automatically performs a total forensic memory scrub and kills the process if the app is minimized or the screen is locked.

### 2. 🛡️ Network & Protocol Hardening
*   **DNS-over-HTTPS (DoH)**: All domain lookups are encrypted via **Cloudflare (1.1.1.1)**, making your browsing invisible even to your Internet Service Provider.
*   **Strict HTTPS-Only Mode**: Force-blocks all insecure `http://` traffic.
*   **URL Tracking Sanitizer**: Automatically scrubs tracking IDs (UTM, FBCLID, GCLID) from links before they are even processed.

### 3. 🎭 Forensic-Level Anonymity (Zero-Fingerprinting)
*   **Multi-Vector Protection**:
    - **Canvas Poisoning**: Injects noise into rendering to break hardware identification.
    - **Web Audio Spoofing**: Masks your device's unique audio hardware signature.
    - **Battery Masking**: Spoofs battery data to prevent its use as an ID.
*   **Tracking API Stubbing**: Disables `navigator.sendBeacon` and other exfiltration APIs trackers use to "phone home."
*   **Global Privacy Control (GPC)**: Injects "Do Not Track/Sell" signals into every request.

### 4. 🧹 Deep-Wipe Kill Switch
*   **Deterministic Purge**: A one-tap button that performs a multi-stage forensic scrub of hardware caches, session silos, and temporary process memory.
*   **WebRTC Leak Protection**: Hardware-locked to prevent IP leakage behind VPNs/Proxies.

---

## 📁 Optimized Project Structure

- **`core/network`**: Encrypted DNS-over-HTTPS & URL sanitation.
- **`core/session`**: RAM-only volatile container & lifecycle security.
- **`core/webview`**: Hardened browser clients with strict permission gates.
- **`ui/`**: Modern, professional Material 3 navigation system.

## 🏗️ Build & Requirements
- **Android API 28+**
- **100% Free / Zero-Cost Features**

## 📜 License
Released under the MIT License.

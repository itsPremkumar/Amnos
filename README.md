# 🛡️ Privacy Browser: Professional-Grade Security

A high-performance, **RAM-Only** Android web browser designed for absolute anonymity. **Every single security feature listed below is fully built, integrated, and active by default.**

---

## 🔒 Built-In Security Shield

### 1. 🧠 RAM-Only Volatile Architecture (Amnediac UI)
*   **Disposable Session Containers**: Every launch creates a unique, randomized Session UUID silo. Your browsing path is 100% isolated and never linked to previous or future use.
*   **Zero-Disk Footprint**: Hardware-locked settings prevent site data (LocalStorage, Cookies, Databases) from ever touching your physical storage chip.
*   **Total Volatility**: The entire engine environment is discarded the moment you exit.

### 2. 🛡️ Network & Protocol Hardening
*   **Strict HTTPS-Only Mode**: Force-blocks all insecure `http://` traffic. Your data never travels over unencrypted channels.
*   **Advanced SSL Guard**: Impossible to bypass certificate errors; the browser enforces a "Safe or Nothing" policy.
*   **URL Tracking Sanitizer**: Automatically scrubs tracking IDs (UTM, FBCLID, GCLID) from links before they are even processed.

### 3. 🎭 Forensic-Level Anonymity (Tor & Brave Inspired)
*   **Multi-Vector Anti-Fingerprinting**:
    - **Canvas Poisoning**: Injects noise into rendering to break hardware identification.
    - **Web Audio Spoofing**: Masks your device's unique audio hardware signature.
    - **Battery Masking**: Spoofs battery data to prevent its use as an ID.
*   **Global Privacy Control (GPC)**: Automatically injects "Do Not Track/Sell" signals into both network headers and the JavaScript environment.

### 4. 🧹 Deep-Wipe Kill Switch
*   **Deterministic Purge**: A one-tap button that performs a multi-stage forensic scrub of hardware caches, session silos, and temporary process memory.
*   **Process Self-Termination**: Ensures the Android OS completely reclaims all memory, leaving zero residual "shards."

### 5. 🚫 Automated Privacy Guard
*   **Zero-Trust Permissions**: Automatically denies access to Camera, Microphone, and Location without ever prompting the user—sites can't even "ask."
*   **Ad & Tracker Blocker**: Built-in network-level interception that kills intrusive ads and cross-site trackers before they load.
*   **Flag Secure**: Built-in protection that blocks all screenshots and screen-recording attempts.

---

## 📁 Project Architecture (Modular & Scalable)

- **`core/network`**: Advanced URL sanitation & GPC header injection logic.
- **`core/session`**: RAM-only volatile container management.
- **`core/webview`**: Hardened browser clients with strict permission/SSL gates.
- **`core/adblock`**: Local high-speed request filtering.
- **`ui/screens`**: Professional, Material 3 browsing interface.

## 🏗️ Build Requirements
- **Android API 28+**
- **Android Studio Giraffe+**

## 📜 License
Released under the MIT License. A 100% free, zero-cost privacy tool.

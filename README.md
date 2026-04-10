# 🛡️ Amnos Browser: The God-Tier Privacy Engine

**Amnos** (derived from *Amnesia* / *The Void*) is an ultra-premium, RAM-only Android web browser engineered for absolute anonymity and forensic-proof session handling. Amnos doesn't just block trackers—it erases the very memory of your existence on the web.

---

## 💎 The Identity Philosophy

### 📛 Why "Amnos"?
The name **Amnos** is rooted in the concept of **Linguistic Amnesia**. In a world where data is permanent, Amnos represents the "Total Void." It is designed with a single goal: to ensure that your digital self has no past and no future—only a fleeting, unrecorded present.

### 🔘 The Chrome Portal Logo
The **Chrome Portal** (a negative-space 'A' floating within a liquid mercury ring) represents the entry point to the web. 
*   **The Ring**: Represents the fluid, high-velocity nature of RAM-only processing.
*   **The Negative Space "A"**: Symbolizes that your identity in Amnos is a "void"—present enough to browse, but leaving zero weight or footprint behind.
*   **The Event Horizon Glow**: A deep purple aura representing the protective boundary between your device and the public internet.

---

## ⚡ Elite Hardware & Network Hardening

### 1. 🧬 Modular Identity Synthesis (Anti-Fingerprinting)
Amnos uses a **FingerprintManager** to create a coherent "Device Personality" for each tab:
*   **Identity Coherence**: Matches User-Agent, Platform, and Screen Specs perfectly to prevent "hybrid identification."
*   **GPU Masking**: Spoofs WebGL Vendor and Renderer (e.g., masking as an unidentifiable Intel/Apple profile).
*   **Font Shielding**: Blocks all non-system fonts to prevent CSS-based font enumeration attacks.
*   **Audio Jitter**: Injects micro-latency into the `AudioContext` to disrupt CPU-timing fingerprinting.

### 2. 🛡️ Network-Level Tactical Defense
*   **WebSocket Shield**: Intercepts and blocks `ws://` handshakes used for real-time tracking loops.
*   **Strict Referer Stripping**: Automatically removes the `Referer` header from all third-party requests.
*   **DNS-over-HTTPS (DoH)**: Encrypts all DNS queries via Cloudflare (1.1.1.1) to prevent ISP-level snooping.
*   **GPC & DNT Enforcement**: Force-injects `Sec-GPC: 1` and `DNT: 1` headers globally.

### 3. 🧠 "Ghost-Grade" Data Isolation
*   **RAM-Only Silos**: Every session launch generates a unique Randomized UUID silo.
*   **Ephemeral Downloads**: Downloads are funneled into a volatile cache that is deep-scrubbed on session exit.
*   **Clipboard Sentinel**: Automatically wipes the system clipboard when the app is backgrounded or killed.
*   **Dead Man's Switch**: Instantly kills the process and purges forensic artifacts if the app is minimized.

---

## 📊 The Security Cockpit
Amnos features a real-time **Security Inspector**:
- **Tracker Kill-Counter**: Live feedback on every privacy threat neutralized.
- **Request Inspector**: A transparent, RAM-only log of every document, script, and XHR request made by the current page.
- **Toggle Suite**: Granular control over the JavaScript Engine, WebGL Masking, and Font Shields.

---

## 📁 Project Architecture
- **`core/fingerprint`**: Coherent Identity generation and JS injection logic.
- **`core/network`**: Hardened protocol handling (Referer, WebSockets, DoH).
- **`core/session`**: Lifecycle management, volatile storage, and the Dead Man's Switch.
- **`ui/`**: Professional Material 3 "Security Cockpit" and state-driven navigation.

---

## 🚀 Getting Started for Developers

### 🛠️ Build Requirements
- **Android Studio Giraffe+**
- **JDK 17**
- **Gradle 8.0+**

### 📦 Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/amnos-browser.git
   ```
2. **Open the project** in Android Studio.
3. **Synchronize Gradle** and wait for dependencies to download.
4. **Run** on a physical device or emulator (API 28+).

---

## 🤝 Community & Support
- **Contributing**: Please see [CONTRIBUTING.md](file:///c:/one/browser/CONTRIBUTING.md) for our coding standards.
- **Reporting Bugs**: Open an [Issue](https://github.com/yourusername/amnos-browser/issues).
- **Security**: Please see [SECURITY.md](file:///c:/one/browser/SECURITY.md) for responsible disclosure.

## 🏁 Future Roadmap
Check our [Architecture Draft](file:///c:/one/browser/ARCHITECTURE.md) for planned features like **Proxy Integration** and **Per-Domain JS Permissions**.

## 📜 License
Released under the **MIT License**. See [LICENSE](file:///c:/one/browser/LICENSE) for details. (Note: Please add a LICENSE file to the root).

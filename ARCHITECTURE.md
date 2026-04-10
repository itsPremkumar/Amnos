# 🏗️ Amnos: Technical Architecture

**Amnos** is built on a modular "Zero-Trust" architecture where every component is isolated and session-bound.

---

## 1. 🧬 Core Components

### `FingerprintManager`
Responsible for synthesizing a coherent "Device Personality." It generates matched profiles (User-Agent, Screen resolution, Hardware specs) to ensure fingerprint coherence.

### `ScriptInjector`
Intercepts the WebView lifecycle to force-inject the `FingerprintObfuscator.js` before any site-scripts can execute. This ensures shields are active at the first millisecond of the page load.

### `NetworkSecurityManager`
A high-performance interceptor that:
- Blocks **WebSocket** handshakes (`ws://`, `wss://`).
- Strips **Referer** headers for all cross-domain requests.
- Force-injects **GPC** (Global Privacy Control) and **DNT** (Do Not Track) headers.

### `StorageController`
Manages volatile data:
- **Ephemeral Downloads**: Funnels files into a session-only cache.
- **Clipboard Sentinel**: Wipes the system clipboard on app pause/exit.

---

## 2. 🧠 Session Lifecycle
Amnos uses a **"Tab-Silo"** model. Each tab is a unique, isolated instance:
1. **Creation**: Unique UUID and profile generation.
2. **Active**: Real-time request monitoring via `SecurityController`.
3. **Destruction**: Forensic memory scrub and process termination.

## 3. 📊 UI Layer
Built with **Jetpack Compose** and **Material 3**:
- **Security Cockpit**: State-driven dashboard for shield management.
- **Request Inspector**: A standard `LazyColumn` displaying the volatile RAM-log of active network requests.

---

## 🔒 Security Guarantees & Threat Model

### What Amnos Protects Against:
- **Local Browser Forensics**: No data survives in the app directory or RAM after a "Deep Wipe."
- **Fingerprinting**: Neutralizes web-based hardware enumeration and font identification.
- **Cross-Site Tracking**: Strict third-party isolation and Referer stripping.
- **WebSocket Tracking**: Blocks the initial handshake for real-time tracking streams.

### What is Out of Scope (Requires a VPN/Tor):
- **IP Address Privacy**: Amnos blocks IP leaks via WebRTC, but it does *not* hide your public IP from the website you visit.
- **ISP Monitoring**: Your ISP can still see *that* you are connecting to a specific domain (use DoH + a VPN for full network anonymity).
- **Physical Memory Extraction**: While we wipe RAM on exit, a sophisticated physical attacker with specialized hardware could potentially capture data from an unlocked device *before* the wipe occurs.

---

## 🏁 Future Roadmap
- [ ] Integration of a built-in Proxy/SOCKS5 manager.
- [ ] Customizable user-agent profile presets.
- [ ] Full per-domain JavaScript permission manager.

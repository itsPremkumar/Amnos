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

## 🔒 Security Guarantees
- **No Disk Persistence**: All WebView databases, cache, and cookies are locked to RAM.
- **Forensic-Proof**: Process termination on background ensures no RAM residue can be extracted.

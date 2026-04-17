# AMNOS CLUSTER ARCHITECTURE & CONFIGURATION (V4.1)

Amnos is built on a **Modular Cluster Architecture**. This 7-cluster model synchronizes your `.env` settings, back-end managers, and UI panels for total forensic and logical isolation.

---

## 👻 Cluster 1: STEALTH (Passive Defense)
Controls how the app hides itself from local physical observation.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `STEALTH_CAMOUFLAGE_PROFILE` | `CALCULATOR` | Changes the app icon and name to a decoy calculator. |
| `STEALTH_ABSOLUTE_CLOAKING` | `true` | The app is completely hidden from the Android "Recent Apps" list. |
| `STEALTH_DECOY_UNLOCK_PIN` | `String` | PIN used to access the disguised app. |

---

## 🧹 Cluster 2: PURGE (Active Defense)
Controls the destruction of session data and memory scrambling.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `PURGE_SANDBOX_ENABLED` | `true` | Enables system-level isolation and Intent Jail. |
| `PURGE_FORENSIC_RAM_SCRAMBLE` | `true` | Overwrites RAM with garbage data before session termination. |
| `PURGE_WIPE_ON_SCREEN_OFF` | `true` | Deletes the active session as soon as the screen is locked. |
| `PURGE_PANIC_GESTURE_ENABLED` | `true` | Enables the Volume-Down double-click to instantly kill the app and wipe data. |

---

## ⚡ Cluster 3: NETWORK (Transport & Anonymity)
Defines the "Pipe" or encryption layer for all web traffic. Orchestrated by the `NetworkTrafficConfigurator`.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `NETWORK_FIREWALL_LEVEL` | `PARANOID` | `PARANOID`: Whitelist-only. `BALANCED`: Regular navigation. |
| `NETWORK_HTTPS_ONLY` | `true` | Strictly blocks all non-HTTPS (cleartext) traffic. |
| `NETWORK_DOH_URL` | `URL` | The DNS-over-HTTPS provider used to hide queries from ISPs. |
| `NETWORK_BLOCK_WEBRTC` | `true` | Prevents IP leakage via WebRTC STUN/TURN requests. |

---

## 🛑 Cluster 4: FILTER (Request Interception)
Defines "What" is allowed to travel through the network pipe. Managed by the `AdBlocker` and `FilterRegistry`.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `FILTER_BLOCK_TRACKERS` | `true` | Blocks 50k+ known tracking and data-mining domains. |
| `FILTER_AGGRESSIVE_AD_BLOCKING`| `true` | Removes cosmetic ads and popups. |
| `FILTER_REMOVE_TRACKING_PARAMS`| `true` | Strips `?utm_source=...` and similar junk from URLs. |
| `FILTER_BLOCK_SERVICE_WORKERS` | `true` | Prevents background site execution. |

---

## 🎭 Cluster 5: IDENTITY (Spoofing)
Defines who the browser "Claims to Be" to websites.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `IDENTITY_UA_TEMPLATE` | `DYNAMIC` | Options: `DYNAMIC`, `S24`, `PIXEL_8`, `S23`, `ONEPLUS`, `XIAOMI`, `XPERIA`, `GENERIC`. |
| `IDENTITY_RESET_ON_REFRESH` | `true` | Changes sub-identifiers every time a page is reloaded. |
| `IDENTITY_SESSION_TIMEOUT_MS` | `Number` | Automatically wipes everything after X milliseconds of inactivity. |

---

## ⚙️ Cluster 6: HARDWARE (Technical Masking)
How your real hardware behaves and leaks unique entropy.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `HARDWARE_FINGERPRINT_LEVEL` | `TITAN` | `TITAN`: Maximum identity noise. `STRICT`: Basic randomization. |
| `HARDWARE_WEBGL_MODE` | `DISABLED`| Disables GPU surfaces to prevent profiling. |
| `HARDWARE_JAVASCRIPT_MODE` | `RESTRICTED` | Disables invasive JS APIs like `battery` or `bluetooth`. |

---

## 🛠️ Cluster 7: DEBUG (Integrity & Audit)
Controls for technical auditing, anti-debugging, and preventing forensic inspection. Managed by the `RiskEngine` and `ForensicAuditLog`.

| Key | Value/Level | Description |
| :--- | :--- | :--- |
| `DEBUG_LOCKDOWN_MODE` | `true` | If true, all developer backdoors (ADB, Remote Debug) are disabled. |
| `DEBUG_ANTI_DEBUGGER` | `true` | App kills itself if a debugger or emulator is detected. |
| `DEBUG_BLOCK_SCREENSHOTS` | `true` | Prevents Android System from taking screenshots or screen recordings. |

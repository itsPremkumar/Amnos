# Amnos Master Configuration Guide (`.env`)

This document provides a comprehensive breakdown of every configuration switch available in the `.env` file. These settings control the core security, privacy, and forensic posture of the Amnos Browser.

---

## 🛠️ Configuration Profiles

Use these profiles as templates for your `.env` file depending on your mission.

### 1. 👻 The "GHOST" Setup (Ultra-Paranoid)
*Maximum isolation, zero persistent residue. Expect many websites to break.*

| Section | Key | Value | Rationale |
| :--- | :--- | :--- | :--- |
| **Integrity** | `SECURITY_FIREWALL_LEVEL` | `PARANOID` | Blocks all unknown domains by default. |
| **Integrity** | `SECURITY_SANDBOX_ENABLED` | `true` | Enables Intent Jail and Forensic Wipes. |
| **Logic** | `SECURITY_BLOCK_THIRD_PARTY_SCRIPTS` | `true` | Prevents cross-site tracking execution. |
| **Network** | `SECURITY_BLOCK_UNSAFE_METHODS` | `true` | Read-only mode; prevents form/login data leakage. |
| **Stealth** | `SECURITY_ABSOLUTE_CLOAKING` | `true` | Hidden from Android Task Manager. |
| **Purge** | `SECURITY_WIPE_ON_BACKGROUND` | `true` | Instant RAM nuke wn multitasking. |
he
### 2. 🛡️ Standard Privacy (Daily Driver)
*Highest privacy while maintaining compatibility with modern websites (YouTube, etc.).*

| Section | Key | Value | Rationale |
| :--- | :--- | :--- | :--- |
| **Integrity** | `SECURITY_FIREWALL_LEVEL` | `BALANCED` | Allows navigation while keeping tracker shields active. |
| **Integrity** | `SECURITY_SANDBOX_ENABLED` | `true` | System isolation remains active. |
| **Engine** | `SECURITY_JAVASCRIPT_MODE` | `RESTRICTED` | Disables invasive JS APIs but kept for site logic. |
| **Fingerprint**| `SECURITY_FINGERPRINT_LEVEL` | `STRICT` | Uses high-entropy noise for Canvas/Audio/WebGL. |
| **Risky** | `SECURITY_BLOCK_THIRD_PARTY_SCRIPTS`| `false` | Required for CDNs (scripts from Cloudflare/Google). |

### 3. 🔧 Developer / Debugging Mode
*Mandatory settings for using Android Studio, Logcat, or Chrome DevTools.*

| Section | Key | Value | Rationale |
| :--- | :--- | :--- | :--- |
| **Lockdown** | `SECURITY_LOCKDOWN_MODE` | `false` | Disables Anti-Debugger self-destruct. |
| **Lockdown** | `SECURITY_ANTI_DEBUGGER` | `false` | Allows ADB attachment. |
| **Logging** | `SECURITY_BLOCK_FORENSIC_LOGGING`| `false` | Enables system Logcat output. |
| **Capture** | `SECURITY_BLOCK_SCREENSHOTS` | `false` | Allows taking screenshots for debugging. |
| **DevTools** | `SECURITY_BLOCK_REMOTE_DEBUGGING`| `false` | Enables `chrome://inspect` on PC. |

---

## 🛡️ DETAILED KEY REFERENCE

### Section 1: Core Integrity & Sandbox
*Focus: Runtime environment security.*

| Key | Type | Risk | Description |
| :--- | :--- | :--- | :--- |
| `SECURITY_FIREWALL_LEVEL` | Enum | Low | `PARANOID`: Whitelist-only navigation. `BALANCED`: Standard browsing. |
| `SECURITY_SANDBOX_ENABLED` | Bool | Low | Master toggle for Intent Jail, Forensic Wipes, and System isolation. |
| `SECURITY_ENFORCE_STRICT_POLICIES` | Bool | Med | If true, minor policy violations will trigger app termination. |
| `SECURITY_ANTI_DEBUGGER` | Bool | **HIGH** | If true, app kills itself if ADB or debugger is detected. |
| `SECURITY_ABSOLUTE_CLOAKING` | Bool | Med | Completely removes the app from the "Recents" screen. |

### Section 2: Developer Cockpit
*Focus: Debugging and Local Hardware exposure.*

| Key | Type | Risk | Description |
| :--- | :--- | :--- | :--- |
| `SECURITY_LOCKDOWN_MODE` | Bool | **HIGH** | Master switch. If true, all developer features are physically fused. |
| `SECURITY_BLOCK_FORENSIC_LOGGING` | Bool | Low | Prevents sensitive URLs from appearing in system Logcat. |
| `SECURITY_BLOCK_SCREENSHOTS` | Bool | Low | Enforces `FLAG_SECURE` to block OS-level screen capture. |
| `SECURITY_BLOCK_REMOTE_DEBUGGING` | Bool | Low | Specifically controls WebView remote inspection. |

### Section 3: Network Isolation
*Focus: Data in transit and IP leak protection.*

| Key | Type | Risk | Description |
| :--- | :--- | :--- | :--- |
| `SECURITY_HTTPS_ONLY` | Bool | Safe | Blocks all plain HTTP traffic. Mandatory for production. |
| `SECURITY_BLOCK_WEBRTC` | Bool | Safe | Prevents IP leakage via STUN/TURN (breaks video chat). |
| `SECURITY_DOH_URL` | String | Safe | URL for DNS-over-HTTPS resolution (e.g., Cloudflare/NextDNS). |
| `SECURITY_BLOCK_UNSAFE_METHODS` | Bool | Med | Blocks POST/PUT/DELETE. Only GET/HEAD allowed. |
| `SECURITY_BLOCK_LOCAL_NETWORK` | Bool | Safe | Blocks access to loops/LAN (Prevents cross-device scanning). |

### Section 5: Anti-Fingerprinting
*Focus: Identity obfuscation.*

| Key | Type | Risk | Description |
| :--- | :--- | :--- | :--- |
| `SECURITY_FINGERPRINT_LEVEL` | Enum | Med | `STRICT` (randomized noise) or `BALANCED` (device templates). |
| `SECURITY_UA_TEMPLATE` | Enum | Low | Pick Profile: `PIXEL_8`, `S23`, `ONEPLUS`. |
| `SECURITY_SESSION_TIMEOUT_MS` | Long | Safe | Auto-wipe timer for idle sessions. Default 120,000ms. |

### Section 6: Experimental & Risky (Site Breaking)
*Focus: Paranoid script/resource blocking.*

> [!CAUTION]
> These settings ARE NOT RECOMMENDED for daily browsing as they will break 90% of the modern web (logins, interactive UI, CDNs).

| Key | Type | Risk | Description |
| :--- | :--- | :--- | :--- |
| `SECURITY_BLOCK_THIRD_PARTY_SCRIPTS` | Bool | **STRICT** | Blocks scripts not on the primary domain. (Breaks Google/FB logins). |
| `SECURITY_BLOCK_INLINE_SCRIPTS` | Bool | **STRICT** | Blocks scripts embedded in HTML. (Breaks most UI frameworks). |
| `SECURITY_ENFORCE_LOOPBACK_PROXY` | Bool | Exp | Forces traffic through a local `127.0.0.1` tunnel. |
| `SECURITY_JAVASCRIPT_MODE` | Enum | Med | `RESTRICTED` (blocks sensors/battery/WebRTC) or `DISABLED`. |

### Section 7: Forensic Purge & Stealth
*Focus: Post-session data destruction.*

| Key | Type | Risk | Description |
| :--- | :--- | :--- | :--- |
| `SECURITY_FORENSIC_RAM_SCRAMBLE` | Bool | Safe | Overwrites RAM buffers with noise before clearing. |
| `SECURITY_PANIC_GESTURE_ENABLED` | Bool | **HIGH** | Double Volume-Down instantly wipes the entire app state. |
| `SECURITY_CAMOUFLAGE_PROFILE` | Enum | Med | Disguises the app icon/label as `CALCULATOR` or `WEATHER`. |
| `SECURITY_DECOY_UNLOCK_PIN` | String | Med | Master PIN to reveal the browser from behind the Decoy UI. |

---

## 📋 MANDATORY DEBUGGING CHECKLIST

If you are developing or debugging Amnos, ensure your `.env` is updated as follows:

1.  Set `SECURITY_LOCKDOWN_MODE=false`
2.  Set `SECURITY_ANTI_DEBUGGER=false`
3.  Set `SECURITY_BLOCK_FORENSIC_LOGGING=false`
4.  Set `SECURITY_BLOCK_REMOTE_DEBUGGING=false` (If you need Chrome DevTools)
5.  Set `SECURITY_BLOCK_SCREENSHOTS=false` (If you need to capture logs/UI)

---

## 🧭 LEGEND

- **Safe**: Highly recommended, minimal site breakage.
- **Med**: Some site breakage possible (e.g., video chat, specific UI).
- **Exp / Med**: Unstable, may cause connection drops or weird behavior.
- **HIGH / STRICT**: Will cause massive breakage; intended for high-threat scenarios only.

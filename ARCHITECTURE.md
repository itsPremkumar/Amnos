# Amnos Modular Security Architecture (v4.0)

Amnos is built on a **Modular Controller Architecture** designed for zero-trust engineering and total forensic isolation. The engine is organized into **7 Security Clusters**, ensuring that every configuration, logic controller, and data hub is specialized and identifiable.

---

## The 7-Cluster Model

Amnos synchronizes its configuration (`.env`), its internal logic (`Managers` & `Guards`), and its user interface (`Panels`) into seven logical clusters:

### 1. 👻 STEALTH (Cloaking & Anti-Trace)
Handles the absolute invisibility of the browser.
- **Back-end**: `NavigationGuard`, `ResourceGuard`.
- **UI**: `StealthPanel`.
- **Logic**: URL sanitization, scheme blocking, and camouflage orchestration.

### 2. 🧹 PURGE (Forensic Sanitization)
Handles the total destruction of session data.
- **Back-end**: `SuperWipeEngine`, `ForensicFileSystemNuke`.
- **UI**: `IdentityHardeningPanel` (Integrity section).
- **Logic**: Recursive disk deletion, RAM scrambling, and cryptographic key obliteration.

### 3. ⚡ NETWORK (Transport & Anonymity)
Handles the loopback proxy and encrypted traffic.
- **Back-end**: `NetworkTrafficConfigurator`, `LoopbackProxyServer`.
- **UI**: `NetworkShieldsPanel`.
- **Logic**: Proxy controller management, DoH (DNS-over-HTTPS), and WebRTC leak prevention.

### 4. 🛑 FILTER (Request Interception)
Handles the blocking of trackers and malicious assets.
- **Back-end**: `AdBlocker`, `FilterRegistry`.
- **UI**: `NetworkShieldsPanel` (Filter section).
- **Data**: `PrivacyAuditLog`.
- **Logic**: High-performance regex matching and domain blocklist management.

### 5. 👤 IDENTITY (Profile Rotation)
Handles the spoofing of the digital fingerprint.
- **Back-end**: `FingerprintManager`, `SecureVault`.
- **UI**: `IdentityHardeningPanel`.
- **Logic**: User-Agent rotation, session ID generation, and encrypted preference siloing.

### 6. 🖥️ HARDWARE (API Restriction)
Handles the lockdown of physical device sensors and APIs.
- **Back-end**: `PolicyController`, `TabManager`.
- **UI**: `AdvancedHardwarePanel`.
- **Logic**: JavaScript mode enforcement, WebGL randomization, and Hardware Concurrency normalization.

### 7. 🛠️ DEBUG (Integrity & Audit)
Handles the internal health and self-protection of the app.
- **Back-end**: `RiskEngine`, `ForensicAuditLog`.
- **UI**: `DebugLockdownPanel`.
- **Logic**: Anti-debugger checks, integrity monitoring, and internal forensic tracing.

---

## Core Infrastructure

### Modular Controller Architecture
Instead of "God Objects," Amnos uses specialized controllers that delegate responsibilities:
- **Managers**: Long-lived infrastructure (e.g., `TabManager`, `NetworkTrafficConfigurator`).
- **Guards**: Specialized security enforcers that intercept WebView events (e.g., `NavigationGuard`, `ResourceGuard`).
- **Data Hubs**: Clustered, thread-safe repositories for UI state and logs (e.g., `TrafficMonitor`, `PrivacyAuditLog`).

### Session Lifecycle
1. **Initialize**: `SessionManager` coordinates with the 7 clusters to establish a virgin environment.
2. **Browse**: Specialized `Guards` and `Controllers` enforce policies in real-time.
3. **Wipe**: `SuperWipeEngine` orchestrates a multi-phase purge across all 7 clusters.

## Design Philosophy: Identifiability
Every line of code in Amnos is designed to be **Identifiable**. You can track a feature by name from the `.env` key, through the manager logic, all the way to the UI panel.

# 🔒 Amnos Ultra-Sandbox Browser Architecture (Zero-Exfiltration Design)

Design and implement a **privacy-first Android browser (Amnos)** that operates as a **sealed execution sandbox**, inspired by containerization systems like Docker and virtualization models like VirtualBox, but adapted to Android app-level constraints.

The system must ensure:

* No persistent data remains after session termination
* No unintended data escapes the app boundary
* No external apps can inject or extract data
* Network communication is the **only allowed I/O channel**

---

# 🎯 Core Objective

Create a **controlled web execution environment** where:

* Web content runs in isolation
* All inputs/outputs are strictly filtered
* The app behaves like a **self-contained ephemeral container**

---

# 🧱 1. Sandbox Model (App-Level Virtualization)

Since OS-level virtualization is not available, implement a **logical sandbox layer**:

### Principles:

* Treat each session as a **container instance**
* Destroy and recreate environment per session
* No shared state between sessions

### Implementation:

* SessionManager = container orchestrator
* Each session:

  * New WebView instance
  * New identity (sessionId)
  * Fresh network stack
  * Fresh encryption key

---

# 🌐 2. Network as the ONLY Allowed I/O

Strictly enforce:

### Allowed:

* HTTPS requests (TLS 1.3)
* DNS over HTTPS (DoH)

### Block or control:

* File system writes (except controlled internal storage)
* Clipboard access (auto-clear)
* External intents (no app switching leaks)
* Downloads (disable or sandbox)
* File uploads (restrict or proxy)

---

# 🚫 3. Data Exfiltration Prevention

### Outbound Leak Controls:

* Strip or randomize:

  * User-Agent
  * Headers
  * Referrers
* Block:

  * WebRTC (IP leaks)
  * Third-party cookies
* Disable:

  * Background sync
  * Push notifications

---

# 🔐 4. Internal Data Containment

### Memory:

* Keep all browsing data in RAM only
* Avoid persistent storage wherever possible

### Disk:

* Disable WebView cache where possible
* Accept unavoidable writes → destroy via Super Wipe

### Clipboard:

* Overwrite immediately after use
* Clear during wipe

---

# 🛡️ 5. WebView Hardening (Critical Layer)

Configure WebView as a **restricted execution engine**:

* Disable:

  * File access
  * Content access
  * Debugging in production
* Restrict:

  * JavaScript interfaces
* Enforce:

  * HTTPS-only mode
* Inject:

  * Content Security Policies (CSP where possible)

---

# 🔄 6. Session Isolation (Container Behavior)

Each session must:

* Generate:

  * New session ID (256-bit random)
  * New encryption key
* Reset:

  * Cookies
  * Storage
  * DNS cache
* Use:

  * Fresh WebView instance

Optional:

* Per-tab isolation (each tab = mini container)

---

# 💀 7. Super Wipe (Container Destruction)

When triggered:

### Phase 0: Cryptographic Kill

* Delete Keystore key

### Phase 1–7:

* Destroy WebView
* Clear storage
* Delete app_webview directory
* Reset network stack
* Zero memory
* Clear UI
* Optional process kill

Result:
👉 No recoverable data remains

---

# 🔑 8. Encryption Integration

Apply encryption only where needed:

* Storage → AES-256-GCM (Keystore)
* Network → TLS 1.3
* DNS → DoH

Key rules:

* New key per session
* No key reuse
* Destroy key before wipe

---

# 🚷 9. External Interaction Lockdown

Prevent data leaving the sandbox:

* Disable:

  * Sharing intents
  * Open-in-browser
  * External file access
* Intercept:

  * All outgoing intents
* Allow only:

  * Controlled internal navigation

---

# 🧠 10. Fingerprint & Tracking Resistance

* Randomize:

  * User-Agent (per session)
* Block:

  * Trackers (basic filtering)
* Limit:

  * Canvas/WebGL fingerprinting (partial)

---

# ⚙️ 11. Process-Level Isolation (Best Effort)

* Use:

  * Separate process for WebView (if possible)
* On kill switch:

  * Terminate process for full RAM cleanup

---

# 📉 12. Logging & Debug Control

* Remove all logs in release build
* No sensitive data in logs
* Disable WebView debugging

---

# 🧪 13. Verification Strategy

### Isolation Tests:

* No data persists after wipe
* New session has no linkage

### Leak Tests:

* No IP leak via WebRTC
* No clipboard residue

### Forensic Tests:

* app_webview fully deleted
* Encrypted data unreadable after key deletion

---

# ⚠️ Realistic Constraints (Must Accept)

* WebView will still write some data internally
* Full OS-level sandboxing is not possible
* Some fingerprinting cannot be fully blocked
* Network traffic cannot be made “invisible,” only encrypted

---

# 🎯 Final Goal

Build a browser that behaves like:

👉 **A disposable, sealed web container**

Where:

* Data comes in (web content)
* User interacts
* Everything is destroyed
* Nothing meaningful leaves or remains

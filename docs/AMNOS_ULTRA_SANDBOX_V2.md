# Amnos Ultra-Sandbox Hardening System (v2.0 — Near Zero-Exfiltration)

Upgrade the Amnos browser into a sealed, sandboxed environment where no data can enter or leave without strict control. The system must enforce full ingress filtering, egress blocking, runtime isolation, and anti-forensic guarantees.

---

## CORE OBJECTIVE

Ensure:

* No external app can inject data into Amnos
* No internal data can leave Amnos without user approval
* No background or hidden channels exist
* No OS-level leakage (clipboard, screenshots, DNS, etc.)
* All sessions remain ephemeral and memory-only

---

## PHASE 1: INBOUND INTENT LOCKDOWN (ENTRY CONTROL)

Modify MainActivity.kt:

* Intercept all ACTION_VIEW intents
* Sanitize all incoming URLs
* Strip:

  * Query parameters (?utm, ?ref, etc.)
  * Fragments (#section)
* Reject non-HTTPS schemes
* Add strict mode to completely block external intents

Implementation requirements:

* Build an IntentSanitizer module
* Add user confirmation dialog for non-strict mode
* Default behavior = BLOCK all external intents

---

## PHASE 2: OUTBOUND INTENT BLOCKING (EXIT CONTROL)

* Intercept all startActivity(intent) calls
* Prevent external navigation by default
* Allow only via explicit user confirmation

Add:

* “You are leaving secure sandbox” warning dialog
* Configurable strict mode to disable all external exits

---

## PHASE 3: FULL NETWORK REQUEST FIREWALL

Modify SecureWebView.kt:

* Override shouldInterceptRequest()
* Inspect every request (scripts, images, iframes, XHR)

Rules:

* Block unknown domains
* Allow only whitelisted domains (initially static list)
* Return empty WebResourceResponse for blocked requests

Add:

* DomainPolicyManager
* Future support for per-session dynamic allowlists

---

## PHASE 4: FORCE PROXY + DNS ISOLATION

* Route ALL traffic through loopback proxy (127.0.0.1)
* Disable system DNS resolution
* Enforce DNS over HTTPS (DoH) only

Requirements:

* Integrate with existing LoopbackProxyServer
* Ensure WebView cannot bypass proxy
* Rebuild network stack after every session wipe

---

## PHASE 5: WEBRTC HARD BLOCK

Modify PrivacyWebChromeClient.kt:

* Deny all PermissionRequest (audio/video)

Disable:

* RTCPeerConnection via injected JavaScript
* Media capture APIs

Ensure:

* No ICE candidate generation
* No IP leakage possible

---

## PHASE 6: SERVICE WORKER & BACKGROUND EXECUTION BLOCK

* Disable Service Worker registration via JS override
* Inject CSP header: worker-src 'none'
* Override navigator.serviceWorker APIs

Ensure:

* No background sync
* No push notifications
* No hidden threads

---

## PHASE 7: CLIPBOARD ISOLATION (INTERNAL VAULT)

* Intercept copy/cut/paste via injected JS
* Prevent Android OS clipboard usage
* Store all copied data in in-memory ClipboardVault

Rules:

* No data written to system clipboard automatically
* Optional user-triggered export to OS clipboard

---

## PHASE 8: UI & SCREEN SECURITY

Modify MainActivity.kt:

* Enable FLAG_SECURE

Prevents:

* Screenshots
* Screen recording
* External display capture

---

## PHASE 9: ACCESSIBILITY & AUTOFILL LOCKDOWN

* Disable autofill:
  importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO

Optional:

* Detect active accessibility services
* Warn user about potential data exposure

---

## PHASE 10: DOWNLOAD & FILE ESCAPE PREVENTION

* Disable WebView downloads completely
  OR
* Route downloads to encrypted internal storage

Ensure:

* No file is written outside sandbox

---

## PHASE 11: WEBVIEW DEBUGGING DISABLE

* Ensure:
  WebView.setWebContentsDebuggingEnabled(false)

* Enforce only in debug builds if needed

---

## PHASE 12: GPU + MEMORY RESIDUE ELIMINATION

* Integrate hard Kill Switch:
  Process.killProcess()

* Add delay (200ms) for cleanup completion

* Ensure:

  * GPU buffers cleared
  * Heap memory released
  * No residual rendering data

---

## PHASE 13: SIDE-CHANNEL MITIGATION

Inject JS overrides:

* Randomize performance.now()
* Add execution noise
* Prevent timing-based fingerprinting

Note:

* Full prevention not possible, only mitigation

---

## PHASE 14: STRICT MODE SYSTEM

Implement runtime modes:

1. PARANOID MODE:

   * No external intents
   * Strict domain allowlist
   * No downloads
   * Full isolation

2. BALANCED MODE:

   * User-gated external actions

3. OPEN MODE:

   * Standard browsing

---

## FINAL RESULT

After implementation:

* All ingress points are sanitized or blocked
* All egress paths are controlled or eliminated
* Network traffic is fully intercepted and encrypted
* OS-level leaks are minimized
* Memory and storage leave no forensic trace

---

## IMPORTANT NOTES

* This system is near-zero exfiltration, not absolute
* Android kernel and WebView engine are still shared components
* For true isolation, a custom browser engine is required

---

## VALIDATION CHECKLIST

* Test with adb network monitoring
* Perform DNS leak tests
* Attempt intent injection from another app
* Verify clipboard isolation
* Inspect app storage after Super Wipe
* Confirm no external app receives data

---

## END OF SPECIFICATION

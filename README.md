# Amnos — Privacy-First Hardened Android Browser
> **Ultra-secure, ephemeral, and anti-fingerprinting browsing for Android.**

[AI Discovery (llms.txt)](llms.txt)

Amnos is a privacy-first hardened Android browser built on `WebView` for local-only sessions and zero durable browsing state. It prioritizes privacy over compatibility: no persistent cookies, no cache retention, aggressive tracker blocking, HTTPS-only upgrades, strict first-party isolation, and deterministic fingerprint silos scoped to the active session.

Amnos is not Tor, not a VPN, and not a custom browser engine. It reduces exposure inside Android WebView constraints; it does not provide network-layer anonymity against every observer.

## Production posture

- Cleartext traffic disabled in the manifest and network security config
- User-installed CAs trusted only through `debug-overrides`
- Release builds no longer fall back to debug signing
- Release APK generation is stable by default; optional shrinking enabled with `-Pamnos.release.minify=true`
- Remote WebView debugging and relaxed diagnostics locked behind debug-build guards
- Navigation routes through a tested resolver that applies the Search Dog heuristic before load
- Address bar updates commit on successful top-level navigation instead of transient load state
- `Set-Cookie` headers are stripped and WebView cookies are disabled
- Session startup and teardown both purge WebView storage to limit crash-leftover persistence
- Diagnostic logging is centralized through `AmnosLog` and surfaced in the in-app dashboard
- Screenshot protection enforced via `FLAG_SECURE` when policy requires it
- Root detection on launch — automatically escalates to STRICT fingerprint mode if rooted device is detected
- Global crash resilience engine prevents force-close and wipes clipboard on fatal exceptions
- **Task Removal Cloaking** — automatically purges the app from Android "Recents" upon close or wipe to prevent forensic residue
- **Forensic RAM Scrambling** — saturates sensitive memory buffers with cryptographically secure noise before zero-filling
- **Absolute Proxy Lock** — forces all JVM-level traffic through the internal secure loopback tunnel

---

## Privacy features

### Network security
- **HTTPS-only enforcement** — all HTTP requests are blocked or upgraded; loopback proxy rejects non-CONNECT requests
- **DNS-over-HTTPS (DoH)** — all DNS resolution routes through Cloudflare DoH (`1.1.1.1` / `1.0.0.1`) via OkHttp; no system resolver leakage
- **IPv6 blocking** — optional IPv4-only resolution to prevent IPv6 address leakage
- **Loopback proxy** — a local `127.0.0.1` CONNECT proxy intercepts all WebView traffic, enforces tunnel policy, and routes DNS through DoH
- **Tracking parameter stripping** — `utm_*`, `fbclid`, `gclid`, `wbraid`, `gbraid`, `msclkid`, `mc_eid`, `yclid`, `_hsenc`, `_hsmi`, `mkt_tok` removed from every URL before load
- **Referrer stripping** — `Referer` and `Origin` headers suppressed; `Referrer-Policy: no-referrer` injected into responses
- **Cookie blocking** — WebView cookies disabled globally; `Set-Cookie` response headers stripped before reaching WebView; session and all cookies purged on wipe
- **Third-party request blocking** — cross-site subresource requests blocked by policy
- **Third-party script blocking** — cross-origin `<script>` loads blocked independently of full third-party blocking
- **WebSocket blocking** — `ws://` and `wss://` connections blocked at both the network layer and via JS wrapper; attempts reported to the Security Cockpit
- **Local network access blocking** — requests to `localhost`, `*.local`, RFC-1918 ranges (`10.x`, `172.16–31.x`, `192.168.x`), and loopback are blocked
- **Unsafe method blocking** — only GET and HEAD are proxied; POST/PUT/DELETE fall through or are blocked
- **Intent jail** — only `http://` and `https://` schemes are allowed in navigation; `intent://`, `tel:`, `mailto:`, `market://`, `javascript:`, `file:`, `data:`, `content:` are all blocked
- **DNT and Sec-GPC headers** — `DNT: 1` and `Sec-GPC: 1` injected on every proxied request
- **Cache disabled** — `Cache-Control: no-cache, no-store` on requests; `Cache-Control: no-store, no-cache, max-age=0` on responses; WebView cache mode set to `LOAD_NO_CACHE`

### Ad and tracker blocking
- **Dual blocklist engine** — loads `blocklist.txt` and `blocklist_comprehensive.txt` from assets at startup in a background thread
- **Domain-based blocking** — hierarchical subdomain matching against a `ConcurrentHashMap` of blocked domains
- **Regex pattern blocking** — compiled patterns for ad/tracker URL structures (`/ads/`, `/tracking/`, `/analytics/`, `/pixel/`, `/beacon/`, etc.)
- **Keyword blocking** — path and query keyword matching for `pagead`, `doubleclick`, `googlesyndication`, `facebook.net/tr`, UTM parameters, `fbclid`, `gclid`
- **Whitelist support** — per-domain allow-list to exempt specific hosts from blocking
- **YouTube functional resource exemption** — media, image, and font resources from `googlevideo.com`, `ytimg.com`, and `youtube.com` are exempted from tracker blocking to preserve playback

### Fingerprint protection
- **Deterministic session silos** — each session gets a UUID session ID; each tab gets a UUID tab ID; profiles are seeded from these IDs so the same session always produces the same fingerprint
- **Three protection levels** — `BALANCED` (rotates across device templates per session), `STRICT` (fixed minimal profile, UTC timezone), `DISABLED`
- **Device template spoofing** — three real-device templates (Pixel 8, Samsung S23, OnePlus) with matching user agents, screen specs, GPU vendor/renderer strings
- **User-Agent spoofing** — spoofed at both the HTTP header level (via `SecurityHeaderFactory`) and the JS level (`navigator.userAgent`, `navigator.appVersion`)
- **Navigator API spoofing** — `platform`, `language`, `languages`, `hardwareConcurrency`, `deviceMemory`, `maxTouchPoints`, `webdriver`, `doNotTrack`, `plugins`, `mimeTypes`, `vendor`, `vendorSub`, `productSub`, `cookieEnabled`, `onLine`, `globalPrivacyControl`
- **Screen spoofing** — `screen.width/height/availWidth/availHeight/colorDepth/pixelDepth`, `devicePixelRatio`, `outerWidth/Height`, `innerWidth/Height`, `screenX/Y`, `screenLeft/Top`, `screen.orientation.type/angle`
- **Timezone spoofing** — `Intl.DateTimeFormat.prototype.resolvedOptions` and `Date.prototype.getTimezoneOffset` overridden; STRICT mode forces UTC
- **Timing noise** — `Date.now`, `performance.now`, and `requestAnimationFrame` timestamps quantized and jittered using a per-tab noise seed (16ms / 3ms jitter in BALANCED; 100ms / 12ms in STRICT)
- **Performance API scrubbing** — `performance.getEntries/getEntriesByType/getEntriesByName` return empty; `PerformanceObserver` filtered; `performance.timing` frozen to a single base timestamp; `performance.memory` spoofed
- **Canvas fingerprinting** — `getImageData` pixel data XOR-noised with session seed; `toDataURL` and `toBlob` inject a 1×1 near-invisible noise pixel; `OffscreenCanvas.convertToBlob` similarly noised; `measureText.width` rounded in STRICT mode
- **WebGL spoofing / blocking** — GPU vendor and renderer strings spoofed via `getParameter`; WebGL disabled entirely in STRICT mode
- **Audio fingerprinting** — `AudioBuffer.getChannelData` noise-injected; `AudioContext.createAnalyser` frequency data noised; `baseLatency` and `outputLatency` spoofed; `OfflineAudioContext.startRendering` result noised
- **Font fingerprinting** — `document.fonts.check` always returns false; `document.fonts.load` returns empty; `FontFace` undefined in STRICT mode; CSS forces `sans-serif, Arial, Helvetica`
- **Storage isolation** — `localStorage` and `sessionStorage` replaced with in-memory noop stores (max 100 entries, session-scoped); `indexedDB` undefined; `openDatabase` undefined; Cache API blocked
- **WebRTC blocking** — `RTCPeerConnection`, `webkitRTCPeerConnection`, `RTCSessionDescription`, `RTCIceCandidate` replaced with fake implementations that report attempts to the Security Cockpit and never leak IP addresses; `RTCDataChannel` and `MediaStreamTrack` undefined
- **WebSocket JS wrapper** — native `WebSocket` wrapped; blocked connections throw `SecurityError`; allowed connections report open/close/error state to the Security Cockpit
- **Sensor blocking** — `DeviceMotionEvent`, `DeviceOrientationEvent`, `Accelerometer`, `Gyroscope`, `Magnetometer`, `AbsoluteOrientationSensor`, `RelativeOrientationSensor`, `LinearAccelerationSensor`, `GravitySensor`, `AmbientLightSensor`, `ProximitySensor` all set to `undefined`
- **Hardware API blocking** — USB, Bluetooth, HID, Serial, MIDI, Presentation API, XR/WebXR all blocked or return empty
- **Geolocation blocking** — `navigator.geolocation.getCurrentPosition` and `watchPosition` call the error callback with `PERMISSION_DENIED`; `Permissions.query` returns `denied` for camera, microphone, geolocation, clipboard, accelerometer, gyroscope, magnetometer
- **Media device blocking** — `getUserMedia` blocked and reported; `enumerateDevices` returns empty array
- **Battery API spoofing** — `getBattery` returns a fixed profile (`charging: true`, `level: 0.76`)
- **Network Information API spoofing** — `effectiveType: "4g"`, `downlink: 10`, `rtt: 50`, `type: "wifi"`
- **Beacon API interception** — `navigator.sendBeacon` intercepted; blocked when tracker blocking is active
- **CSS media query spoofing** — `prefers-color-scheme` forced to `light`; `prefers-reduced-motion` forced to `false`; `prefers-contrast` forced to `no-preference`; `inverted-colors` forced to `false`
- **Pointer/Touch event normalization** — `PointerEvent` pressure/tilt/twist normalized; `Touch` force/rotation/radius normalized
- **Error stack sanitization** — in STRICT mode, file paths in stack traces replaced with `<sanitized>`
- **History length spoofing** — `history.length` returns `2` in STRICT mode
- **Notification API** — `Notification.permission` always `"denied"`; `requestPermission` resolves to `"denied"`
- **Vibration API** — `navigator.vibrate` returns `false`
- **Wake Lock API** — `navigator.wakeLock.request` and `screen.keepAwake` blocked
- **Idle Detection API** — `IdleDetector` set to `undefined`
- **Gamepad API** — `navigator.getGamepads` returns empty array
- **Keyboard Lock API** — `navigator.keyboard.lock` blocked
- **Speech Synthesis** — `speechSynthesis.getVoices` returns at most one voice
- **Scheduling API** — `navigator.scheduling.isInputPending` always returns `false`
- **Service worker blocking** — `navigator.serviceWorker.register` blocked (except on search engines); `getRegistration/getRegistrations` return empty
- **eval / Function constructor blocking** — `window.eval` and `window.Function` throw `SecurityError`; `WebAssembly.compile/instantiate` blocked
- **DNS prefetch / preconnect scrubbing** — `<link rel="dns-prefetch|preconnect|prefetch|prerender|modulepreload">` elements removed by MutationObserver
- **Inline script blocking** — dynamically injected `<script>` tags without `src` removed by MutationObserver when policy requires
- **Referrer JS spoofing** — `document.referrer` returns `""` when referrer stripping is active
- **Storage quota spoofing** — `navigator.storage.estimate` returns `{ usage: 0, quota: 0 }`; `persisted/persist` return `false`
- **Clipboard JS blocking** — `navigator.clipboard.readText/writeText/read/write` all blocked

### Session and storage security
- **Zero persistent state** — `domStorageEnabled` and `databaseEnabled` disabled in WebView settings; `cacheMode = LOAD_NO_CACHE`
- **Physical storage nuke** — `StorageService.purgeGlobalStorage` deletes cookies, `WebStorage`, form data, HTTP auth credentials, client cert preferences, and physically deletes the `app_webview_amnos_session` directory
- **Volatile downloads** — downloads stored in `cacheDir/volatile_downloads` with UUID-prefixed filenames; directory wiped on session end
- **Clipboard wipe** — `ClipboardSentinel` uses `clearPrimaryClip()` on API 33+; falls back to empty sensitive clip with `IS_SENSITIVE` extra on API 31+
- **Background wipe** — `onStop` triggers `killAll` when not rotating; `onTrimMemory` triggers wipe at `TRIM_MEMORY_UI_HIDDEN`
- **Session timeout** — configurable inactivity timeout (default 2 minutes) triggers automatic wipe and session reset
- **GHOST wipe** — kill switch destroys all tabs, clears logs, wipes clipboard, purges storage, rotates session ID, and optionally calls `Process.killProcess`
- **Strict first-party isolation** — cross-site top-level navigations recreate the WebView tab with a fresh fingerprint profile
- **Identity reset on refresh** — tab profile can be regenerated on page refresh

### WebView hardening
- **Ghost Keyboard** — custom in-app Compose keyboard (alpha + symbol layouts).
  - **Premium Feedback** — real-time key popups and Material 3-grade haptics.
  - **Sanitization Shortcuts** — "Long-press to Clear All" on the backspace key for instant field zeroing.
  - **High-Density Adaptive** — automatically reduces height in landscape to preserve browsing visibility.
- **Interactive UI & Safety**
  - **Sandbox Mode Selector** — toggle between `PARANOID` (Silent zero-trust) and `BALANCED` (Gated confirm) modes.
  - **Navigation Safety Dialogs** — explicit user confirmation required to leave the secure sandbox for external apps.
  - **Threat Alert Banner** — real-time detection and HUD warning if Accessibility Scrapers are active on the device.
- **Autofill disabled** — `IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS`
- **Password/form save disabled** — `savePassword = false`, `saveFormData = false`
- **File access disabled** — `allowFileAccess`, `allowContentAccess`, `allowFileAccessFromFileURLs`, `allowUniversalAccessFromFileURLs` all `false`
- **Geolocation disabled** — `setGeolocationEnabled(false)`
- **Mixed content blocked** — `MIXED_CONTENT_NEVER_ALLOW`
- **Safe Browsing** — enabled; `onSafeBrowsingHit` intercepted to show Amnos blocked page instead of Google's interstitial
- **SSL error handling** — all SSL errors cancel navigation and log to diagnostics
- **Pop-up blocking** — `setSupportMultipleWindows(false)`; `onCreateWindow` returns `false`
- **File chooser blocked** — `onShowFileChooser` returns `null` to the callback
- **Hardware permission denial** — `PermissionSentinel` silently denies all `PermissionRequest` resources (camera, microphone, etc.)
- **Geolocation permission denial** — `onGeolocationPermissionsShowPrompt` always denies
- **Long-press disabled** — `setOnLongClickListener { true }` suppresses context menus
- **Over-scroll disabled** — `OVER_SCROLL_NEVER`
- **Haptic feedback disabled** — `isHapticFeedbackEnabled = false`
- **Media autoplay blocked** — `mediaPlaybackRequiresUserGesture = true`
- **Document-start script injection** — fingerprint obfuscation script injected via `WebViewCompat.addDocumentStartJavaScript` before any page JS runs
- **Security bridge** — `WebMessageListener` (`amnosBridge`) validates HTTPS origin and host match before accepting messages from page JS
- **Service worker hardening** — `ServiceWorkerControllerCompat` sets `LOAD_NO_CACHE`, disables content/file access
- **WebView data directory isolation** — `WebView.setDataDirectorySuffix("amnos_session")` isolates storage from other WebView users

### Content security
- **Permissions-Policy header** — injected on document responses: blocks accelerometer, ambient-light-sensor, autoplay, battery, camera, clipboard-read/write, geolocation, gyroscope, magnetometer, microphone, payment, USB, XR
- **Content-Security-Policy** — dynamically built per-request: `default-src https:`, `object-src 'none'`, `base-uri 'none'`, `frame-ancestors 'none'`, `upgrade-insecure-requests`; script/connect sources tightened based on active policy flags
- **X-DNS-Prefetch-Control: off** — injected on all document responses
- **Referrer-Policy: no-referrer** — injected on all responses

### UI security
- **Screenshot protection** — `FLAG_SECURE` applied to the window when `blockScreenshots` policy is active
- **Security Cockpit** — bottom-sheet dashboard with three tabs:
  - **SHIELDS** — toggles for HTTPS-only, tracker blocking, third-party blocking, inline scripts, WebSockets, JavaScript mode, WebGL, fingerprint level, first-party isolation, identity reset on refresh
  - **INSPECTOR** — live request log (last 100 entries) with URL, method, type, disposition (ALLOWED / BLOCKED / PASSTHROUGH), third-party flag, and block reason
  - **IDENTITY** — session ID, fingerprint level, proxy status, DoH status, WebRTC status, WebSocket status, attempt counters, active connections
- **Tracker badge** — live counter of blocked trackers in the browser toolbar
- **Burn session button** — animated kill-switch button with burn overlay animation
- **Internal diagnostics log** — last 100 internal log entries surfaced in the dashboard

---

## Core modules

- `core/adblock`
  Dual-list domain and pattern-based ad/tracker blocker with whitelist support.
- `core/network`
  Request classification, URL sanitization, navigation resolution, HTTPS-only enforcement, DoH, loopback proxy, security header injection, and CSP generation.
- `core/session`
  Tab lifecycle, GHOST wipe flow, volatile downloads, clipboard sentinel, session timeout, internal diagnostics, and loopback proxy orchestration.
- `core/webview`
  Hardened `SecureWebView` settings, document-start script injection, security bridge, privacy WebView client, and Chrome client.
- `core/fingerprint`
  Deterministic device-profile generation, device template registry, locale presets, and document-start API spoofing script wrapper.
- `core/security`
  Privacy policy model, permission sentinel, clipboard sentinel, root detector.
- `core/service`
  Ephemeral download management and global WebView storage purge.
- `ui`
  Compose browser shell, Security Cockpit (Shields / Inspector / Identity tabs), Ghost Keyboard, tracker badge, burn overlay.

---

## Validation commands

Windows:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleRelease
```

Project verification script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify_build.ps1
```

---

## Documentation map

- [ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [VALIDATION.md](docs/VALIDATION.md)
- [docs/PRODUCTION_READINESS.md](docs/PRODUCTION_READINESS.md)
- [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md)
- [docs/FINGERPRINT_PROTECTION.md](docs/FINGERPRINT_PROTECTION.md)
- [docs/AD_TRACKER_BLOCKING_SYSTEM.md](docs/AD_TRACKER_BLOCKING_SYSTEM.md)
- [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md)

---

## Honest limits

- Some sites will break by design because Amnos disables cookies, strips tracking state, and blocks invasive browser capabilities.
- Real leak validation still requires device or emulator testing against live WebRTC, DNS, and fingerprint test sites.
- WebView and Android system behavior still define the ultimate trust boundary.
- Amnos does not provide network-layer anonymity — it is not Tor and does not route traffic through an anonymizing network.
- DoH protects DNS from local observers but does not hide DNS queries from the upstream resolver (Cloudflare).

---

<!-- 
KEYWORDS: privacy browser, hardened android, security browser, anti-fingerprinting, webrtc protection, tracker blocking, ephemeral browser, kotlin, android webview, security dashboard, private search, anon browsing, dns over https, ghost keyboard, canvas fingerprinting, audio fingerprinting, webgl spoofing, loopback proxy, session isolation, clipboard wipe.
-->

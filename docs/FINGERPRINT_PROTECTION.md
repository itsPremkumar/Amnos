# Amnos Fingerprinting Protection - Complete Coverage

This document lists all browser fingerprinting vectors that Amnos protects against.

## ✅ Navigator API Spoofing

| Property | Fake Value | Purpose |
|----------|-----------|---------|
| `navigator.userAgent` | Fake device UA (Pixel 8, Samsung S23, etc.) | Hide real device model |
| `navigator.appVersion` | Derived from fake UA | Consistency with UA |
| `navigator.vendor` | "Google Inc." | Match Chrome/WebView |
| `navigator.vendorSub` | "" | Standard Chrome value |
| `navigator.productSub` | "20030107" | Standard Chrome value |
| `navigator.platform` | "Linux armv8l" | Fake OS platform |
| `navigator.oscpu` | undefined | Block Firefox-specific leak |
| `navigator.buildID` | undefined | Block Firefox build ID |
| `navigator.language` | Fake locale (en-US, de-DE, etc.) | Hide real language |
| `navigator.languages` | Fake locale array | Hide language preferences |
| `navigator.hardwareConcurrency` | 8 | Fake CPU core count |
| `navigator.deviceMemory` | 8 | Fake RAM (8GB) |
| `navigator.maxTouchPoints` | 5 | Fake touch capability |
| `navigator.webdriver` | false | Hide automation detection |
| `navigator.doNotTrack` | "1" | Privacy signal |
| `navigator.globalPrivacyControl` | true | Privacy signal (GPC) |
| `navigator.plugins` | [] | Empty plugin list |
| `navigator.mimeTypes` | [] | Empty MIME type list |

## ✅ Screen & Window Spoofing

| Property | Fake Value | Purpose |
|----------|-----------|---------|
| `screen.width` | Fake resolution (412, 384, 393) | Hide real screen size |
| `screen.height` | Fake resolution (915, 854, 873) | Hide real screen size |
| `screen.availWidth` | Fake available width | Hide real viewport |
| `screen.availHeight` | Fake available height | Hide real viewport |
| `screen.colorDepth` | 24 | Standard color depth |
| `screen.pixelDepth` | 24 | Standard pixel depth |
| `window.devicePixelRatio` | Fake DPR (2.625, 3.0, 2.75) | Hide real pixel density |
| `window.outerWidth` | Matches fake screen width | Consistent window size |
| `window.outerHeight` | Matches fake screen height | Consistent window size |
| `window.innerWidth` | Matches fake availWidth | Consistent viewport |
| `window.innerHeight` | Matches fake availHeight | Consistent viewport |
| `window.screenX/Y` | 0 | Hide window position |
| `window.screenLeft/Top` | 0 | Hide window position |
| `screen.orientation.type` | "portrait-primary" | Fake orientation |
| `screen.orientation.angle` | 0 | Fake rotation angle |

## ✅ Timezone & Timing Spoofing

| Property | Fake Value | Purpose |
|----------|-----------|---------|
| `Intl.DateTimeFormat` timezone | Fake TZ (America/New_York, UTC, etc.) | Hide real timezone |
| `Date.prototype.getTimezoneOffset()` | Fake offset (300, 0, -60 minutes) | Match fake timezone |
| `Date.now()` | Quantized + jittered | Reduce timing precision |
| `performance.now()` | Quantized + jittered | Reduce timing precision |
| `requestAnimationFrame` timestamp | Quantized + jittered | Reduce timing precision |

**Timing Resolution:** 16ms quantization + optional jitter

## ✅ Graphics & Canvas Spoofing

| API | Protection | Purpose |
|-----|-----------|---------|
| WebGL `VENDOR` (0x9245) | Fake GPU vendor (Qualcomm, ARM, Google) | Hide real GPU |
| WebGL `RENDERER` (0x9246) | Fake GPU renderer (Adreno 740, Mali-G710) | Hide real GPU model |
| WebGL2 parameters | Same as WebGL1 | Consistent GPU spoofing |
| `canvas.getImageData()` | XOR noise injection | Prevent canvas fingerprinting |
| `canvas.toDataURL()` | Pixel noise injection | Prevent canvas fingerprinting |
| `canvas.measureText()` | Rounded width (STRICT mode) | Reduce font fingerprinting |

**Noise Seed:** Deterministic per-session/tab for consistency

## ✅ Audio Fingerprinting Protection

| API | Protection | Purpose |
|-----|-----------|---------|
| `AudioBuffer.getChannelData()` | Noise injection every 100 samples | Prevent audio fingerprinting |
| `AudioContext.createAnalyser()` | Noise injection in frequency data | Prevent audio fingerprinting |

## ✅ Font Fingerprinting Protection

| API | Protection | Purpose |
|-----|-----------|---------|
| `document.fonts.check()` | Always returns false | Block font enumeration |
| `document.fonts.load()` | Returns empty array | Block font loading detection |
| `FontFace` constructor | Disabled (STRICT mode) | Block font API |
| CSS font-family | Forced to sans-serif/Arial | Reduce font variation |

## ✅ Hardware Sensor Blocking

| API | Protection | Purpose |
|-----|-----------|---------|
| `navigator.getBattery()` | Fake: 76% charge, always charging | Hide real battery status |
| `navigator.connection` | Fake: 4G, 10Mbps, 50ms RTT | Hide real network info |
| `navigator.getGamepads()` | Empty array | Block gamepad enumeration |
| `DeviceMotionEvent` | Disabled | Block accelerometer |
| `DeviceOrientationEvent` | Disabled | Block gyroscope |
| `Accelerometer` | Disabled | Block sensor API |
| `Gyroscope` | Disabled | Block sensor API |
| `Magnetometer` | Disabled | Block sensor API |

## ✅ Media & Device APIs

| API | Protection | Purpose |
|-----|-----------|---------|
| `navigator.mediaDevices.getUserMedia()` | Blocked | Prevent camera/mic access |
| `navigator.mediaDevices.enumerateDevices()` | Empty array | Hide device list |
| `navigator.mediaCapabilities.decodingInfo()` | Generic response | Hide codec support |
| `speechSynthesis.getVoices()` | Single voice only | Reduce voice fingerprinting |

## ✅ Keyboard & Input APIs

| API | Protection | Purpose |
|-----|-----------|---------|
| `navigator.keyboard.getLayoutMap()` | Empty Map | Hide keyboard layout |
| `navigator.keyboard.lock()` | Blocked | Prevent keyboard lock |

## ✅ Scheduling & Performance APIs

| API | Protection | Purpose |
|-----|-----------|---------|
| `navigator.scheduling.isInputPending()` | Always false | Hide input timing |

## ✅ Storage & Persistence Blocking

| API | Protection | Purpose |
|-----|-----------|---------|
| `localStorage` | Blocked (throws SecurityError) | No persistent storage |
| `sessionStorage` | Blocked (throws SecurityError) | No session storage |
| `indexedDB` | Disabled | No database storage |
| `openDatabase` | Disabled | No WebSQL |
| `caches` (Cache API) | Blocked | No cache storage |
| `navigator.storage` | Fake: 0 usage, 0 quota | Hide storage info |

## ✅ Permission & Geolocation Blocking

| API | Protection | Purpose |
|-----|-----------|---------|
| `navigator.permissions.query()` | "denied" for sensitive permissions | Block permission probing |
| `navigator.geolocation` | Always returns error | Block location access |
| `navigator.clipboard` | Blocked | Prevent clipboard access |

## ✅ WebRTC Blocking (when enabled)

| API | Protection | Purpose |
|-----|-----------|---------|
| `RTCPeerConnection` | Fake object, always closed | Prevent IP leaks |
| `RTCDataChannel` | Blocked | Prevent data channels |
| `RTCSessionDescription` | Fake empty SDP | Prevent connection |
| `RTCIceCandidate` | Fake empty candidate | Prevent IP discovery |
| `MediaStreamTrack` | Disabled | Block media streams |

## ✅ Service Worker Blocking (when enabled)

| API | Protection | Purpose |
|-----|-----------|---------|
| `navigator.serviceWorker.register()` | Blocked | No persistent workers |
| `navigator.serviceWorker.getRegistration()` | Returns undefined | No worker enumeration |

## ✅ Code Execution Blocking (when enabled)

| API | Protection | Purpose |
|-----|-----------|---------|
| `eval()` | Blocked | Prevent dynamic code |
| `Function()` constructor | Blocked | Prevent dynamic code |
| `WebAssembly.compile()` | Blocked | Prevent WASM execution |
| `WebAssembly.instantiate()` | Blocked | Prevent WASM execution |

## 🎯 Fingerprint Protection Modes

### BALANCED Mode (Recommended)
- Session-consistent identity (same fingerprint per session)
- Different identity per tab
- Realistic device profiles (Pixel 8, Samsung S23, OnePlus)
- Varied timezones (America/New_York, Europe/London, Europe/Berlin, Europe/Paris)
- Blends into crowd of real devices

### STRICT Mode (Maximum Privacy)
- Tab-specific identity (different per tab)
- UTC timezone only
- More aggressive randomization
- Harder to track but potentially more detectable
- Rounded font metrics

## 📊 Fingerprinting Test Results

Test your protection at:
- https://browserleaks.com/canvas
- https://coveryourtracks.eff.org/
- https://amiunique.org/
- https://fingerprintjs.com/demo

**Expected Results:**
- Unique fingerprint per session/tab
- Consistent within session
- No real device information leaked
- Canvas/Audio fingerprints randomized

## 🔧 Configuration

Set in `.env`:
```bash
SECURITY_FINGERPRINT_LEVEL=BALANCED  # or STRICT
```

Rebuild app after changing:
```bash
./gradlew clean assembleDebug
```

## 📝 Notes

- All spoofing happens at **document-start** (before any site JavaScript runs)
- Fake values are **deterministic** (same session = same fingerprint)
- Noise injection uses **session-specific seed** for consistency
- Protection is **always active** - you can only choose the level
- Some advanced fingerprinting (e.g., CSS media queries) may still work but return fake values

## 🚀 Future Enhancements

Potential additions:
- [ ] CSS media query spoofing (matchMedia)
- [ ] Pointer Events API spoofing
- [ ] Vibration API blocking
- [ ] Ambient Light Sensor blocking
- [ ] Proximity Sensor blocking
- [ ] USB/Bluetooth device enumeration blocking
- [ ] MIDI device enumeration blocking

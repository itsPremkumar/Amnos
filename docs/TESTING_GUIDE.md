# Amnos Fingerprint Protection Testing Guide

## 🧪 How to Test Your Fingerprint Protection

Since I cannot directly run the Android app, here's a comprehensive guide for you to test all protections yourself.

---

## 📋 Quick Test Checklist

### Method 1: Local Test Page (Recommended)

1. **Build and install the app:**
   ```bash
   cd c:\one\Amnos
   ./gradlew clean assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Copy test file to device:**
   ```bash
   adb push test-fingerprint.html /sdcard/Download/
   ```

3. **Open in Amnos:**
   - Launch Amnos browser
   - Navigate to: `file:///sdcard/Download/test-fingerprint.html`
   - Tests will run automatically
   - Check the results

4. **Expected Results:**
   - ✅ Protection Score: 85-95%
   - ✅ Most tests should show "PASS"
   - ⚠️ Some "WARN" is acceptable (device-specific)
   - ❌ "FAIL" means protection not working

---

### Method 2: Online Testing Sites

#### Test 1: EFF Cover Your Tracks
**URL:** https://coveryourtracks.eff.org/

**What to check:**
- ✅ "Your browser has a **randomized** fingerprint"
- ✅ Canvas fingerprint should be unique per session
- ✅ WebGL fingerprint should be spoofed
- ✅ Font fingerprint should be minimal

**Expected Score:** 
- Unique bits: 8-12 (lower is better)
- Tracking protection: Strong

---

#### Test 2: BrowserLeaks Canvas Test
**URL:** https://browserleaks.com/canvas

**What to check:**
- ✅ Canvas hash changes on each page reload
- ✅ "Noise detected" or "Modified canvas"
- ✅ Different hash than your real device

**How to test:**
1. Open the page
2. Note the canvas hash
3. Refresh the page (F5)
4. Hash should be DIFFERENT (proves noise injection works)

---

#### Test 3: AmIUnique
**URL:** https://amiunique.org/

**What to check:**
- ✅ Fingerprint should be "Not unique" or "Partially unique"
- ✅ Canvas fingerprint: Different from real device
- ✅ WebGL fingerprint: Spoofed GPU
- ✅ Fonts: Minimal list
- ✅ Plugins: Empty
- ✅ Timezone: Spoofed

**Expected Results:**
- Uniqueness: 1 in 100 - 1 in 1000 (not 1 in millions)
- Similar to other users

---

#### Test 4: Audio Fingerprint Test
**URL:** https://audiofingerprint.openwpm.com/

**What to check:**
- ✅ Audio fingerprint changes on reload
- ✅ Different from real device
- ✅ Noise injection detected

**How to test:**
1. Run the test
2. Note the audio hash
3. Refresh and run again
4. Hash should be DIFFERENT

---

#### Test 5: FingerprintJS Demo
**URL:** https://fingerprintjs.com/demo

**What to check:**
- ✅ Visitor ID changes on each session
- ✅ Canvas: Spoofed
- ✅ WebGL: Spoofed
- ✅ Audio: Spoofed
- ✅ Fonts: Limited
- ✅ Screen: Spoofed

---

## 🔍 Detailed Manual Testing

### Test Navigator API Spoofing

Open browser console (if available) or use the test page:

```javascript
// Test 1: User Agent
console.log('User-Agent:', navigator.userAgent);
// Expected: Should show fake device (Pixel 8, Samsung S23, etc.)

// Test 2: Hardware
console.log('CPU Cores:', navigator.hardwareConcurrency);
// Expected: 8

console.log('Memory:', navigator.deviceMemory);
// Expected: 8

console.log('Max Touch Points:', navigator.maxTouchPoints);
// Expected: 5

// Test 3: Vendor
console.log('Vendor:', navigator.vendor);
// Expected: "Google Inc."

// Test 4: Platform
console.log('Platform:', navigator.platform);
// Expected: "Linux armv8l"

// Test 5: Plugins
console.log('Plugins:', navigator.plugins.length);
// Expected: 0

// Test 6: WebDriver
console.log('WebDriver:', navigator.webdriver);
// Expected: false

// Test 7: Do Not Track
console.log('DNT:', navigator.doNotTrack);
// Expected: "1"
```

---

### Test Screen Spoofing

```javascript
console.log('Screen Width:', screen.width);
console.log('Screen Height:', screen.height);
console.log('Color Depth:', screen.colorDepth);
console.log('Pixel Ratio:', window.devicePixelRatio);
// Expected: Should NOT match your real device screen
```

---

### Test Canvas Fingerprinting Protection

```javascript
// Create canvas
const canvas = document.createElement('canvas');
canvas.width = 200;
canvas.height = 50;
const ctx = canvas.getContext('2d');

// Draw something
ctx.fillStyle = '#f60';
ctx.fillRect(0, 0, 200, 50);
ctx.fillStyle = '#069';
ctx.font = '14px Arial';
ctx.fillText('Test', 10, 25);

// Get fingerprint
const hash1 = canvas.toDataURL();
console.log('Canvas Hash 1:', hash1.substring(0, 50));

// Reload page and run again - hash should be SAME within session
// But DIFFERENT across sessions
```

---

### Test WebGL Fingerprinting Protection

```javascript
const canvas = document.createElement('canvas');
const gl = canvas.getContext('webgl');

if (gl) {
    const vendor = gl.getParameter(gl.VENDOR);
    const renderer = gl.getParameter(gl.RENDERER);
    
    console.log('WebGL Vendor:', vendor);
    console.log('WebGL Renderer:', renderer);
    // Expected: Fake GPU (Qualcomm, ARM, Google Inc.)
    // Should NOT show your real GPU
}
```

---

### Test Audio Fingerprinting Protection

```javascript
const AudioCtx = window.AudioContext || window.webkitAudioContext;
const ctx = new AudioCtx();

console.log('Sample Rate:', ctx.sampleRate);
console.log('Base Latency:', ctx.baseLatency);
console.log('Output Latency:', ctx.outputLatency);
// Expected: baseLatency = 0.01, outputLatency = 0.02

ctx.close();
```

---

### Test Storage Blocking

```javascript
// Test localStorage
try {
    localStorage.setItem('test', 'value');
    console.log('localStorage: FAIL - Should be blocked');
} catch (e) {
    console.log('localStorage: PASS - Blocked');
}

// Test sessionStorage
try {
    sessionStorage.setItem('test', 'value');
    console.log('sessionStorage: FAIL - Should be blocked');
} catch (e) {
    console.log('sessionStorage: PASS - Blocked');
}

// Test indexedDB
console.log('indexedDB:', window.indexedDB === undefined ? 'PASS - Blocked' : 'FAIL - Available');
```

---

### Test Sensor APIs

```javascript
console.log('DeviceMotionEvent:', typeof DeviceMotionEvent);
// Expected: "undefined"

console.log('DeviceOrientationEvent:', typeof DeviceOrientationEvent);
// Expected: "undefined"

console.log('Accelerometer:', typeof Accelerometer);
// Expected: "undefined"

console.log('Gyroscope:', typeof Gyroscope);
// Expected: "undefined"

console.log('AmbientLightSensor:', typeof AmbientLightSensor);
// Expected: "undefined"
```

---

### Test Device Enumeration

```javascript
// Test USB
if (navigator.usb) {
    navigator.usb.getDevices().then(devices => {
        console.log('USB Devices:', devices.length);
        // Expected: 0
    });
}

// Test Bluetooth
if (navigator.bluetooth) {
    navigator.bluetooth.getAvailability().then(available => {
        console.log('Bluetooth Available:', available);
        // Expected: false
    });
}

// Test Media Devices
if (navigator.mediaDevices) {
    navigator.mediaDevices.enumerateDevices().then(devices => {
        console.log('Media Devices:', devices.length);
        // Expected: 0
    });
}
```

---

### Test Timing APIs

```javascript
// Test Date.now() quantization
const times = [];
for (let i = 0; i < 10; i++) {
    times.push(Date.now());
}
console.log('Date.now() samples:', times);
// Expected: Should see quantization (values rounded to 16ms)

// Test performance.now() quantization
const perfTimes = [];
for (let i = 0; i < 10; i++) {
    perfTimes.push(performance.now());
}
console.log('performance.now() samples:', perfTimes);
// Expected: Should see quantization

// Test timezone
console.log('Timezone Offset:', new Date().getTimezoneOffset());
// Expected: Should be spoofed (not your real timezone)
```

---

### Test Performance API Blocking

```javascript
console.log('Performance Entries:', performance.getEntries().length);
// Expected: 0

console.log('Performance Entries by Type:', performance.getEntriesByType('resource').length);
// Expected: 0

if (performance.memory) {
    console.log('JS Heap Size:', performance.memory.jsHeapSizeLimit);
    // Expected: 2172649472 (fake value)
}
```

---

## 📊 Expected Test Results Summary

| Category | Expected Pass Rate | Critical Tests |
|----------|-------------------|----------------|
| Navigator API | 95-100% | userAgent, vendor, platform, hardwareConcurrency |
| Screen API | 100% | width, height, colorDepth, devicePixelRatio |
| Canvas Fingerprinting | 100% | toDataURL noise, getImageData noise |
| WebGL Fingerprinting | 100% | VENDOR, RENDERER spoofing |
| Audio Fingerprinting | 100% | baseLatency, outputLatency, noise injection |
| Storage APIs | 100% | localStorage, sessionStorage, indexedDB blocked |
| Sensor APIs | 100% | All sensors disabled |
| Device Enumeration | 100% | USB, Bluetooth, HID, Serial blocked |
| Timing APIs | 90-100% | Quantization working |
| Performance APIs | 100% | getEntries() returns empty |

---

## 🐛 Troubleshooting

### If tests are failing:

1. **Check if app was rebuilt:**
   ```bash
   ./gradlew clean assembleDebug
   ```

2. **Verify .env configuration:**
   ```bash
   SECURITY_FINGERPRINT_LEVEL=BALANCED
   ```

3. **Check if JavaScript is enabled:**
   - Amnos should have JS enabled by default

4. **Check logs:**
   ```bash
   adb logcat | grep -i amnos
   ```

5. **Try STRICT mode:**
   - Change `.env`: `SECURITY_FINGERPRINT_LEVEL=STRICT`
   - Rebuild app
   - Test again

---

## 📸 Screenshot Evidence

When testing, take screenshots of:
1. ✅ EFF Cover Your Tracks results
2. ✅ BrowserLeaks canvas test (showing different hashes on reload)
3. ✅ AmIUnique results
4. ✅ Local test page results (test-fingerprint.html)

---

## 🎯 Success Criteria

Your fingerprint protection is working if:

- ✅ **Canvas hash changes** between sessions
- ✅ **WebGL shows fake GPU** (not your real GPU)
- ✅ **Audio fingerprint changes** between sessions
- ✅ **Navigator properties are spoofed** (8 cores, 8GB RAM, etc.)
- ✅ **Storage APIs are blocked** (localStorage throws error)
- ✅ **Sensor APIs are disabled** (undefined)
- ✅ **Device enumeration returns empty** (0 devices)
- ✅ **Performance API returns empty** (0 entries)
- ✅ **EFF gives you 8-12 unique bits** (not 18-20)

---

## 📝 Reporting Issues

If tests fail, please report with:
1. Which test failed
2. Expected vs actual result
3. Screenshot of the test
4. Device model and Android version
5. Amnos version
6. `.env` configuration

---

## 🚀 Next Steps

After testing:
1. ✅ Verify all critical tests pass
2. ✅ Test on multiple websites
3. ✅ Compare with other browsers (Chrome, Brave)
4. ✅ Document any failures
5. ✅ Share results with the team

---

**Good luck with testing! 🔒**

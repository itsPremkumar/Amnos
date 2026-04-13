# Quick Test Reference Card

## 🚀 Build & Deploy Commands

```bash
# Clean and build
./gradlew clean assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Push test file to device
adb push test-fingerprint.html /sdcard/Download/

# View logs
adb logcat | grep -i amnos
```

---

## 🌐 Online Test URLs

| Test Site | URL | What It Tests |
|-----------|-----|---------------|
| **EFF Cover Your Tracks** | https://coveryourtracks.eff.org/ | Overall fingerprint uniqueness |
| **BrowserLeaks Canvas** | https://browserleaks.com/canvas | Canvas fingerprinting |
| **BrowserLeaks WebRTC** | https://browserleaks.com/webrtc | WebRTC IP leaks |
| **BrowserLeaks WebGL** | https://browserleaks.com/webgl | WebGL fingerprinting |
| **Audio Fingerprint** | https://audiofingerprint.openwpm.com/ | Audio fingerprinting |
| **AmIUnique** | https://amiunique.org/ | Complete fingerprint analysis |
| **FingerprintJS Demo** | https://fingerprintjs.com/demo | Commercial fingerprinting |
| **Device Info** | https://www.deviceinfo.me/ | Device information leaks |
| **IP Leak Test** | https://ipleak.net/ | IP, DNS, WebRTC leaks |

---

## 🧪 Quick Console Tests

### Test 1: Navigator Spoofing (30 seconds)
```javascript
console.log({
  ua: navigator.userAgent,
  vendor: navigator.vendor,
  platform: navigator.platform,
  cores: navigator.hardwareConcurrency,
  memory: navigator.deviceMemory,
  touch: navigator.maxTouchPoints,
  webdriver: navigator.webdriver,
  plugins: navigator.plugins.length
});
```

**Expected Output:**
```
{
  ua: "Mozilla/5.0 (Linux; Android 14; Pixel 8)...",
  vendor: "Google Inc.",
  platform: "Linux armv8l",
  cores: 8,
  memory: 8,
  touch: 5,
  webdriver: false,
  plugins: 0
}
```

---

### Test 2: Canvas Fingerprint (1 minute)
```javascript
const c = document.createElement('canvas');
c.width = 200; c.height = 50;
const ctx = c.getContext('2d');
ctx.fillStyle = '#f60';
ctx.fillRect(0, 0, 200, 50);
ctx.fillStyle = '#069';
ctx.font = '14px Arial';
ctx.fillText('Test', 10, 25);
console.log('Canvas Hash:', c.toDataURL().substring(0, 50));
// Reload page and run again - should be SAME within session
```

---

### Test 3: WebGL Fingerprint (30 seconds)
```javascript
const c = document.createElement('canvas');
const gl = c.getContext('webgl');
console.log({
  vendor: gl.getParameter(gl.VENDOR),
  renderer: gl.getParameter(gl.RENDERER)
});
```

**Expected Output:**
```
{
  vendor: "Google Inc. (Qualcomm)" or "Qualcomm" or "ARM",
  renderer: "ANGLE (Qualcomm, Adreno 740...)" or "Adreno (TM) 740" or "Mali-G710"
}
```

---

### Test 4: Storage Blocking (30 seconds)
```javascript
try {
  localStorage.setItem('test', 'value');
  console.log('❌ FAIL: localStorage accessible');
} catch (e) {
  console.log('✅ PASS: localStorage blocked');
}

try {
  sessionStorage.setItem('test', 'value');
  console.log('❌ FAIL: sessionStorage accessible');
} catch (e) {
  console.log('✅ PASS: sessionStorage blocked');
}

console.log(window.indexedDB === undefined ? '✅ PASS: indexedDB blocked' : '❌ FAIL: indexedDB accessible');
```

---

### Test 5: Sensor APIs (30 seconds)
```javascript
console.log({
  motion: typeof DeviceMotionEvent,
  orientation: typeof DeviceOrientationEvent,
  accelerometer: typeof Accelerometer,
  gyroscope: typeof Gyroscope,
  light: typeof AmbientLightSensor
});
```

**Expected Output:**
```
{
  motion: "undefined",
  orientation: "undefined",
  accelerometer: "undefined",
  gyroscope: "undefined",
  light: "undefined"
}
```

---

### Test 6: Performance API (30 seconds)
```javascript
console.log({
  entries: performance.getEntries().length,
  resources: performance.getEntriesByType('resource').length,
  navigation: performance.getEntriesByType('navigation').length
});
```

**Expected Output:**
```
{
  entries: 0,
  resources: 0,
  navigation: 0
}
```

---

## ✅ Quick Pass/Fail Checklist

Run these tests in order (5 minutes total):

- [ ] **Navigator API**: `navigator.hardwareConcurrency === 8`
- [ ] **Screen API**: `screen.colorDepth === 24`
- [ ] **Canvas**: Hash changes between sessions
- [ ] **WebGL**: Shows fake GPU (not real GPU)
- [ ] **Audio**: `AudioContext.baseLatency === 0.01`
- [ ] **Storage**: localStorage throws error
- [ ] **Sensors**: All return `undefined`
- [ ] **Devices**: USB/Bluetooth return empty
- [ ] **Performance**: `getEntries().length === 0`
- [ ] **Timing**: Date.now() is quantized

---

## 🎯 Expected Scores

| Test Site | Expected Result |
|-----------|----------------|
| **EFF Cover Your Tracks** | 8-12 unique bits |
| **AmIUnique** | "Not unique" or "Partially unique" |
| **BrowserLeaks Canvas** | Hash changes on reload |
| **Audio Fingerprint** | Hash changes on reload |
| **Local Test Page** | 85-95% pass rate |

---

## 🐛 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Tests failing | Rebuild: `./gradlew clean assembleDebug` |
| Canvas not protected | Check if JS is enabled |
| Storage accessible | Check `.env` file |
| Real GPU showing | Verify FingerprintObfuscator.js injected |
| Blank pages | Set `SECURITY_RELAX_FOR_DEBUG=true` |

---

## 📱 Test on Device

1. Open Amnos browser
2. Navigate to: `file:///sdcard/Download/test-fingerprint.html`
3. Wait for tests to complete (10 seconds)
4. Check protection score (should be 85-95%)
5. Export results (JSON file)

---

## 🔄 Compare with Chrome

Test the same URLs in Chrome to see the difference:

1. Open Chrome on same device
2. Visit https://coveryourtracks.eff.org/
3. Note the unique bits (usually 18-20)
4. Open Amnos
5. Visit same URL
6. Note the unique bits (should be 8-12)
7. **Lower is better!**

---

## 📊 Success Metrics

| Metric | Target | Critical? |
|--------|--------|-----------|
| Protection Score | 85-95% | ✅ Yes |
| Unique Bits (EFF) | 8-12 | ✅ Yes |
| Canvas Protected | Yes | ✅ Yes |
| WebGL Spoofed | Yes | ✅ Yes |
| Storage Blocked | Yes | ✅ Yes |
| Sensors Disabled | Yes | ⚠️ Medium |
| Performance Blocked | Yes | ⚠️ Medium |

---

## 🎓 Understanding Results

### Good Results:
- ✅ Protection score > 85%
- ✅ Unique bits < 12
- ✅ Canvas hash changes between sessions
- ✅ WebGL shows fake GPU
- ✅ Storage APIs throw errors

### Bad Results:
- ❌ Protection score < 70%
- ❌ Unique bits > 16
- ❌ Canvas hash never changes
- ❌ WebGL shows real GPU
- ❌ Storage APIs work normally

---

## 📞 Need Help?

If tests are failing:
1. Check `TESTING_GUIDE.md` for detailed instructions
2. Review `ADVANCED_FINGERPRINT_PROTECTION.md` for what should be protected
3. Check logs: `adb logcat | grep -i amnos`
4. Report issue with test results

---

**Quick Reference Version 1.0**
**Last Updated:** After deep analysis implementation

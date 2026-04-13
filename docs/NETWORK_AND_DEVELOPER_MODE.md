# Amnos Network & Developer Mode Guide

## 🌐 Network Connectivity: How Does Amnos Work?

### ✅ **YES - Amnos Works on ALL Network Types:**

Amnos browser works on **ANY** internet connection:

#### 1. **WiFi** ✅ WORKS
```
Your Phone → WiFi Router → Internet → Websites
```
- ✅ Home WiFi
- ✅ Office WiFi
- ✅ Public WiFi (Starbucks, Airport, etc.)
- ✅ Mobile Hotspot (from another phone)
- ✅ Any WiFi network

**Privacy Level:** Same protection on all WiFi networks

---

#### 2. **Mobile Data (4G/5G/LTE)** ✅ WORKS
```
Your Phone → Cell Tower → Mobile Network → Internet → Websites
```
- ✅ 4G LTE
- ✅ 5G
- ✅ 3G (slower but works)
- ✅ Any mobile carrier (Verizon, AT&T, T-Mobile, etc.)

**Privacy Level:** Same protection on mobile data

---

#### 3. **Ethernet (via USB adapter)** ✅ WORKS
```
Your Phone → USB-C to Ethernet adapter → Wired Network → Internet
```
- ✅ Works if your phone supports USB Ethernet adapters
- ✅ Rare but possible on some Android devices

---

#### 4. **VPN Connection** ✅ WORKS (Recommended!)
```
Your Phone → VPN → Internet → Websites
```
- ✅ Works with ANY VPN app (NordVPN, ExpressVPN, ProtonVPN, etc.)
- ✅ **RECOMMENDED** for maximum privacy
- ✅ Amnos + VPN = Best privacy combination

**How to use:**
1. Connect to VPN first
2. Open Amnos browser
3. Browse normally
4. VPN hides your IP, Amnos hides your fingerprint

---

#### 5. **Tor Network** ✅ WORKS (Maximum Privacy!)
```
Your Phone → Orbot (Tor) → Tor Network → Internet → Websites
```
- ✅ Works with Orbot app (Tor for Android)
- ✅ **MAXIMUM PRIVACY** setup
- ✅ Amnos + Tor = Near-anonymous browsing

**How to use:**
1. Install Orbot app
2. Enable "VPN Mode" in Orbot
3. Open Amnos browser
4. Browse anonymously

---

### ❌ **NO - Amnos Does NOT Work Offline:**

```
❌ No Internet = No Browsing
```

**Why?**
- Amnos is a **web browser** (needs internet to load websites)
- No offline mode (by design - for privacy)
- No cached pages (everything wiped on exit)

**What happens without internet:**
- App opens normally
- You can type URLs
- But pages won't load (no connection)
- Shows "No internet connection" error

---

## 🔧 Developer Mode: Required or Not?

### ❌ **NO - Developer Mode is NOT Required**

Amnos works perfectly **WITHOUT** enabling Developer Mode on your Android device.

#### **For Normal Users (99% of people):**
```
✅ Install Amnos
✅ Open and browse
✅ No developer mode needed
✅ No special settings needed
```

**You can use Amnos like any other app - just install and browse!**

---

### ⚙️ **Developer Mode is ONLY for:**

#### 1. **App Developers** (Building the app)
```
Developer Mode → USB Debugging → Install from Android Studio
```
**Who needs this:** People modifying Amnos source code

**Steps:**
1. Settings → About Phone → Tap "Build Number" 7 times
2. Settings → Developer Options → Enable "USB Debugging"
3. Connect phone to computer
4. Run: `adb install app-debug.apk`

---

#### 2. **Advanced Testing** (Checking logs)
```
Developer Mode → USB Debugging → View app logs
```
**Who needs this:** People testing fingerprint protection

**Steps:**
1. Enable USB Debugging (as above)
2. Connect phone to computer
3. Run: `adb logcat | grep -i amnos`
4. See detailed logs

---

#### 3. **Installing Test Builds** (Not from Play Store)
```
Developer Mode → USB Debugging → Install APK manually
```
**Who needs this:** Beta testers, contributors

**Steps:**
1. Enable USB Debugging
2. Run: `adb install amnos.apk`

---

### 📱 **For Normal Installation (No Developer Mode):**

#### Method 1: From APK File (Easiest)
```
1. Download amnos.apk
2. Tap the file
3. Allow "Install from Unknown Sources" (one-time prompt)
4. Install
5. Open and browse
```
**Developer Mode needed?** ❌ NO

---

#### Method 2: From Play Store (Future)
```
1. Open Play Store
2. Search "Amnos Browser"
3. Install
4. Open and browse
```
**Developer Mode needed?** ❌ NO

---

## 🔍 Current Code Analysis

### WebView Debugging Status:

**In the code (MainActivity.kt):**
```kotlin
WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
```

**What this means:**
- ✅ **Debug builds:** WebView debugging ENABLED (for developers)
- ✅ **Release builds:** WebView debugging DISABLED (for users)

**For normal users:**
- No developer mode needed
- No USB debugging needed
- WebView debugging is automatically OFF in production

**For developers:**
- Debug build has WebView debugging ON
- Can inspect web pages using Chrome DevTools
- Useful for testing fingerprint protection

---

## 🎯 Summary Table

| Feature | WiFi | Mobile Data | VPN | Tor | Offline |
|---------|------|-------------|-----|-----|----------|
| **Works?** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Speed** | Fast | Fast | Slower | Slowest | N/A |
| **Privacy** | Good | Good | Better | Best | N/A |
| **Developer Mode Needed?** | ❌ No | ❌ No | ❌ No | ❌ No | N/A |

---

## 📱 Installation Methods

### Method 1: Direct APK Install (No Developer Mode)
```
1. Download amnos.apk
2. Tap to install
3. Allow "Unknown Sources" (one-time)
4. Done!
```
**Developer Mode:** ❌ NOT NEEDED

### Method 2: ADB Install (Requires Developer Mode)
```
1. Enable Developer Options
2. Enable USB Debugging
3. Connect to PC
4. Run: adb install amnos.apk
```
**Developer Mode:** ✅ NEEDED (for this method only)

### Method 3: Play Store (Future - No Developer Mode)
```
1. Open Play Store
2. Search "Amnos"
3. Install
```
**Developer Mode:** ❌ NOT NEEDED

---

## 🔧 Developer Mode: When Do You Need It?

### ❌ **You DON'T Need Developer Mode If:**
- ✅ Installing from APK file (tap to install)
- ✅ Installing from Play Store
- ✅ Just browsing normally
- ✅ Using the app as a regular user

### ✅ **You DO Need Developer Mode If:**
- 🔧 Building the app from source code
- 🔧 Installing via ADB (USB debugging)
- 🔧 Viewing app logs (adb logcat)
- 🔧 Testing and debugging
- 🔧 Contributing to development

---

## 🌐 Network Requirements

### Minimum Requirements:
```
✅ Any internet connection (WiFi or Mobile Data)
✅ Android 9.0+ (API 28+)
✅ ~100 MB free RAM
✅ ~20 MB free storage
```

### Recommended Setup:
```
✅ WiFi or 4G/5G connection
✅ VPN enabled (for IP privacy)
✅ Android 10+ (API 29+)
✅ 500 MB free RAM
```

### Maximum Privacy Setup:
```
✅ Tor (Orbot app) + Amnos
✅ VPN + Tor + Amnos (triple layer)
✅ Public WiFi (not your home network)
✅ Android 12+ (latest security patches)
```

---

## ⚡ Network Speed Impact

| Connection Type | Speed | Privacy | Recommended? |
|----------------|-------|---------|--------------|
| **WiFi (no VPN)** | ⚡⚡⚡⚡⚡ Fast | 🔒🔒🔒 Good | ✅ Yes |
| **Mobile Data (no VPN)** | ⚡⚡⚡⚡ Fast | 🔒🔒🔒 Good | ✅ Yes |
| **WiFi + VPN** | ⚡⚡⚡ Medium | 🔒🔒🔒🔒 Better | ✅ Recommended |
| **Mobile Data + VPN** | ⚡⚡⚡ Medium | 🔒🔒🔒🔒 Better | ✅ Recommended |
| **Tor (Orbot)** | ⚡⚡ Slow | 🔒🔒🔒🔒🔒 Best | ✅ Maximum Privacy |
| **VPN + Tor** | ⚡ Very Slow | 🔒🔒🔒🔒🔒 Maximum | ⚠️ Overkill |

---

## 🚀 Quick Start Guide

### For Normal Users (No Developer Mode):
```
1. Download amnos.apk
2. Tap to install
3. Open Amnos
4. Connect to WiFi or Mobile Data
5. Browse!
```

### For Maximum Privacy (No Developer Mode):
```
1. Install Orbot (Tor) from Play Store
2. Enable VPN mode in Orbot
3. Install Amnos
4. Open Amnos
5. Browse anonymously!
```

### For Developers (Developer Mode Required):
```
1. Enable Developer Options (tap Build Number 7x)
2. Enable USB Debugging
3. Connect phone to PC
4. Run: ./gradlew installDebug
5. Test and debug
```

---

## ❓ Common Questions

### Q: Can I use Amnos without WiFi?
**A:** Yes! Works on mobile data (4G/5G/LTE)

### Q: Can I use Amnos offline?
**A:** No. It's a web browser - needs internet to load websites

### Q: Do I need to enable Developer Mode?
**A:** No! Only needed if you're building from source or using ADB

### Q: Can I use Amnos with a VPN?
**A:** Yes! Recommended for maximum privacy

### Q: Can I use Amnos with Tor?
**A:** Yes! Use Orbot app + Amnos for maximum anonymity

### Q: Does Amnos work on public WiFi?
**A:** Yes! Works on any WiFi network

### Q: Will Amnos use my mobile data?
**A:** Yes, if you're not on WiFi. Same as any browser

### Q: Can I use Amnos without internet?
**A:** No. Browser needs internet to load websites

---

## 🎯 Bottom Line

### Network:
- ✅ **Works on:** WiFi, Mobile Data, VPN, Tor, Any internet connection
- ❌ **Does NOT work:** Offline (no internet)

### Developer Mode:
- ❌ **NOT required** for normal use (99% of users)
- ✅ **Only required** for developers building from source or using ADB

### Installation:
- ✅ **Easy:** Just tap APK file to install (no developer mode)
- ✅ **Normal:** Like any other Android app

---

**Amnos works on ANY internet connection. No developer mode needed for normal use!**

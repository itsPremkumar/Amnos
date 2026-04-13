# Amnos Resource Access - Quick Visual Guide

## 📱 What Does Amnos Access From Your Phone?

```
┌─────────────────────────────────────────────────────────────┐
│                    YOUR ANDROID DEVICE                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ✅ AMNOS ACCESSES (Minimal - Required to Function):        │
│  ┌────────────────────────────────────────────────────┐    │
│  │  🌐 Internet Connection        (to browse web)     │    │
│  │  🖥️  Screen/Display            (to show pages)     │    │
│  │  ⚡ CPU/RAM                    (to run app)        │    │
│  │  💾 Temporary Storage          (wiped on exit)     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ❌ AMNOS DOES NOT ACCESS (Blocked/Disabled):               │
│  ┌────────────────────────────────────────────────────┐    │
│  │  📷 Camera                     ❌ BLOCKED           │    │
│  │  🎤 Microphone                 ❌ BLOCKED           │    │
│  │  📍 GPS/Location               ❌ BLOCKED           │    │
│  │  📞 Phone/SMS                  ❌ BLOCKED           │    │
│  │  👥 Contacts                   ❌ BLOCKED           │    │
│  │  📅 Calendar                   ❌ BLOCKED           │    │
│  │  📁 Files/Photos               ❌ BLOCKED           │    │
│  │  🔵 Bluetooth                  ❌ BLOCKED           │    │
│  │  🔌 USB Devices                ❌ BLOCKED           │    │
│  │  📡 Motion Sensors             ❌ BLOCKED           │    │
│  │  💡 Light Sensor               ❌ BLOCKED           │    │
│  │  📏 Proximity Sensor           ❌ BLOCKED           │    │
│  │  🔐 Fingerprint/Biometrics     ❌ BLOCKED           │    │
│  │  📶 NFC                        ❌ BLOCKED           │    │
│  │  📋 Clipboard                  ❌ BLOCKED           │    │
│  │  🔔 Notifications              ❌ BLOCKED           │    │
│  │  🔋 Real Battery Status        ❌ FAKED             │    │
│  │  📊 Real Device Info           ❌ FAKED             │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔒 What Websites See

```
┌─────────────────────────────────────────────────────────────┐
│                    WEBSITE TRYING TO                         │
│                  FINGERPRINT YOUR DEVICE                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  🎭 FAKE INFORMATION (Spoofed):                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Device Model:    Pixel 8 / Samsung S23  (FAKE)   │    │
│  │  CPU Cores:       8 cores                (FAKE)   │    │
│  │  RAM:             8 GB                   (FAKE)   │    │
│  │  Screen:          412x915                (FAKE)   │    │
│  │  GPU:             Adreno 740             (FAKE)   │    │
│  │  Timezone:        America/New_York       (FAKE)   │    │
│  │  Language:        en-US                  (FAKE)   │    │
│  │  Battery:         76%, charging          (FAKE)   │    │
│  │  Network:         4G, WiFi               (FAKE)   │    │
│  │  Canvas:          Noise-injected         (FAKE)   │    │
│  │  Audio:           Noise-injected         (FAKE)   │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ❌ BLOCKED INFORMATION (Denied):                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Camera:          Permission Denied      ❌        │    │
│  │  Microphone:      Permission Denied      ❌        │    │
│  │  Location:        Permission Denied      ❌        │    │
│  │  Sensors:         Not Available          ❌        │    │
│  │  USB Devices:     Empty List             ❌        │    │
│  │  Bluetooth:       Not Available          ❌        │    │
│  │  Storage:         Blocked                ❌        │    │
│  │  Cookies:         Disabled               ❌        │    │
│  │  localStorage:    Blocked                ❌        │    │
│  │  indexedDB:       Blocked                ❌        │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ✅ RESULT: Website CANNOT identify your real device!       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Permission Comparison

### Chrome (Standard Browser):
```
Permissions Requested: 10+
├── ✅ Camera
├── ✅ Microphone
├── ✅ Location
├── ✅ Storage
├── ✅ Contacts
├── ✅ Phone
├── ✅ SMS
├── ✅ Calendar
├── ✅ Bluetooth
└── ✅ Notifications

Real Device Info Leaked: YES
Fingerprinting Possible: YES
Persistent Tracking: YES
```

### Amnos (Privacy Browser):
```
Permissions Requested: 1
└── ✅ Internet (required for browsing)

Real Device Info Leaked: NO (all fake)
Fingerprinting Possible: NO (blocked)
Persistent Tracking: NO (wiped on exit)
```

---

## 💾 Storage Comparison

### Chrome:
```
/data/data/com.android.chrome/
├── Cookies:          ✅ Stored (persistent)
├── localStorage:     ✅ Stored (persistent)
├── indexedDB:        ✅ Stored (persistent)
├── Cache:            ✅ Stored (persistent)
├── History:          ✅ Stored (persistent)
├── Passwords:        ✅ Stored (persistent)
├── Form Data:        ✅ Stored (persistent)
└── Downloads:        ✅ Stored (persistent)

Total: 100+ MB persistent data
Survives: App restart, device reboot
```

### Amnos:
```
/data/data/com.amnos.browser/
├── Cookies:          ❌ Disabled
├── localStorage:     ❌ Blocked
├── indexedDB:        ❌ Blocked
├── Cache:            🗑️ Wiped on exit
├── History:          🗑️ Wiped on exit
├── Passwords:        ❌ Disabled
├── Form Data:        ❌ Disabled
└── Downloads:        🗑️ RAM-only (wiped)

Total: 0 MB persistent data
Survives: NOTHING (all wiped)
```

---

## 🎯 Quick Summary

| What | Chrome | Amnos |
|------|--------|-------|
| **Permissions** | 10+ | 1 (Internet only) |
| **Camera Access** | ✅ Yes | ❌ No |
| **Location Access** | ✅ Yes | ❌ No |
| **Real Device Info** | ✅ Leaked | ❌ Faked |
| **Fingerprinting** | ✅ Possible | ❌ Blocked |
| **Persistent Storage** | ✅ Yes | ❌ No (wiped) |
| **Tracking** | ✅ Yes | ❌ No |
| **Privacy Level** | ⚠️ Low | ✅ Maximum |

---

## 🔐 Bottom Line

**Amnos accesses ONLY what's absolutely necessary:**
- ✅ Internet (to browse)
- ✅ Screen (to display)
- ✅ CPU/RAM (to run)
- ✅ Temporary storage (wiped on exit)

**Everything else is:**
- ❌ BLOCKED (sensors, camera, mic, location)
- 🎭 FAKED (device info, screen, GPU)
- 🗑️ WIPED (storage, cache, history)

**Your real device information NEVER leaves your phone.**

---

**Amnos = Maximum Privacy, Minimal Access**

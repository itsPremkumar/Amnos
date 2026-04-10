# Amnos Validation Guide

Last updated: April 10, 2026

## Current status from this machine

Completed:

- `./gradlew testDebugUnitTest`

Not completed:

- physical device testing
- emulator testing
- live WebRTC leak-site validation
- live DNS leak validation

Environment note:

- `adb` is installed
- no Android devices were attached when this update was performed
- no emulator binary was available on PATH

## Manual real-device checklist

### 1. Build and install

```bash
./gradlew installDebug
adb devices -l
```

### 2. Confirm loopback proxy and DoH status

Open Amnos and verify in the Security Dashboard:

- proxy status is active
- DoH status reports loopback/global coverage
- no active connections remain after returning to home or wiping the session

### 3. WebRTC leak testing

Visit real leak-test pages such as:

- browserleaks WebRTC test
- ipleak WebRTC test

Expected behavior:

- dashboard shows WebRTC as blocked/spoofed
- no real ICE candidates appear
- no public or local IP candidates are exposed

### 4. DNS leak testing

Use a DNS leak test website and confirm:

- observed DNS provider matches the configured DoH path
- system DNS is not visible when loopback proxy support is active

### 5. Fingerprint testing

Use a fingerprint test site and compare:

- two fresh sessions with strict fingerprint protection
- balanced vs strict modes
- refresh identity reset enabled vs disabled

Expected behavior:

- hardware concurrency and device memory stay normalized
- timezone remains consistent in strict mode
- fingerprint values change less erratically and remain internally coherent

### 6. Compatibility testing

Test at least:

- a login-heavy site
- a media streaming site
- a JavaScript-heavy SPA
- a site that tries WebSocket or WebRTC features

Record:

- crashes
- broken layouts
- blocked-media regressions
- login failures tied to strict third-party or inline-script policies

### 7. Session wipe verification

Verify after:

- tab close
- kill switch
- app background
- app relaunch

that:

- cookies do not persist
- downloads are deleted
- clipboard is cleared
- request inspector is empty on a fresh session

## Suggested adb-assisted workflow

Useful commands:

```bash
adb logcat | findstr Amnos
adb shell pm list packages | findstr webview
adb shell dumpsys webviewupdate
```

Capture and record:

- device model
- Android version
- WebView provider package and version
- whether proxy override is reported as active in the dashboard

## Honest validation note

If a device or emulator is not available, Amnos should not claim that live leak testing was completed. In that case, report only JVM/build validation plus the pending manual checklist above.

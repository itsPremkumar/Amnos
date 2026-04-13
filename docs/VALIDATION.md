# Amnos Validation Guide

Last updated: April 12, 2026

## Automated validation

Run from the repository root:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleRelease
```

Or use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify_build.ps1
```

The automated gate covers:

- JVM unit tests for navigation resolution, URL sanitization, network boundary logic, and fingerprint profile determinism
- Android lint for manifest, WebView, and security-config regressions
- release build generation

## Manual device validation

Automated checks are not enough for a privacy browser. Before shipping a release candidate, validate on at least:

- 1 physical Android 14 device
- 1 alternate WebView provider version if available
- 1 emulator image for repeatable smoke tests

## Device checklist

### Install and launch

```powershell
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb devices -l
```

Verify:

- app launches without crash
- initial dashboard loads
- backgrounding the app triggers a wipe and relaunches cleanly

### Navigation and Search Dog validation

Test these inputs in the address bar:

- `duckduck`
- `https://duckduck`
- `google.com`
- `https://example.com/?utm_source=test&id=7`
- `what is the weather`

Verify:

- malformed bare hosts become searches
- direct domains normalize to `https://`
- tracking parameters are stripped before page load
- failed navigations do not flicker a transient malformed URL into the address bar

### Privacy transport validation

In the Security Cockpit, confirm:

- loopback proxy status is active when supported
- DoH status reflects the proxy path
- active connections fall back to zero after wipe

Use live sites to validate:

- DNS leak tests
- WebRTC leak tests
- fingerprint test pages across fresh sessions

### Persistence validation

After browsing, then triggering:

- kill switch
- app background
- app relaunch

Verify:

- cookies do not survive
- volatile downloads are deleted
- diagnostics/request logs reset
- clipboard scrub still occurs

## Release sign-off checklist

- `testDebugUnitTest` passed
- `lintDebug` passed
- `assembleRelease` passed
- physical-device checklist completed
- WebRTC/DNS/fingerprint leak checks recorded
- release keys present and used for signing

## Honest note

If device or emulator validation has not been run, do not claim live privacy validation was completed. Report only the automated checks that actually passed.

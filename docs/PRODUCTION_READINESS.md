# Amnos Production Readiness

## Scope

This document defines the minimum bar for calling an Amnos build production-ready.

## Runtime security requirements

- cleartext traffic disabled
- user CAs trusted only in debug overrides
- cookies disabled in WebView
- `Set-Cookie` stripped from intercepted responses
- volatile downloads stored only in cache-backed ephemeral storage
- backgrounding and kill-switch flows purge session state
- remote debugging disabled in release builds
- relaxed diagnostics disabled in release builds

## Navigation correctness requirements

- Search Dog heuristic classifies search terms and direct URLs deterministically
- explicit-scheme malformed hosts such as `https://duckduck` fall back to search
- tracking parameters are stripped before load
- address-bar state commits only after top-level navigation commit
- failed navigations must not smear transient malformed URLs into the address bar

## Build and release requirements

- `.\gradlew.bat clean testDebugUnitTest lintDebug assembleRelease`
- no release fallback to debug signing
- release artifact reviewed for correct package name and versioning
- real release keystore configured before distribution
- optional release shrinking can be enabled on higher-memory builders with `-Pamnos.release.minify=true`

## Manual release checklist

1. Run the automated gate with `scripts/verify_build.ps1`.
2. Install the debug build on a physical device and complete the validation checklist in `VALIDATION.md`.
3. Verify Security Cockpit counters and internal logs during:
   - direct navigation
   - blocked tracker requests
   - WebRTC/WebSocket attempts
   - background wipe
4. Record the tested Android version, device model, and WebView provider version.
5. Build the final signed release with production keys.

## Non-goals

- claiming Tor-grade anonymity
- claiming device validation when only JVM/static checks were run
- enabling insecure compatibility fallbacks for cookie-dependent sites

# Amnos Forensic Diagnostics Guide (Catlogs)

Amnos keeps diagnostics in RAM by default. Android Logcat mirroring is disabled unless `SECURITY_BLOCK_FORENSIC_LOGGING=false`, so production builds do not emit visited URLs, typed text, or request metadata to system logs.

## Configuration

Diagnostic controls come from `.env` / generated `BuildConfig`:

| Flag | Recommended Debug Value | Effect |
| :--- | :--- | :--- |
| `SECURITY_BLOCK_FORENSIC_LOGGING` | `false` | Enables Logcat mirroring for sensitive runtime diagnostics. Leave `true` for production. |
| `SECURITY_ENFORCE_STRICT_POLICIES` | `false` | Relaxes the security engine to isolate site-breakage root causes. |
| `SECURITY_ENFORCE_LOOPBACK_PROXY` | `true` | Keeps WebView traffic pinned to the loopback proxy / DoH path. |

> [!CAUTION]
> When `SECURITY_BLOCK_FORENSIC_LOGGING=false`, sensitive navigation and console details are mirrored into Android Logcat. Re-enable blocking before shipping.

## Tag Dictionary

Use these tags in Android Studio or `adb logcat`:

### Security
- `Amnos`: fatal crash handoff and top-level runtime guardrails.
- `SecurityEngine`: blocked requests, scheme jail decisions, first/third-party enforcement.
- `FingerprintManager`: session/tab identity generation.
- `FingerprintShield`: JS-reported browser property spoof events.
- `PermissionSentinel`: camera, mic, sensor, media-ID, and other permission denials.

### Networking
- `LoopbackProxyServer`: proxy startup, CONNECT tunnel acceptance/rejection, shutdown.
- `DnsManager`: DoH lookups and resolution failures.
- `NetworkFetcher`: proxied response timing and upstream fetch failures.

### UI / Runtime
- `MainActivity`: bootstrap, lifecycle transitions, Ghost Wipe triggers.
- `SecureWebView`: hardening application, bridge rejections, Ghost Keyboard activity.
- `WebConsole`: JavaScript console messages from loaded pages.
- `StorageService`: WebView storage purge and isolated profile cleanup.

## Internal Catlogs

The Inspector tab remains the source of truth even when Logcat mirroring is disabled:

1. Open the in-app Inspector tab.
2. Use `Requests` for blocked / allowed / passthrough network decisions.
3. Use `Console` for the in-memory Catlogs ring buffer.

The Catlogs buffer is RAM-only and capped at 10,000 entries.

## ADB Workflow

On a development machine with a connected device:

```bash
adb logcat --pid=$(adb shell pidof com.amnos.browser) -v time
```

Recommended filters for this build:

```bash
adb logcat -s Amnos SecurityEngine LoopbackProxyServer DnsManager FingerprintManager WebConsole PermissionSentinel
```

## Site Breakage Triage

When a page looks broken:

1. Check `SecurityEngine` for `BLOCKED` entries and note the resource type.
2. Check `WebConsole` for `[ERROR]` output from the page itself.
3. Confirm whether the loopback proxy is active in the Inspector.
4. Temporarily set `SECURITY_ENFORCE_STRICT_POLICIES=false` only on a debug build to isolate policy-caused regressions.

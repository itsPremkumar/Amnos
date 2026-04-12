# Amnos Diagnostics

## Sources of truth

- `SecurityController.requestLog`
  RAM-only request inspection entries shown in the Security Cockpit.
- `SecurityController.internalLogs`
  Ordered internal diagnostics for navigation, storage, transport, and subsystem failures.
- `SecurityController.activeConnections`
  Live tunnel and WebSocket visibility.

## Navigation trace sequence

Main-frame navigations should emit these tags in order:

1. `[Nav:Navigate]`
2. `[Nav:Transform]`
3. `[Nav:Sanitize]`
4. `[Nav:Load]`
5. `[Nav:Commit]` or `[Nav:Failure]`

If a failure occurs, the committed address bar should remain on the previous committed URL or the pending user-entered intent, not the transient malformed target.

## Common diagnostic events

- `PrivacyWebViewClient`
  SSL errors, resource errors, HTTP errors, and blocked main-frame navigations.
- `SessionManager`
  wipe activity, proxy changes, and security event parsing failures.
- `NetworkSecurityManager`
  navigation normalization, proxied fetch timing, and fallback to system handling.
- `DnsManager`
  DoH lookup activity and resolver failures.

## What to check when debugging

### Search vs URL issues

- confirm `NavigationResolver` transformed the raw input correctly
- verify the sanitized URL is the same one loaded by `SessionManager`
- confirm `[Nav:Commit]` only appears for successful top-level commits

### Blank page or load failure

- inspect `PrivacyWebViewClient` navigation errors and error codes
- check whether the request was blocked by policy, tracker filter, or unsupported scheme
- compare request-log entries against internal logs for the same host

### Transport questions

- verify proxy status in the dashboard
- compare DoH status with active tunnel entries
- inspect whether requests were `ALLOWED`, `BLOCKED`, or `PASSTHROUGH`

## Retention model

Diagnostics are intentionally RAM-only and are cleared during session wipe. They are for live debugging, not long-term telemetry.

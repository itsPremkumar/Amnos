# Security Policy

## Commitment

Amnos prioritizes privacy, storage minimization, and WebView hardening. Reports involving:

- data persistence
- WebRTC leaks
- DNS leaks
- fingerprinting bypasses
- tracker-blocking bypasses
- unsafe external-launch or file-handling behavior

should be treated as high priority.

## Reporting a vulnerability

Do not open a public issue for a security vulnerability.

Please report privately with:

- a clear description of the issue
- Android version and device model
- installed WebView package version
- steps to reproduce
- whether the issue reproduces with fingerprint protection set to `BALANCED` or `STRICT`
- whether loopback proxy support was active in the dashboard

## What Amnos does and does not claim

Amnos aims to provide strong local privacy protections inside Android WebView.

Amnos does **not** claim:

- full anonymity like Tor Browser
- complete control over every Chromium internal network path
- guaranteed prevention of all fingerprinting strategies

If a report demonstrates one of those WebView platform boundaries more clearly, that is still useful and should be documented.

## Out of scope

- attacks requiring physical access to an unlocked device before the wipe occurs
- anonymity claims that depend on Tor-like network routing, which Amnos does not provide
- failures caused by external network services outside the app’s control, unless Amnos handles them unsafely

## Validation expectation

Whenever possible, privacy bugs should be reproduced on:

- a physical Android device
- at least one emulator
- a known leak-test site or repeatable reproduction page

See [VALIDATION.md](/C:/one/browser/VALIDATION.md) for the current validation workflow.

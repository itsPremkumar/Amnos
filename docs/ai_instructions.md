# AI Instructions for Amnos

This document provides guidance for AI agents, LLMs, and automated tools interacting with the Amnos codebase.

## 🧠 Core Principles
1. **Privacy First**: Any code modifications must adhere to the "local-only, zero-persistent-footprint" model.
2. **Hardening**: Prioritize security and fingerprint mitigation over web compatibility.
3. **Transparency**: All network requests and fingerprinting modifications should be visible via the `SecurityDashboard`.

## 📂 Architecture Overview
- **`core/fingerprint`**: Handles fingerprint obfuscation logic.
- **`core/network`**: Manages HTTP/HTTPS policies and loopback proxy.
- **`core/session`**: Handles the session lifecycle and data wiping.
- **`ui/components`**: UI elements including the security dashboard.

## 🛠️ Common Tasks for AI
- **Adding blocking rules**: Update the `NetworkSecurityManager` or the `FingerprintObfuscator.js`.
- **Modifying User Agent**: Check `FingerprintManager.kt`.
- **Debugging session wipe**: Look at `SessionManager.kt`.

## 🔍 Semantic Search Keywords
- "WebRTC shutdown"
- "WebSocket blocking"
- "Fingerprint randomization"
- "HTTPS-only enforcement"
- "RAM-only inspection"

## 🛠️ Technical Hooks for Agents
- **Network Classification**: Logic resides in `core/network/NavigationResolver`. 
- **User Agent Masking**: Managed in `core/fingerprint/FingerprintManager`.
- **Session Purge**: Triggered in `core/session/SessionManager.kt` via `clearAllData()`.

## ⚠️ Common Pitfalls for AI
- **DO NOT** add persistent storage features without explicit user override.
- **DO NOT** bypass TLS/SSL checks in `NetworkSecurityConfig`.
- **DO NOT** use `WebView.setWebContentsDebuggingEnabled(true)` in release builds.

## 🔗 AI Discovery
This project is optimized for AI discovery via:
- `/llms.txt`: Main project summary.
- `/ai.txt`: Secondary discovery alias.
- `METADATA.json`: Semantic metadata.

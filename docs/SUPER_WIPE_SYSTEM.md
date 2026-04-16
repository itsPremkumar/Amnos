# Amnos Elite-Tier "Super Wipe" System

## Overview
A key pillar of the Amnos privacy-first architecture is the ability to reliably, completely, and deterministically eliminate all local traces of a browsing session. The **Super Wipe System** is our anti-forensics orchestration engine that activates when a session expires or when the user engages the Kill Switch. 

Its goal is to guarantee a **true zero-trace browsing environment** by irreversibly destroying data across memory, disk storage, WebView subsystems, and networking layers within the constraints of the Android application sandbox.

## Threat Model & Scope
This system provides maximum anti-forensic protection *within the Android application sandbox*. 
- **Protected Against**: Physical device extraction tools (if the app was wiped before acquisition), sibling malicious apps attempting cross-app data leakage, and malicious sites trying to link previous browsing sessions via persistent identifiers.
- **Out of Scope**: Root-level kernel analysis, hardware-level cold boot attacks on physical memory, or forensic analysis performed *before* the Kill Switch is triggered.

## The 8-Phase Destruction Pipeline
The Super Wipe sequence is strictly orchestrated by the `SuperWipeEngine`, ensuring no race conditions outrun the destruction processes.

### Phase 1: Surgical WebView Teardown
Every active WebView instance undergoes a deterministic takedown to clear rendering paths and subsystem caches:
1. Stop all loading operations and load `about:blank` to instantly clear the rendered screen buffer.
2. Clear navigation history, disk caches, form data, and SSL preferences.
3. Call `WebStorage.getInstance().deleteAllData()` to eliminate localStorage, sessionStorage, and IndexedDB endpoints globally.
4. Purge Cookies attached to the specific WebViews.
5. Pause timers, disassemble views, and physically call `.destroy()`.

### Phase 2: Storage Sanitization 
All disk and clipboard artifacts are hunted down:
- **Cookies**: `CookieManager.removeAllCookies()` is invoked followed immediately by a hard `.flush()` to ensure disk persistence of the deletion block.
- **Clipboard**: The system clipboard is overwritten to clear any copied passwords or identifiers, suppressing system UI leakage on modern Android APIs.
- **Physical Nuke**: The app's base `app_webview` storage directory, along with `cache` and `code_cache` directories, are recursively deleted using native file IO, ensuring raw SQLite databases are destroyed.

### Phase 3: Memory Invalidation
Forensic RAM data wiping:
- `SecurityController.obliterate()` actively overwrites connection states, proxy statuses, tracker block lists, and metadata caches with empty defaults.
- Garbage Context limits are requested to be flushed immediately (`System.gc()` and `System.runFinalization()`).

### Phase 4: Network & Identity Rotation
All connection pools are closed and DNS states are dropped:
1. The OkHttp `ipv4OnlyClient`, `dualStackClient`, and `bootstrapClient` instances are drained of their connection pools using `.evictAll()` and `.dispatcher.cancelAll()`.
2. All clients are dereferenced and nullified to eliminate TLS state reuse.
3. The internal DNS resolver is re-bootstrapped and a completely new, cryptographically secure fingerprint/Session ID identity is generated for the incoming blank slate.

### Phase 5: Service Worker Purge
Since Service Workers are notorious for bypassing simple wipes, the physical deletion sweep in Phase 2 targets `app_webview/Service Worker` trees directly, completely eradicating background web workers or persistent Cache API entities.

### Phase 6: UI Zeroing
The `BrowserViewModel` hooks into the wipe completion stream:
- Triggers `zeroAllUIState()`, overwriting input fields, session labels, passwords, and progression metrics back to empty states.
- Re-secures the keyboard and resets lockdown flags safely.

### Phase 7: Heap Hardening (Best Effort)
A conservative 8MB byte array is allocated and actively zeroed out to overwrite potentially freed but undeleted string memory residing within reusable VM heap slabs. This reduces the footprint of readable forensic strings floating in process memory.

### Phase 8: Process Termination Execution
Depending on the trigger context, the engine resolves with:
- **Soft Kill**: Retains process execution but returns the user to the completely sanitized home dashboard with a fresh Session ID.
- **Hard Kill**: Delays exactly 200 milliseconds to allow async storage I/O deletion calls (like cookie flushing) to comfortably settle against the kernel, after which `android.os.Process.killProcess(android.os.Process.myPid())` is called, destroying the entire memory execution space instantly.

## Architecture Integration
The central orchestration lies in `SuperWipeEngine.kt`, completely decoupled from individual lifecycle loops. A singleton instance in the `SessionManager` routes `killSwitch()` UI interactions, system `onTrimMemory` background triggers, or unexpected fatal crashes directly into the pipeline ensuring Amnos safely disarms in all volatile scenarios.

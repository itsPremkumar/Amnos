# Amnos Multi-Layer Encryption Architecture

## 1. Encryption Placement & Threat Modeling
In a "Pure RAM + Super Wipe" context, the goal of encryption is to secure data in transit, protect configuration data at rest, and render any unavoidable disk-spill unreadable.

| Layer | Why Encryption is Needed | Data Protected | Threat Mitigated |
| :--- | :--- | :--- | :--- |
| **Network (TLS)** | Data in transit over hostile networks | HTTP payloads, headers, auth tokens | ISP snooping, Rogue Wi-Fi, MitM attacks |
| **DNS (DoH)** | Resolve domains without plaintext leakage | Hostnames (e.g., `bank.com`) | Network surveillance, DNS hijacking |
| **App Storage** | Protect app-managed internal disk writes | Settings, local bookmarks, custom lists | Offline forensic extraction (Cellebrite/GrayKey) |
| **Session State** | Obfuscate raw tokens in memory dumps | Session IDs, anti-fingerprinting seeds | Advanced heap/RAM analysis |
| **Integrity (Anti-Debug)**| Prevent runtime memory analysis/hooking | App execution logic, key material | Debugger-assisted memory dumping |

> **What NOT to Encrypt:**
> *   **WebView Core Memory/DOM:** Attempting to encrypt live DOM or WebView memory buffers introduces massive latency, breaks V8/JNI integration, and provides no real benefit against our threat model (which relies on Android Process Isolation).
> *   **System Clipboard:** Encrypting copied text breaks OS-wide paste functionality. Instead, we use ephemeral transit and auto-clearing via Super Wipe Phase 2.
> *   **WebView Auto-Generated Files:** Android `WebView` makes uncontrollable disk writes (e.g., rendering caches). We cannot natively encrypt these on-the-fly without a custom Chromium build. **Solution:** Disable WebView caching (`LOAD_NO_CACHE`) and rely entirely on **Super Wipe physical shredding** to destroy the raw files.

---

## 2. Dynamic multi-layer encryption System

We intentionally avoid stacking algorithms. Each layer uses the most performant, secure primitive for its specific operational domain.

*   **Network & DNS Pipeline:** `TLS 1.3`
    *   **Data Cipher:** `ChaCha20-Poly1305` (Preferred on ARM devices without hardware AES acceleration) or `AES-256-GCM` (If hardware accelerated).
    *   **Key Exchange:** `X25519` (Elliptic Curve Diffie-Hellman Ephemeral).
*   **Local App Storage (Settings/Downloads):** `AES-256-GCM` via Android Jetpack Security.
*   **Session Identity Obfuscation:** `HMAC-SHA256`.
*   **Crash Payload Wrap (If exported):** `ECIES` (Elliptic Curve Integrated Encryption Scheme) using a pre-embedded public key.

---

## 3. Dynamic Key Generation Strategy

Amnos strictly follows a **Zero Key Reuse** and **Ephemeral Origin** policy.

1.  **Session Scope:** A new `SessionMasterKey` is generated asynchronously every time the app launches from a cold start, or immediately following a Super Wipe.
2.  **Hardware Binding:** Keys are strictly generated within the Android Keystore system. We enforce `setIsStrongBoxBacked(true)` where available, keeping key material isolated in the Trusted Execution Environment (TEE).
3.  **Secure Randomness:** Driven by `java.security.SecureRandom` using `/dev/urandom`.
4. **Zero-Trace Destruction:** When the Kill Switch is pulled, the key alias is instantly deleted from the Keystore. Any encrypted files on disk immediately become mathematically impossible to decrypt, even before the physical wipe (Phase 2) reaches them.
5.  **Anti-Debugger Trap:** The encryption engine is gated by a runtime integrity check. If a debugger is detected (`isDebuggerConnected()`), the system triggers a `SuperWipe` and obliterates the Master Key immediately.

---

## 4. Key Lifecycle Management

*   **1. Generation (Creation):** Managed by the `SessionManager` interacting with the Keystore API immediately upon a fresh boot (post-wipe).
*   **2. Scope (Access):** Only the `StorageService` can request decryption/encryption ciphers from the Keystore. Network keys remain strictly scoped within OkHttp's runtime memory pool (managed by Conscrypt/BoringSSL).
*   **3. Rotation (Turnover):** Network/DNS keys rotate automatically per TLS 1.3 standards. `SessionMasterKey` is structurally tied 1:1 to the user's current session. It does not rotate *during* a session to avoid race conditions, but is completely discarded and rebuilt upon every reset.
*   **4. Destruction (End-of-life):** Intercepted via `SuperWipeEngine`. Driven by `KeyStore.deleteEntry(SESSION_KEY_ALIAS)` ensuring hardware-level repudiation.

---

## 5. Architectural Map

| Component | Encryption Role | Chosen Algorithm | Key Type / Location |
| :--- | :--- | :--- | :--- |
| **OkHttp / DnsManager** | Secure Transport | `TLS 1.3 (ChaCha20-Poly1305)` | Ephemeral (`x25519`), In-Memory Context |
| **StorageService** | App-Managed Files | `AES-256-GCM` | Symmetric (`SessionMasterKey`), Keystore |
| **SessionManager** | Identity Wrapping | `HMAC-SHA256` | Symmetric (`SessionMasterKey`), Keystore |
| **CrashReporter** | Dump Obfuscation | `ECIES (Secp256r1)` | Asymmetric (Public PKCS#8), Embedded |

---

## 6. System Integration & Data Flow

### Data Flow Model:
1.  **User Input:** User enters URL -> *Plaintext (Live UI Memory).*
2.  **WebView Routing:** Request spawned inside Chromium engine -> *Memory.*
3.  **Network Resolution:** Request intercepted or natively handled -> *Encrypted DoH + TLS 1.3.*
4.  **Data Fetch:** Resources downloaded -> *Decrypted in memory pipeline.*
5.  **Storage Spill (If any):** Cached fragments hit `app_webview/` -> *Relies on FBE + Super Wipe Shredding.* Custom app settings hit `EncryptedSharedPreferences` -> *Encrypted by SessionMasterKey (AES-256-GCM).*
6.  **Kill Switch (Wipe):** Super Wipe activated -> *Keystore Key Destroyed -> Network Pools Evicted -> Memory Obliterated -> Disk storage nuked.*

### Component Integrations:
*   **Super Wipe Engine:** Hooked into Phase 1 (Keystore Disarmament) and Phase 2 (Disk Deletion). Wiping the Keystore key *first* gives an instant cryptographic kill switch while the physical disk wiping continues asynchronously.
*   **WebView Lifecycle:** Prevented from keeping data by injecting configuration flags (`setCacheMode(LOAD_NO_CACHE)`, disabled DOM storage) allowing us to mostly offset encryption needs for WebView components to pure RAM.
*   **DnsManager:** Fed into OkHttp using a custom `Dns` interface leveraging a built-in standard DoH provider to bypass local DNS proxy leaks.
*   **StorageService:** Standardized `EncryptedSharedPreferences` wrapper utilizing the Keystore.

---

## 7. Android Practical Implementation Practices

*   **Avoid String Abuse:** Sensitive data and keys must be handled as `char[]` or `byte[]` natively so they can be explicitly overwritten via `Arrays.fill(buffer, (byte) 0)`. Strings in Java are immutable and linger in the Garbage Collector pool.
*   **Memory Paging:** Android doesn't traditionally swap to disk (like Linux Swap), but devices using zRAM compress memory in RAM. While zRAM doesn't survive a reboot, keeping memory clean (Super Wipe Phase 3) acts as our primary defense.
*   **Log Stripping:** Release builds must use R8 rules (`-assumenosideeffects class android.util.Log { *; }`) to entirely strip `Log.*` statements at compile time to prevent plaintext state leaks.

---

## 8. Pseudocode Examples

### 1. Cryptographic Kill Switch (Key Destruction)
```kotlin
fun obliterateKeystore() {
    try {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // The moment this completes, all local AES-GCM data becomes cryptographically shredded.
        keyStore.deleteEntry("Amnos_Session_Master_Key")
    } catch (e: Exception) {
        // Silently fail but continue to physical wipe
    }
}
```

### 2. Session Key Generation (Hardware Backed)
```kotlin
fun generateSessionKey() {
    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
    )
    val specBuilder = KeyGenParameterSpec.Builder(
        "Amnos_Session_Master_Key",
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        
    // Enforce hardware protection if available
    val hasStrongBox = packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    if (hasStrongBox) { specBuilder.setIsStrongBoxBacked(true) }

    keyGenerator.init(specBuilder.build())
    keyGenerator.generateKey()
}
```

### 3. Ephemeral ByteArray Encryption & Sanitization
```kotlin
fun encryptAndSanitize(plaintextData: ByteArray): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val secretKey = keyStore.getKey("Amnos_Session_Master_Key", null) as javax.crypto.SecretKey
    val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
    
    val ciphertext = cipher.doFinal(plaintextData)
    
    // Forensic-grade scrubbing: Scramble with noise before zeroing
    val secureRandom = java.security.SecureRandom()
    for (i in plaintextData.indices) {
        plaintextData[i] = secureRandom.nextInt(256).toByte()
    }
    plaintextData.fill(0.toByte())
    
    return ciphertext
}
```

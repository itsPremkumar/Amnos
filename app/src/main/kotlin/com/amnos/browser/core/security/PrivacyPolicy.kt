package com.amnos.browser.core.security

enum class JavaScriptMode {
    FULL,
    RESTRICTED,
    DISABLED
}

enum class WebGlMode {
    SPOOF,
    DISABLED
}

enum class FingerprintProtectionLevel {
    BALANCED,
    STRICT,
    DISABLED
}
enum class AmnosSandboxMode {
    PARANOID,
    BALANCED,
    OPEN
}

data class PrivacyPolicy(
    val sandboxMode: AmnosSandboxMode = when (com.amnos.browser.BuildConfig.SECURITY_SANDBOX_MODE.uppercase()) {
        "PARANOID" -> AmnosSandboxMode.PARANOID
        "BALANCED" -> AmnosSandboxMode.BALANCED
        "OPEN" -> AmnosSandboxMode.OPEN
        else -> AmnosSandboxMode.PARANOID
    },
    // MASTER DEBUG TOGGLE
    val forceRelaxSecurityForDebug: Boolean = !com.amnos.browser.BuildConfig.SECURITY_LOCKDOWN_MODE,
    
    // UI SECURITY
    val blockScreenshots: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_SCREENSHOTS,
    
    // V2 SECURITY FEATURES
    val antiDebuggerEnabled: Boolean = com.amnos.browser.BuildConfig.SECURITY_LOCKDOWN_MODE && com.amnos.browser.BuildConfig.SECURITY_ANTI_DEBUGGER,
    val absoluteCloakingEnabled: Boolean = com.amnos.browser.BuildConfig.SECURITY_ABSOLUTE_CLOAKING,
    val forensicScrambleEnabled: Boolean = com.amnos.browser.BuildConfig.SECURITY_FORENSIC_RAM_SCRAMBLE,

    // NETWORK SECURITY
    val httpsOnlyEnabled: Boolean = com.amnos.browser.BuildConfig.SECURITY_HTTPS_ONLY,
    val blockTrackers: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_TRACKERS,
    val blockThirdPartyRequests: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_THIRD_PARTY_REQUESTS,
    val blockThirdPartyScripts: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_THIRD_PARTY_SCRIPTS,
    val blockInlineScripts: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_INLINE_SCRIPTS,
    val stripReferrers: Boolean = com.amnos.browser.BuildConfig.SECURITY_STRIP_REFERRERS,
    val removeTrackingParameters: Boolean = com.amnos.browser.BuildConfig.SECURITY_REMOVE_TRACKING_PARAMS,
    val blockWebSockets: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_WEBSOCKETS,
    val allowFirstPartyWebSockets: Boolean = false,
    val blockWebRtc: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_WEBRTC,
    val blockDnsPrefetch: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_DNS_PREFETCH,
    val blockPreconnect: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_PRECONNECT,
    
    // DEBUGGING
    val enableRemoteDebugging: Boolean = !com.amnos.browser.BuildConfig.SECURITY_LOCKDOWN_MODE && !com.amnos.browser.BuildConfig.SECURITY_BLOCK_REMOTE_DEBUGGING,
    val blockForensicLogging: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_FORENSIC_LOGGING,
    
    // WEB ENGINE SECURITY
    val blockServiceWorkers: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_SERVICE_WORKERS,
    val blockEval: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_EVAL,
    val blockIpv6: Boolean = com.amnos.browser.BuildConfig.SECURITY_BLOCK_IPV6,
    val domStorageEnabled: Boolean = true, // Always true for result rendering
    val strictFirstPartyIsolation: Boolean = com.amnos.browser.BuildConfig.SECURITY_STRICT_FIRST_PARTY_ISOLATION,
    val enforceLoopbackProxy: Boolean = com.amnos.browser.BuildConfig.SECURITY_ENFORCE_LOOPBACK_PROXY,
    val webGlMode: WebGlMode = WebGlMode.DISABLED,
    val fingerprintProtectionLevel: FingerprintProtectionLevel = when (com.amnos.browser.BuildConfig.SECURITY_FINGERPRINT_LEVEL.uppercase()) {
        "BALANCED" -> FingerprintProtectionLevel.BALANCED
        "STRICT" -> FingerprintProtectionLevel.STRICT
        "DISABLED", "OFF", "FALSE" -> FingerprintProtectionLevel.DISABLED
        else -> FingerprintProtectionLevel.STRICT
    },
    val javascriptMode: JavaScriptMode = JavaScriptMode.RESTRICTED,
    val resetIdentityOnRefresh: Boolean = true,
    val sessionTimeoutMillis: Long = com.amnos.browser.BuildConfig.SECURITY_SESSION_TIMEOUT_MS
) {
    val isJavaScriptEnabled: Boolean
        get() = javascriptMode != JavaScriptMode.DISABLED

    val isRestrictedJavaScript: Boolean
        get() = javascriptMode == JavaScriptMode.RESTRICTED
}

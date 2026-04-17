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

enum class FirewallLevel {
    PARANOID,
    BALANCED,
    OPEN
}

enum class CamouflageProfile {
    CALCULATOR,
    WEATHER,
    DISABLED
}

data class PrivacyPolicy(
    // --- CLUSTER 1: STEALTH (PASSIVE) ---
    val stealthCamouflageProfile: CamouflageProfile = when (com.amnos.browser.BuildConfig.STEALTH_CAMOUFLAGE_PROFILE.uppercase()) {
        "CALCULATOR" -> CamouflageProfile.CALCULATOR
        "WEATHER" -> CamouflageProfile.WEATHER
        else -> CamouflageProfile.DISABLED
    },
    val stealthAbsoluteCloaking: Boolean = com.amnos.browser.BuildConfig.STEALTH_ABSOLUTE_CLOAKING,
    val stealthDecoyUnlockPin: String = com.amnos.browser.BuildConfig.STEALTH_DECOY_UNLOCK_PIN,

    // --- CLUSTER 2: PURGE (ACTIVE) ---
    val purgeSandboxEnabled: Boolean = com.amnos.browser.BuildConfig.PURGE_SANDBOX_ENABLED,
    val purgeForensicRamScramble: Boolean = com.amnos.browser.BuildConfig.PURGE_FORENSIC_RAM_SCRAMBLE,
    val purgeWipeOnScreenOff: Boolean = com.amnos.browser.BuildConfig.PURGE_WIPE_ON_SCREEN_OFF,
    val purgeWipeOnBackground: Boolean = com.amnos.browser.BuildConfig.PURGE_WIPE_ON_BACKGROUND,
    val purgeBackgroundWipeDelayMs: Long = com.amnos.browser.BuildConfig.PURGE_BACKGROUND_WIPE_DELAY_MS,
    val purgePanicGestureEnabled: Boolean = com.amnos.browser.BuildConfig.PURGE_PANIC_GESTURE_ENABLED,

    // --- CLUSTER 3: NETWORK ENGINE (PROTOCOLS) ---
    val networkFirewallLevel: FirewallLevel = when (com.amnos.browser.BuildConfig.NETWORK_FIREWALL_LEVEL.uppercase()) {
        "PARANOID" -> FirewallLevel.PARANOID
        "BALANCED" -> FirewallLevel.BALANCED
        "OPEN" -> FirewallLevel.OPEN
        else -> FirewallLevel.PARANOID
    },
    val networkHttpsOnly: Boolean = com.amnos.browser.BuildConfig.NETWORK_HTTPS_ONLY,
    val networkDohUrl: String = com.amnos.browser.BuildConfig.NETWORK_DOH_URL,
    val networkEnforceLoopbackProxy: Boolean = com.amnos.browser.BuildConfig.NETWORK_ENFORCE_LOOPBACK_PROXY,
    val networkBlockLocalNetwork: Boolean = com.amnos.browser.BuildConfig.NETWORK_BLOCK_LOCAL_NETWORK,
    val networkBlockWebRtc: Boolean = com.amnos.browser.BuildConfig.NETWORK_BLOCK_WEBRTC,
    val networkBlockIpv6: Boolean = com.amnos.browser.BuildConfig.NETWORK_BLOCK_IPV6,
    val networkBlockDnsPrefetch: Boolean = com.amnos.browser.BuildConfig.NETWORK_BLOCK_DNS_PREFETCH,
    val networkBlockPreconnect: Boolean = com.amnos.browser.BuildConfig.NETWORK_BLOCK_PRECONNECT,

    // --- CLUSTER 4: PRIVACY FILTER (CONTENT) ---
    val filterBlockTrackers: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_TRACKERS,
    val filterAggressiveAdBlocking: Boolean = com.amnos.browser.BuildConfig.FILTER_AGGRESSIVE_AD_BLOCKING,
    val filterBlockThirdPartyRequests: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_THIRD_PARTY_REQUESTS,
    val filterBlockThirdPartyScripts: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_THIRD_PARTY_SCRIPTS,
    val filterBlockInlineScripts: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_INLINE_SCRIPTS,
    val filterBlockEval: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_EVAL,
    val filterBlockServiceWorkers: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_SERVICE_WORKERS,
    val filterBlockWebSockets: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_WEBSOCKETS,
    val filterRemoveTrackingParams: Boolean = com.amnos.browser.BuildConfig.FILTER_REMOVE_TRACKING_PARAMS,
    val filterStripReferrers: Boolean = com.amnos.browser.BuildConfig.FILTER_STRIP_REFERRERS,
    val filterStrictFirstPartyIsolation: Boolean = com.amnos.browser.BuildConfig.FILTER_STRICT_FIRST_PARTY_ISOLATION,
    val filterBlockUnsafeMethods: Boolean = com.amnos.browser.BuildConfig.FILTER_BLOCK_UNSAFE_METHODS,

    // --- CLUSTER 5: IDENTITY (SPOOFING) ---
    val identityUaTemplate: String = com.amnos.browser.BuildConfig.IDENTITY_UA_TEMPLATE,
    val identityResetOnRefresh: Boolean = com.amnos.browser.BuildConfig.IDENTITY_RESET_ON_REFRESH,
    val identitySessionTimeoutMs: Long = com.amnos.browser.BuildConfig.IDENTITY_SESSION_TIMEOUT_MS,

    // --- CLUSTER 6: HARDWARE (TECHNICAL) ---
    val hardwareFingerprintLevel: FingerprintProtectionLevel = when (com.amnos.browser.BuildConfig.HARDWARE_FINGERPRINT_LEVEL.uppercase()) {
        "BALANCED" -> FingerprintProtectionLevel.BALANCED
        "STRICT" -> FingerprintProtectionLevel.STRICT
        "TITAN" -> FingerprintProtectionLevel.TITAN
        "DISABLED", "OFF", "FALSE" -> FingerprintProtectionLevel.DISABLED
        else -> FingerprintProtectionLevel.STRICT
    },
    val hardwareWebGlMode: WebGlMode = when (com.amnos.browser.BuildConfig.HARDWARE_WEBGL_MODE.uppercase()) {
        "SPOOF" -> WebGlMode.SPOOF
        "DISABLED" -> WebGlMode.DISABLED
        else -> WebGlMode.DISABLED
    },
    val hardwareJavascriptMode: JavaScriptMode = when (com.amnos.browser.BuildConfig.HARDWARE_JAVASCRIPT_MODE.uppercase()) {
        "FULL" -> JavaScriptMode.FULL
        "RESTRICTED" -> JavaScriptMode.RESTRICTED
        "DISABLED" -> JavaScriptMode.DISABLED
        else -> JavaScriptMode.RESTRICTED
    },

    // --- CLUSTER 7: DEBUGGER ---
    val debugLockdownMode: Boolean = com.amnos.browser.BuildConfig.DEBUG_LOCKDOWN_MODE,
    val debugAntiDebugger: Boolean = com.amnos.browser.BuildConfig.DEBUG_ANTI_DEBUGGER,
    val debugBlockRemoteDebugging: Boolean = com.amnos.browser.BuildConfig.DEBUG_BLOCK_REMOTE_DEBUGGING,
    val debugBlockForensicLogging: Boolean = com.amnos.browser.BuildConfig.DEBUG_BLOCK_FORENSIC_LOGGING,
    val debugBlockScreenshots: Boolean = com.amnos.browser.BuildConfig.DEBUG_BLOCK_SCREENSHOTS,

    // Derived Logic (Internal Only)
    val forceRelaxSecurityForDebug: Boolean = !com.amnos.browser.BuildConfig.DEBUG_LOCKDOWN_MODE,
    val isJavaScriptEnabled: Boolean = hardwareJavascriptMode != JavaScriptMode.DISABLED,
    val isRestrictedJavaScript: Boolean = hardwareJavascriptMode == JavaScriptMode.RESTRICTED
)

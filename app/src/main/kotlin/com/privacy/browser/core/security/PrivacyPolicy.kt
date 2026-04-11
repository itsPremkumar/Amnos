package com.privacy.browser.core.security

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
    STRICT
}

data class PrivacyPolicy(
    // MASTER DEBUG TOGGLE
    val forceRelaxSecurityForDebug: Boolean = com.privacy.browser.BuildConfig.SECURITY_RELAX_FOR_DEBUG,
    
    // UI SECURITY
    val blockScreenshots: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_SCREENSHOTS,
    
    // NETWORK SECURITY
    val httpsOnlyEnabled: Boolean = com.privacy.browser.BuildConfig.SECURITY_HTTPS_ONLY,
    val blockTrackers: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_TRACKERS,
    val blockThirdPartyRequests: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_THIRD_PARTY_REQUESTS,
    val blockThirdPartyScripts: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_THIRD_PARTY_SCRIPTS,
    val blockInlineScripts: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_INLINE_SCRIPTS,
    val stripReferrers: Boolean = com.privacy.browser.BuildConfig.SECURITY_STRIP_REFERRERS,
    val removeTrackingParameters: Boolean = com.privacy.browser.BuildConfig.SECURITY_REMOVE_TRACKING_PARAMS,
    val blockWebSockets: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_WEBSOCKETS,
    val allowFirstPartyWebSockets: Boolean = false,
    val blockWebRtc: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_WEBRTC,
    val blockDnsPrefetch: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_DNS_PREFETCH,
    val blockPreconnect: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_PRECONNECT,
    
    // WEB ENGINE SECURITY
    val blockServiceWorkers: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_SERVICE_WORKERS,
    val blockEval: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_EVAL,
    val blockIpv6: Boolean = com.privacy.browser.BuildConfig.SECURITY_BLOCK_IPV6,
    val domStorageEnabled: Boolean = true, // Always true for result rendering
    val strictFirstPartyIsolation: Boolean = com.privacy.browser.BuildConfig.SECURITY_STRICT_FIRST_PARTY_ISOLATION,
    val enforceLoopbackProxy: Boolean = com.privacy.browser.BuildConfig.SECURITY_ENFORCE_LOOPBACK_PROXY,
    val webGlMode: WebGlMode = WebGlMode.DISABLED,
    val fingerprintProtectionLevel: FingerprintProtectionLevel = FingerprintProtectionLevel.STRICT,
    val javascriptMode: JavaScriptMode = JavaScriptMode.RESTRICTED,
    val resetIdentityOnRefresh: Boolean = true,
    val sessionTimeoutMillis: Long = 2 * 60 * 1000L
) {
    val isJavaScriptEnabled: Boolean
        get() = javascriptMode != JavaScriptMode.DISABLED

    val isRestrictedJavaScript: Boolean
        get() = javascriptMode == JavaScriptMode.RESTRICTED
}

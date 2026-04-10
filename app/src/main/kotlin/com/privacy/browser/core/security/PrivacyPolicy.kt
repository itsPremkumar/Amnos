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
    val httpsOnlyEnabled: Boolean = true,
    val blockThirdPartyRequests: Boolean = true,
    val blockThirdPartyScripts: Boolean = true,
    val blockInlineScripts: Boolean = true,
    val stripReferrers: Boolean = true,
    val removeTrackingParameters: Boolean = true,
    val blockWebSockets: Boolean = true,
    val allowFirstPartyWebSockets: Boolean = false,
    val blockWebRtc: Boolean = true,
    val blockDnsPrefetch: Boolean = true,
    val blockPreconnect: Boolean = true,
    val blockServiceWorkers: Boolean = true,
    val blockEval: Boolean = true,
    val blockIpv6: Boolean = true,
    val strictFirstPartyIsolation: Boolean = true,
    val enforceLoopbackProxy: Boolean = true,
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

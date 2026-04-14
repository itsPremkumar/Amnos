package com.amnos.browser.core.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.ui.screens.browser.layouts.AmnosLayouts
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayInputStream
import java.util.Locale

class NetworkSecurityManager(
    private val adBlocker: AdBlocker,
    private val policyProvider: () -> PrivacyPolicy
) {
    private val ruleEngine = SecurityRuleEngine(adBlocker)
    private val fetcher = NetworkFetcher(policyProvider)

    fun sanitizeNavigationUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.equals("about:blank", ignoreCase = true)) return trimmed

        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> "https://${trimmed.removePrefix("http://")}"
            trimmed.contains("://") -> null
            else -> "https://$trimmed"
        }

        return normalized?.let { if (policyProvider().removeTrackingParameters) UrlSanitizer.sanitize(it) else it }
    }

    fun buildNavigationHeaders(url: String, profile: DeviceProfile, topLevelHost: String? = null): Map<String, String> {
        val httpUrl = url.toHttpUrlOrNull() ?: return emptyMap()
        return SecurityHeaderFactory.buildRequestHeaders(
            originalHeaders = emptyMap(),
            url = httpUrl,
            profile = profile,
            topLevelHost = topLevelHost,
            policy = policyProvider()
        ).toMultimap().mapValues { it.value.joinToString(",") }
    }

    fun evaluateRequest(request: WebResourceRequest, topLevelHost: String?): RequestDecision {
        val policy = policyProvider()
        val decision = ruleEngine.evaluateRequest(request, topLevelHost, policy)
        
        if (policy.forceRelaxSecurityForDebug && decision.isBlocked) {
            return decision.copy(blockReason = null)
        }
        return decision
    }

    fun createBlockedResponse(reason: BlockReason, isMainFrame: Boolean): WebResourceResponse {
        val body = if (isMainFrame) AmnosLayouts.blockedPageHtml(reason.name) else ""
        return WebResourceResponse(
            if (isMainFrame) "text/html" else "text/plain",
            "UTF-8",
            451,
            "Blocked by Amnos",
            mapOf("Cache-Control" to "no-store, no-cache, max-age=0", "Pragma" to "no-cache"),
            ByteArrayInputStream(body.toByteArray())
        )
    }

    fun fetchResponse(request: WebResourceRequest, decision: RequestDecision, profile: DeviceProfile, topLevelHost: String?): WebResourceResponse? {
        return fetcher.fetchResponse(request, decision, profile, topLevelHost)
    }

    fun blockReasonLabel(reason: BlockReason): String = when (reason) {
        BlockReason.HTTPS_ONLY -> "https_only"
        BlockReason.TRACKER -> "tracker"
        BlockReason.THIRD_PARTY -> "third_party"
        BlockReason.THIRD_PARTY_SCRIPT -> "third_party_script"
        BlockReason.WEBSOCKET -> "websocket"
        BlockReason.UNSUPPORTED_SCHEME -> "unsupported_scheme"
        BlockReason.LOCAL_NETWORK -> "local_network"
        BlockReason.UNSAFE_METHOD -> "unsafe_method"
        BlockReason.SECURITY_THREAT -> "security_threat"
    }

    fun siteKeyForUrl(url: String?): String? {
        val host = url?.toHttpUrlOrNull()?.host ?: return null
        val normalizedHost = host.lowercase(Locale.US).trim().removeSuffix(".")
        if (normalizedHost.isBlank()) return null
        val labels = normalizedHost.split(".").filter { it.isNotBlank() }
        return if (labels.size >= 2) labels.takeLast(2).joinToString(".") else normalizedHost
    }

    fun isCrossSiteNavigation(currentUrl: String?, nextUrl: String): Boolean {
        val currentSite = siteKeyForUrl(currentUrl)
        val nextSite = siteKeyForUrl(nextUrl)
        return currentSite != null && nextSite != null && currentSite != nextSite
    }

    fun isTunnelAllowed(host: String, port: Int): Boolean {
        if (port <= 0 || port > 65535) return false
        if (policyProvider().httpsOnlyEnabled && port == 80) return false
        return !ruleEngine.isLocalNetworkHost(host)
    }
}

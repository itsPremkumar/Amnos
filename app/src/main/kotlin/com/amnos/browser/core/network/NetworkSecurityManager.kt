package com.amnos.browser.core.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.ui.screens.browser.layouts.AmnosLayouts
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.HttpUrl
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
            trimmed.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) -> null
            trimmed.contains("://") -> null
            else -> {
                // Smart Search Detection
                val isProbablyUrl = trimmed.contains(".") || trimmed.equals("localhost", ignoreCase = true) || trimmed.startsWith("[")
                if (!isProbablyUrl || trimmed.contains(" ")) {
                    "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
                } else {
                    "https://$trimmed"
                }
            }
        }

        if (normalized == null) {
            AmnosLog.w("NetworkSecurity", "URL blocked: Invalid scheme or format in '$rawUrl'")
            return null
        }

        val policy = policyProvider()
        val finalUrl = if (policy.filterRemoveTrackingParams) {
            val sanitized = UrlSanitizer.sanitize(normalized)
            if (sanitized != normalized) {
                AmnosLog.d("NetworkSecurity", "URL sanitized: Tracking parameters removed from '$normalized'")
            }
            sanitized
        } else {
            normalized
        }
        return finalUrl
    }

    fun buildNavigationHeaders(url: String, profile: DeviceProfile, topLevelHost: String? = null): Map<String, String> {
        val httpUrl = url.toHttpUrlOrNull() ?: return emptyMap()
        AmnosLog.d("NetworkSecurity", "Injecting security headers for: ${httpUrl.host}")
        return SecurityHeaderFactory.buildRequestHeaders(
            originalHeaders = emptyMap(),
            url = httpUrl,
            profile = profile,
            topLevelHost = topLevelHost,
            policy = policyProvider()
        ).toMultimap().mapValues { it.value.joinToString(",") }
    }

    fun evaluateRequest(request: WebResourceRequest, topLevelHost: String?): RequestDecision {
        val urlStr = request.url.toString()
        val policy = policyProvider()
        
        // 1. EXPLICIT FIREWALL BLOCKS (Manual Rules)
        if (DomainPolicyManager.isExplicitlyBlocked(urlStr)) {
            AmnosLog.w("NetworkSecurity", "FIREWALL BLOCKED via manual rule: $urlStr")
            return RequestDecision(
                sanitizedUrl = urlStr,
                kind = RequestKind.OTHER,
                blockReason = BlockReason.FIREWALL_RULE
            )
        }

        // 2. V2 PARANOID MODE FIREWALL (Whitelist only)
        if (policy.networkFirewallLevel == com.amnos.browser.core.security.FirewallLevel.PARANOID) {
            if (!DomainPolicyManager.isAllowed(urlStr)) {
                AmnosLog.w("NetworkSecurity", "FIREWALL BLOCKED unknown domain in PARANOID mode: $urlStr")
                return RequestDecision(
                    sanitizedUrl = urlStr,
                    kind = RequestKind.OTHER,
                    blockReason = BlockReason.SECURITY_THREAT
                )
            }
        }

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
        BlockReason.FIREWALL_RULE -> "firewall_rule"
    }

    fun siteKeyForUrl(url: String?): String? {
        val httpUrl = url?.toHttpUrlOrNull() ?: return null
        val normalizedHost = httpUrl.host.lowercase(Locale.US).trim().removeSuffix(".")
        if (normalizedHost.isBlank()) return null
        return httpUrl.topPrivateDomain()?.lowercase(Locale.US) ?: normalizedHost
    }

    fun isCrossSiteNavigation(currentUrl: String?, nextUrl: String): Boolean {
        val currentSite = siteKeyForUrl(currentUrl)
        val nextSite = siteKeyForUrl(nextUrl)
        return currentSite != null && nextSite != null && currentSite != nextSite
    }

    fun isTunnelAllowed(host: String, port: Int): Boolean {
        if (port <= 0 || port > 65535) return false
        if (policyProvider().networkHttpsOnly && port == 80) return false
        return !ruleEngine.isLocalNetworkHost(host)
    }

    fun isLocalNetworkHost(host: String): Boolean = ruleEngine.isLocalNetworkHost(host)
}

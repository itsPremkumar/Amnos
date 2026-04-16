package com.amnos.browser.core.network

import android.webkit.WebResourceRequest
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

class SecurityRuleEngine(
    private val adBlocker: AdBlocker
) {
    fun evaluateRequest(request: WebResourceRequest, topLevelHost: String?, policy: PrivacyPolicy): RequestDecision {
        val rawUrl = request.url.toString()
        val sanitizedUrl = if (policy.removeTrackingParameters) UrlSanitizer.sanitize(rawUrl) else rawUrl
        val parsed = sanitizedUrl.toHttpUrlOrNull()

        if (isWebSocketUrl(rawUrl) && policy.blockWebSockets) {
            val host = parsed?.host ?: rawUrl
            AmnosLog.w("SecurityEngine", "BLOCKED: WebSocket refused to $host (Policy: blockWebSockets=true)")
            return RequestDecision(rawUrl, RequestKind.WEBSOCKET, BlockReason.WEBSOCKET)
        }

        if (request.url.scheme?.equals("http", ignoreCase = true) == true && policy.httpsOnlyEnabled) {
            val kind = classifyRequest(request, parsed)
            AmnosLog.w("SecurityEngine", "BLOCKED: Downgrade attempt (HTTP) to ${parsed?.host ?: rawUrl}. [HTTPS-Only active]")
            return RequestDecision(sanitizedUrl, kind, BlockReason.HTTPS_ONLY)
        }

        if (parsed == null || parsed.scheme != "https") {
            val kind = classifyRequest(request, parsed)
            if (rawUrl != "about:blank") {
                AmnosLog.w("SecurityEngine", "BLOCKED: Insecure or unsupported scheme in '$rawUrl' (Required: https)")
            }
            return RequestDecision(sanitizedUrl, kind, BlockReason.UNSUPPORTED_SCHEME)
        }

        if (isLocalNetworkHost(parsed.host)) {
            val kind = classifyRequest(request, parsed)
            AmnosLog.w("SecurityEngine", "BLOCKED: Local network access to ${parsed.host} prohibited for security.")
            return RequestDecision(sanitizedUrl, kind, BlockReason.LOCAL_NETWORK)
        }

        val thirdParty = isThirdPartyHost(parsed.host, topLevelHost)
        val kind = classifyRequest(request, parsed)

        if (policy.blockThirdPartyRequests && thirdParty && !request.isForMainFrame) {
            AmnosLog.d("SecurityEngine", "BLOCKED: Third-party resource from ${parsed.host} (Policy: blockThirdPartyRequests=true)")
            return RequestDecision(sanitizedUrl, kind, BlockReason.THIRD_PARTY, thirdParty = true)
        }

        if (policy.blockThirdPartyScripts && thirdParty && kind == RequestKind.SCRIPT) {
            AmnosLog.d("SecurityEngine", "BLOCKED: Third-party script from ${parsed.host} (Policy: blockThirdPartyScripts=true)")
            return RequestDecision(sanitizedUrl, kind, BlockReason.THIRD_PARTY_SCRIPT, thirdParty = true)
        }

        if (policy.blockTrackers && !request.isForMainFrame) {
            // FIRST-PARTY BYPASS: Never block resources loaded from the same site the user is visiting.
            // This prevents the AdBlocker from breaking YouTube, Google, etc.
            val isFirstParty = !isThirdPartyHost(parsed.host, topLevelHost)
            
            // FUNCTIONAL MEDIA BYPASS: Known video platform CDN domains are always allowed
            // regardless of blocklist status, to prevent video playback failures.
            val isVideoPlatformCdn = parsed.host.contains("googlevideo.com") || 
                parsed.host.contains("ytimg.com") ||
                parsed.host.contains("ggpht.com") ||
                parsed.host.contains("gstatic.com") ||
                parsed.host.contains("youtube.com") ||
                parsed.host.contains("youtu.be")
            
            if (!isFirstParty && !isVideoPlatformCdn && adBlocker.shouldBlock(sanitizedUrl)) {
                AmnosLog.w("SecurityEngine", "BLOCKED: Ad/Tracker detected at ${parsed.host}")
                return RequestDecision(sanitizedUrl, kind, BlockReason.TRACKER, thirdParty = thirdParty)
            }
        }

        return RequestDecision(sanitizedUrl, kind, thirdParty = thirdParty)
    }

    fun classifyRequest(request: WebResourceRequest, url: okhttp3.HttpUrl?): RequestKind {
        val destination = request.requestHeaders.entries
            .firstOrNull { it.key.equals("Sec-Fetch-Dest", ignoreCase = true) }
            ?.value
            ?.lowercase(Locale.US)

        return when (destination) {
            "document", "iframe" -> RequestKind.DOCUMENT
            "script" -> RequestKind.SCRIPT
            "style" -> RequestKind.STYLESHEET
            "image" -> RequestKind.IMAGE
            "audio", "video" -> RequestKind.MEDIA
            "font" -> RequestKind.FONT
            "serviceworker", "worker", "sharedworker" -> RequestKind.SERVICE_WORKER
            "empty" -> if (request.isForMainFrame) RequestKind.DOCUMENT else RequestKind.XHR
            else -> inferRequestKindFromPath(url, request.isForMainFrame)
        }
    }

    private fun inferRequestKindFromPath(url: okhttp3.HttpUrl?, isMainFrame: Boolean): RequestKind {
        if (isMainFrame) return RequestKind.DOCUMENT
        val path = url?.encodedPath?.lowercase(Locale.US).orEmpty()
        return when {
            path.endsWith(".js") || path.endsWith(".mjs") -> RequestKind.SCRIPT
            path.endsWith(".css") -> RequestKind.STYLESHEET
            path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") -> RequestKind.IMAGE
            path.endsWith(".mp4") || path.endsWith(".mp3") || path.endsWith(".webm") -> RequestKind.MEDIA
            path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".otf") -> RequestKind.FONT
            else -> RequestKind.XHR
        }
    }

    fun isThirdPartyHost(host: String, topLevelHost: String?): Boolean {
        if (topLevelHost.isNullOrBlank()) return false
        val normalizedHost = host.lowercase(Locale.US).trim().removeSuffix(".")
        val normalizedTopLevel = topLevelHost.lowercase(Locale.US).trim().removeSuffix(".")
        val hostSite = registrableDomain(normalizedHost)
        val topLevelSite = registrableDomain(normalizedTopLevel)
        return hostSite != topLevelSite
    }

    fun isLocalNetworkHost(host: String): Boolean {
        val normalizedHost = host.lowercase(Locale.US)
        if (normalizedHost == "localhost" || normalizedHost.endsWith(".local")) return true
        val parts = normalizedHost.split(".")
        if (parts.size != 4 || parts.any { it.toIntOrNull() == null }) return normalizedHost.contains(":")
        val octets = parts.map { it.toInt() }
        return octets[0] == 10 || octets[0] == 127 || (octets[0] == 169 && octets[1] == 254) ||
            (octets[0] == 172 && octets[1] in 16..31) || (octets[0] == 192 && octets[1] == 168)
    }

    fun isWebSocketUrl(url: String): Boolean = url.startsWith("ws://", ignoreCase = true) || url.startsWith("wss://", ignoreCase = true)

    private fun registrableDomain(host: String): String {
        if (host.isBlank()) return host
        val candidate = "https://$host".toHttpUrlOrNull()
        return candidate?.topPrivateDomain()?.lowercase(Locale.US) ?: host
    }
}

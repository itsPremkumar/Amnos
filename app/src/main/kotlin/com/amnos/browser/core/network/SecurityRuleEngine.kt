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
            return RequestDecision(rawUrl, RequestKind.WEBSOCKET, BlockReason.WEBSOCKET)
        }

        if (request.url.scheme?.equals("http", ignoreCase = true) == true && policy.httpsOnlyEnabled) {
            return RequestDecision(sanitizedUrl, classifyRequest(request, parsed), BlockReason.HTTPS_ONLY)
        }

        if (parsed == null || parsed.scheme != "https") {
            return RequestDecision(sanitizedUrl, classifyRequest(request, parsed), BlockReason.UNSUPPORTED_SCHEME)
        }

        if (isLocalNetworkHost(parsed.host)) {
            return RequestDecision(sanitizedUrl, classifyRequest(request, parsed), BlockReason.LOCAL_NETWORK)
        }

        val thirdParty = isThirdPartyHost(parsed.host, topLevelHost)
        val kind = classifyRequest(request, parsed)

        if (policy.blockThirdPartyRequests && thirdParty && !request.isForMainFrame) {
            return RequestDecision(sanitizedUrl, kind, BlockReason.THIRD_PARTY, thirdParty = true)
        }

        if (policy.blockThirdPartyScripts && thirdParty && kind == RequestKind.SCRIPT) {
            return RequestDecision(sanitizedUrl, kind, BlockReason.THIRD_PARTY_SCRIPT, thirdParty = true)
        }

        if (policy.blockTrackers && !request.isForMainFrame) {
            val isFunctional = (kind == RequestKind.MEDIA || kind == RequestKind.IMAGE || kind == RequestKind.FONT) && 
                (parsed.host.contains("googlevideo.com") || parsed.host.contains("ytimg.com") || parsed.host.contains("youtube.com"))
            
            if (!isFunctional && adBlocker.shouldBlock(sanitizedUrl)) {
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
        val normalizedHost = host.lowercase(Locale.US)
        val normalizedTopLevel = topLevelHost.lowercase(Locale.US)
        return normalizedHost != normalizedTopLevel &&
            !normalizedHost.endsWith(".$normalizedTopLevel") &&
            !normalizedTopLevel.endsWith(".$normalizedHost")
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
}

package com.privacy.browser.core.network

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.security.PrivacyPolicy
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.Locale

class NetworkSecurityManager(
    private val policyProvider: () -> PrivacyPolicy
) {
    fun sanitizeNavigationUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.equals("about:blank", ignoreCase = true)) return trimmed

        val explicitScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
        if (explicitScheme.containsMatchIn(trimmed) &&
            !trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return null
        }

        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> "https://${trimmed.removePrefix("http://")}"
            trimmed.contains("://") -> null
            else -> "https://$trimmed"
        }

        return normalized?.let { sanitizeUrlIfEnabled(it) }
    }

    fun buildNavigationHeaders(
        url: String,
        profile: DeviceProfile,
        topLevelHost: String? = null
    ): Map<String, String> {
        val httpUrl = url.toHttpUrlOrNull() ?: return emptyMap()
        return buildRequestHeaders(
            originalHeaders = emptyMap(),
            url = httpUrl,
            profile = profile,
            topLevelHost = topLevelHost
        ).toMultimap().mapValues { it.value.joinToString(",") }
    }

    fun evaluateRequest(request: WebResourceRequest, topLevelHost: String?): RequestDecision {
        val policy = policyProvider()
        val decision = evaluateRequestInner(request, topLevelHost, policy)
        
        // GLOBAL DEBUG BYPASS - If relaxed mode is active, override blocking decisions
        if (policy.forceRelaxSecurityForDebug && decision.isBlocked) {
            Log.w("NetworkSecurityManager", "OVERRIDING block decision for ${decision.sanitizedUrl} due to RELAXED mode")
            return decision.copy(blockReason = null)
        }
        
        return decision
    }

    private fun evaluateRequestInner(request: WebResourceRequest, topLevelHost: String?, policy: PrivacyPolicy): RequestDecision {
        val rawUrl = request.url.toString()
        val sanitizedUrl = sanitizeUrlIfEnabled(rawUrl)
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

        return RequestDecision(sanitizedUrl, kind, thirdParty = thirdParty)
    }

    fun createBlockedResponse(
        reason: BlockReason,
        isMainFrame: Boolean
    ): WebResourceResponse {
        val body = if (isMainFrame) {
            """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Blocked by Amnos</title>
                    <style>
                        body { background:#0d1117; color:#f8fafc; font-family:sans-serif; padding:24px; }
                        .card { max-width:560px; margin:48px auto; background:#161b22; border:1px solid #223; border-radius:16px; padding:24px; }
                        h1 { margin:0 0 12px; font-size:24px; }
                        p { color:#94a3b8; line-height:1.5; }
                        code { color:#7dd3fc; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Request blocked</h1>
                        <p>Amnos stopped this request because it violated the active privacy policy.</p>
                        <p><code>${reason.name}</code></p>
                    </div>
                </body>
                </html>
            """.trimIndent()
        } else {
            ""
        }

        return WebResourceResponse(
            if (isMainFrame) "text/html" else "text/plain",
            "UTF-8",
            451,
            "Blocked by Amnos",
            mapOf(
                "Cache-Control" to "no-store, no-cache, max-age=0",
                "Pragma" to "no-cache"
            ),
            ByteArrayInputStream(body.toByteArray())
        )
    }

    fun fetchResponse(
        request: WebResourceRequest,
        decision: RequestDecision,
        profile: DeviceProfile,
        topLevelHost: String?
    ): WebResourceResponse? {
        val policy = policyProvider()
        val httpUrl = decision.sanitizedUrl.toHttpUrlOrNull() ?: return null
        if (!request.method.equals("GET", ignoreCase = true) && !request.method.equals("HEAD", ignoreCase = true)) {
            return null
        }

        val okHttpRequest = Request.Builder()
            .url(httpUrl)
            .headers(buildRequestHeaders(request.requestHeaders, httpUrl, profile, topLevelHost))
            .method(request.method, null)
            .build()

        return try {
            val startTime = System.currentTimeMillis()
            Log.d("NetworkSecurityManager", "Fetching proxied request: ${request.method} $httpUrl")
            val response = DnsManager.secureClient(policy.blockIpv6).newCall(okHttpRequest).execute()
            val duration = System.currentTimeMillis() - startTime
            Log.d("NetworkSecurityManager", "Proxied response received in ${duration}ms: code=${response.code} url=$httpUrl")
            
            val body = response.body
            val contentLength = body?.contentLength() ?: -1
            Log.v("NetworkSecurityManager", "Response body size: $contentLength bytes")
            val contentType = body?.contentType()
            val mimeType = if (contentType != null) {
                "${contentType.type}/${contentType.subtype}"
            } else {
                Log.w("NetworkSecurityManager", "Missing Content-Type for $httpUrl, defaulting to text/html")
                "text/html" // Default to HTML for better compatibility
            }
            val charset = contentType?.charset(Charsets.UTF_8)?.name() ?: "UTF-8"

            WebResourceResponse(
                mimeType,
                charset,
                response.code,
                response.message.ifBlank { "OK" },
                buildResponseHeaders(response, decision.kind),
                body?.byteStream() // Use the already-captured body reference
            )
        } catch (e: Exception) {
            Log.e("NetworkSecurityManager", "Proxied fetch FAILED for $httpUrl", e)
            null
        }
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
    }

    fun isWebSocketUrl(url: String): Boolean {
        return url.startsWith("ws://", ignoreCase = true) || url.startsWith("wss://", ignoreCase = true)
    }

    fun siteKeyForUrl(url: String?): String? {
        val host = url?.toHttpUrlOrNull()?.host ?: return null
        return siteKeyForHost(host)
    }

    fun siteKeyForHost(host: String?): String? {
        val normalizedHost = host?.lowercase(Locale.US)?.trim()?.removeSuffix(".") ?: return null
        if (normalizedHost.isBlank()) return null
        if (normalizedHost == "localhost" || normalizedHost.contains(":")) return normalizedHost

        val labels = normalizedHost.split(".").filter { it.isNotBlank() }
        return if (labels.size >= 2) {
            labels.takeLast(2).joinToString(".")
        } else {
            normalizedHost
        }
    }

    fun isCrossSiteNavigation(currentUrl: String?, nextUrl: String): Boolean {
        val currentSite = siteKeyForUrl(currentUrl)
        val nextSite = siteKeyForUrl(nextUrl)
        return currentSite != null && nextSite != null && currentSite != nextSite
    }

    fun isTunnelAllowed(host: String, port: Int): Boolean {
        val policy = policyProvider()
        if (port <= 0 || port > 65535) return false
        if (policy.httpsOnlyEnabled && port == 80) return false
        return !isLocalNetworkHost(host)
    }

    private fun sanitizeUrlIfEnabled(url: String): String {
        return if (policyProvider().removeTrackingParameters) UrlSanitizer.sanitize(url) else url
    }

    private fun classifyRequest(request: WebResourceRequest, url: HttpUrl?): RequestKind {
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

    private fun inferRequestKindFromPath(url: HttpUrl?, isMainFrame: Boolean): RequestKind {
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

    private fun buildRequestHeaders(
        originalHeaders: Map<String, String>,
        url: HttpUrl,
        profile: DeviceProfile,
        topLevelHost: String?
    ): Headers {
        val policy = policyProvider()
        val builder = Headers.Builder()

        originalHeaders.forEach { (key, value) ->
            val lower = key.lowercase(Locale.US)
            if (lower in setOf("cookie", "referer", "origin", "x-requested-with")) {
                return@forEach
            }
            if (lower == "accept-language" || lower == "user-agent") {
                return@forEach
            }
            if (lower == "host" || lower == "connection" || lower == "content-length") {
                return@forEach
            }
            builder.add(key, value)
        }

        builder.set("User-Agent", profile.userAgent)
        builder.set("Accept-Language", profile.acceptLanguageHeader)
        builder.set("Accept-Encoding", "identity") // Force uncompressed for manual intercept to avoid header clash
        builder.set("DNT", "1")
        builder.set("Sec-GPC", "1")
        builder.set("Cache-Control", "no-cache, no-store")
        builder.set("Pragma", "no-cache")

        if (!policy.stripReferrers && topLevelHost != null) {
            builder.set("Referer", "https://$topLevelHost/")
        }

        if (isThirdPartyHost(url.host, topLevelHost)) {
            builder.removeAll("Sec-Fetch-Site")
        }

        return builder.build()
    }

    private fun buildResponseHeaders(
        response: okhttp3.Response,
        kind: RequestKind
    ): Map<String, String> {
        val policy = policyProvider()
        val headers = linkedMapOf<String, String>()

        response.headers.forEach { (name, value) ->
            if (name.equals("Set-Cookie", ignoreCase = true)) {
                 // Log cookies but keep them for now to test if it fixes search results
                Log.v("NetworkSecurityManager", "Preserving cookie from server for privacy debug")
                headers[name] = value
                return@forEach
            }
            if (name.equals("Content-Security-Policy", ignoreCase = true)) return@forEach
            if (name.equals("Content-Encoding", ignoreCase = true)) return@forEach // Strip encoding since we forced identity
            if (name.equals("Content-Length", ignoreCase = true)) return@forEach // Strip length to let WebView calculate it
            headers[name] = value
        }

        headers["Cache-Control"] = "no-store, no-cache, max-age=0"
        headers["Pragma"] = "no-cache"
        headers["Referrer-Policy"] = "no-referrer"
        headers["X-DNS-Prefetch-Control"] = "off"

        if (kind == RequestKind.DOCUMENT) {
            headers["Permissions-Policy"] =
                "accelerometer=(), ambient-light-sensor=(), autoplay=(), battery=(), camera=(), clipboard-read=()," +
                    " clipboard-write=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=()," +
                    " usb=(), xr-spatial-tracking=()"

            buildContentSecurityPolicy(policy)?.let { headers["Content-Security-Policy"] = it }
        }

        return headers
    }

    private fun buildContentSecurityPolicy(policy: PrivacyPolicy): String? {
        if (policy.forceRelaxSecurityForDebug) {
            Log.d("NetworkSecurityManager", "Skipping CSP injection due to RELAXED mode")
            return null
        }
        
        if (!policy.isRestrictedJavaScript &&
            !policy.blockDnsPrefetch &&
            !policy.blockPreconnect &&
            !policy.blockWebSockets
        ) {
            return null
        }

        val scriptSource = if (policy.blockInlineScripts) {
            "'self' https:"
        } else {
            "'self' https: 'unsafe-inline'"
        }
        val connectSource = if (policy.blockWebSockets) {
            "'self' https:"
        } else {
            "'self' https: wss:"
        }

        return listOf(
            "default-src https: data: blob:",
            "base-uri 'none'",
            "object-src 'none'",
            "frame-ancestors 'none'",
            "img-src https: data: blob:",
            "media-src https: blob:",
            "style-src 'self' https: 'unsafe-inline'",
            "font-src 'none'",
            "connect-src $connectSource",
            "script-src $scriptSource",
            "worker-src 'none'",
            "upgrade-insecure-requests"
        ).joinToString("; ")
    }

    private fun isThirdPartyHost(host: String, topLevelHost: String?): Boolean {
        if (topLevelHost.isNullOrBlank()) return false
        val normalizedHost = host.lowercase(Locale.US)
        val normalizedTopLevel = topLevelHost.lowercase(Locale.US)
        return normalizedHost != normalizedTopLevel &&
            !normalizedHost.endsWith(".$normalizedTopLevel") &&
            !normalizedTopLevel.endsWith(".$normalizedHost")
    }

    fun isLocalNetworkHost(host: String): Boolean {
        val normalizedHost = host.lowercase(Locale.US)
        if (normalizedHost == "localhost" || normalizedHost.endsWith(".local")) {
            return true
        }
        val parts = normalizedHost.split(".")
        if (parts.size != 4 || parts.any { it.toIntOrNull() == null }) {
            return normalizedHost.contains(":")
        }

        val octets = parts.map { it.toInt() }
        return octets[0] == 10 ||
            octets[0] == 127 ||
            (octets[0] == 169 && octets[1] == 254) ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168)
    }
}

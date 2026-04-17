package com.amnos.browser.core.network

import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.session.AmnosLog
import okhttp3.Headers
import okhttp3.HttpUrl
import java.util.Locale

object SecurityHeaderFactory {

    fun buildRequestHeaders(
        originalHeaders: Map<String, String>,
        url: HttpUrl,
        profile: DeviceProfile,
        topLevelHost: String?,
        policy: PrivacyPolicy
    ): Headers {
        val builder = Headers.Builder()
        val fingerprintEnabled = policy.fingerprintProtectionLevel != FingerprintProtectionLevel.DISABLED

        originalHeaders.forEach { (key, value) ->
            val lower = key.lowercase(Locale.US)
            if (lower in setOf("cookie", "referer", "origin", "x-requested-with", "host", "connection", "content-length")) {
                return@forEach
            }
            if (!fingerprintEnabled && (lower == "user-agent" || lower == "accept-language")) {
                builder.set(key, value)
                return@forEach
            }
            if (lower == "accept-language" || lower == "user-agent") {
                return@forEach
            }
            builder.add(key, value)
        }

        if (fingerprintEnabled) {
            builder.set("User-Agent", profile.userAgent)
            builder.set("Accept-Language", profile.acceptLanguageHeader)
        }
        
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

    fun buildResponseHeaders(
        response: okhttp3.Response,
        kind: RequestKind,
        policy: PrivacyPolicy
    ): Map<String, String> {
        val headers = linkedMapOf<String, String>()

        response.headers.forEach { (name, value) ->
            if (name.equals("Set-Cookie", ignoreCase = true) ||
                name.equals("Content-Security-Policy", ignoreCase = true) ||
                name.equals("Content-Encoding", ignoreCase = true) ||
                name.equals("Content-Length", ignoreCase = true)
            ) {
                return@forEach
            }
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

            buildContentSecurityPolicy(policy, response.request.url.host)
                ?.let { headers["Content-Security-Policy"] = it }
        }

        return headers
    }

    private fun buildContentSecurityPolicy(policy: PrivacyPolicy, host: String): String? {
        if (policy.forceRelaxSecurityForDebug) return null
        
        if (!policy.isRestrictedJavaScript &&
            !policy.blockDnsPrefetch &&
            !policy.blockPreconnect &&
            !policy.blockWebSockets
        ) {
            return null
        }

        val scriptSources = linkedSetOf("'self'", "https:")
        
        if (!policy.blockInlineScripts || isCompatibilityCriticalHost(host)) {
            scriptSources.add("'unsafe-inline'")
        }
        if (!policy.blockEval || isCompatibilityCriticalHost(host)) {
            scriptSources.add("'unsafe-eval'")
        }
        
        val scriptSourceStr = scriptSources.joinToString(" ")
        val connectSources = mutableListOf("'self'", "https:")
        if (!policy.blockWebSockets) connectSources.add("wss:")
        val connectSourceStr = connectSources.joinToString(" ")
        
        val workerSource = if (policy.blockServiceWorkers && !isCompatibilityCriticalHost(host)) {
            "'none'"
        } else {
            "'self' blob:"
        }

        return listOf(
            "default-src https: data: blob:",
            "base-uri 'none'",
            "object-src 'none'",
            "frame-ancestors 'none'",
            "img-src https: data: blob:",
            "media-src https: blob:",
            "style-src 'self' https: 'unsafe-inline'",
            "font-src 'self' https: data:",
            "connect-src $connectSourceStr",
            "script-src $scriptSourceStr",
            "worker-src $workerSource",
            "upgrade-insecure-requests"
        ).joinToString("; ")
    }

    private fun isCompatibilityCriticalHost(host: String): Boolean {
        val normalizedHost = host.lowercase(Locale.US)
        val compatibilityHosts = setOf(
            "duckduckgo.com",
            "google.com",
            "www.google.com",
            "www.google.co.in",
            "youtube.com",
            "www.youtube.com",
            "youtube-nocookie.com",
            "youtu.be",
            "vimeo.com",
            "player.vimeo.com",
            "vimeocdn.com"
        )
        return compatibilityHosts.any { normalizedHost.endsWith(it) }
    }

    private fun isThirdPartyHost(host: String, topLevelHost: String?): Boolean {
        if (topLevelHost.isNullOrBlank()) return false
        val normalizedHost = host.lowercase(Locale.US)
        val normalizedTopLevel = topLevelHost.lowercase(Locale.US)
        return normalizedHost != normalizedTopLevel &&
            !normalizedHost.endsWith(".$normalizedTopLevel") &&
            !normalizedTopLevel.endsWith(".$normalizedHost")
    }
}

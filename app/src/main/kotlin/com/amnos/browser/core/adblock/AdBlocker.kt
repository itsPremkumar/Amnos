package com.amnos.browser.core.adblock

import android.content.Context
import com.amnos.browser.core.session.AmnosLog
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStreamReader
import java.util.regex.Pattern

class AdBlocker(context: Context) {
    private val knownAdPatterns = listOf(
        ".*\\/ads?\\/.*",
        ".*\\/advert.*",
        ".*\\/banner.*",
        ".*\\/tracking.*",
        ".*\\/analytics.*",
        ".*\\/telemetry.*",
        ".*\\/pixel.*",
        ".*\\/beacon.*",
        ".*ad[sx]?[0-9]*\\.",
        ".*tracker[0-9]*\\.",
        ".*analytics[0-9]*\\.",
        ".*pixel[0-9]*\\."
    )
    private val adKeywords = listOf(
        "/ad/", "/ads/", "/adv/",
        "/banner/", "/banners/",
        "/track/", "/tracking/", "/tracker/",
        "/analytics/", "/analytic/",
        "/telemetry/",
        "/pixel/", "/pixels/",
        "/beacon/", "/beacons/",
        "/impression/", "/impressions/",
        "/click/", "/clicks/",
        "/conversion/", "/conversions/",
        "pagead", "doubleclick",
        "googleads", "googlesyndication",
        "facebook.net/tr", "facebook.com/tr",
        "?utm_", "&utm_",
        "?fbclid=", "&fbclid=",
        "?gclid=", "&gclid="
    )
    @Volatile
    private var blockedDomains: Set<String> = emptySet()
    private val blockedPatterns: List<Pattern> = knownAdPatterns.mapNotNull { pattern ->
        try {
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            AmnosLog.e("AdBlocker", "Error compiling pattern: $pattern", e)
            null
        }
    }
    private val allowedDomains = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    init {
        try {
            blockedDomains = loadFilterLists(context)
            AmnosLog.d("AdBlocker", "Initialization complete. Blocked domains: ${blockedDomains.size}")
        } catch (e: Exception) {
            AmnosLog.e("AdBlocker", "Initialization failed", e)
        }
    }

    private fun loadFilterLists(context: Context): Set<String> {
        val domains = linkedSetOf<String>()
        loadFilterList(context, "blocklist.txt", domains)
        try {
            loadFilterList(context, "blocklist_comprehensive.txt", domains)
        } catch (e: Exception) {
            AmnosLog.d("AdBlocker", "Comprehensive blocklist not found, using main list only")
        }
        AmnosLog.d("AdBlocker", "Loaded ${domains.size} blocked domains")
        return domains
    }

    private fun loadFilterList(context: Context, filename: String, domains: MutableSet<String>) {
        try {
            context.assets.open(filename).use { inputStream ->
                InputStreamReader(inputStream).buffered().use { reader ->
                    reader.forEachLine { line ->
                        normalizeRule(line)?.let { domains.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            AmnosLog.e("AdBlocker", "Error loading $filename", e)
        }
    }

    private fun normalizeRule(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("!") || trimmed.startsWith("[")) return null
        if (trimmed.startsWith("#")) return null

        val withoutOptions = trimmed.substringBefore('$')
        val domain = when {
            withoutOptions.startsWith("||") -> withoutOptions.removePrefix("||").substringBefore('^')
            withoutOptions.startsWith("|http") -> withoutOptions.removePrefix("|").toHttpUrlOrNull()?.host
            withoutOptions.contains("://") -> withoutOptions.toHttpUrlOrNull()?.host
            else -> withoutOptions.substringBefore('^').substringBefore('/')
        }

        return domain
            ?.removePrefix("www.")
            ?.removePrefix(".")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
    }

    fun shouldBlock(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host?.removePrefix("www.")?.lowercase() ?: return false

        if (allowedDomains.contains(host)) {
            return false
        }

        if (isBlockedByDomain(host)) {
            AmnosLog.d("AdBlocker", "Rule Hit: Blocked per Domain List -> $host")
            return true
        }

        if (isBlockedByPattern(url)) {
            AmnosLog.d("AdBlocker", "Rule Hit: Blocked per Regex Pattern -> $url")
            return true
        }

        if (containsAdKeywords(url)) {
            AmnosLog.d("AdBlocker", "Rule Hit: Blocked per Keyword Detection -> $url")
            return true
        }

        return false
    }

    private fun isBlockedByDomain(host: String): Boolean {
        var currentHost = host
        while (currentHost.isNotEmpty()) {
            if (blockedDomains.contains(currentHost)) {
                return true
            }
            currentHost = currentHost.substringAfter('.', "")
            if (currentHost == host) {
                break
            }
            if (!currentHost.contains('.')) {
                if (blockedDomains.contains(currentHost)) {
                    return true
                }
                break
            }
        }
        return false
    }

    private fun isBlockedByPattern(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return blockedPatterns.any { pattern ->
            pattern.matcher(lowerUrl).matches()
        }
    }

    private fun containsAdKeywords(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return adKeywords.any { keyword -> lowerUrl.contains(keyword) }
    }

    fun addToWhitelist(domain: String) {
        allowedDomains.add(domain.removePrefix("www.").lowercase())
    }

    fun removeFromWhitelist(domain: String) {
        allowedDomains.remove(domain.removePrefix("www.").lowercase())
    }

    fun getBlockedCount(): Int = blockedDomains.size

    fun getPatternCount(): Int = blockedPatterns.size
}

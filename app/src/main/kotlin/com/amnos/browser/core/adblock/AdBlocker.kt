package com.amnos.browser.core.adblock

import android.content.Context
import com.amnos.browser.core.session.AmnosLog
import java.io.BufferedReader
import java.io.InputStreamReader
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.regex.Pattern

class AdBlocker(context: Context) {
    private val blockedDomains = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    private val blockedPatterns = java.util.concurrent.CopyOnWriteArrayList<Pattern>()
    private val allowedDomains = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    
    // Flag to track initialization state
    @Volatile
    private var isInitialized = false

    // Known ad/tracker URL patterns
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

    init {
        // Load in a background thread to prevent UI jank
        Thread {
            try {
                loadFilterLists(context)
                compileAdPatterns()
                isInitialized = true
                AmnosLog.d("AdBlocker", "Background initialization complete. Blocked domains: ${blockedDomains.size}")
            } catch (e: Exception) {
                AmnosLog.e("AdBlocker", "Async initialization failed", e)
            }
        }.start()
    }

    private fun loadFilterLists(context: Context) {
        // Load main blocklist
        loadFilterList(context, "blocklist.txt")
        
        // Load comprehensive blocklist if available
        try {
            loadFilterList(context, "blocklist_comprehensive.txt")
        } catch (e: Exception) {
            AmnosLog.d("AdBlocker", "Comprehensive blocklist not found, using main list only")
        }
        
        AmnosLog.d("AdBlocker", "Loaded ${blockedDomains.size} blocked domains")
    }

    private fun loadFilterList(context: Context, filename: String) {
        try {
            context.assets.open(filename).use { inputStream ->
                InputStreamReader(inputStream).buffered().use { reader ->
                    reader.forEachLine { line ->
                        normalizeRule(line)?.let { blockedDomains.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            AmnosLog.e("AdBlocker", "Error loading $filename", e)
        }
    }
    
    private fun compileAdPatterns() {
        knownAdPatterns.forEach { pattern ->
            try {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
            } catch (e: Exception) {
                AmnosLog.e("AdBlocker", "Error compiling pattern: $pattern", e)
            }
        }
    }

    private fun normalizeRule(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("!") || trimmed.startsWith("[")) return null
        if (trimmed.startsWith("#")) return null // Comment

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
        // Quick check for allowed domains (whitelist)
        val host = url.toHttpUrlOrNull()?.host?.removePrefix("www.")?.lowercase() ?: return false
        
        if (allowedDomains.contains(host)) {
            return false
        }
        
        // Check domain blocklist
        if (isBlockedByDomain(host)) {
            AmnosLog.d("AdBlocker", "Blocked by domain: $url")
            return true
        }
        
        // Check URL pattern matching
        if (isBlockedByPattern(url)) {
            AmnosLog.d("AdBlocker", "Blocked by pattern: $url")
            return true
        }
        
        // Check for common ad/tracker keywords in path
        if (containsAdKeywords(url)) {
            AmnosLog.d("AdBlocker", "Blocked by keyword: $url")
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
        val adKeywords = listOf(
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

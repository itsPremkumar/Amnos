package com.amnos.browser.core.adblock

import android.content.Context
import com.amnos.browser.core.session.AmnosLog
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class AdBlocker(context: Context) {
    private val registry = FilterRegistry(context)
    private val allowedDomains = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

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
        val blocked = registry.blockedDomains
        while (currentHost.isNotEmpty()) {
            if (blocked.contains(currentHost)) return true
            currentHost = currentHost.substringAfter('.', "")
            if (currentHost == host) break
            if (!currentHost.contains('.')) {
                if (blocked.contains(currentHost)) return true
                break
            }
        }
        return false
    }

    private fun isBlockedByPattern(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return registry.compiledPatterns.any { it.matcher(lowerUrl).matches() }
    }

    private fun containsAdKeywords(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return registry.adKeywords.any { lowerUrl.contains(it) }
    }

    fun addToWhitelist(domain: String) {
        allowedDomains.add(domain.removePrefix("www.").lowercase())
    }

    fun removeFromWhitelist(domain: String) {
        allowedDomains.remove(domain.removePrefix("www.").lowercase())
    }

    fun getBlockedCount(): Int = registry.blockedDomains.size
    fun getPatternCount(): Int = registry.compiledPatterns.size
}

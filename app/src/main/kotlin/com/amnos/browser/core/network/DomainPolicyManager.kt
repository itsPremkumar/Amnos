package com.amnos.browser.core.network

import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap

object DomainPolicyManager {
    // Persistent-ish sets for the current session
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>().apply {
        addAll(setOf(
            "duckduckgo.com",
            "google.com",
            "www.google.com",
            "youtube.com",
            "www.youtube.com",
            "youtube-nocookie.com",
            "ytimg.com",
            "ggpht.com",
            "gstatic.com",
            "youtubei.googleapis.com",
            "googlevideo.com",
            "vimeo.com",
            "player.vimeo.com",
            "vimeocdn.com",
            "vod-progressive.akamaized.net",
            "wikipedia.org",
            "en.wikipedia.org"
        ))
    }

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()

    fun isAllowed(url: String): Boolean {
        return try {
            val host = url.toHttpUrlOrNull()?.host ?: Uri.parse(url).host ?: return false
            val normalizedHost = host.lowercase()

            // 1. Check EXPLICIT BLOCKS first (Precedence)
            if (blockedDomains.any { blocked -> matches(normalizedHost, blocked) }) {
                return false
            }

            // 2. Check ALLOWS
            allowedDomains.any { allowed -> matches(normalizedHost, allowed) }
        } catch (e: Exception) {
            false
        }
    }

    fun isExplicitlyBlocked(url: String): Boolean {
        return try {
            val host = url.toHttpUrlOrNull()?.host ?: Uri.parse(url).host ?: return false
            val normalizedHost = host.lowercase()
            blockedDomains.any { blocked -> matches(normalizedHost, blocked) }
        } catch (e: Exception) {
            false
        }
    }

    fun addAllowedDomain(domain: String) {
        allowedDomains.add(domain.lowercase().trim())
        blockedDomains.remove(domain.lowercase().trim())
    }

    fun removeAllowedDomain(domain: String) {
        allowedDomains.remove(domain.lowercase().trim())
    }

    fun addBlockedDomain(domain: String) {
        blockedDomains.add(domain.lowercase().trim())
        allowedDomains.remove(domain.lowercase().trim())
    }

    fun removeBlockedDomain(domain: String) {
        blockedDomains.remove(domain.lowercase().trim())
    }

    fun getAllowedDomains(): List<String> = allowedDomains.toList().sorted()
    fun getBlockedDomains(): List<String> = blockedDomains.toList().sorted()

    private fun matches(host: String, pattern: String): Boolean {
        return if (pattern.startsWith("*.")) {
            val suffix = pattern.substring(2)
            host == suffix || host.endsWith(".$suffix")
        } else {
            host == pattern || host.endsWith(".$pattern")
        }
    }

    fun clear() {
        // Reset to minimal safe set or empty? 
        // For Amnos, we reset to defaults on session termination anyway.
        allowedDomains.clear()
        blockedDomains.clear()
    }
}

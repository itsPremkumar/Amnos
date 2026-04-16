package com.amnos.browser.core.network

import android.net.Uri

object DomainPolicyManager {
    // Initial static whitelist for Phase 3 testing to prevent total breakage
    private val allowedDomains = setOf(
        "duckduckgo.com",
        "google.com",
        "www.google.com",
        "youtube.com",
        "www.youtube.com",
        "googlevideo.com",
        "wikipedia.org",
        "en.wikipedia.org"
    )

    fun isAllowed(url: String): Boolean {
        return try {
            val host = Uri.parse(url).host ?: return false
            // Check exact match or subdomain
            allowedDomains.any { allowed ->
                host == allowed || host.endsWith(".$allowed")
            }
        } catch (e: Exception) {
            false
        }
    }
}

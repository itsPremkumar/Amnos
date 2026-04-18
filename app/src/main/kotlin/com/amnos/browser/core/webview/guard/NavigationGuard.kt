package com.amnos.browser.core.webview.guard

import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.amnos.browser.core.network.BlockReason
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.security.PrivacyPolicy

class NavigationGuard(
    private val networkSecurityManager: NetworkSecurityManager,
    private val securityController: SecurityController,
    private val policyProvider: () -> PrivacyPolicy,
    private val onNavigationRequested: (String) -> Boolean
) {
    fun shouldOverride(request: WebResourceRequest?): Boolean {
        request ?: return false
        val uri = request.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        
        // 1. INTENT JAIL
        if (scheme != "http" && scheme != "https") {
            val level = if (policyProvider().networkFirewallLevel == com.amnos.browser.core.security.FirewallLevel.PARANOID) "CRITICAL" else "WARN"
            AmnosLog.w("NavigationGuard", "INTENT JAIL: Blocked escape attempt to scheme: $scheme ($level)")
            securityController.logInternal("SecurityJail", "Blocked external app launch: $scheme", level)
            return true 
        }

        // 2. MAIN FRAME SANITIZATION
        if (request.isForMainFrame) {
            val url = uri.toString()
            val sanitizedUrl = networkSecurityManager.sanitizeNavigationUrl(url)
            if (sanitizedUrl == null) {
                return true // Should be handled by showBlockedPage in client
            }

            if (sanitizedUrl.startsWith("about:blank", ignoreCase = true)) {
                return false
            }

            if (sanitizedUrl != url) {
                return onNavigationRequested(sanitizedUrl)
            }
        }

        return onNavigationRequested(uri.toString())
    }
}

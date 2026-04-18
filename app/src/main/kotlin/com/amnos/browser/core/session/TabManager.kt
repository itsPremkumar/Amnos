package com.amnos.browser.core.session

import android.content.Context
import com.amnos.browser.core.fingerprint.FingerprintManager
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.webview.PrivacyWebChromeClient
import com.amnos.browser.core.webview.PrivacyWebViewClient
import com.amnos.browser.core.webview.AmnosWebView
import com.amnos.browser.core.webview.SecureWebView
import com.amnos.browser.core.adblock.AdBlocker

class TabManager(
    private val context: Context,
    private val adBlocker: AdBlocker,
    private val networkSecurityManager: NetworkSecurityManager,
    private val securityController: SecurityController,
    private val webViewFactory: (Context) -> AmnosWebView = { ctx -> SecureWebView(ctx) }
) {
    private val tabs = mutableListOf<TabInstance>()

    fun getTabs(): List<TabInstance> = tabs.toList()

    fun createTab(
        activeSessionId: String,
        privacyPolicy: PrivacyPolicy,
        onStateChanged: (url: String, back: Boolean, forward: Boolean) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onTrackerBlocked: () -> Unit,
        onNavigationRequested: (String) -> Boolean,
        onNavigationCommitted: (String) -> Unit,
        onNavigationFailed: (String?) -> Unit,
        onKeyboardRequested: (Boolean) -> Unit,
        onSecurityEvent: (String) -> Unit,
        touchSession: () -> Unit,
        buildInjectionScript: (com.amnos.browser.core.fingerprint.DeviceProfile) -> String
    ): TabInstance {
        AmnosLog.d("TabManager", "Creating new tab instance")
        val tabId = FingerprintManager.newTabId()
        val profile = FingerprintManager.generateCoherentProfile(activeSessionId, tabId, privacyPolicy)

        val webView = webViewFactory(context)
        val finalScript = buildInjectionScript(profile)

        webView.applyHardening(profile, privacyPolicy, finalScript, onSecurityEvent)
        webView.resumeTimers()

        // Local State Changed wrapper to touch session
        val wrappedStateChanged: (String) -> Unit = { url ->
            touchSession()
            onStateChanged(url, webView.canGoBack(), webView.canGoForward())
        }

        val client = PrivacyWebViewClient(
            adBlocker = adBlocker,
            deviceProfile = profile,
            networkSecurityManager = networkSecurityManager,
            securityController = securityController,
            policyProvider = { privacyPolicy },
            onTrackerBlocked = onTrackerBlocked,
            onStateChanged = wrappedStateChanged,
            onNavigationRequested = onNavigationRequested,
            onNavigationCommitted = onNavigationCommitted,
            onNavigationFailed = onNavigationFailed
        )

        webView.setWebViewClient(client)
        webView.setWebChromeClient(PrivacyWebChromeClient(onProgressChanged))

        val tab = TabInstance(
            sessionId = activeSessionId,
            tabId = tabId,
            profile = profile,
            webView = webView,
            onKeyboardRequested = onKeyboardRequested,
            onSecurityEvent = onSecurityEvent
        )
        tabs.add(tab)
        touchSession()
        return tab
    }

    fun removeTab(tab: TabInstance, touchSession: () -> Unit) {
        tab.webView.surgicalTeardown()
        tabs.remove(tab)
        touchSession()
    }

    fun clearAll(touchSession: (() -> Unit)? = null) {
        tabs.forEach { it.webView.surgicalTeardown() }
        tabs.clear()
        touchSession?.invoke()
    }
    
    fun indexOf(tab: TabInstance): Int = tabs.indexOf(tab)
    
    fun addAt(index: Int, tab: TabInstance) {
        tabs.add(index.coerceAtMost(tabs.size), tab)
    }
}

package com.privacy.browser.core.session

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.fingerprint.FingerprintManager
import com.privacy.browser.core.fingerprint.ScriptInjector
import com.privacy.browser.core.network.NetworkSecurityManager
import com.privacy.browser.core.security.JavaScriptMode
import com.privacy.browser.core.security.PrivacyPolicy
import com.privacy.browser.core.security.WebGlMode
import com.privacy.browser.core.webview.PrivacyWebChromeClient
import com.privacy.browser.core.webview.PrivacyWebViewClient
import com.privacy.browser.core.webview.SecureWebView

class SessionManager(private val context: Context) {
    private val adBlocker = AdBlocker(context)
    private val tabs = mutableListOf<TabInstance>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { timeoutListener?.invoke() }
    private var timeoutListener: (() -> Unit)? = null
    private var activeSessionId: String = FingerprintManager.newSessionId()

    val securityController = SecurityController()
    val storageController = StorageController(context)
    private val networkSecurityManager = NetworkSecurityManager { privacyPolicy }

    var privacyPolicy: PrivacyPolicy = PrivacyPolicy()
        private set

    private val baseObfuscatorScript: String by lazy {
        context.assets.open("FingerprintObfuscator.js").bufferedReader().use { it.readText() }
    }

    val sessionId: String
        get() = activeSessionId

    val activeTabs: List<TabInstance>
        get() = tabs.toList()

    fun registerTimeoutListener(listener: () -> Unit) {
        timeoutListener = listener
        touchSession()
    }

    fun touchSession() {
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, privacyPolicy.sessionTimeoutMillis)
    }

    fun createTab(
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onTrackerBlocked: () -> Unit
    ): TabInstance {
        val tabId = FingerprintManager.newTabId()
        val profile = FingerprintManager.generateCoherentProfile(activeSessionId, tabId)
        val webView = SecureWebView(context)
        val finalScript = buildInjectionScript(profile)
        webView.applyHardening(profile, privacyPolicy, finalScript)
        webView.resumeTimers()

        webView.setDownloadListener { url, _, _, _, _ ->
            Log.d("SessionManager", "Ephemeral download triggered: $url")
            storageController.downloadEphemeralFile(url, profile.userAgent)
        }

        val client = PrivacyWebViewClient(
            adBlocker = adBlocker,
            deviceProfile = profile,
            networkSecurityManager = networkSecurityManager,
            securityController = securityController,
            onTrackerBlocked = onTrackerBlocked
        ) { url ->
            touchSession()
            onStateChanged(url, webView.canGoBack(), webView.canGoForward())
        }

        webView.webViewClient = client
        webView.webChromeClient = PrivacyWebChromeClient(onProgressChanged)

        val tab = TabInstance(
            sessionId = activeSessionId,
            tabId = tabId,
            profile = profile,
            webView = webView
        )
        tabs.add(tab)
        touchSession()
        return tab
    }

    fun recreateTab(
        tab: TabInstance,
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onTrackerBlocked: () -> Unit
    ): TabInstance {
        val previousUrl = tab.currentUrl
        val previousIndex = tabs.indexOf(tab).coerceAtLeast(0)
        removeTab(tab)
        val replacement = createTab(onStateChanged, onProgressChanged, onTrackerBlocked)
        tabs.remove(replacement)
        tabs.add(previousIndex.coerceAtMost(tabs.size), replacement)
        previousUrl?.let { loadUrl(replacement, it) }
        return replacement
    }

    fun updatePrivacyPolicy(update: (PrivacyPolicy) -> PrivacyPolicy) {
        privacyPolicy = update(privacyPolicy)
        tabs.forEach { tab ->
            tab.webView.updateRuntimePolicy(tab.profile, privacyPolicy, buildInjectionScript(tab.profile))
        }
        touchSession()
    }

    fun setJavaScriptMode(mode: JavaScriptMode) {
        updatePrivacyPolicy { it.copy(javascriptMode = mode) }
    }

    fun setWebGlEnabled(enabled: Boolean) {
        updatePrivacyPolicy {
            it.copy(webGlMode = if (enabled) WebGlMode.SPOOF else WebGlMode.DISABLED)
        }
    }

    fun loadUrl(tab: TabInstance, rawUrl: String): Boolean {
        val sanitizedUrl = networkSecurityManager.sanitizeNavigationUrl(rawUrl) ?: return false
        tab.currentUrl = sanitizedUrl
        if (sanitizedUrl == "about:blank") {
            tab.webView.loadUrl(sanitizedUrl)
            return true
        }
        val headers = networkSecurityManager.buildNavigationHeaders(
            url = sanitizedUrl,
            profile = tab.profile,
            topLevelHost = Uri.parse(sanitizedUrl).host
        )
        tab.webView.loadUrl(sanitizedUrl, headers)
        touchSession()
        return true
    }

    fun removeTab(tab: TabInstance) {
        tab.webView.clearVolatileState()
        tab.webView.destroy()
        tabs.remove(tab)
        purgeGlobalStorage()
        touchSession()
    }

    fun killAll(terminateProcess: Boolean = false) {
        Log.d("SessionManager", "AMNOS GHOST WIPE ACTIVATED")
        mainHandler.removeCallbacks(timeoutRunnable)

        storageController.wipeClipboard()
        storageController.clearVolatileDownloads()
        securityController.clearLog()

        tabs.toList().forEach { tab ->
            tab.webView.clearVolatileState()
            tab.webView.destroy()
        }
        tabs.clear()
        purgeGlobalStorage()
        activeSessionId = FingerprintManager.newSessionId()

        if (terminateProcess) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun purgeGlobalStorage() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies { Log.d("SessionManager", "Cookies purged") }
        cookieManager.flush()

        WebStorage.getInstance().deleteAllData()
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        webViewDB.clearUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()
        try {
            android.webkit.WebView.clearClientCertPreferences(null)
        } catch (ignored: Throwable) {
            Log.w("SessionManager", "Client cert wipe unavailable", ignored)
        }
    }

    private fun buildInjectionScript(profile: DeviceProfile): String {
        return ScriptInjector(profile, privacyPolicy).wrapScript(baseObfuscatorScript)
    }
}

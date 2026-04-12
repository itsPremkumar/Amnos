package com.privacy.browser.core.session

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.fingerprint.FingerprintManager
import com.privacy.browser.core.fingerprint.ScriptInjector
import com.privacy.browser.core.network.LoopbackProxyServer
import com.privacy.browser.core.network.NetworkSecurityManager
import com.privacy.browser.core.security.FingerprintProtectionLevel
import com.privacy.browser.core.security.JavaScriptMode
import com.privacy.browser.core.security.PrivacyPolicy
import com.privacy.browser.core.security.WebGlMode
import com.privacy.browser.core.webview.PrivacyWebChromeClient
import com.privacy.browser.core.webview.PrivacyWebViewClient
import com.privacy.browser.core.webview.SecureWebView
import org.json.JSONObject

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
    private val loopbackProxyServer = LoopbackProxyServer(
        networkSecurityManager = networkSecurityManager,
        onTunnelOpened = { id, host, port ->
            securityController.addConnection(id, host, port, "TUNNEL")
        },
        onTunnelClosed = { id ->
            securityController.removeConnection(id)
        }
    )

    var privacyPolicy: PrivacyPolicy = PrivacyPolicy(
        enableRemoteDebugging = context.getSharedPreferences("amnos_debug_prefs", Context.MODE_PRIVATE)
            .getBoolean("enable_remote_debugging", false)
    )
        private set

    private val baseObfuscatorScript: String by lazy {
        context.assets.open("FingerprintObfuscator.js").bufferedReader().use { it.readText() }
    }

    init {
        Log.d("SessionManager", "Initializing SessionManager")
        securityController.setFingerprintLevel(privacyPolicy.fingerprintProtectionLevel)
        try {
            configureProxy()
            Log.d("SessionManager", "Proxy configured successfully")
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to configure proxy during init", e)
        }
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
        onTrackerBlocked: () -> Unit,
        onNavigationRequested: (String) -> Boolean
    ): TabInstance {
        Log.d("SessionManager", "Creating new tab instance")
        val tabId = FingerprintManager.newTabId()
        val profile = FingerprintManager.generateCoherentProfile(
            activeSessionId,
            tabId,
            privacyPolicy.fingerprintProtectionLevel
        )
        
        Log.d("SessionManager", "Instantiating SecureWebView")
        val webView = SecureWebView(context)
        val finalScript = buildInjectionScript(profile)
        
        Log.d("SessionManager", "Applying hardening to WebView")
        webView.applyHardening(profile, privacyPolicy, finalScript, ::handleSecurityEvent)
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
            onTrackerBlocked = onTrackerBlocked,
            onStateChanged = { url ->
                touchSession()
                onStateChanged(url, webView.canGoBack(), webView.canGoForward())
            },
            onNavigationRequested = onNavigationRequested
        )

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
        onTrackerBlocked: () -> Unit,
        onNavigationRequested: (String) -> Boolean
    ): TabInstance {
        val previousUrl = tab.currentUrl
        val previousIndex = tabs.indexOf(tab).coerceAtLeast(0)
        removeTab(tab)
        val replacement = createTab(
            onStateChanged = onStateChanged,
            onProgressChanged = onProgressChanged,
            onTrackerBlocked = onTrackerBlocked,
            onNavigationRequested = onNavigationRequested
        )
        tabs.remove(replacement)
        tabs.add(previousIndex.coerceAtMost(tabs.size), replacement)
        previousUrl?.let { loadUrl(replacement, it) }
        return replacement
    }

    fun updatePrivacyPolicy(update: (PrivacyPolicy) -> PrivacyPolicy) {
        val oldDebugging = privacyPolicy.enableRemoteDebugging
        privacyPolicy = update(privacyPolicy)
        
        if (privacyPolicy.enableRemoteDebugging != oldDebugging) {
            context.getSharedPreferences("amnos_debug_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("enable_remote_debugging", privacyPolicy.enableRemoteDebugging)
                .apply()
        }

        securityController.setFingerprintLevel(privacyPolicy.fingerprintProtectionLevel)
        configureProxy()
        tabs.forEach { tab ->
            tab.webView.updateRuntimePolicy(
                tab.profile,
                privacyPolicy,
                buildInjectionScript(tab.profile),
                ::handleSecurityEvent
            )
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

    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) {
        updatePrivacyPolicy { current ->
            current.copy(
                fingerprintProtectionLevel = level,
                webGlMode = when (level) {
                    FingerprintProtectionLevel.STRICT -> WebGlMode.DISABLED
                    FingerprintProtectionLevel.DISABLED -> WebGlMode.SPOOF // Allow WebGL if not strict/disabled (spoof is safer than raw but works)
                    else -> current.webGlMode
                },
                blockInlineScripts = if (level == FingerprintProtectionLevel.STRICT) true else current.blockInlineScripts,
                strictFirstPartyIsolation = level != FingerprintProtectionLevel.DISABLED
            )
        }
    }

    fun loadUrl(tab: TabInstance, rawUrl: String): Boolean {
        securityController.logInternal("SessionManager", "loadUrl raw: $rawUrl", "DEBUG")
        val sanitizedUrl = networkSecurityManager.sanitizeNavigationUrl(rawUrl) ?: run {
            securityController.logInternal("SessionManager", "Sanitization REJECTED URL: $rawUrl", "WARN")
            return false
        }
        securityController.logInternal("SessionManager", "loadUrl sanitized: $sanitizedUrl", "DEBUG")
        
        tab.currentUrl = sanitizedUrl
        tab.siteKey = networkSecurityManager.siteKeyForUrl(sanitizedUrl)
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

    fun shouldRecreateForTopLevelNavigation(tab: TabInstance, nextUrl: String): Boolean {
        if (!privacyPolicy.strictFirstPartyIsolation) {
            return false
        }
        if (tab.currentUrl.isNullOrBlank()) {
            return false
        }
        return networkSecurityManager.isCrossSiteNavigation(tab.currentUrl, nextUrl)
    }

    fun removeTab(tab: TabInstance) {
        tab.webView.clearVolatileState()
        tab.webView.destroy()
        tabs.remove(tab)
        purgeGlobalStorage()
        touchSession()
    }

    fun killAll(terminateProcess: Boolean = false) {
        Log.d("SessionManager", "AMNOS GHOST WIPE ACTIVATED (terminateProcess=$terminateProcess)")
        mainHandler.removeCallbacks(timeoutRunnable)

        storageController.wipeClipboard()
        storageController.clearVolatileDownloads()
        securityController.clearLog()

        tabs.toList().forEach { tab ->
            try {
                tab.webView.clearVolatileState()
                tab.webView.destroy()
            } catch (e: Exception) {
                Log.e("SessionManager", "Error destroying webView during wipe", e)
            }
        }
        tabs.clear()
        purgeGlobalStorage()
        activeSessionId = FingerprintManager.newSessionId()
        configureProxy()

        if (terminateProcess) {
            Log.w("SessionManager", "SELF-TERMINATING PROCESS AS REQUESTED")
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

    private fun configureProxy() {
        if (privacyPolicy.forceRelaxSecurityForDebug) {
            Log.w("SessionManager", "TOTAL PROXY BYPASS - Diagnostics mode active")
            ProxyController.getInstance().clearProxyOverride(ContextCompat.getMainExecutor(context)) {
                securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
            }
            return
        }

        if (!privacyPolicy.enforceLoopbackProxy || !WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
            return
        }

        val port = loopbackProxyServer.start()
        val proxyConfig = ProxyConfig.Builder()
            .removeImplicitRules()
            .addProxyRule("http://127.0.0.1:$port", ProxyConfig.MATCH_ALL_SCHEMES)
            .build()

        ProxyController.getInstance().setProxyOverride(
            proxyConfig,
            ContextCompat.getMainExecutor(context)
        ) {
            securityController.updateProxyStatus(active = true, dohGlobal = true, port = port)
        }
    }

    private fun handleSecurityEvent(rawMessage: String) {
        try {
            val payload = JSONObject(rawMessage)
            when (payload.optString("type")) {
                "webrtc" -> {
                    securityController.recordWebRtcAttempt(
                        detail = payload.optString("detail", "webrtc"),
                        blocked = payload.optBoolean("blocked", true)
                    )
                }
                "websocket" -> {
                    val detail = payload.optString("url", payload.optString("detail", "websocket"))
                    val blocked = payload.optBoolean("blocked", true)
                    securityController.recordWebSocketAttempt(detail, blocked)

                    val socketId = payload.optString("id")
                    when (payload.optString("state")) {
                        "open" -> securityController.addConnection(
                            id = socketId,
                            host = payload.optString("host"),
                            port = payload.optInt("port", 443),
                            type = "WEBSOCKET"
                        )
                        "close", "blocked" -> securityController.removeConnection(socketId)
                    }
                }
            }
        } catch (error: Exception) {
            Log.w("SessionManager", "Failed to parse security event: $rawMessage", error)
        }
    }
}

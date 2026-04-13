package com.amnos.browser.core.session

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.core.content.edit
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.amnos.browser.BuildConfig
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.fingerprint.FingerprintManager
import com.amnos.browser.core.fingerprint.ScriptInjector
import com.amnos.browser.core.network.LoopbackProxyServer
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.JavaScriptMode
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.WebGlMode
import com.amnos.browser.core.webview.PrivacyWebChromeClient
import com.amnos.browser.core.webview.PrivacyWebViewClient
import com.amnos.browser.core.webview.SecureWebView
import com.amnos.browser.core.service.StorageService
import com.amnos.browser.core.model.*
import org.json.JSONObject

class SessionManager(private val context: Context) {
    private val adBlocker = AdBlocker(context)
    private val tabs = mutableListOf<TabInstance>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { timeoutListener?.invoke() }
    private var timeoutListener: (() -> Unit)? = null
    private var onSessionWiped: (() -> Unit)? = null
    private var activeSessionId: String = FingerprintManager.newSessionId()

    val securityController = SecurityController()
    val storageService = StorageService(context)
    private val networkSecurityManager = NetworkSecurityManager(adBlocker) { privacyPolicy }
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
        enableRemoteDebugging = BuildConfig.DEBUG &&
            context.getSharedPreferences("amnos_debug_prefs", Context.MODE_PRIVATE)
                .getBoolean("enable_remote_debugging", false)
    )
        private set

    private val baseObfuscatorScript: String by lazy {
        context.assets.open("FingerprintObfuscator.js").bufferedReader().use { it.readText() }
    }

    init {
        AmnosLog.attach { securityController }
        AmnosLog.d("SessionManager", "Initializing SessionManager")
        securityController.setFingerprintLevel(privacyPolicy.fingerprintProtectionLevel)
        try {
            storageService.purgeGlobalStorage(securityController::logInternal)
            storageService.clearVolatileDownloads()
            configureProxy()
            AmnosLog.d("SessionManager", "Proxy configured successfully")
        } catch (e: Exception) {
            AmnosLog.e("SessionManager", "Failed to configure proxy during init", e)
        }
    }

    val sessionId: String
        get() = activeSessionId

    fun registerTimeoutListener(listener: () -> Unit) {
        timeoutListener = listener
        touchSession()
    }

    fun registerWipeListener(listener: () -> Unit) {
        onSessionWiped = listener
    }

    fun touchSession() {
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, privacyPolicy.sessionTimeoutMillis)
    }

    fun createTab(
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onTrackerBlocked: () -> Unit,
        onNavigationRequested: (String) -> Boolean,
        onNavigationCommitted: (String) -> Unit,
        onNavigationFailed: (String?) -> Unit,
        onKeyboardRequested: (show: Boolean) -> Unit
    ): TabInstance {
        AmnosLog.d("SessionManager", "Creating new tab instance")
        val tabId = FingerprintManager.newTabId()
        val profile = FingerprintManager.generateCoherentProfile(
            activeSessionId,
            tabId,
            privacyPolicy.fingerprintProtectionLevel
        )

        AmnosLog.d("SessionManager", "Instantiating SecureWebView")
        val webView = SecureWebView(context)
        val finalScript = buildInjectionScript(profile)

        AmnosLog.d("SessionManager", "Applying hardening to WebView")
        webView.applyHardening(profile, privacyPolicy, finalScript) { rawMessage ->
            handleSecurityEvent(rawMessage, onKeyboardRequested)
        }
        webView.resumeTimers()

        webView.setDownloadListener { url, _, _, _, _ ->
            AmnosLog.d("SessionManager", "Ephemeral download triggered: $url")
            storageService.downloadEphemeralFile(url, profile.userAgent)
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
            onNavigationRequested = onNavigationRequested,
            onNavigationCommitted = onNavigationCommitted,
            onNavigationFailed = onNavigationFailed
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
        onNavigationRequested: (String) -> Boolean,
        onNavigationCommitted: (String) -> Unit,
        onNavigationFailed: (String?) -> Unit,
        onKeyboardRequested: (show: Boolean) -> Unit
    ): TabInstance {
        val previousUrl = tab.currentUrl
        val previousIndex = tabs.indexOf(tab).coerceAtLeast(0)
        removeTab(tab)
        val replacement = createTab(
            onStateChanged = onStateChanged,
            onProgressChanged = onProgressChanged,
            onTrackerBlocked = onTrackerBlocked,
            onNavigationRequested = onNavigationRequested,
            onNavigationCommitted = onNavigationCommitted,
            onNavigationFailed = onNavigationFailed,
            onKeyboardRequested = onKeyboardRequested
        )
        tabs.remove(replacement)
        tabs.add(previousIndex.coerceAtMost(tabs.size), replacement)
        previousUrl?.let { loadUrl(replacement, it) }
        return replacement
    }

    fun updatePrivacyPolicy(update: (PrivacyPolicy) -> PrivacyPolicy) {
        val oldDebugging = privacyPolicy.enableRemoteDebugging
        privacyPolicy = update(privacyPolicy).let { updated ->
            if (BuildConfig.DEBUG) updated else updated.copy(enableRemoteDebugging = false, forceRelaxSecurityForDebug = false)
        }
        
        if (BuildConfig.DEBUG && privacyPolicy.enableRemoteDebugging != oldDebugging) {
            context.getSharedPreferences("amnos_debug_prefs", Context.MODE_PRIVATE).edit {
                putBoolean("enable_remote_debugging", privacyPolicy.enableRemoteDebugging)
            }
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
            topLevelHost = sanitizedUrl.toUri().host
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
        storageService.purgeGlobalStorage(securityController::logInternal)
        touchSession()
    }

    fun killAll(terminateProcess: Boolean = false) {
        AmnosLog.d("SessionManager", "AMNOS GHOST WIPE ACTIVATED (terminateProcess=$terminateProcess)")
        mainHandler.removeCallbacks(timeoutRunnable)

        storageService.wipeClipboard()
        storageService.clearVolatileDownloads()
        securityController.clearLog()

        tabs.toList().forEach { tab ->
            try {
                tab.webView.clearVolatileState()
                tab.webView.destroy()
            } catch (e: Exception) {
                AmnosLog.e("SessionManager", "Error destroying webView during wipe", e)
            }
        }
        tabs.clear()
        storageService.purgeGlobalStorage(securityController::logInternal)
        activeSessionId = FingerprintManager.newSessionId()
        onSessionWiped?.invoke()
        configureProxy()

        if (terminateProcess) {
            AmnosLog.w("SessionManager", "SELF-TERMINATING PROCESS AS REQUESTED")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun buildInjectionScript(profile: DeviceProfile): String {
        return ScriptInjector(profile, privacyPolicy).wrapScript(baseObfuscatorScript)
    }

    @SuppressLint("RequiresFeature")
    private fun configureProxy() {
        if (privacyPolicy.forceRelaxSecurityForDebug) {
            AmnosLog.w("SessionManager", "TOTAL PROXY BYPASS - Diagnostics mode active")
            clearProxyOverride()
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

    private fun handleSecurityEvent(rawMessage: String, onKeyboardRequested: ((Boolean) -> Unit)? = null) {
        try {
            val payload = JSONObject(rawMessage)
            when (payload.optString("type")) {
                "keyboard_event" -> {
                    val action = payload.optString("action")
                    onKeyboardRequested?.invoke(action == "show")
                }
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
            AmnosLog.w("SessionManager", "Failed to parse security event: $rawMessage", error)
        }
    }

    @SuppressLint("RequiresFeature")
    private fun clearProxyOverride() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
            return
        }

        ProxyController.getInstance().clearProxyOverride(ContextCompat.getMainExecutor(context)) {
            securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
        }
    }
}

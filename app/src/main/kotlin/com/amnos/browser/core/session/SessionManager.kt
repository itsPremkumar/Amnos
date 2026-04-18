package com.amnos.browser.core.session

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import org.json.JSONObject
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.WebGlMode
import com.amnos.browser.core.webview.PrivacyWebChromeClient
import com.amnos.browser.core.webview.PrivacyWebViewClient
import com.amnos.browser.core.webview.AmnosWebView
import com.amnos.browser.core.webview.SecureWebView
import com.amnos.browser.core.service.StorageService
import com.amnos.browser.core.security.KeyManager
import com.amnos.browser.core.wipe.SuperWipeEngine
import com.amnos.browser.core.wipe.WipeReason
import com.amnos.browser.core.model.*
import com.amnos.browser.core.network.NetworkTrafficConfigurator

class SessionManager private constructor(
    private val context: Context,
    private val webViewDataSuffix: String,
    private val webViewFactory: (Context) -> AmnosWebView = { Context -> SecureWebView(Context) }
) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(
            context: Context? = null,
            webViewDataSuffix: String? = null,
            webViewFactory: ((Context) -> AmnosWebView)? = null
        ): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val ctx = context?.applicationContext ?: throw IllegalStateException("SessionManager not initialized and no context provided")
                    SessionManager(
                        ctx,
                        webViewDataSuffix ?: "Amnos_Secure_Profile",
                        webViewFactory ?: { Context -> SecureWebView(Context) }
                    ).also { INSTANCE = it }
                }
            }
        }
    }
    private val adBlocker = AdBlocker(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val wipeDebounceMs = 5_000L
    private val timeoutRunnable = Runnable { timeoutListener?.invoke() }
    private var timeoutListener: (() -> Unit)? = null
    private val onSessionWipedListeners = mutableListOf<() -> Unit>()
    private var activeSessionId: String = FingerprintManager.newSessionId()
    private var lastWipeElapsedRealtime: Long = 0L

    val securityController = SecurityController()
    val storageService = StorageService(context, webViewDataSuffix)
    private val networkSecurityManager = NetworkSecurityManager(adBlocker) { privacyPolicy }
    
    private val tabManager = TabManager(context, adBlocker, networkSecurityManager, securityController, webViewFactory)
    private val securityEventRouter = SecurityEventRouter(securityController)
    
    private val loopbackProxyServer = LoopbackProxyServer(
        networkSecurityManager = networkSecurityManager,
        onTunnelOpened = { id, host, port ->
            securityController.addConnection(id, host, port, "TUNNEL")
        },
        onTunnelClosed = { id ->
            securityController.removeConnection(id)
        }
    )

    private val networkTrafficConfigurator = NetworkTrafficConfigurator(context, loopbackProxyServer, securityController)

    private val superWipeEngine by lazy {
        SuperWipeEngine(
            tabManager = tabManager,
            storageService = storageService,
            securityController = securityController,
            loopbackProxyServer = loopbackProxyServer,
            onNewSessionNeeded = {
                activeSessionId = FingerprintManager.newSessionId()
                KeyManager.generateSessionKey(context)
                networkTrafficConfigurator.configure(privacyPolicy)
            },
            onWipeCompleted = {
                onSessionWipedListeners.forEach { it.invoke() }
            }
        )
    }

    var privacyPolicy: PrivacyPolicy = PrivacyPolicy().let { initial ->
        if (BuildConfig.DEBUG) initial else initial.copy(debugBlockRemoteDebugging = true, debugLockdownMode = true)
    }
        private set

    private val baseObfuscatorScript: String by lazy {
        try {
            context.assets.open("FingerprintObfuscator.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                AmnosLog.w("SessionManager", "Failed to load FingerprintObfuscator.js, using empty script for test")
                ""
            } else {
                throw e
            }
        }
    }

        init {
        AmnosLog.attach { securityController }
        AmnosLog.d("SessionManager", "Initializing SessionManager (Modular)")
        securityController.setFingerprintLevel(privacyPolicy.hardwareFingerprintLevel)
        syncForensicLogging()
        try {
            KeyManager.generateSessionKey(context)
            storageService.purgeGlobalStorage(securityController::logInternal)
            storageService.clearVolatileDownloads()
            networkTrafficConfigurator.configure(privacyPolicy)
            AmnosLog.d("SessionManager", "Network infrastructure configured")
        } catch (e: Exception) {
            AmnosLog.e("SessionManager", "Init failed", e)
        }
    }

    val sessionId: String
        get() = activeSessionId

    fun registerTimeoutListener(listener: () -> Unit) {
        timeoutListener = listener
        touchSession()
    }

    fun registerWipeListener(listener: () -> Unit) {
        onSessionWipedListeners.add(listener)
    }

    fun touchSession() {
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, privacyPolicy.identitySessionTimeoutMs)
    }

    fun createTab(
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onTrackerBlocked: () -> Unit,
        onNavigationRequested: (String) -> Boolean,
        onNavigationCommitted: (String) -> Unit,
        onNavigationFailed: (String?) -> Unit,
        onKeyboardRequested: (Boolean) -> Unit,
        onSecurityEvent: ((String) -> Unit)? = null
    ): TabInstance {
        return tabManager.createTab(
            activeSessionId = activeSessionId,
            privacyPolicy = privacyPolicy,
            onStateChanged = onStateChanged,
            onProgressChanged = onProgressChanged,
            onTrackerBlocked = onTrackerBlocked,
            onNavigationRequested = onNavigationRequested,
            onNavigationCommitted = onNavigationCommitted,
            onNavigationFailed = onNavigationFailed,
            onKeyboardRequested = onKeyboardRequested,
            onSecurityEvent = { raw ->
                securityEventRouter.route(raw, onKeyboardRequested)
                onSecurityEvent?.invoke(raw)
            },
            touchSession = ::touchSession,
            buildInjectionScript = ::buildInjectionScript
        )
    }

    fun recreateTab(
        tab: TabInstance,
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (Int) -> Unit,
        onTrackerBlocked: () -> Unit,
        onNavigationRequested: (String) -> Boolean,
        onNavigationCommitted: (String) -> Unit,
        onNavigationFailed: (String?) -> Unit,
        onKeyboardRequested: (Boolean) -> Unit,
        onSecurityEvent: ((String) -> Unit)? = null
    ): TabInstance {
        val previousUrl = tab.currentUrl
        val previousIndex = tabManager.indexOf(tab).coerceAtLeast(0)
        removeTab(tab)
        val replacement = createTab(
            onStateChanged = onStateChanged,
            onProgressChanged = onProgressChanged,
            onTrackerBlocked = onTrackerBlocked,
            onNavigationRequested = onNavigationRequested,
            onNavigationCommitted = onNavigationCommitted,
            onNavigationFailed = onNavigationFailed,
            onKeyboardRequested = onKeyboardRequested,
            onSecurityEvent = onSecurityEvent
        )
        tabManager.removeTab(replacement) {} // Temp remove to re-add at index
        tabManager.addAt(previousIndex, replacement)
        previousUrl?.let { loadUrl(replacement, it) }
        return replacement
    }

    fun updatePrivacyPolicy(update: (PrivacyPolicy) -> PrivacyPolicy) {
        privacyPolicy = update(privacyPolicy).let { updated ->
            if (BuildConfig.DEBUG_LOCKDOWN_MODE) updated.copy(debugBlockRemoteDebugging = true) else updated
        }

        securityController.setFingerprintLevel(privacyPolicy.hardwareFingerprintLevel)
        syncForensicLogging()
        networkTrafficConfigurator.configure(privacyPolicy)
        tabManager.getTabs().forEach { tab ->
            tab.webView.updateRuntimePolicy(
                tab.profile,
                privacyPolicy,
                buildInjectionScript(tab.profile),
                tab.onSecurityEvent
            )
        }
        touchSession()
    }

    fun setJavaScriptMode(mode: JavaScriptMode) {
        updatePrivacyPolicy { it.copy(hardwareJavascriptMode = mode) }
    }

    fun setWebGlEnabled(enabled: Boolean) {
        updatePrivacyPolicy {
            it.copy(hardwareWebGlMode = if (enabled) WebGlMode.SPOOF else WebGlMode.DISABLED)
        }
    }

    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) {
        updatePrivacyPolicy { current ->
            current.copy(
                hardwareFingerprintLevel = level,
                hardwareWebGlMode = when (level) {
                    FingerprintProtectionLevel.STRICT -> WebGlMode.DISABLED
                    FingerprintProtectionLevel.DISABLED -> WebGlMode.SPOOF // Allow WebGL if not strict/disabled (spoof is safer than raw but works)
                    else -> current.hardwareWebGlMode
                },
                filterBlockInlineScripts = if (level == FingerprintProtectionLevel.STRICT) true else current.filterBlockInlineScripts,
                filterStrictFirstPartyIsolation = level != FingerprintProtectionLevel.DISABLED
            )
        }
    }

    fun loadUrl(tab: TabInstance, rawUrl: String, forceBypassSandbox: Boolean = false): Boolean {
        securityController.logInternal("SessionManager", "loadUrl raw: $rawUrl (bypass=$forceBypassSandbox)", "DEBUG")
        
        val sanitizedUrl = if (forceBypassSandbox) {
            rawUrl
        } else {
            networkSecurityManager.sanitizeNavigationUrl(rawUrl) ?: run {
                securityController.logInternal("SessionManager", "Sanitization REJECTED URL: $rawUrl", "WARN")
                return false
            }
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
        if (!privacyPolicy.filterStrictFirstPartyIsolation) {
            return false
        }
        if (tab.currentUrl.isNullOrBlank()) {
            return false
        }
        return networkSecurityManager.isCrossSiteNavigation(tab.currentUrl, nextUrl)
    }

    fun removeTab(tab: TabInstance) {
        tabManager.removeTab(tab, ::touchSession)
    }

    fun killAll(terminateProcess: Boolean = false, wipeClipboard: Boolean = true) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            val posted = mainHandler.post {
                killAll(terminateProcess = terminateProcess, wipeClipboard = wipeClipboard)
            }
            if (posted) {
                return
            }
            AmnosLog.w("SessionManager", "Failed to post wipe to main thread. Continuing inline as a fallback.")
        }

        val shouldTerminate = terminateProcess || privacyPolicy.networkFirewallLevel == com.amnos.browser.core.security.FirewallLevel.PARANOID
        val now = SystemClock.elapsedRealtime()
        if (!shouldTerminate && now - lastWipeElapsedRealtime < wipeDebounceMs) {
            AmnosLog.w("SessionManager", "Wipe request ignored because a recent wipe already completed.")
            return
        }

        val reason = if (shouldTerminate) WipeReason.KILL_SWITCH else WipeReason.BACKGROUND_WIPE
        if (!shouldTerminate) {
            lastWipeElapsedRealtime = now
        }
        mainHandler.removeCallbacks(timeoutRunnable)
        superWipeEngine.execute(
            reason = reason,
            terminateProcess = shouldTerminate,
            wipeClipboard = wipeClipboard
        )
    }

    private fun buildInjectionScript(profile: DeviceProfile): String {
        return ScriptInjector(profile, privacyPolicy).wrapScript(baseObfuscatorScript)
    }

    private fun syncForensicLogging() {
        val allowSystemLogging = !privacyPolicy.debugBlockForensicLogging
        securityController.setForensicLoggingBlocked(privacyPolicy.debugBlockForensicLogging)
        AmnosLog.setSystemLoggingAllowed(allowSystemLogging)
    }
}

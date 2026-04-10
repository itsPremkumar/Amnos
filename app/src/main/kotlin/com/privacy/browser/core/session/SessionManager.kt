package com.privacy.browser.core.session

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.FingerprintManager
import com.privacy.browser.core.fingerprint.ScriptInjector
import com.privacy.browser.core.network.NetworkSecurityManager
import com.privacy.browser.core.webview.PrivacyWebChromeClient
import com.privacy.browser.core.webview.PrivacyWebViewClient
import com.privacy.browser.core.webview.SecureWebView

class SessionManager(private val context: Context) {
    private val adBlocker = AdBlocker(context)
    private val tabs = mutableListOf<TabInstance>()
    
    // Elite Modular Managers
    val securityController = SecurityController()
    val storageController = StorageController(context)
    private val networkSecurityManager = NetworkSecurityManager()

    // Architecture v2 Settings
    var isJavaScriptEnabled = true
    var isWebGLEnabled = false

    private val baseObfuscatorScript: String by lazy {
        context.assets.open("FingerprintObfuscator.js").bufferedReader().use { it.readText() }
    }

    fun createTab(
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onTrackerBlocked: () -> Unit
    ): TabInstance {
        val profile = FingerprintManager.generateCoherentProfile()
        val webView = SecureWebView(context)
        webView.applyHardening(profile.userAgent)
        webView.applyVolatileSettings(isJavaScriptEnabled)

        // Ephemeral Download Integration
        webView.setDownloadListener { url, _, _, _, _ ->
            Log.d("SessionManager", "EPHEMERAL DOWNLOAD TRIGGERED: $url")
            // In a production app, we would launch a download manager here 
            // targeting StorageController.getVolatileDownloadPath()
        }

        val injector = ScriptInjector(profile)
        val finalScript = injector.wrapScript(baseObfuscatorScript)

        val client = PrivacyWebViewClient(
            context = context,
            adBlocker = adBlocker,
            deviceProfile = profile,
            injectionScript = finalScript,
            networkSecurityManager = networkSecurityManager,
            securityController = securityController,
            onTrackerBlocked = onTrackerBlocked
        ) { url ->
            onStateChanged(url, webView.canGoBack(), webView.canGoForward())
        }
        
        webView.webViewClient = client
        webView.webChromeClient = PrivacyWebChromeClient(onProgressChanged)
        
        val tab = TabInstance(profile, webView)
        tabs.add(tab)
        return tab
    }

    fun updateAllSettings() {
        tabs.forEach { it.webView.applyVolatileSettings(isJavaScriptEnabled) }
    }

    fun removeTab(tab: TabInstance) {
        tab.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearCache(true)
            clearHistory()
            destroy()
        }
        tabs.remove(tab)
    }

    fun killAll() {
        Log.d("SessionManager", "AMNOS GHOST WIPE ACTIVATED")
        
        // 1. Wipe Clipboard & Downloads
        storageController.wipeClipboard()
        storageController.clearVolatileDownloads()
        
        // 2. Wipe Request Log
        securityController.clearLog()

        // 3. Wipe WebViews
        tabs.forEach { tab ->
            tab.webView.apply {
                stopLoading()
                clearCache(true)
                clearHistory()
                clearFormData()
                clearSslPreferences()
                destroy()
            }
        }
        tabs.clear()

        // 4. Wipe Global Storage
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies { Log.d("SessionManager", "Cookies purged") }
        cookieManager.flush()
        
        WebStorage.getInstance().deleteAllData()
        
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()

        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

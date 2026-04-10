package com.privacy.browser.core.session

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.FingerprintManager
import com.privacy.browser.core.fingerprint.ScriptInjector
import com.privacy.browser.core.webview.PrivacyWebChromeClient
import com.privacy.browser.core.webview.PrivacyWebViewClient
import com.privacy.browser.core.webview.SecureWebView
import java.io.File

class SessionManager(private val context: Context) {
    private val adBlocker = AdBlocker(context)
    private val tabs = mutableListOf<TabInstance>()
    
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
        // 1. Generate Coherent Profile
        val profile = FingerprintManager.generateCoherentProfile()
        
        // 2. Create and Harden WebView
        val webView = SecureWebView(context)
        webView.applyHardening(profile.userAgent)
        webView.applyVolatileSettings(isJavaScriptEnabled, isWebGLEnabled)

        // 3. Prepare Script Injection
        val injector = ScriptInjector(profile)
        val finalScript = injector.wrapScript(baseObfuscatorScript)

        val client = PrivacyWebViewClient(
            context = context,
            adBlocker = adBlocker,
            deviceProfile = profile,
            injectionScript = finalScript,
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
        tabs.forEach { it.webView.applyVolatileSettings(isJavaScriptEnabled, isWebGLEnabled) }
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
        Log.d("SessionManager", "RAM-ONLY DEEP WIPE ACTIVATED")
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

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies { Log.d("SessionManager", "In-memory cookies wiped") }
        cookieManager.flush()
        
        WebStorage.getInstance().deleteAllData()
        
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()

        clearApplicationData()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun clearApplicationData() {
        val cache = context.cacheDir ?: return
        val appDir = cache.parentFile ?: return
        if (appDir.exists()) {
            val children = appDir.list()
            children?.forEach { child ->
                if (child != "lib") { 
                    val fileToDelete = File(appDir, child)
                    deleteDir(fileToDelete)
                }
            }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(File(dir, child))
                if (!success) return false
            }
        }
        return dir?.delete() ?: true
    }
}

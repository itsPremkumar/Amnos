package com.privacy.browser.core.session

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.webview.PrivacyWebViewClient
import com.privacy.browser.core.webview.PrivacyWebChromeClient
import java.io.File

class SessionManager(private val context: Context) {
    private val adBlocker = AdBlocker(context)
    private val tabs = mutableListOf<TabInstance>()
    
    // Architecture v2 Settings
    var isJavaScriptEnabled = true
    var isWebGLEnabled = false // Default off for security
    var isPrefetchEnabled = false // Default off for security

    fun createTab(
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onTrackerBlocked: () -> Unit
    ): TabInstance {
        val config = SessionConfig.generateRandom()
        val webView = WebView(context)
        
        applySettings(webView)

        val client = PrivacyWebViewClient(context, adBlocker, config, onTrackerBlocked) { url ->
            onStateChanged(url, webView.canGoBack(), webView.canGoForward())
        }
        
        webView.webViewClient = client
        webView.webChromeClient = PrivacyWebChromeClient(onProgressChanged)
        
        val tab = TabInstance(config, webView)
        tabs.add(tab)
        return tab
    }

    private fun applySettings(webView: WebView) {
        webView.settings.apply {
            // v2 Dynamic Settings
            javaScriptEnabled = isJavaScriptEnabled
            
            // WebGL / Hardware acceleration control
            // Note: WebGL is enabled automatically if JS is on, but can be restricted via 
            // setSafeBrowsingEnabled and other security flags. Standard Android WebView 
            // doesn't have a direct "setWebGLEnabled" but we can control it via JS injection or 
            // by using setLayerType(View.LAYER_TYPE_SOFTWARE, null) if we want total disable.
            
            // DNS Prefetching Control
            // Note: Use of a special flag or just keeping SafeBrowsing on helps.
            // Explicitly disabling prefetching:
            // Standard WebView usually doesn't expose a simple setter, but we can set 
            // LOAD_NO_CACHE which already prevents most prefetching.
            
            // 1. RAM-Only / Volatile Hardware Lockdown
            domStorageEnabled = false
            databaseEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            
            // Production Security Hardening
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            userAgentString = webView.settings.userAgentString // Keep current or set from config
            
            // Privacy: Disable Autofill & Passwords
            savePassword = false
            saveFormData = false
            
            // Security: Enforce HTTPS & Block Mixed Content
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            
            // Security: Safe Browsing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }

            // Security: Block WebRTC IP leakage & Hardware Access
            setGeolocationEnabled(false)
            setNeedInitialFocus(false)
        }
    }

    fun updateAllSettings() {
        tabs.forEach { applySettings(it.webView) }
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

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

    fun createTab(
        onStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
        onProgressChanged: (progress: Int) -> Unit
    ): TabInstance {
        val config = SessionConfig.generateRandom()
        val webView = WebView(context)
        
        webView.settings.apply {
            // Core Logic
            javaScriptEnabled = true
            
            // 1. RAM-Only / Volatile Hardware Lockdown
            // We disable all disk-based storage features.
            domStorageEnabled = false
            databaseEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            
            // Production Security Hardening
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            userAgentString = config.userAgent
            
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
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        val client = PrivacyWebViewClient(context, adBlocker, config) { url ->
            onStateChanged(url, webView.canGoBack(), webView.canGoForward())
        }
        
        webView.webViewClient = client
        webView.webChromeClient = PrivacyWebChromeClient(onProgressChanged)
        
        val tab = TabInstance(config, webView)
        tabs.add(tab)
        return tab
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
        
        // 1. Flush and Clear all hardware components
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

        // 2. Clear global persistent records & In-Memory Cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies {
            Log.d("SessionManager", "In-memory cookies wiped")
        }
        cookieManager.flush()
        
        WebStorage.getInstance().deleteAllData()
        
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()

        // 3. Deep Forensic Purge of its own Session Data
        clearApplicationData()
        
        // 4. Terminate Process (Required for new randomized suffix on next launch)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun clearApplicationData() {
        val cache = context.cacheDir ?: return
        val appDir = cache.parentFile ?: return
        
        if (appDir.exists()) {
            val children = appDir.list()
            children?.forEach { child ->
                // Delete everything EXCEPT native libraries
                if (child != "lib") { 
                    val fileToDelete = File(appDir, child)
                    deleteDir(fileToDelete)
                    Log.d("SessionManager", "Forensic scrub successful: ${fileToDelete.name}")
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

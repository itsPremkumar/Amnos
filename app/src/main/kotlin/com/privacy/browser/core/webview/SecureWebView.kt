package com.privacy.browser.core.webview

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

class SecureWebView(context: Context) : WebView(context) {

    fun applyHardening(userAgent: String) {
        settings.apply {
            // 1. RAM-Only / Volatile Hardware Lockdown
            javaScriptEnabled = true
            domStorageEnabled = false
            databaseEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            
            // 2. Identity Hijacking Protection
            userAgentString = userAgent
            
            // 3. Hardware Leakage Protection
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            setNeedInitialFocus(false)
            
            // 4. Privacy: Disable Autofill & Passwords
            savePassword = false
            saveFormData = false
            
            // 5. Network Security
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }
        
        // Disable WebRTC leakage via hardware acceleration if possible
        // (Note: Already covered by permission denial in ChromeClient)
    }

    fun applyVolatileSettings(jsEnabled: Boolean, webGLEnabled: Boolean) {
        settings.javaScriptEnabled = jsEnabled
        // WebGL is handled via script injection layering
    }
}

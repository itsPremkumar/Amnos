package com.amnos.browser.core.service

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.amnos.browser.core.session.AmnosLog

class WebStoragePurgeAgent(private val context: Context) {
    
    fun purge(onCompleted: () -> Unit) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies {
            cookieManager.flush()
            performStandardPurge()
            onCompleted()
        }
    }

    private fun performStandardPurge() {
        AmnosLog.d("WebPurgeAgent", "Purging WebStorage and Database...")
        WebStorage.getInstance().deleteAllData()
        
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()

        try {
            android.webkit.WebView.clearClientCertPreferences(null)
        } catch (ignored: Throwable) {}
    }
}

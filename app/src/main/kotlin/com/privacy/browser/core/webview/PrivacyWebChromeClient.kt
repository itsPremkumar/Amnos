package com.privacy.browser.core.webview

import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.util.Log

class PrivacyWebChromeClient(
    private val onProgressChanged: (Int) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        com.privacy.browser.core.security.PermissionSentinel.handlePermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        // Production Hardening: Automatically deny Geolocation via callback
        Log.d("PrivacyChromeClient", "BLOCKING Geolocation Request from $origin")
        callback?.invoke(origin, false, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        // Avoid leaking logs in production browser if needed, but useful for debugging now.
        return true
    }

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
        // Production Hardening: Prevent websites from opening new windows/popups for tracking
        return false
    }
}

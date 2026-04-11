package com.privacy.browser.core.webview

import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.ValueCallback
import android.webkit.WebView

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
        callback?.invoke(origin, false, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            val level = when (it.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                ConsoleMessage.MessageLevel.WARNING -> "WARNING"
                else -> "LOG"
            }
            android.util.Log.d("WebConsole", "[$level] ${it.message()} (at ${it.sourceId()}:${it.lineNumber()})")
        }
        return true
    }

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
        return false
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<android.net.Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        return true
    }
}

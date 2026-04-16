package com.amnos.browser.core.webview

import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.ValueCallback
import android.webkit.WebView
import com.amnos.browser.core.session.AmnosLog

class PrivacyWebChromeClient(
    private val onProgressChanged: (Int) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        com.amnos.browser.core.security.PermissionSentinel.handlePermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        request?.deny()
        AmnosLog.d("PermissionSentinel", "Permission request cancelled by page before grant.")
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, false, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            val message = "${it.message()} (at ${it.sourceId()}:${it.lineNumber()})"
            when (it.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> AmnosLog.e("WebConsole", message)
                ConsoleMessage.MessageLevel.WARNING -> AmnosLog.w("WebConsole", message)
                ConsoleMessage.MessageLevel.LOG -> AmnosLog.i("WebConsole", message)
                ConsoleMessage.MessageLevel.TIP -> AmnosLog.v("WebConsole", "TIP: $message")
                else -> AmnosLog.d("WebConsole", message)
            }
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

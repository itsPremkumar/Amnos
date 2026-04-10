package com.privacy.browser.core.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.*
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.network.NetworkSecurityManager
import com.privacy.browser.core.network.UrlSanitizer
import com.privacy.browser.core.session.SecurityController
import java.io.ByteArrayInputStream

class PrivacyWebViewClient(
    private val context: Context,
    private val adBlocker: AdBlocker,
    private val deviceProfile: DeviceProfile,
    private val injectionScript: String,
    private val networkSecurityManager: NetworkSecurityManager,
    private val securityController: SecurityController,
    private val onTrackerBlocked: () -> Unit,
    private val onStateChanged: (String) -> Unit
) : WebViewClient() {

    private var currentHost: String? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        
        // 1. URL Sanitization
        val sanitizedUrl = UrlSanitizer.sanitize(url)
        if (sanitizedUrl != url) {
            val headers = networkSecurityManager.getHardenedHeaders(sanitizedUrl, currentHost)
            view?.loadUrl(sanitizedUrl, headers)
            return true
        }

        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url.toString()
        
        // 2. WebSocket Shield
        if (networkSecurityManager.isWebSocketHandshake(url)) {
            securityController.logRequest(url, SecurityController.RequestType.WEBSOCKET)
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("Blocked".toByteArray()))
        }

        // 3. Strict HTTPS Enforcement
        if (url.startsWith("http://")) {
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("Blocked: HTTPS Only".toByteArray()))
        }

        // 4. Ad & Tracker Blocking
        if (adBlocker.shouldBlock(url)) {
            securityController.logRequest(url, SecurityController.RequestType.TRACKER)
            onTrackerBlocked()
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
        }

        // 5. Elite Request Logging
        val type = if (request?.isForMainFrame == true) SecurityController.RequestType.DOCUMENT else SecurityController.RequestType.XHR
        securityController.logRequest(url, type)

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentHost = url?.let { Uri.parse(it).host }
        view?.evaluateJavascript(injectionScript, null)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onStateChanged(it) }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
        handler?.cancel()
    }
}

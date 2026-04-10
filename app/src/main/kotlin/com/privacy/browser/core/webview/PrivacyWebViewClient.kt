package com.privacy.browser.core.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.*
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.network.UrlSanitizer
import java.io.ByteArrayInputStream

class PrivacyWebViewClient(
    private val context: Context,
    private val adBlocker: AdBlocker,
    private val deviceProfile: DeviceProfile,
    private val injectionScript: String,
    private val onTrackerBlocked: () -> Unit,
    private val onStateChanged: (String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        
        // 1. URL Sanitization
        val sanitizedUrl = UrlSanitizer.sanitize(url)
        if (sanitizedUrl != url) {
            Log.d("PrivacyClient", "Sanitized URL: $url -> $sanitizedUrl")
            view?.loadUrl(sanitizedUrl, mapOf("Sec-GPC" to "1"))
            return true
        }

        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url.toString()

        // 2. Strict HTTPS Enforcement
        if (url.startsWith("http://")) {
            Log.w("PrivacyClient", "BLOCKING UNSECURE REQUEST: $url")
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("Blocked: HTTPS Only Mode.".toByteArray()))
        }

        // 3. Ad & Tracker Blocking
        if (adBlocker.shouldBlock(url)) {
            Log.d("PrivacyClient", "Blocked Tracker: $url")
            onTrackerBlocked()
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        
        // 4. Identity Injection (Fingerprinting Guard)
        // This is injected at the start of every page load
        view?.evaluateJavascript(injectionScript) {
            Log.d("PrivacyClient", "Modular Identity injected for $url")
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onStateChanged(it) }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
        Log.e("PrivacyClient", "SSL ERROR: ${error?.url}. Blocking.")
        handler?.cancel()
    }
}

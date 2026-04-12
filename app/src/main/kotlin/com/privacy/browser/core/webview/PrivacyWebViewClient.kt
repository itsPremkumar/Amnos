package com.privacy.browser.core.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.network.BlockReason
import com.privacy.browser.core.network.NetworkSecurityManager
import com.privacy.browser.core.session.AmnosLog
import com.privacy.browser.core.session.SecurityController

class PrivacyWebViewClient(
    private val adBlocker: AdBlocker,
    private val deviceProfile: DeviceProfile,
    private val networkSecurityManager: NetworkSecurityManager,
    private val securityController: SecurityController,
    private val onTrackerBlocked: () -> Unit,
    private val onStateChanged: (String) -> Unit,
    private val onNavigationRequested: (String) -> Boolean,
    private val onNavigationCommitted: (String) -> Unit,
    private val onNavigationFailed: (String?) -> Unit
) : WebViewClient() {

    private var currentHost: String? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (request?.isForMainFrame != true) {
            return false
        }

        val url = request.url.toString()
        val sanitizedUrl = networkSecurityManager.sanitizeNavigationUrl(url)
        if (sanitizedUrl == null) {
            showBlockedPage(view, BlockReason.UNSUPPORTED_SCHEME)
            return true
        }

        if (sanitizedUrl.startsWith("about:blank", ignoreCase = true)) {
            return false
        }

        if (sanitizedUrl == url) {
            return onNavigationRequested(url)
        }

        return onNavigationRequested(sanitizedUrl)
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        request ?: return null
        val url = request.url.toString()
        if (url.startsWith("about:", ignoreCase = true) || url.startsWith("data:", ignoreCase = true) || url.startsWith("blob:", ignoreCase = true)) {
            return null
        }

        val decision = networkSecurityManager.evaluateRequest(request, currentHost)

        if (adBlocker.shouldBlock(decision.sanitizedUrl)) {
            securityController.logRequest(
                url = decision.sanitizedUrl,
                method = request.method,
                type = securityController.mapKind(decision.kind),
                disposition = SecurityController.RequestDisposition.BLOCKED,
                thirdParty = decision.thirdParty,
                reason = networkSecurityManager.blockReasonLabel(BlockReason.TRACKER)
            )
            onTrackerBlocked()
            return networkSecurityManager.createBlockedResponse(BlockReason.TRACKER, request.isForMainFrame)
        }

        if (decision.isBlocked) {
            securityController.logRequest(
                url = decision.sanitizedUrl,
                method = request.method,
                type = securityController.mapKind(decision.kind),
                disposition = SecurityController.RequestDisposition.BLOCKED,
                thirdParty = decision.thirdParty,
                reason = decision.blockReason?.let { networkSecurityManager.blockReasonLabel(it) }
            )
            if (decision.blockReason == BlockReason.TRACKER ||
                decision.blockReason == BlockReason.THIRD_PARTY ||
                decision.blockReason == BlockReason.THIRD_PARTY_SCRIPT
            ) {
                onTrackerBlocked()
            }
            return networkSecurityManager.createBlockedResponse(decision.blockReason!!, request.isForMainFrame)
        }

        try {
            val proxiedResponse = networkSecurityManager.fetchResponse(request, decision, deviceProfile, currentHost)
            if (proxiedResponse != null) {
                securityController.logRequest(
                    url = decision.sanitizedUrl,
                    method = request.method,
                    type = securityController.mapKind(decision.kind),
                    disposition = SecurityController.RequestDisposition.ALLOWED,
                    thirdParty = decision.thirdParty
                )
                AmnosLog.d("PrivacyWebViewClient", "Interception SUCCESS: ${decision.sanitizedUrl}")
                return proxiedResponse
            }
        } catch (e: Exception) {
            AmnosLog.e("PrivacyWebViewClient", "Interception CRITICAL FAILURE for ${decision.sanitizedUrl}", e)
        }

        AmnosLog.d("PrivacyWebViewClient", "Interception FALLTHROUGH to system: ${decision.sanitizedUrl}")
        securityController.logRequest(
            url = decision.sanitizedUrl,
            method = request.method,
            type = securityController.mapKind(decision.kind),
            disposition = SecurityController.RequestDisposition.PASSTHROUGH,
            thirdParty = decision.thirdParty,
            reason = if (request.method.equals("GET", ignoreCase = true) || request.method.equals("HEAD", ignoreCase = true)) {
                "proxy_fallback"
            } else {
                networkSecurityManager.blockReasonLabel(BlockReason.UNSAFE_METHOD)
            }
        )
        return null
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentHost = url?.let { Uri.parse(it).host }
        (view as? SecureWebView)?.injectFallbackScript()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let {
            currentHost = Uri.parse(it).host
            onStateChanged(it)
        }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        url?.let {
            currentHost = Uri.parse(it).host
            onNavigationCommitted(it)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
        securityController.logInternal("PrivacyWebViewClient", "SSL ERROR: ${error?.url ?: "unknown"}", "ERROR")
        onNavigationFailed(error?.url)
        handler?.cancel()
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
        if (request?.isForMainFrame == true) {
            securityController.logInternal(
                "PrivacyWebViewClient", 
                "NAVIGATION ERROR: ${error?.description} (${error?.errorCode}) for ${request.url}", 
                "ERROR"
            )
            onNavigationFailed(request.url.toString())
        } else {
            securityController.logInternal(
                "PrivacyWebViewClient",
                "RESOURCE ERROR: ${error?.description} for ${request?.url}",
                "WARN"
            )
        }
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        if (request?.isForMainFrame == true) {
            securityController.logInternal(
                "PrivacyWebViewClient",
                "HTTP ERROR: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase} for ${request.url}",
                "ERROR"
            )
        }
    }

    private fun showBlockedPage(view: WebView?, reason: BlockReason) {
        securityController.logInternal("PrivacyWebViewClient", "Blocked main-frame navigation due to ${reason.name}", "WARN")
        view?.loadDataWithBaseURL(
            null,
            """
                <!doctype html>
                <html lang="en">
                <body style="background:#0d1117;color:#f8fafc;font-family:sans-serif;padding:24px;">
                    <h1>Request blocked</h1>
                    <p>Amnos blocked this navigation due to <strong>${reason.name}</strong>.</p>
                </body>
                </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }
}

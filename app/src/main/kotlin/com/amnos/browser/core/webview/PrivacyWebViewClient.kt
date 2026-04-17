package com.amnos.browser.core.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.core.net.toUri
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.amnos.browser.core.model.*
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.network.BlockReason
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController

class PrivacyWebViewClient(
    private val adBlocker: AdBlocker,
    private val deviceProfile: DeviceProfile,
    private val networkSecurityManager: NetworkSecurityManager,
    private val securityController: SecurityController,
    private val policyProvider: () -> com.amnos.browser.core.security.PrivacyPolicy,
    private val onTrackerBlocked: () -> Unit,
    private val onStateChanged: (String) -> Unit,
    private val onNavigationRequested: (String) -> Boolean,
    private val onNavigationCommitted: (String) -> Unit,
    private val onNavigationFailed: (String?) -> Unit
) : WebViewClient() {

    private var currentHost: String? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request ?: return false
        val uri = request.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        
        // 1. INTENT JAIL: Force all navigation to stay within the secure browser.
        val policy = policyProvider()
        if (scheme != "http" && scheme != "https") {
            val level = if (policy.networkFirewallLevel == com.amnos.browser.core.security.FirewallLevel.PARANOID) "CRITICAL" else "WARN"
            AmnosLog.w("PrivacyWebViewClient", "INTENT JAIL: Blocked escape attempt to scheme: $scheme ($level)")
            securityController.logInternal("SecurityJail", "Blocked external app launch: $scheme", level)
            return true // Block the navigation
        }

        // 2. MAIN FRAME SANITIZATION
        if (request.isForMainFrame) {
            val url = uri.toString()
            val sanitizedUrl = networkSecurityManager.sanitizeNavigationUrl(url)
            if (sanitizedUrl == null) {
                showBlockedPage(view, BlockReason.UNSUPPORTED_SCHEME)
                return true
            }

            if (sanitizedUrl.startsWith("about:blank", ignoreCase = true)) {
                return false
            }

            if (sanitizedUrl != url) {
                return onNavigationRequested(sanitizedUrl)
            }
        }

        return onNavigationRequested(uri.toString())
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        request ?: return null
        val url = request.url.toString()
        if (url.startsWith("about:", ignoreCase = true) || url.startsWith("data:", ignoreCase = true) || url.startsWith("blob:", ignoreCase = true)) {
            return null
        }

        val decision = networkSecurityManager.evaluateRequest(request, currentHost)

        if (decision.isBlocked) {
            securityController.logRequest(
                url = decision.sanitizedUrl,
                method = request.method,
                type = securityController.mapKind(decision.kind),
                disposition = RequestDisposition.BLOCKED,
                thirdParty = decision.thirdParty,
                reason = decision.blockReason?.let { networkSecurityManager.blockReasonLabel(it) }
            )
            if (decision.blockReason == BlockReason.TRACKER ||
                decision.blockReason == BlockReason.THIRD_PARTY ||
                decision.blockReason == BlockReason.THIRD_PARTY_SCRIPT
            ) {
                if ((view as? SecureWebView)?.isDecommissioned != true) {
                    onTrackerBlocked()
                }
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
                    disposition = RequestDisposition.ALLOWED,
                    thirdParty = decision.thirdParty
                )
                
                if (!policyProvider().debugBlockForensicLogging) {
                    AmnosLog.v("PrivacyWebViewClient", "Interception SUCCESS (Proxied): ${request.method} ${decision.sanitizedUrl}")
                }
                return proxiedResponse
            }
        } catch (e: Exception) {
            AmnosLog.e("PrivacyWebViewClient", "Interception FAILURE: Network fetch error for ${decision.sanitizedUrl}", e)
        }

        AmnosLog.v("PrivacyWebViewClient", "Interception FALLTHROUGH to System Network: ${decision.sanitizedUrl}")
        securityController.logRequest(
            url = decision.sanitizedUrl,
            method = request.method,
            type = securityController.mapKind(decision.kind),
            disposition = RequestDisposition.PASSTHROUGH,
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
        currentHost = url?.toUri()?.host
        (view as? SecureWebView)?.injectFallbackScript()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if ((view as? SecureWebView)?.isDecommissioned == true) return
        
        AmnosLog.d("PrivacyWebViewClient", "Page load finished: $url")
        url?.let {
            currentHost = it.toUri().host
            onStateChanged(it)
        }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        AmnosLog.d("PrivacyWebViewClient", "Page content committed: $url")
        url?.let {
            currentHost = it.toUri().host
            onNavigationCommitted(it)
        }
    }


    @SuppressLint("RequiresFeature")
    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: android.webkit.SafeBrowsingResponse?
    ) {
        // AMNOS PHISHING GUARD: Intercept Google Safe Browsing and handle it locally
        // to prevent URL reporting back to Google's interstitial engine.
        AmnosLog.e("PrivacyWebViewClient", "SAFE BROWSING HIT: Threat type $threatType for ${request?.url}")
        
        // Show our own blocked page instead of Google's red warning page
        showBlockedPage(view, BlockReason.SECURITY_THREAT)
        
        // Tell the engine to "Back to Safety"
        callback?.backToSafety(true)
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

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        securityController.logInternal(
            "PrivacyWebViewClient",
            "Render process gone. didCrash=${detail?.didCrash()} priority=${detail?.rendererPriorityAtExit()}",
            "ERROR"
        )
        onNavigationFailed(view?.url)
        (view as? SecureWebView)?.surgicalTeardown()
        return true
    }

    private fun showBlockedPage(view: WebView?, reason: BlockReason) {
        if ((view as? SecureWebView)?.isDecommissioned == true) return
        
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

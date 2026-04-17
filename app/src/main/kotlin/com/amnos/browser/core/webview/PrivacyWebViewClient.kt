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
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.network.BlockReason
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.webview.guard.NavigationGuard
import com.amnos.browser.core.webview.guard.ResourceGuard

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

    private val navigationGuard = NavigationGuard(
        networkSecurityManager = networkSecurityManager,
        securityController = securityController,
        policyProvider = policyProvider,
        onNavigationRequested = onNavigationRequested
    )

    private val resourceGuard = ResourceGuard(
        networkSecurityManager = networkSecurityManager,
        securityController = securityController,
        deviceProfile = deviceProfile,
        policyProvider = policyProvider,
        onTrackerBlocked = {
            if ((webView as? SecureWebView)?.isDecommissioned != true) {
                onTrackerBlocked()
            }
        }
    )

    private var webView: WebView? = null

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        this.webView = view
        val blockedByJail = navigationGuard.shouldOverride(view, request)
        if (blockedByJail && request?.isForMainFrame == true) {
             // Handle jail block visual if main frame
             // (NavigationGuard already logged, we just return true to stop it)
        }
        return blockedByJail
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        this.webView = view
        return resourceGuard.intercept(view, request, currentHost)
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

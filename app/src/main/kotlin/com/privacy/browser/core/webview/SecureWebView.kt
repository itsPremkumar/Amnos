package com.privacy.browser.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ScriptHandler
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.security.PrivacyPolicy
import com.privacy.browser.core.session.AmnosLog
import android.net.Uri

class SecureWebView(context: Context) : WebView(context) {
    companion object {
        private const val BRIDGE_NAME = "amnosBridge"
    }

    private var scriptHandler: ScriptHandler? = null
    private var fallbackInjectionScript: String? = null
    var isDecommissioned: Boolean = false
        private set

    override fun destroy() {
        isDecommissioned = true
        super.destroy()
    }

    @Suppress("DEPRECATION")
    fun applyHardening(
        profile: DeviceProfile,
        policy: PrivacyPolicy,
        injectionScript: String,
        onSecurityEvent: (String) -> Unit
    ) {
        fallbackInjectionScript = injectionScript

        settings.apply {
            javaScriptEnabled = if (policy.forceRelaxSecurityForDebug) true else policy.isJavaScriptEnabled
            javaScriptCanOpenWindowsAutomatically = false
            domStorageEnabled = if (policy.forceRelaxSecurityForDebug) true else policy.domStorageEnabled
            databaseEnabled = if (policy.forceRelaxSecurityForDebug) true else policy.domStorageEnabled
            cacheMode = if (policy.forceRelaxSecurityForDebug) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE

            userAgentString = if (policy.forceRelaxSecurityForDebug) android.webkit.WebSettings.getDefaultUserAgent(context) else profile.userAgent
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            setGeolocationEnabled(false)
            setNeedInitialFocus(false)
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = true

            savePassword = false
            saveFormData = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
        }

        overScrollMode = View.OVER_SCROLL_NEVER
        isHapticFeedbackEnabled = false
        setOnLongClickListener { true }

        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

        configureCookies()
        configureServiceWorkers()
        installSecurityBridge(onSecurityEvent)
        installDocumentStartScript(policy, injectionScript)
    }

    fun updateRuntimePolicy(
        profile: DeviceProfile,
        policy: PrivacyPolicy,
        injectionScript: String,
        onSecurityEvent: (String) -> Unit
    ) {
        applyHardening(profile, policy, injectionScript, onSecurityEvent)
    }

    fun injectFallbackScript() {
        if (scriptHandler == null && settings.javaScriptEnabled) {
            fallbackInjectionScript?.let { evaluateJavascript(it, null) }
        }
    }

    fun clearVolatileState() {
        removeDocumentStartScript()
        scriptHandler = null
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            try {
                WebViewCompat.removeWebMessageListener(this, BRIDGE_NAME)
            } catch (ignored: Exception) {
            }
        }

        stopLoading()
        loadUrl("about:blank")
        onPause()
        pauseTimers()
        clearHistory()
        clearCache(true)
        clearFormData()
        clearSslPreferences()
        removeAllViews()
    }

    private fun installDocumentStartScript(policy: PrivacyPolicy, injectionScript: String) {
        removeDocumentStartScript()
        scriptHandler = null

        if (!policy.isJavaScriptEnabled) {
            return
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(
                this,
                injectionScript,
                setOf("*")
            )
        }
    }

    @SuppressLint("RequiresFeature")
    private fun installSecurityBridge(onSecurityEvent: (String) -> Unit) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            AmnosLog.d("SecureWebView", "WEB_MESSAGE_LISTENER not supported")
            return
        }

        try {
            WebViewCompat.removeWebMessageListener(this, BRIDGE_NAME)
        } catch (ignored: Exception) {
        }

        try {
            WebViewCompat.addWebMessageListener(
                this,
                BRIDGE_NAME,
                setOf("*")
            ) { _: WebView, message: WebMessageCompat, _: Uri, _: Boolean, _: JavaScriptReplyProxy ->
                message.data?.let(onSecurityEvent)
            }
            AmnosLog.d("SecureWebView", "Security bridge (WebMessageListener) installed")
        } catch (e: Exception) {
            AmnosLog.e("SecureWebView", "Failed to install security bridge", e)
        }
    }

    private fun configureCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.setAcceptThirdPartyCookies(this, false)
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()
    }

    private fun configureServiceWorkers() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            return
        }

        val controller = ServiceWorkerControllerCompat.getInstance()
        val serviceWorkerSettings = controller.serviceWorkerWebSettings

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_CACHE_MODE)) {
            serviceWorkerSettings.setCacheMode(WebSettings.LOAD_NO_CACHE)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS)) {
            serviceWorkerSettings.setAllowContentAccess(false)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_FILE_ACCESS)) {
            serviceWorkerSettings.setAllowFileAccess(false)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS)) {
            // We allow network loads globally but control registration via injected JS and CSP
            serviceWorkerSettings.setBlockNetworkLoads(false)
        }
    }

    private fun removeDocumentStartScript() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandler?.remove()
        }
    }
}

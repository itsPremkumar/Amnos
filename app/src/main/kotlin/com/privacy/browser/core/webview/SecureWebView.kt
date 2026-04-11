package com.privacy.browser.core.webview

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ScriptHandler
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.security.PrivacyPolicy
import android.net.Uri

class SecureWebView(context: Context) : WebView(context) {
    companion object {
        private const val BRIDGE_NAME = "amnosBridge"
    }

    private var scriptHandler: ScriptHandler? = null
    private var fallbackInjectionScript: String? = null

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
            cacheMode = if (policy.forceRelaxSecurityForDebug) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_DEFAULT

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        overScrollMode = View.OVER_SCROLL_NEVER
        isHapticFeedbackEnabled = false
        setOnLongClickListener { true }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        configureCookies()
        configureRequestedWithHeader()
        configureServiceWorkers(policy)
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
        scriptHandler?.remove()
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
        scriptHandler?.remove()
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

    private fun installSecurityBridge(onSecurityEvent: (String) -> Unit) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            Log.d("SecureWebView", "WEB_MESSAGE_LISTENER not supported")
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
            Log.d("SecureWebView", "Security bridge (WebMessageListener) installed")
        } catch (e: Exception) {
            Log.e("SecureWebView", "Failed to install security bridge", e)
        }
    }

    private fun configureRequestedWithHeader() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        }
    }

    private fun configureCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, false)
        cookieManager.flush()
    }

    private fun configureServiceWorkers(policy: PrivacyPolicy) {
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
            serviceWorkerSettings.setBlockNetworkLoads(policy.blockServiceWorkers)
        }
    }
}

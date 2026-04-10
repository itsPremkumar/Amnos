package com.privacy.browser.core.webview

import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ScriptHandler
import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.security.PrivacyPolicy

class SecureWebView(context: Context) : WebView(context) {
    private var scriptHandler: ScriptHandler? = null
    private var fallbackInjectionScript: String? = null

    @Suppress("DEPRECATION")
    fun applyHardening(
        profile: DeviceProfile,
        policy: PrivacyPolicy,
        injectionScript: String
    ) {
        fallbackInjectionScript = injectionScript

        settings.apply {
            javaScriptEnabled = policy.isJavaScriptEnabled
            javaScriptCanOpenWindowsAutomatically = false
            domStorageEnabled = false
            databaseEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE

            userAgentString = profile.userAgent
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
        installDocumentStartScript(policy, injectionScript)
    }

    fun updateRuntimePolicy(
        profile: DeviceProfile,
        policy: PrivacyPolicy,
        injectionScript: String
    ) {
        applyHardening(profile, policy, injectionScript)
    }

    fun injectFallbackScript() {
        if (scriptHandler == null && settings.javaScriptEnabled) {
            fallbackInjectionScript?.let { evaluateJavascript(it, null) }
        }
    }

    fun clearVolatileState() {
        scriptHandler?.remove()
        scriptHandler = null

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

    private fun configureRequestedWithHeader() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        }
    }

    private fun configureCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
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

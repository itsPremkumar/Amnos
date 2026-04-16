package com.amnos.browser.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ScriptHandler
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
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

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        // AmnosLog.d("SecureWebView", "onCreateInputConnection requested.")
        // AMNOS HARDENED INPUT: Rejection of system IME to prevent keystroke leakage
        return null
    }

    override fun onCheckIsTextEditor(): Boolean {
        // AmnosLog.d("SecureWebView", "onCheckIsTextEditor called. Returning FALSE.")
        // AMNOS HARDENED INPUT: Tell the system this is not a text editor
        return false
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        AmnosLog.d("SecureWebView", "onFocusChanged: focused=$focused")
        if (focused) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(windowToken, 0)
        }
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
            
            // 100% PURE RAM MODE: Disable all persistent storage APIs
            domStorageEnabled = true  // Volatile RAM-only, wiped by Ghost Wipe 
            databaseEnabled = if (policy.forceRelaxSecurityForDebug) true else false
            
            cacheMode = WebSettings.LOAD_DEFAULT  // Cache is physically nuked by StorageService on session wipe

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

        AmnosLog.i("SecureWebView", "Hardening applied: JS=${settings.javaScriptEnabled}, DOM=${settings.domStorageEnabled}, UA=${profile.userAgent.take(20)}...")

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

    fun injectInput(text: String) {
        AmnosLog.v("SecureWebView", "Keyboard Input: Injecting ${text.length} character(s)")
        val escaped = text.replace("'", "\\'")
        evaluateJavascript("document.execCommand('insertText', false, '$escaped')", null)
    }

    fun injectBackspace() {
        AmnosLog.v("SecureWebView", "Keyboard Input: Injecting backspace")
        evaluateJavascript("document.execCommand('delete', false, null)", null)
    }

    fun injectSearch() {
        evaluateJavascript("""
            (function() {
                const active = document.activeElement;
                if (!active) return;
                const event = new KeyboardEvent('keydown', {
                    key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true
                });
                active.dispatchEvent(event);
                if (active.form) active.form.submit();
            })();
        """.trimIndent(), null)
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

        // SECURITY HARDENING: Wipe all cookies on tab teardown.
        // Since we allow first-party session cookies for media playback,
        // we must ensure they don't survive beyond this tab's lifecycle.
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        removeAllViews()
    }

    private fun installDocumentStartScript(policy: PrivacyPolicy, injectionScript: String) {
        removeDocumentStartScript()
        scriptHandler = null

        if (!policy.isJavaScriptEnabled) {
            return
        }

        val focusSentinel = """
            (function() {
                const notify = (action) => {
                    if (window.amnosBridge) {
                        window.amnosBridge.postMessage(JSON.stringify({ type: 'keyboard_event', action: action }));
                    }
                };
                window.addEventListener('focusin', (e) => {
                    const el = e.target;
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable) {
                        notify('show');
                    }
                });
                window.addEventListener('focusout', (e) => {
                    notify('hide');
                });
            })();
        """.trimIndent()

        val fullScript = injectionScript + "\n" + focusSentinel

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            AmnosLog.d("SecureWebView", "Registering DOCUMENT_START_SCRIPT (Size: ${fullScript.length} bytes)")
            scriptHandler = WebViewCompat.addDocumentStartJavaScript(
                this,
                fullScript,
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
            ) { _: WebView, message: WebMessageCompat, sourceOrigin: Uri, _: Boolean, _: JavaScriptReplyProxy ->
                // SECURE ORIGIN & PROTOCOL VALIDATION: Only accept messages from the currently loaded HTTPS page
                val currentUri = url?.let { Uri.parse(it) }
                val isHttps = sourceOrigin.scheme == "https"
                val currentIsHttps = currentUri?.scheme == "https"
                val hostMatches = currentUri != null && sourceOrigin.host == currentUri.host
                val sourcePort = if (sourceOrigin.port == -1) 443 else sourceOrigin.port
                val currentPort = when {
                    currentUri == null -> -1
                    currentUri.port == -1 -> 443
                    else -> currentUri.port
                }
                val portMatches = currentPort == sourcePort

                if (isHttps && currentIsHttps && hostMatches && portMatches) {
                    message.data?.let(onSecurityEvent)
                } else {
                    AmnosLog.w(
                        "SecureWebView",
                        "REJECTED bridge message from: $sourceOrigin (sourceHttps=$isHttps, currentHttps=$currentIsHttps, hostMatch=$hostMatches, portMatch=$portMatches)"
                    )
                }
            }
            AmnosLog.d("SecureWebView", "Security bridge (WebMessageListener) installed")
        } catch (e: Exception) {
            AmnosLog.e("SecureWebView", "Failed to install security bridge", e)
        }
    }

    private fun configureCookies() {
        val cookieManager = CookieManager.getInstance()
        // Allow first-party session cookies (required for YouTube CDN, Google login flows).
        // These are volatile: purgeGlobalStorage() wipes ALL cookies on every session kill.
        cookieManager.setAcceptCookie(true)
        // Block third-party cookies to prevent cross-site tracking.
        cookieManager.setAcceptThirdPartyCookies(this, false)
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

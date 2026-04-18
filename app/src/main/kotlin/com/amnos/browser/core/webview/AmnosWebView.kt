package com.amnos.browser.core.webview

import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.security.PrivacyPolicy

/**
 * AMNOS CORE INTERFACE: Decouples browser security logic from native Android UI.
 * Allows for forensic identity spoofing and high-performance End-to-End testing.
 */
interface AmnosWebView {
    fun getWebViewClient(): WebViewClient
    fun setWebViewClient(client: WebViewClient)
    fun getWebChromeClient(): WebChromeClient?
    fun setWebChromeClient(client: WebChromeClient?)
    
    fun injectInput(text: String)
    fun injectBackspace()
    fun injectSearch()
    
    fun getContext(): Context
    fun getUrl(): String?
    fun loadUrl(url: String)
    fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>)
    fun stopLoading()
    fun reload()
    fun clearHistory()
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean
    fun goBack()
    fun goForward()
    fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean
    fun pauseTimers()
    fun resumeTimers()
    fun destroy()
    
    /**
     * Exposes the underlying UI component. In production, this returns the WebView instance.
     * In E2E tests, this can return a mock view.
     */
    fun asView(): View
    
    fun applyHardening(
        profile: DeviceProfile,
        policy: PrivacyPolicy,
        injectionScript: String,
        onSecurityEvent: (String) -> Unit
    )
    
    fun updateRuntimePolicy(
        profile: DeviceProfile,
        policy: PrivacyPolicy,
        injectionScript: String,
        onSecurityEvent: (String) -> Unit
    )
    
    fun surgicalTeardown()
    fun clearVolatileState()
}

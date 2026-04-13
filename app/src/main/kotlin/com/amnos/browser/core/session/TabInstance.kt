package com.amnos.browser.core.session

import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.webview.SecureWebView

data class TabInstance(
    val sessionId: String,
    val tabId: String,
    val profile: DeviceProfile,
    val webView: SecureWebView,
    var currentUrl: String? = null,
    var siteKey: String? = null
)

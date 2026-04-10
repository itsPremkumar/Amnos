package com.privacy.browser.core.session

import com.privacy.browser.core.fingerprint.DeviceProfile
import com.privacy.browser.core.webview.SecureWebView

data class TabInstance(
    val profile: DeviceProfile,
    val webView: SecureWebView
)

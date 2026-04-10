package com.privacy.browser.core.session

import android.webkit.WebView

data class TabInstance(
    val config: SessionConfig,
    val webView: WebView
)

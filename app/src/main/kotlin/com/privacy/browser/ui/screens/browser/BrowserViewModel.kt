package com.privacy.browser.ui.screens.browser

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.privacy.browser.core.session.SessionManager
import com.privacy.browser.core.session.TabInstance

class BrowserViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var currentTab = mutableStateOf<TabInstance?>(null)
    var urlInput = mutableStateOf("")
    var uiState = mutableStateOf(BrowserUIState.HOME)
    
    // Navigation State
    var canGoBack = mutableStateOf(false)
    var canGoForward = mutableStateOf(false)
    
    // Progress State
    var loadingProgress = mutableStateOf(0)

    init {
        initializeSession()
    }

    private fun initializeSession() {
        val tab = sessionManager.createTab(
            onStateChanged = { url, back, forward ->
                if (uiState.value == BrowserUIState.BROWSING) {
                    urlInput.value = url
                }
                canGoBack.value = back
                canGoForward.value = forward
                // Reset progress when a new page finish is reported (though usually it hits 100)
                if (loadingProgress.value >= 100) {
                    loadingProgress.value = 0
                }
            },
            onProgressChanged = { progress ->
                loadingProgress.value = progress
            }
        )
        currentTab.value = tab
    }

    fun navigate(input: String) {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) return

        val isUrl = (trimmedInput.startsWith("http://") || trimmedInput.startsWith("https://")) ||
                    (trimmedInput.contains(".") && !trimmedInput.contains(" ") && trimmedInput.length > 3)

        val destinationUrl = if (isUrl) {
            if (trimmedInput.startsWith("http")) trimmedInput else "https://$trimmedInput"
        } else {
            "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(trimmedInput, "UTF-8")}"
        }
        
        // 1. Sanitize the URL before loading
        val sanitizedUrl = com.privacy.browser.core.network.UrlSanitizer.sanitize(destinationUrl)
        
        uiState.value = BrowserUIState.BROWSING
        
        // 2. Load with GPC Header
        currentTab.value?.webView?.loadUrl(sanitizedUrl, mapOf("Sec-GPC" to "1"))
    }

    fun goBack() {
        currentTab.value?.webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                uiState.value = BrowserUIState.HOME
            }
        }
    }

    fun goForward() {
        currentTab.value?.webView?.let {
            if (it.canGoForward()) {
                it.goForward()
            }
        }
    }

    fun goHome() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
    }

    fun reload() {
        currentTab.value?.webView?.reload()
    }

    fun killSwitch() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        
        currentTab.value?.let { sessionManager.removeTab(it) }
        currentTab.value = null
        
        sessionManager.killAll()
        
        initializeSession()
    }
}

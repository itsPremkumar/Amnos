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
    
    // Architecture v2: Security State
    var blockedTrackersCount = mutableStateOf(0)
    var isJavaScriptEnabled = mutableStateOf(sessionManager.isJavaScriptEnabled)
    var isWebGLEnabled = mutableStateOf(sessionManager.isWebGLEnabled)
    var showSecurityDashboard = mutableStateOf(false)
    val requestLog = sessionManager.securityController.requestLog
    
    // PIN Lock
    var isLocked = mutableStateOf(false) // Start unlocked for now, or true for auto-lock
    var userPin = "1111" // Default PIN for this session
    var pinInput = mutableStateOf("")

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
                if (loadingProgress.value >= 100) {
                    loadingProgress.value = 0
                }
            },
            onProgressChanged = { progress ->
                loadingProgress.value = progress
            },
            onTrackerBlocked = {
                blockedTrackersCount.value++
            }
        )
        currentTab.value = tab
    }

    fun toggleJavaScript(enabled: Boolean) {
        isJavaScriptEnabled.value = enabled
        sessionManager.isJavaScriptEnabled = enabled
        sessionManager.updateAllSettings()
        reload()
    }

    fun toggleWebGL(enabled: Boolean) {
        isWebGLEnabled.value = enabled
        sessionManager.isWebGLEnabled = enabled
        sessionManager.updateAllSettings()
        reload()
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
        
        val sanitizedUrl = com.privacy.browser.core.network.UrlSanitizer.sanitize(destinationUrl)
        uiState.value = BrowserUIState.BROWSING
        currentTab.value?.webView?.loadUrl(sanitizedUrl, mapOf("Sec-GPC" to "1"))
    }

    fun goBack() {
        currentTab.value?.webView?.let {
            if (it.canGoBack()) it.goBack() else uiState.value = BrowserUIState.HOME
        }
    }

    fun goForward() {
        currentTab.value?.webView?.let {
            if (it.canGoForward()) it.goForward()
        }
    }

    fun goHome() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        blockedTrackersCount.value = 0 // Reset on home for fresh display
    }

    fun reload() {
        currentTab.value?.webView?.reload()
    }

    fun killSwitch() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        blockedTrackersCount.value = 0
        
        currentTab.value?.let { sessionManager.removeTab(it) }
        currentTab.value = null
        
        sessionManager.killAll()
        initializeSession()
    }
}

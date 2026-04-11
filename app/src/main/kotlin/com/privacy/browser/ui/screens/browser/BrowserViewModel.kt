package com.privacy.browser.ui.screens.browser

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.privacy.browser.core.network.UrlSanitizer
import com.privacy.browser.core.security.FingerprintProtectionLevel
import com.privacy.browser.core.security.JavaScriptMode
import com.privacy.browser.core.security.WebGlMode
import com.privacy.browser.core.session.SessionManager
import com.privacy.browser.core.session.TabInstance

class BrowserViewModel(private val sessionManager: SessionManager) : ViewModel() {
    var currentTab = mutableStateOf<TabInstance?>(null)
    var urlInput = mutableStateOf("")
    var uiState = mutableStateOf(BrowserUIState.HOME)

    var canGoBack = mutableStateOf(false)
    var canGoForward = mutableStateOf(false)
    var loadingProgress = mutableStateOf(0)

    var blockedTrackersCount = mutableStateOf(0)
    var privacyPolicy = mutableStateOf(sessionManager.privacyPolicy)
    var javaScriptMode = mutableStateOf(sessionManager.privacyPolicy.javascriptMode)
    var isWebGLEnabled = mutableStateOf(sessionManager.privacyPolicy.webGlMode == WebGlMode.SPOOF)
    var fingerprintProtectionLevel = mutableStateOf(sessionManager.privacyPolicy.fingerprintProtectionLevel)
    var showSecurityDashboard = mutableStateOf(false)
    var sessionLabel = mutableStateOf(sessionManager.sessionId.take(8))
    val requestLog = sessionManager.securityController.requestLog
    val activeConnections = sessionManager.securityController.activeConnections
    val proxyStatus = sessionManager.securityController.proxyStatus
    val dohStatus = sessionManager.securityController.dohStatus
    val webRtcStatus = sessionManager.securityController.webRtcStatus
    val webSocketStatus = sessionManager.securityController.webSocketStatus
    val webRtcAttemptCount = sessionManager.securityController.webRtcAttemptCount
    val webSocketAttemptCount = sessionManager.securityController.webSocketAttemptCount
    val privacyWarning = sessionManager.securityController.warningMessage
    var isLocked = mutableStateOf(false)
    var userPin = "1111"
    var pinInput = mutableStateOf("")

    init {
        Log.d("BrowserViewModel", "Initializing BrowserViewModel")
        sessionManager.registerTimeoutListener {
            Log.d("BrowserViewModel", "Session timeout triggered")
            handleSessionTimeout()
        }
        initializeSession()
    }

    private val stateChangedCallback: (String, Boolean, Boolean) -> Unit = { url, back, forward ->
        Log.v("BrowserViewModel", "State changed: $url (back=$back, forward=$forward)")
...
        currentTab.value?.currentUrl = url
        if (uiState.value == BrowserUIState.BROWSING) {
            urlInput.value = url
        }
        canGoBack.value = back
        canGoForward.value = forward
        if (loadingProgress.value >= 100) {
            loadingProgress.value = 0
        }
    }

    private val progressChangedCallback: (Int) -> Unit = { progress ->
        loadingProgress.value = progress
    }

    private val trackerBlockedCallback: () -> Unit = {
        blockedTrackersCount.value = sessionManager.securityController.trackerBlockCount()
    }

    private fun initializeSession(loadUrl: String? = null) {
        Log.d("BrowserViewModel", "Initializing session (loadUrl=$loadUrl)")
        try {
            val tab = sessionManager.createTab(
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation
            )
            currentTab.value = tab
            sessionLabel.value = sessionManager.sessionId.take(8)
            refreshPolicyState()
            loadUrl?.let {
                Log.d("BrowserViewModel", "Initial URL load: $it")
                uiState.value = BrowserUIState.BROWSING
                sessionManager.loadUrl(tab, it)
            }
        } catch (e: Exception) {
            Log.e("BrowserViewModel", "CRITICAL: Initialization of session failed", e)
            throw e
        }
    }

    fun setJavaScriptMode(mode: JavaScriptMode) {
        sessionManager.setJavaScriptMode(mode)
        refreshPolicyState()
        reload()
    }

    fun toggleWebGL(enabled: Boolean) {
        sessionManager.setWebGlEnabled(enabled)
        refreshPolicyState()
        reload()
    }

    fun toggleHttpsOnly(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(httpsOnlyEnabled = enabled) }
        refreshPolicyState()
        reload()
    }

    fun toggleThirdPartyBlocking(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy {
            it.copy(
                blockThirdPartyRequests = enabled,
                blockThirdPartyScripts = enabled
            )
        }
        refreshPolicyState()
        reload()
    }

    fun toggleInlineScriptBlocking(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy {
            it.copy(
                blockInlineScripts = enabled,
                blockEval = enabled,
                javascriptMode = if (enabled && it.javascriptMode == JavaScriptMode.FULL) {
                    JavaScriptMode.RESTRICTED
                } else {
                    it.javascriptMode
                }
            )
        }
        refreshPolicyState()
        reload()
    }

    fun toggleResetIdentityOnRefresh(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(resetIdentityOnRefresh = enabled) }
        refreshPolicyState()
    }

    fun toggleStrictFirstPartyIsolation(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(strictFirstPartyIsolation = enabled) }
        refreshPolicyState()
    }

    fun toggleWebSockets(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(blockWebSockets = enabled) }
        refreshPolicyState()
        reload()
    }

    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) {
        sessionManager.setFingerprintProtectionLevel(level)
        refreshPolicyState()
        currentTab.value?.let { tab ->
            currentTab.value = sessionManager.recreateTab(
                tab = tab,
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation
            )
        }
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

        val sanitizedUrl = UrlSanitizer.sanitize(destinationUrl)
        uiState.value = BrowserUIState.BROWSING
        handleMainFrameNavigation(sanitizedUrl)
    }

    fun goBack() {
        currentTab.value?.webView?.let {
            if (it.canGoBack()) it.goBack() else uiState.value = BrowserUIState.HOME
        }
        sessionManager.touchSession()
    }

    fun goForward() {
        currentTab.value?.webView?.let {
            if (it.canGoForward()) it.goForward()
        }
        sessionManager.touchSession()
    }

    fun goHome() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        currentTab.value?.apply {
            currentUrl = "about:blank"
            webView.loadUrl("about:blank")
        }
        blockedTrackersCount.value = sessionManager.securityController.trackerBlockCount()
    }

    fun reload() {
        val tab = currentTab.value ?: return
        if (privacyPolicy.value.resetIdentityOnRefresh && !tab.currentUrl.isNullOrBlank()) {
            currentTab.value = sessionManager.recreateTab(
                tab = tab,
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation
            )
            return
        }

        tab.webView.reload()
        sessionManager.touchSession()
    }

    fun killSwitch() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        blockedTrackersCount.value = 0

        currentTab.value = null
        sessionManager.killAll(terminateProcess = false)
        initializeSession()
    }

    private fun handleSessionTimeout() {
        currentTab.value = null
        blockedTrackersCount.value = 0
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        sessionManager.killAll(terminateProcess = false)
        initializeSession()
    }

    private fun refreshPolicyState() {
        privacyPolicy.value = sessionManager.privacyPolicy
        javaScriptMode.value = sessionManager.privacyPolicy.javascriptMode
        isWebGLEnabled.value = sessionManager.privacyPolicy.webGlMode == WebGlMode.SPOOF
        fingerprintProtectionLevel.value = sessionManager.privacyPolicy.fingerprintProtectionLevel
        blockedTrackersCount.value = sessionManager.securityController.trackerBlockCount()
    }

    private fun handleMainFrameNavigation(url: String): Boolean {
        uiState.value = BrowserUIState.BROWSING
        val current = currentTab.value ?: return false
        val activeTab = if (sessionManager.shouldRecreateForTopLevelNavigation(current, url)) {
            sessionManager.recreateTab(
                tab = current,
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation
            ).also { currentTab.value = it }
        } else {
            current
        }

        return sessionManager.loadUrl(activeTab, url).also { loaded ->
            if (loaded) {
                urlInput.value = activeTab.currentUrl ?: url
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.killAll(terminateProcess = false)
    }
}

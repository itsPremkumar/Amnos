package com.amnos.browser.ui.screens.browser

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.amnos.browser.BuildConfig
import com.amnos.browser.core.fingerprint.FingerprintManager
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.JavaScriptMode
import com.amnos.browser.core.security.WebGlMode
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.core.session.TabInstance
import com.amnos.browser.ui.screens.browser.logic.NavigationHandler
import com.amnos.browser.ui.screens.browser.logic.SecurityHandler

class BrowserViewModel(private val sessionManager: SessionManager) : ViewModel() {
    var currentTab = mutableStateOf<TabInstance?>(null)
    var urlInput = mutableStateOf("")
    var uiState = mutableStateOf(BrowserUIState.HOME)

    var canGoBack = mutableStateOf(false)
    var canGoForward = mutableStateOf(false)
    var isBurning = mutableStateOf(false)
    var loadingProgress = mutableIntStateOf(0)

    var blockedTrackersCount = mutableIntStateOf(0)
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
    val internalLogs = sessionManager.securityController.internalLogs
    var isLocked = mutableStateOf(false)
    var userPin = FingerprintManager.newUnlockPin()
    var pinInput = mutableStateOf("")
    var enableRemoteDebugging = mutableStateOf(sessionManager.privacyPolicy.enableRemoteDebugging)
    var forceRelaxSecurityForDebug = mutableStateOf(sessionManager.privacyPolicy.forceRelaxSecurityForDebug)
    val debugControlsAvailable = BuildConfig.DEBUG

    private val navHandler = NavigationHandler(this, sessionManager)
    private val securityHandler = SecurityHandler(this, sessionManager)

    internal var pendingAddressBarValue: String? = null

    internal val stateChangedCallback: (String, Boolean, Boolean) -> Unit = { url, back, forward ->
        AmnosLog.d("BrowserViewModel", "State changed: $url (back=$back, forward=$forward)")
        currentTab.value?.currentUrl = url
        canGoBack.value = back
        canGoForward.value = forward
        if (loadingProgress.intValue >= 100) {
            loadingProgress.intValue = 0
        }
    }

    internal val progressChangedCallback: (Int) -> Unit = { progress ->
        loadingProgress.intValue = progress
    }

    internal val trackerBlockedCallback: () -> Unit = {
        if (currentTab.value != null) {
            blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount()
        }
    }

    internal val navigationCommittedCallback: (String) -> Unit = { committedUrl ->
        sessionManager.securityController.logInternal("[Nav:Commit]", committedUrl, "DEBUG")
        currentTab.value?.currentUrl = committedUrl
        if (uiState.value == BrowserUIState.BROWSING) {
            urlInput.value = committedUrl
        }
        pendingAddressBarValue = null
    }

    internal val navigationFailedCallback: (String?) -> Unit = { failedUrl ->
        sessionManager.securityController.logInternal(
            "[Nav:Failure]",
            failedUrl ?: pendingAddressBarValue ?: "unknown",
            "WARN"
        )
        pendingAddressBarValue = null
    }

    internal val keyboardRequestedCallback: (Boolean) -> Unit = { show ->
        AmnosLog.d("BrowserViewModel", "WebView keyboard requested: $show")
        webKeyboardRequested.value = show
    }

    var webKeyboardRequested = mutableStateOf(false)

    init {
        AmnosLog.d("BrowserViewModel", "Initializing BrowserViewModel")
        sessionManager.registerTimeoutListener {
            AmnosLog.d("BrowserViewModel", "Session timeout triggered")
            handleSessionTimeout()
        }
        sessionManager.registerWipeListener {
            AmnosLog.d("BrowserViewModel", "Session wipe triggered from external event")
            resetUIState()
        }
        initializeSession()
    }

    internal fun initializeSession(loadUrl: String? = null) {
        AmnosLog.d("BrowserViewModel", "Initializing session (loadUrl=$loadUrl)")
        try {
            val tab = sessionManager.createTab(
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = { navHandler.handleMainFrameNavigation(it) },
                onNavigationCommitted = navigationCommittedCallback,
                onNavigationFailed = navigationFailedCallback,
                onKeyboardRequested = keyboardRequestedCallback
            )
            currentTab.value = tab
            sessionLabel.value = sessionManager.sessionId.take(8)
            refreshPolicyState()
            loadUrl?.let {
                AmnosLog.d("BrowserViewModel", "Initial URL load: $it")
                uiState.value = BrowserUIState.BROWSING
                sessionManager.loadUrl(tab, it)
            }
        } catch (e: Exception) {
            AmnosLog.e("BrowserViewModel", "CRITICAL: Initialization of session failed", e)
            throw e
        }
    }

    // Security Delegates
    fun setJavaScriptMode(mode: JavaScriptMode) = securityHandler.setJavaScriptMode(mode)
    fun toggleWebGL(enabled: Boolean) = securityHandler.toggleWebGL(enabled)
    fun toggleHttpsOnly(enabled: Boolean) = securityHandler.toggleHttpsOnly(enabled)
    fun toggleThirdPartyBlocking(enabled: Boolean) = securityHandler.toggleThirdPartyBlocking(enabled)
    fun toggleInlineScriptBlocking(enabled: Boolean) = securityHandler.toggleInlineScriptBlocking(enabled)
    fun toggleResetIdentityOnRefresh(enabled: Boolean) = securityHandler.toggleResetIdentityOnRefresh(enabled)
    fun toggleStrictFirstPartyIsolation(enabled: Boolean) = securityHandler.toggleStrictFirstPartyIsolation(enabled)
    fun toggleWebSockets(enabled: Boolean) = securityHandler.toggleWebSockets(enabled)
    fun toggleRemoteDebugging(enabled: Boolean) = securityHandler.toggleRemoteDebugging(enabled)
    fun toggleForceRelaxSecurity(enabled: Boolean) = securityHandler.toggleForceRelaxSecurity(enabled)
    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) = securityHandler.setFingerprintProtectionLevel(level)

    // Navigation Delegates
    fun navigate(input: String) = navHandler.navigate(input)
    fun goBack() = navHandler.goBack()
    fun goForward() = navHandler.goForward()
    fun goHome() = navHandler.goHome()
    fun reload() = navHandler.reload()

    fun injectWebInput(text: String) {
        currentTab.value?.webView?.injectInput(text)
    }

    fun injectWebBackspace() {
        currentTab.value?.webView?.injectBackspace()
    }

    fun injectWebSearch() {
        currentTab.value?.webView?.injectSearch()
    }

    fun killSwitch() {
        viewModelScope.launch {
            isBurning.value = true
            sessionManager.killAll(terminateProcess = false)
            delay(1200) // Allow animation to play
            initializeSession()
            isBurning.value = false
        }
    }

    private fun handleSessionTimeout() {
        sessionManager.killAll(terminateProcess = false)
        initializeSession()
    }

    private fun resetUIState() {
        currentTab.value = null
        blockedTrackersCount.intValue = 0
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        pendingAddressBarValue = null
    }

    internal fun refreshPolicyState() {
        privacyPolicy.value = sessionManager.privacyPolicy
        javaScriptMode.value = sessionManager.privacyPolicy.javascriptMode
        isWebGLEnabled.value = sessionManager.privacyPolicy.webGlMode == WebGlMode.SPOOF
        fingerprintProtectionLevel.value = sessionManager.privacyPolicy.fingerprintProtectionLevel
        blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount()
        enableRemoteDebugging.value = sessionManager.privacyPolicy.enableRemoteDebugging
        forceRelaxSecurityForDebug.value = sessionManager.privacyPolicy.forceRelaxSecurityForDebug
    }

    internal fun recreateCurrentTab() {
        currentTab.value?.let { tab ->
            currentTab.value = sessionManager.recreateTab(
                tab = tab,
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = { navHandler.handleMainFrameNavigation(it) },
                onNavigationCommitted = navigationCommittedCallback,
                onNavigationFailed = navigationFailedCallback,
                onKeyboardRequested = keyboardRequestedCallback
            )
        }
    }

    internal fun updatePendingAddressBar(value: String?) { pendingAddressBarValue = value }
    internal fun updateBlockedTrackersCount() {
        blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount()
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.killAll(terminateProcess = false)
    }
}

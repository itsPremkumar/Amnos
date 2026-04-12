package com.privacy.browser.ui.screens.browser

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.privacy.browser.BuildConfig
import com.privacy.browser.core.network.NavigationResolver
import com.privacy.browser.core.security.FingerprintProtectionLevel
import com.privacy.browser.core.security.JavaScriptMode
import com.privacy.browser.core.security.WebGlMode
import com.privacy.browser.core.session.AmnosLog
import com.privacy.browser.core.session.SessionManager
import com.privacy.browser.core.session.TabInstance

class BrowserViewModel(private val sessionManager: SessionManager) : ViewModel() {
    var currentTab = mutableStateOf<TabInstance?>(null)
    var urlInput = mutableStateOf("")
    var uiState = mutableStateOf(BrowserUIState.HOME)

    var canGoBack = mutableStateOf(false)
    var canGoForward = mutableStateOf(false)
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
    var userPin = "1111"
    var pinInput = mutableStateOf("")
    var enableRemoteDebugging = mutableStateOf(sessionManager.privacyPolicy.enableRemoteDebugging)
    var forceRelaxSecurityForDebug = mutableStateOf(sessionManager.privacyPolicy.forceRelaxSecurityForDebug)
    val debugControlsAvailable = BuildConfig.DEBUG

    private var pendingAddressBarValue: String? = null

    private val stateChangedCallback: (String, Boolean, Boolean) -> Unit = { url, back, forward ->
        AmnosLog.d("BrowserViewModel", "State changed: $url (back=$back, forward=$forward)")
        currentTab.value?.currentUrl = url
        canGoBack.value = back
        canGoForward.value = forward
        if (loadingProgress.intValue >= 100) {
            loadingProgress.intValue = 0
        }
    }

    private val progressChangedCallback: (Int) -> Unit = { progress ->
        loadingProgress.intValue = progress
    }

    private val trackerBlockedCallback: () -> Unit = {
        blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount()
    }

    private val navigationCommittedCallback: (String) -> Unit = { committedUrl ->
        sessionManager.securityController.logInternal("[Nav:Commit]", committedUrl, "DEBUG")
        currentTab.value?.currentUrl = committedUrl
        if (uiState.value == BrowserUIState.BROWSING) {
            urlInput.value = committedUrl
        }
        pendingAddressBarValue = null
    }

    private val navigationFailedCallback: (String?) -> Unit = { failedUrl ->
        sessionManager.securityController.logInternal(
            "[Nav:Failure]",
            failedUrl ?: pendingAddressBarValue ?: "unknown",
            "WARN"
        )
        pendingAddressBarValue = null
    }

    init {
        AmnosLog.d("BrowserViewModel", "Initializing BrowserViewModel")
        sessionManager.registerTimeoutListener {
            AmnosLog.d("BrowserViewModel", "Session timeout triggered")
            handleSessionTimeout()
        }
        initializeSession()
    }

    private fun initializeSession(loadUrl: String? = null) {
        AmnosLog.d("BrowserViewModel", "Initializing session (loadUrl=$loadUrl)")
        try {
            val tab = sessionManager.createTab(
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation,
                onNavigationCommitted = navigationCommittedCallback,
                onNavigationFailed = navigationFailedCallback
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

    fun toggleRemoteDebugging(enabled: Boolean) {
        if (!BuildConfig.DEBUG) {
            sessionManager.securityController.logInternal(
                "[Diagnostics:RemoteDebugging]",
                "Ignored remote debugging toggle outside debug builds.",
                "WARN"
            )
            return
        }
        sessionManager.updatePrivacyPolicy { it.copy(enableRemoteDebugging = enabled) }
        refreshPolicyState()
        try {
            android.webkit.WebView.setWebContentsDebuggingEnabled(enabled)
            AmnosLog.d("BrowserViewModel", "Remote debugging dynamically set to: $enabled")
        } catch (e: Exception) {
            AmnosLog.e("BrowserViewModel", "Failed to set remote debugging dynamically", e)
        }
    }

    fun toggleForceRelaxSecurity(enabled: Boolean) {
        if (!BuildConfig.DEBUG) {
            sessionManager.securityController.logInternal(
                "[Diagnostics:RelaxedMode]",
                "Ignored relaxed security toggle outside debug builds.",
                "WARN"
            )
            return
        }
        sessionManager.updatePrivacyPolicy { it.copy(forceRelaxSecurityForDebug = enabled) }
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
                onNavigationRequested = ::handleMainFrameNavigation,
                onNavigationCommitted = navigationCommittedCallback,
                onNavigationFailed = navigationFailedCallback
            )
        }
    }

    fun navigate(input: String) {
        val resolvedNavigation = NavigationResolver.resolve(input) ?: return
        sessionManager.securityController.logInternal("[Nav:Navigate]", resolvedNavigation.input, "DEBUG")
        sessionManager.securityController.logInternal("[Nav:Transform]", resolvedNavigation.transformedUrl, "DEBUG")
        sessionManager.securityController.logInternal("[Nav:Sanitize]", resolvedNavigation.sanitizedUrl, "DEBUG")

        uiState.value = BrowserUIState.BROWSING
        urlInput.value = resolvedNavigation.displayText
        pendingAddressBarValue = resolvedNavigation.displayText
        handleMainFrameNavigation(resolvedNavigation.sanitizedUrl, resolvedNavigation.displayText)
    }

    fun goBack() {
        currentTab.value?.webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                goHome()
            }
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
        pendingAddressBarValue = null
        currentTab.value?.apply {
            currentUrl = "about:blank"
            webView.loadUrl("about:blank")
        }
        blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount()
    }

    fun reload() {
        val tab = currentTab.value ?: return
        if (privacyPolicy.value.resetIdentityOnRefresh && !tab.currentUrl.isNullOrBlank()) {
            currentTab.value = sessionManager.recreateTab(
                tab = tab,
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation,
                onNavigationCommitted = navigationCommittedCallback,
                onNavigationFailed = navigationFailedCallback
            )
            return
        }

        tab.webView.reload()
        sessionManager.touchSession()
    }

    fun killSwitch() {
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        blockedTrackersCount.intValue = 0
        pendingAddressBarValue = null

        currentTab.value = null
        sessionManager.killAll(terminateProcess = false)
        initializeSession()
    }

    private fun handleSessionTimeout() {
        currentTab.value = null
        blockedTrackersCount.intValue = 0
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        pendingAddressBarValue = null
        sessionManager.killAll(terminateProcess = false)
        initializeSession()
    }

    private fun refreshPolicyState() {
        privacyPolicy.value = sessionManager.privacyPolicy
        javaScriptMode.value = sessionManager.privacyPolicy.javascriptMode
        isWebGLEnabled.value = sessionManager.privacyPolicy.webGlMode == WebGlMode.SPOOF
        fingerprintProtectionLevel.value = sessionManager.privacyPolicy.fingerprintProtectionLevel
        blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount()
        enableRemoteDebugging.value = sessionManager.privacyPolicy.enableRemoteDebugging
        forceRelaxSecurityForDebug.value = sessionManager.privacyPolicy.forceRelaxSecurityForDebug
    }

    private fun handleMainFrameNavigation(url: String): Boolean = handleMainFrameNavigation(url, null)

    private fun handleMainFrameNavigation(url: String, addressBarValue: String? = null): Boolean {
        AmnosLog.d("BrowserViewModel", "Handling main frame navigation to: $url")
        sessionManager.securityController.logInternal("[Nav:Load]", url, "DEBUG")
        uiState.value = BrowserUIState.BROWSING
        val current = currentTab.value ?: return false
        val activeTab = if (sessionManager.shouldRecreateForTopLevelNavigation(current, url)) {
            sessionManager.recreateTab(
                tab = current,
                onStateChanged = stateChangedCallback,
                onProgressChanged = progressChangedCallback,
                onTrackerBlocked = trackerBlockedCallback,
                onNavigationRequested = ::handleMainFrameNavigation,
                onNavigationCommitted = navigationCommittedCallback,
                onNavigationFailed = navigationFailedCallback
            ).also { currentTab.value = it }
        } else {
            current
        }

        return sessionManager.loadUrl(activeTab, url).also { loaded ->
            if (loaded && addressBarValue != null) {
                pendingAddressBarValue = addressBarValue
                urlInput.value = addressBarValue
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.killAll(terminateProcess = false)
    }
}

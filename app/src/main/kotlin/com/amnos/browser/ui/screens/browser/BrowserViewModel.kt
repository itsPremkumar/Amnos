package com.amnos.browser.ui.screens.browser

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.amnos.browser.BuildConfig
import com.amnos.browser.core.fingerprint.FingerprintManager
import com.amnos.browser.core.security.*
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.core.session.TabInstance
import com.amnos.browser.ui.screens.browser.logic.*
import com.amnos.browser.core.network.DomainPolicyManager
import org.json.JSONObject

class BrowserViewModel(private val sessionManager: SessionManager) : ViewModel() {
    // 1. PRIMITIVE STATE (Sources of Truth for Compose)
    var currentTab = mutableStateOf<TabInstance?>(null)
    var urlInput = mutableStateOf("")
    var uiState = mutableStateOf<BrowserUIState>(BrowserUIState.HOME)
    var canGoBack = mutableStateOf(false)
    var canGoForward = mutableStateOf(false)
    var isBurning = mutableStateOf(false)
    var loadingProgress = mutableIntStateOf(0)
    var blockedTrackersCount = mutableIntStateOf(0)
    var showSecurityDashboard = mutableStateOf(false)
    var blockedNavigationUrl = mutableStateOf<String?>(null)
    var isDecoyVisible = mutableStateOf(false)
    var showAccessibilityWarning = mutableStateOf(false)
    
    // Policy-Linked State
    var privacyPolicy = mutableStateOf(sessionManager.privacyPolicy)
    var javaScriptMode = mutableStateOf(sessionManager.privacyPolicy.hardwareJavascriptMode)
    var isWebGLEnabled = mutableStateOf(sessionManager.privacyPolicy.hardwareWebGlMode == WebGlMode.SPOOF)
    var fingerprintProtectionLevel = mutableStateOf(sessionManager.privacyPolicy.hardwareFingerprintLevel)
    var firewallLevel = mutableStateOf(sessionManager.privacyPolicy.networkFirewallLevel)
    var isSandboxEnabled = mutableStateOf(sessionManager.privacyPolicy.purgeSandboxEnabled)

    // Security Dash State
    val requestLog = sessionManager.securityController.privacyLog.requestLog
    val activeConnections = sessionManager.securityController.monitor.activeConnections
    val proxyStatus = sessionManager.securityController.monitor.proxyStatus
    val dohStatus = sessionManager.securityController.monitor.dohStatus
    val webRtcStatus = sessionManager.securityController.monitor.webRtcStatus
    val webSocketStatus = sessionManager.securityController.monitor.webSocketStatus
    val webRtcAttemptCount = sessionManager.securityController.monitor.webRtcAttemptCount
    val webSocketAttemptCount = sessionManager.securityController.monitor.webSocketAttemptCount
    val internalLogs = sessionManager.securityController.forensicLog.internalLogs
    val privacyWarning = sessionManager.securityController.warningMessage

    // Identity State
    var sessionLabel = mutableStateOf(sessionManager.sessionId.take(8))
    var isLocked = mutableStateOf(false)
    var userPin = FingerprintManager.newUnlockPin()
    var pinInput = mutableStateOf("")
    var deviceProfile = mutableStateOf<com.amnos.browser.core.fingerprint.DeviceProfile?>(null)
    
    // Debug State
    var blockRemoteDebugging = mutableStateOf(sessionManager.privacyPolicy.debugBlockRemoteDebugging)
    var forceRelaxSecurityForDebug = mutableStateOf(sessionManager.privacyPolicy.forceRelaxSecurityForDebug)
    val debugControlsAvailable = !BuildConfig.DEBUG_LOCKDOWN_MODE

    // Firewall State
    var firewallAllowedDomains = mutableStateListOf<String>()
    var firewallBlockedDomains = mutableStateListOf<String>()

    // 2. IDENTIFIABLE CONTROLLERS
    private val nav = NavigationController(
        sessionManager = sessionManager,
        uiState = uiState,
        currentTab = currentTab,
        urlInput = urlInput,
        canBack = canGoBack,
        canForward = canGoForward,
        progress = loadingProgress,
        onNavigationBlocked = { blockedNavigationUrl.value = it }
    )

    private val policy = PolicyEnforcementController(
        sessionManager = sessionManager,
        policy = privacyPolicy,
        jsMode = javaScriptMode,
        webGl = isWebGLEnabled,
        fpLevel = fingerprintProtectionLevel,
        firewall = firewallLevel,
        sandbox = isSandboxEnabled,
        onPolicyUpdated = ::refreshPolicyState
    )

    private val purge = PurgeOrchestrationController(
        scope = viewModelScope,
        sessionManager = sessionManager,
        isBurning = isBurning,
        currentTab = currentTab,
        uiState = uiState,
        urlInput = urlInput,
        initializeSession = ::initializeSession
    )

    // 3. CALLBACKS & BRIDGE LOGIC
    internal val stateChangedCallback: (String, Boolean, Boolean) -> Unit = { url, back, forward ->
        currentTab.value?.currentUrl = url
        canGoBack.value = back
        canGoForward.value = forward
        if (loadingProgress.intValue >= 100) loadingProgress.intValue = 0
    }

    init {
        AmnosLog.d("BrowserViewModel", "Initializing Modular BrowserViewModel")
        sessionManager.registerTimeoutListener { handleSessionTimeout() }
        sessionManager.registerWipeListener { zeroAllUIState() }
        
        viewModelScope.launch {
            while (true) {
                RiskEngine.monitor(privacyPolicy.value) { hardKillSwitch() }
                delay(2000)
            }
        }
        
        initializeSession()
        syncFirewallState()
    }

    internal fun initializeSession(loadUrl: String? = null) {
        val tab = sessionManager.createTab(
            onStateChanged = stateChangedCallback,
            onProgressChanged = { loadingProgress.intValue = it },
            onTrackerBlocked = { blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount() },
            onNavigationRequested = { url -> nav.handleMainFrameNavigation(url) { recreateCurrentTab() } },
            onNavigationCommitted = { url -> 
                currentTab.value?.currentUrl = url
                if (uiState.value == BrowserUIState.BROWSING) urlInput.value = url
            },
            onNavigationFailed = { nav.pendingAddressBarValue = null },
            onKeyboardRequested = { webKeyboardRequested.value = it },
            onSecurityEvent = ::handleSecurityEvent
        )
        currentTab.value = tab
        deviceProfile.value = tab.profile
        sessionLabel.value = sessionManager.sessionId.take(8)
        refreshPolicyState()
        loadUrl?.let { nav.navigate(it) }
    }

    // 4. DELEGATED ACTIONS
    fun navigate(input: String) = nav.navigate(input)
    fun goBack() = nav.goBack()
    fun goForward() = nav.goForward()
    fun goHome() = nav.goHome()
    fun reload() = nav.reload()

    fun setJavaScriptMode(mode: JavaScriptMode) = policy.setJavaScriptMode(mode)
    fun toggleWebGL(enabled: Boolean) = policy.toggleWebGL(enabled)
    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) = policy.setFingerprintProtectionLevel(level)
    fun setFirewallLevel(l: FirewallLevel) = policy.setFirewallLevel(l)
    fun toggleSandboxEnabled(e: Boolean) = policy.toggleSandboxEnabled(e)

    fun toggleHttpsOnly(e: Boolean) = policy.toggleGenericPolicy { it.copy(networkHttpsOnly = e) }
    fun toggleThirdPartyBlocking(e: Boolean) = policy.toggleGenericPolicy { it.copy(filterBlockThirdPartyRequests = e) }
    fun toggleInlineScriptBlocking(e: Boolean) = policy.toggleGenericPolicy { it.copy(filterBlockInlineScripts = e) }
    fun toggleResetIdentityOnRefresh(e: Boolean) = policy.toggleGenericPolicy { it.copy(identityResetOnRefresh = e) }
    fun toggleStrictFirstPartyIsolation(e: Boolean) = policy.toggleGenericPolicy { it.copy(filterStrictFirstPartyIsolation = e) }
    fun toggleWebSockets(e: Boolean) = policy.toggleGenericPolicy { it.copy(filterBlockWebSockets = e) }
    fun toggleRemoteDebugging(e: Boolean) = policy.toggleGenericPolicy { it.copy(debugBlockRemoteDebugging = e) }
    fun toggleForceRelaxSecurity(e: Boolean) = policy.toggleGenericPolicy { it.copy(debugLockdownMode = !e) }

    fun killSwitch() = purge.initiateKillSwitch(terminateProcess = false)
    fun hardKillSwitch() = purge.initiateKillSwitch(terminateProcess = true)

    // UI Navigation Actions
    fun openPrivacyChecklist() { uiState.value = BrowserUIState.PRIVACY_CHECKLIST }
    fun closePrivacyChecklist() { uiState.value = BrowserUIState.HOME }
    fun openFirewall() { uiState.value = BrowserUIState.FIREWALL }
    fun closeFirewall() { uiState.value = BrowserUIState.HOME }

    // Web Input Injection
    fun injectWebInput(text: String) { currentTab.value?.webView?.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_UNKNOWN)) }
    fun injectWebBackspace() { currentTab.value?.webView?.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL)) }
    fun injectWebSearch() { currentTab.value?.webView?.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)) }

    // 5. MISC LOGIC
    fun handleSecurityEvent(json: String) {
        if (JSONObject(json).optString("type") == "tamper_detected") hardKillSwitch()
    }

    fun confirmBlockedNavigation() {
        blockedNavigationUrl.value?.let { sessionManager.loadUrl(currentTab.value!!, it, forceBypassSandbox = true) }
        blockedNavigationUrl.value = null
    }

    fun cancelBlockedNavigation() { blockedNavigationUrl.value = null }

    fun addFirewallRule(host: String, allow: Boolean) {
        if (allow) DomainPolicyManager.addAllowedDomain(host) else DomainPolicyManager.addBlockedDomain(host)
        syncFirewallState()
    }

    fun removeFirewallRule(host: String, isAllow: Boolean) {
        if (isAllow) DomainPolicyManager.removeAllowedDomain(host) else DomainPolicyManager.removeBlockedDomain(host)
        syncFirewallState()
    }

    private fun syncFirewallState() {
        firewallAllowedDomains.clear()
        firewallAllowedDomains.addAll(DomainPolicyManager.getAllowedDomains())
        firewallBlockedDomains.clear()
        firewallBlockedDomains.addAll(DomainPolicyManager.getBlockedDomains())
    }

    internal fun refreshPolicyState(): Unit {
        policy.syncUIState()
    }

    private fun handleSessionTimeout() {
        sessionManager.killAll(terminateProcess = false)
        initializeSession()
    }

    private fun zeroAllUIState() {
        currentTab.value = null
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        loadingProgress.intValue = 0
        blockedTrackersCount.intValue = 0
        isLocked.value = false
    }

    internal fun recreateCurrentTab() {
        currentTab.value?.let { tab ->
            currentTab.value = sessionManager.recreateTab(
                tab = tab,
                onStateChanged = stateChangedCallback,
                onProgressChanged = { loadingProgress.intValue = it },
                onTrackerBlocked = { blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount() },
                onNavigationRequested = { url -> nav.handleMainFrameNavigation(url) { recreateCurrentTab() } },
                onNavigationCommitted = { urlInput.value = it },
                onNavigationFailed = { nav.pendingAddressBarValue = null },
                onKeyboardRequested = { webKeyboardRequested.value = it },
                onSecurityEvent = ::handleSecurityEvent
            )
        }
    }

    var webKeyboardRequested = mutableStateOf(false)

    // Callbacks for legacy/modular handlers
    internal val progressChangedCallback: (Int) -> Unit = { loadingProgress.intValue = it }
    internal val trackerBlockedCallback: () -> Unit = { blockedTrackersCount.intValue = sessionManager.securityController.trackerBlockCount() }
    internal val navigationCommittedCallback: (String) -> Unit = { url -> 
        currentTab.value?.currentUrl = url
        if (uiState.value == BrowserUIState.BROWSING) urlInput.value = url
    }
    internal val navigationFailedCallback: (String?) -> Unit = { nav.pendingAddressBarValue = null }
    internal val keyboardRequestedCallback: (Boolean) -> Unit = { webKeyboardRequested.value = it }

    fun updatePendingAddressBar(value: String?) { nav.pendingAddressBarValue = value }
    fun updateBlockedTrackersCount() { trackerBlockedCallback() }
}

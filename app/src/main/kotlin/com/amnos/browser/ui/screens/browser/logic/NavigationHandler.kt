package com.amnos.browser.ui.screens.browser.logic

import com.amnos.browser.core.network.NavigationResolver
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.ui.screens.browser.BrowserStateReducer
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.ui.screens.browser.BrowserUIState
import com.amnos.browser.ui.screens.browser.BrowserViewModel

class NavigationHandler(
    private val viewModel: BrowserViewModel,
    private val sessionManager: SessionManager
) {
    fun navigate(input: String) {
        val resolvedNavigation = NavigationResolver.resolve(input) ?: return
        AmnosLog.d("NavigationHandler", "Navigating to: ${resolvedNavigation.sanitizedUrl}")

        viewModel.uiState.value = BrowserStateReducer.showBrowsing()
        viewModel.urlInput.value = resolvedNavigation.displayText
        viewModel.updatePendingAddressBar(resolvedNavigation.displayText)
        handleMainFrameNavigation(resolvedNavigation.sanitizedUrl, resolvedNavigation.displayText)
    }

    fun goBack() {
        viewModel.currentTab.value?.webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                goHome()
            }
        }
        sessionManager.touchSession()
    }

    fun goForward() {
        viewModel.currentTab.value?.webView?.let {
            if (it.canGoForward()) it.goForward()
        }
        sessionManager.touchSession()
    }

    fun goHome() {
        viewModel.uiState.value = BrowserStateReducer.showHome()
        viewModel.urlInput.value = ""
        viewModel.updatePendingAddressBar(null)
        viewModel.currentTab.value?.apply {
            currentUrl = "about:blank"
            webView.loadUrl("about:blank")
        }
        viewModel.updateBlockedTrackersCount()
    }

    fun reload() {
        val tab = viewModel.currentTab.value ?: return
        if (viewModel.privacyPolicy.value.resetIdentityOnRefresh && !tab.currentUrl.isNullOrBlank()) {
            AmnosLog.i("NavigationHandler", "Reload triggered with 'Identity Reset' enabled. Recreating tab.")
            viewModel.recreateCurrentTab()
            return
        }

        AmnosLog.d("NavigationHandler", "Reloading current page: ${tab.currentUrl}")
        tab.webView.reload()
        sessionManager.touchSession()
    }

    fun handleMainFrameNavigation(url: String): Boolean = handleMainFrameNavigation(url, null)

    fun handleMainFrameNavigation(url: String, addressBarValue: String? = null): Boolean {
        AmnosLog.d("NavigationHandler", "Handling main frame navigation to: $url")
        sessionManager.securityController.logInternal("[Nav:Load]", url, "DEBUG")
        var current = viewModel.currentTab.value
        if (current == null) {
            AmnosLog.d("NavigationHandler", "No active tab during navigation, initializing new session")
            viewModel.initializeSession()
            current = viewModel.currentTab.value ?: return false
        }

        val activeTab = if (sessionManager.shouldRecreateForTopLevelNavigation(current, url)) {
            AmnosLog.i("NavigationHandler", "FIRST PARTY ISOLATION: Recreating tab for cross-site navigation to ${url}")
            sessionManager.recreateTab(
                tab = current,
                onStateChanged = viewModel.stateChangedCallback,
                onProgressChanged = viewModel.progressChangedCallback,
                onTrackerBlocked = viewModel.trackerBlockedCallback,
                onNavigationRequested = { handleMainFrameNavigation(it) },
                onNavigationCommitted = viewModel.navigationCommittedCallback,
                onNavigationFailed = viewModel.navigationFailedCallback,
                onKeyboardRequested = viewModel.keyboardRequestedCallback
            ).also { viewModel.currentTab.value = it }
        } else {
            current
        }

        return sessionManager.loadUrl(activeTab, url).also { loaded ->
            if (loaded && addressBarValue != null) {
                viewModel.updatePendingAddressBar(addressBarValue)
                viewModel.urlInput.value = addressBarValue
            }
        }
    }
}

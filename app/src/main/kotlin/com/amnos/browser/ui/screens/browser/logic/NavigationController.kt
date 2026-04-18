package com.amnos.browser.ui.screens.browser.logic

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.core.session.TabInstance
import com.amnos.browser.core.webview.AmnosWebView
import com.amnos.browser.ui.screens.browser.BrowserUIState

class NavigationController(
    private val sessionManager: SessionManager,
    private val uiState: MutableState<BrowserUIState>,
    private val currentTab: MutableState<TabInstance?>,
    private val urlInput: MutableState<String>,
    private val canBack: MutableState<Boolean>,
    private val canForward: MutableState<Boolean>,
    private val progress: MutableState<Int>,
    private val onNavigationBlocked: (String) -> Unit
) {
    var pendingAddressBarValue: String? = null

    fun navigate(input: String) {
        val tab = currentTab.value ?: return
        AmnosLog.d("NavController", "Requesting navigation to: $input")
        
        pendingAddressBarValue = input
        val success = sessionManager.loadUrl(tab, input)
        
        if (success) {
            uiState.value = BrowserUIState.BROWSING
        } else {
            onNavigationBlocked(input)
        }
    }

    fun goBack() {
        currentTab.value?.webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                goHome()
            }
        }
    }

    fun goForward() {
        currentTab.value?.webView?.goForward()
    }

    fun goHome() {
        currentTab.value?.let { sessionManager.loadUrl(it, "about:blank") }
        uiState.value = BrowserUIState.HOME
        urlInput.value = ""
        canBack.value = false
        canForward.value = false
    }

    fun reload() {
        currentTab.value?.webView?.reload()
    }

    fun handleMainFrameNavigation(url: String, triggerRecreation: () -> Unit): Boolean {
        AmnosLog.i("NavController", "Main-frame navigation requested: $url")
        val tab = currentTab.value ?: return true
        
        if (sessionManager.shouldRecreateForTopLevelNavigation(tab, url)) {
            AmnosLog.w("NavController", "CROSS-SITE isolation trigger. Tab recreation required.")
            triggerRecreation()
            return true // TRUE tells the old WebView to ABORT loading.
        }
        
        return false // FALSE tells the current WebView to PROCEED loading normally.
    }
}

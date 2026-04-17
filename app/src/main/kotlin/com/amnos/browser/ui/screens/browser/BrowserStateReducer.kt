package com.amnos.browser.ui.screens.browser

object BrowserStateReducer {
    fun showPrivacyChecklist(): BrowserUIState = BrowserUIState.PRIVACY_CHECKLIST

    fun showHome(): BrowserUIState = BrowserUIState.HOME

    fun showBrowsing(): BrowserUIState = BrowserUIState.BROWSING
}

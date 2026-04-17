package com.amnos.browser.ui.screens.browser

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BrowserFlowRegressionTest {

    @Test
    fun privacyChecklistHomeBrowsingLoopRemainsStable() {
        var state = BrowserUIState.HOME

        repeat(50) {
            state = BrowserStateReducer.showPrivacyChecklist()
            assertEquals(BrowserUIState.PRIVACY_CHECKLIST, state)

            state = BrowserStateReducer.showHome()
            assertEquals(BrowserUIState.HOME, state)

            state = BrowserStateReducer.showBrowsing()
            assertEquals(BrowserUIState.BROWSING, state)

            state = BrowserStateReducer.showHome()
            assertEquals(BrowserUIState.HOME, state)
        }
    }
}

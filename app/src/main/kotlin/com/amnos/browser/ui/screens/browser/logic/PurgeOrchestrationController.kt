package com.amnos.browser.ui.screens.browser.logic

import androidx.compose.runtime.MutableState
import androidx.lifecycle.viewModelScope
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.core.session.TabInstance
import com.amnos.browser.ui.screens.browser.BrowserUIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

class PurgeOrchestrationController(
    private val scope: CoroutineScope,
    private val sessionManager: SessionManager,
    private val isBurning: MutableState<Boolean>,
    private val currentTab: MutableState<TabInstance?>,
    private val uiState: MutableState<BrowserUIState>,
    private val urlInput: MutableState<String>,
    private val initializeSession: () -> Unit
) {
    fun initiateKillSwitch(terminateProcess: Boolean) {
        scope.launch {
            try {
                isBurning.value = true
                AmnosLog.w("PurgeController", "CRITICAL: Initiating Purge Sequence (Terminate=$terminateProcess)")
                
                // 1. UI Detachment
                currentTab.value = null
                delay(120)
                
                // 2. Core Wipe
                sessionManager.killAll(terminateProcess = terminateProcess)
                
                if (!terminateProcess) {
                    delay(1500)
                    initializeSession()
                    uiState.value = BrowserUIState.HOME
                    urlInput.value = ""
                    AmnosLog.i("PurgeController", "Session Purge Complete. New identity established.")
                }
            } catch (e: Exception) {
                AmnosLog.e("PurgeController", "FATAL error during purge sequence", e)
            } finally {
                if (!terminateProcess) {
                    delay(500)
                    isBurning.value = false
                }
            }
        }
    }
}

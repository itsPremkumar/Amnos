package com.amnos.browser.ui.screens.browser.logic

import androidx.compose.runtime.MutableState
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.core.session.TabInstance
import com.amnos.browser.core.wipe.BurnState
import com.amnos.browser.ui.screens.browser.BrowserUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PurgeOrchestrationController(
    private val scope: CoroutineScope,
    private val sessionManager: SessionManager,
    private val isBurning: MutableState<Boolean>,
    private val currentTab: MutableState<TabInstance?>,
    private val uiState: MutableState<BrowserUIState>,
    private val urlInput: MutableState<String>,
    private val initializeSession: () -> Unit
) {
    init {
        scope.launch {
            sessionManager.burnState.collectLatest { state ->
                handleBurnStateTransition(state)
            }
        }
    }

    private suspend fun handleBurnStateTransition(state: BurnState) {
        when (state) {
            is BurnState.Idle -> {
                isBurning.value = false
            }
            is BurnState.Preparing -> {
                isBurning.value = true
                currentTab.value = null // Immediate detachment
                AmnosLog.w("PurgeController", "Sequence started: Preparing environment.")
            }
            is BurnState.Running -> {
                AmnosLog.i("PurgeController", "Phase: ${state.taskName}")
            }
            is BurnState.Completing -> {
                AmnosLog.d("PurgeController", "Finalizing cleanup...")
            }
            is BurnState.Success -> {
                delay(800) // Visual buffer
                initializeSession()
                uiState.value = BrowserUIState.HOME
                urlInput.value = ""
                AmnosLog.i("PurgeController", "✔ Sequence Complete. Identity reset.")
            }
            is BurnState.Failed -> {
                AmnosLog.e("PurgeController", "✘ Sequence Failed at task: ${state.taskName}", state.error)
                // In a production app, we might show a retry or catastrophic failure alert here
            }
        }
    }

    fun initiateKillSwitch(terminateProcess: Boolean) {
        scope.launch {
            AmnosLog.w("PurgeController", "CRITICAL: Manual kill switch triggered.")
            sessionManager.killAllSuspend(terminateProcess = terminateProcess)
        }
    }
}

package com.amnos.browser.core.wipe

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.amnos.browser.core.network.LoopbackProxyServer
import com.amnos.browser.core.service.StorageService
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.session.TabManager
import com.amnos.browser.core.wipe.tasks.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WipeReason {
    KILL_SWITCH,
    SESSION_TIMEOUT,
    BACKGROUND_WIPE,
    CRASH,
    TAMPER
}

class SuperWipeEngine(
    private val context: Context,
    private val tabManager: TabManager,
    private val storageService: StorageService,
    private val securityController: SecurityController,
    private val loopbackProxyServer: LoopbackProxyServer,
    private val onNewSessionNeeded: () -> Unit,
    private val onWipeCompleted: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isWiping = java.util.concurrent.atomic.AtomicBoolean(false)

    private val _burnState = MutableStateFlow<BurnState>(BurnState.Idle)
    val burnState: StateFlow<BurnState> = _burnState.asStateFlow()

    suspend fun execute(reason: WipeReason, terminateProcess: Boolean = false, wipeClipboard: Boolean = true) {
        if (!isWiping.compareAndSet(false, true)) {
            AmnosLog.w("SuperWipeEngine", "ABORT: Wipe already in progress. Ignoring concurrent request.")
            return
        }

        try {
            _burnState.value = BurnState.Preparing
            AmnosLog.w("SuperWipeEngine", "SUPER WIPE TRIGGERED | Reason: $reason | Terminate: $terminateProcess")

            val tasks = listOf(
                CryptographicTask(context),
                WebViewTeardownTask(tabManager),
                StorageSanitizationTask(storageService, securityController, wipeClipboard),
                MemoryInvalidationTask(securityController),
                NetworkRotationTask(loopbackProxyServer)
            )

            tasks.forEach { task ->
                _burnState.value = BurnState.Running(task.name)
                val result = task.execute()
                
                result.onFailure { error ->
                    AmnosLog.e("SuperWipeEngine", "FATAL CLUSTER FAILURE: ${task.name} failed!", error)
                    _burnState.value = BurnState.Failed(error, task.name)
                    
                    // EMERGENCY ESCALATION: 
                    // If a purge task fails, we cannot guarantee security.
                    // We must terminate the process immediately.
                    executeHardKill("AUTOMATIC_EMERGENCY_SHUTDOWN")
                    return@execute // Stop the sequence
                }
            }

            _burnState.value = BurnState.Completing
            onNewSessionNeeded()
            onWipeCompleted()
            _burnState.value = BurnState.Success

            if (terminateProcess) {
                executeHardKill("MANUAL_KILL_SWITCH")
            }
        } catch (e: Exception) {
            AmnosLog.e("SuperWipeEngine", "FATAL error in sequence runner", e)
            _burnState.value = BurnState.Failed(e, "Sequence Runner")
            executeHardKill("SEQUENCE_RUNNER_PANIC")
        } finally {
            if (!terminateProcess) {
                isWiping.set(false)
                _burnState.value = BurnState.Idle
            }
        }
    }

    private fun executeHardKill(reason: String) {
        AmnosLog.w("SuperWipeEngine", "!!! TERMINATING PROCESS: $reason !!!")
        mainHandler.postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(0)
        }, 500)
    }
}

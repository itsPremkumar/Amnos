package com.amnos.browser.core.security

import android.os.Debug
import com.amnos.browser.core.session.AmnosLog

object RiskEngine {

    /**
     * Checks for known debugging and analysis tools.
     * Returns TRUE if the environment is suspicious.
     */
    fun checkIntegrity(): Boolean {
        // 1. Check for connected debugger
        if (Debug.isDebuggerConnected()) {
            AmnosLog.e("RiskEngine", "TAMPER DETECTED: Remote Debugger Connected!")
            return true
        }

        // 2. Check for waiting debugger
        if (Debug.waitingForDebugger()) {
            AmnosLog.e("RiskEngine", "TAMPER DETECTED: System waiting for Debugger!")
            return true
        }

        return false
    }

    /**
     * Periodic check that can be called from Lifecycle or UI loops.
     */
    fun monitor(onTamper: () -> Unit) {
        if (checkIntegrity()) {
            AmnosLog.w("RiskEngine", "INTEGRITY FAILURE: Triggering immediate hard wipe.")
            onTamper()
        }
    }
}

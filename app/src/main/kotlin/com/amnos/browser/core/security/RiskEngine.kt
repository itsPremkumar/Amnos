package com.amnos.browser.core.security

import android.os.Build
import android.os.Debug
import com.amnos.browser.core.session.AmnosLog
import java.io.File

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

        // 3. Root Detection (Common su paths)
        if (isRooted()) {
            AmnosLog.e("RiskEngine", "TAMPER DETECTED: Device is Rooted!")
            return true
        }

        // 4. Emulator Detection
        if (isEmulator()) {
            AmnosLog.e("RiskEngine", "TAMPER DETECTED: Running in Emulator!")
            return true
        }

        return false
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Periodic check that can be called from Lifecycle or UI loops.
     */
    fun monitor(policy: PrivacyPolicy, onTamper: () -> Unit) {
        if (!policy.debugAntiDebugger) return

        if (checkIntegrity()) {
            AmnosLog.e("RiskEngine", "INTEGRITY FAILURE: Automated Nuclear Exit triggered.")
            onTamper()
        }
    }
}

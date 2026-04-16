package com.amnos.browser.core.security

import android.content.Context
import com.amnos.browser.core.session.AmnosLog
import java.io.File

object RootDetector {
    private val rootPaths = arrayOf(
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

    @Suppress("UNUSED_PARAMETER")
    fun isRooted(context: Context): Boolean {
        // 1. Check for common su binaries
        for (path in rootPaths) {
            if (File(path).exists()) {
                AmnosLog.w("RootDetector", "ROOT DETECTED: Found su binary at $path")
                return true
            }
        }

        // 2. Check for Test-Keys tag in build info
        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            AmnosLog.w("RootDetector", "ROOT DETECTED: Build tags contain 'test-keys'")
            return true
        }

        // 3. Check executing su command
        return try {
            val found = Runtime.getRuntime().exec("which su").inputStream.bufferedReader().use { it.readLine() != null }
            if (found) AmnosLog.w("RootDetector", "ROOT DETECTED: 'which su' command succeeded")
            found
        } catch (e: Exception) {
            false
        }
    }
}

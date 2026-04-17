package com.amnos.browser.core.session

import androidx.compose.runtime.mutableStateListOf
import com.amnos.browser.BuildConfig
import com.amnos.browser.core.model.InternalLogEntry

class ForensicAuditLog {
    val internalLogs = mutableStateListOf<InternalLogEntry>()
    private val lock = Any()

    @Volatile
    private var mirrorToSystemLog = !BuildConfig.DEBUG_LOCKDOWN_MODE

    fun log(tag: String, message: String, level: String = "INFO") {
        synchronized(lock) {
            internalLogs.add(0, InternalLogEntry(tag = tag, message = message, level = level))
            while (internalLogs.size > 10000) {
                internalLogs.removeAt(internalLogs.size - 1)
            }
        }

        if (mirrorToSystemLog) {
            android.util.Log.println(
                when (level) {
                    "DEBUG" -> android.util.Log.DEBUG
                    "WARN" -> android.util.Log.WARN
                    "ERROR" -> android.util.Log.ERROR
                    else -> android.util.Log.INFO
                },
                tag,
                message
            )
        }
    }

    fun setMirroringEnabled(enabled: Boolean) {
        mirrorToSystemLog = enabled
    }

    fun clear() {
        synchronized(lock) {
            internalLogs.clear()
        }
    }
}

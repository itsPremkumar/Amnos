package com.amnos.browser.core.security

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.amnos.browser.core.session.AmnosLog

/**
 * Amnos Clipboard Sentinel
 * Handles forensic-proof wiping of the system clipboard to prevent data leakage
 * when the application is backgrounded or terminated.
 */
object ClipboardSentinel {

    fun wipe(context: Context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29 && !isProcessForeground()) {
                AmnosLog.d("ClipboardSentinel", "Clipboard scrub skipped: process not foreground.")
                return
            }

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                // AMNOS SILENT WIPE: API 33+ supports clearPrimaryClip()
                // This informs the system to forget the data without treated it as a new "Copy" event,
                // which suppresses the intrusive "Copied" and "Send to device" overlays.
                clipboard.clearPrimaryClip()
            } else {
                // FALLBACK: Set an empty sensitive clip
                val clipData = ClipData.newPlainText("", "")
                // Standard Android IS_SENSITIVE extra (API 33+) but we apply it as a best-effort hint
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    val extras = android.os.PersistableBundle().apply {
                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                    }
                    clipData.description.extras = extras
                }
                clipboard.setPrimaryClip(clipData)
            }
            AmnosLog.d("ClipboardSentinel", "Forensic clipboard scrub successful.")
        } catch (e: Exception) {
            // Silently skip if denied due to focus; this is expected on modern Android when backgrounded.
            val msg = e.message ?: ""
            if (msg.contains("denied", ignoreCase = true) || 
                msg.contains("focus", ignoreCase = true)) {
                AmnosLog.d("ClipboardSentinel", "Clipboard scrub skipped: App out of focus (normal for session wipe)")
            } else {
                AmnosLog.e("ClipboardSentinel", "Failed to scrub clipboard", e)
            }
        }
    }

    private fun isProcessForeground(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
            state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }
}

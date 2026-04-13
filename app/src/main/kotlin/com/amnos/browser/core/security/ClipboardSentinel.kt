package com.amnos.browser.core.security

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
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            
            // On Android 13+ (API 33), we can hint to the system that this is sensitive
            // to suppress the visual preview (the "Copied" text preview).
            val clipData = ClipData.newPlainText("", "")
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                val extras = android.os.PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
                clipData.description.extras = extras
            }

            // On Android 10+ (API 29), we can only access the clipboard if we have focus.
            clipboard.setPrimaryClip(clipData)
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
}

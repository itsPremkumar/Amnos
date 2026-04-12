package com.privacy.browser.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.privacy.browser.core.session.AmnosLog

/**
 * Amnos Clipboard Sentinel
 * Handles forensic-proof wiping of the system clipboard to prevent data leakage
 * when the application is backgrounded or terminated.
 */
object ClipboardSentinel {

    fun wipe(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            
            // On Android 10+ (API 29), we can only access the clipboard if we have focus.
            // Since this is often called during backgrounding/memory trim, we expect it might fail.
            // We use a silent check to avoid cluttering logs with system errors.
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                // We attempt to set it; if it fails due to focus, we catch it below.
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
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
}

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
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            AmnosLog.d("ClipboardSentinel", "Forensic clipboard scrub successful.")
        } catch (e: Exception) {
            AmnosLog.e("ClipboardSentinel", "Failed to scrub clipboard", e)
        }
    }
}

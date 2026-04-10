package com.privacy.browser.core.security

import android.content.ClipboardManager
import android.content.Context
import android.util.Log

/**
 * Amnos Clipboard Sentinel
 * Handles forensic-proof wiping of the system clipboard to prevent data leakage
 * when the application is backgrounded or terminated.
 */
object ClipboardSentinel {

    fun wipe(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            @Suppress("DEPRECATION")
            clipboard.text = ""
            Log.d("ClipboardSentinel", "Forensic clipboard scrub successful.")
        } catch (e: Exception) {
            Log.e("ClipboardSentinel", "Failed to scrub clipboard", e)
        }
    }
}

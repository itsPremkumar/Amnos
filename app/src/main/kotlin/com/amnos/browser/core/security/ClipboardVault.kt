package com.amnos.browser.core.security

import com.amnos.browser.core.session.AmnosLog
import java.util.Arrays

/**
 * Amnos Clipboard Vault
 * Holds copied text securely in volatile memory.
 * Never touches the Android OS Clipboard.
 */
object ClipboardVault {
    private var volatileCache: CharArray? = null

    fun write(text: String) {
        AmnosLog.v("ClipboardVault", "Writing to secure volatile pasteboard")
        wipe() // Wipe existing safely before replacing
        volatileCache = text.toCharArray()
    }

    fun read(): String? {
        return volatileCache?.let { String(it) }
    }

    fun wipe() {
        volatileCache?.let {
            Arrays.fill(it, '\u0000') // Zero out memory
            AmnosLog.v("ClipboardVault", "Volatile pasteboard shredded")
        }
        volatileCache = null
    }
}

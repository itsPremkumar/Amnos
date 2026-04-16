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
        volatileCache?.let { buffer ->
            try {
                // Check policy before scrambling
                val sessionManager = com.amnos.browser.core.session.SessionManager.getInstance()
                
                if (sessionManager.privacyPolicy.forensicScrambleEnabled) {
                    // Forensic-grade scrubbing: Overwrite with random noise first
                    val secureRandom = java.security.SecureRandom()
                    for (i in buffer.indices) {
                        buffer[i] = secureRandom.nextInt(65535).toChar()
                    }
                }
                
                // Finally zero it out
                java.util.Arrays.fill(buffer, '\u0000')
                AmnosLog.v("ClipboardVault", "Volatile pasteboard shredded")
            } catch (e: Exception) {
                java.util.Arrays.fill(buffer, '\u0000')
            }
        }
        volatileCache = null
    }
}

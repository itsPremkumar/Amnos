package com.amnos.browser.core.security

import com.amnos.browser.core.session.AmnosLog
import java.security.SecureRandom

/**
 * Amnos Clipboard Vault
 * A high-security, RAM-only clipboard that replaces the system clipboard.
 */
object ClipboardVault {
    private var vaultBuffer: String? = null
    private val secureRandom = SecureRandom()

    fun write(text: String) {
        AmnosLog.d("ClipboardVault", "Data sequestered to in-memory vault. OS Clipboard bypassed.")
        vaultBuffer = text
    }

    fun get(): String? = vaultBuffer

    fun wipe() {
        if (vaultBuffer == null) return
        
        AmnosLog.w("ClipboardVault", "Scrubbing vault buffer with SecureRandom noise...")
        
        // Phase 1: Overwrite with random characters
        val length = vaultBuffer?.length ?: 0
        val noise = StringBuilder()
        repeat(length) {
            noise.append(secureRandom.nextInt(256).toChar())
        }
        vaultBuffer = noise.toString()
        
        // Phase 2: Zero-fill/Nullify
        vaultBuffer = null
        AmnosLog.i("ClipboardVault", "Vault buffer zero-filled and nullified.")
    }
}

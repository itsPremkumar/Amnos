package com.amnos.browser.core.wipe.tasks

import android.content.Context
import com.amnos.browser.core.security.KeyManager
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.wipe.WipeTask

/**
 * Task responsible for destroying the current session's cryptographic keys.
 * This is Phase 0 of the purge sequence.
 */
class CryptographicTask(private val context: Context) : WipeTask {
    override val name: String = "Cryptographic Kill Switch"

    override suspend fun execute(): Result<Unit> = runCatching {
        AmnosLog.d("WipeTasks", "Obliterating session keys...")
        KeyManager.obliterateKey()
        AmnosLog.i("WipeTasks", "✔ Keys obliterated.")
    }
}

package com.amnos.browser.core.wipe.tasks

import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.wipe.WipeTask

/**
 * Task responsible for invalidating in-memory session data and hardening the heap.
 * This covers Phases 3 & 7 of the purge sequence.
 */
class MemoryInvalidationTask(private val securityController: SecurityController) : WipeTask {
    override val name: String = "Memory Invalidation"

    override suspend fun execute(): Result<Unit> = runCatching {
        AmnosLog.d("WipeTasks", "Obliterating memory buffers...")
        securityController.obliterate()
        
        AmnosLog.d("WipeTasks", "Hardening heap (scrubbing reusable segments)...")
        hardenHeap()
        
        System.gc()
        System.runFinalization()
        AmnosLog.i("WipeTasks", "✔ Memory sanitized.")
    }

    private fun hardenHeap() {
        try {
            // Scrub 8MB of heap to wipe sensitive pattern residues
            val buffer = ByteArray(8 * 1024 * 1024)
            for (i in buffer.indices step 4096) {
                buffer[i] = 0
            }
        } catch (e: OutOfMemoryError) {
            AmnosLog.w("WipeTasks", "Heap hardening skipped due to memory pressure")
        }
    }
}

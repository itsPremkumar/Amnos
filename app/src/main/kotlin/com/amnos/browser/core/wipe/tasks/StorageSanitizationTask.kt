package com.amnos.browser.core.wipe.tasks

import com.amnos.browser.core.service.StorageService
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.wipe.WipeTask
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Task responsible for cleaning the filesystem, cookies, cache, and clipboard.
 * This covers Phases 2 & 5 of the purge sequence.
 */
class StorageSanitizationTask(
    private val storageService: StorageService,
    private val securityController: SecurityController,
    private val wipeClipboard: Boolean
) : WipeTask {
    override val name: String = "Storage Sanitization"

    override suspend fun execute(): Result<Unit> = runCatching {
        AmnosLog.d("WipeTasks", "Initiating Storage Purge...")
        
        if (wipeClipboard) {
            com.amnos.browser.core.security.ClipboardVault.wipe()
            storageService.wipeClipboard()
        }
        storageService.clearVolatileDownloads()

        suspendCancellableCoroutine<Unit> { continuation ->
            storageService.superPurge(
                onCompleted = {
                    continuation.resume(Unit)
                },
                logCallback = securityController::logInternal
            )
        }
        
        AmnosLog.i("WipeTasks", "✔ Storage sanitized.")
    }
}

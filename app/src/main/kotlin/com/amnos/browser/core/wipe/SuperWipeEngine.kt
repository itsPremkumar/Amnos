package com.amnos.browser.core.wipe

import android.os.Handler
import android.os.Looper
import android.os.Process
import com.amnos.browser.core.network.DnsManager
import com.amnos.browser.core.network.LoopbackProxyServer
import com.amnos.browser.core.service.StorageService
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.session.TabInstance
import com.amnos.browser.core.security.KeyManager

enum class WipeReason {
    KILL_SWITCH,
    SESSION_TIMEOUT,
    BACKGROUND_WIPE,
    CRASH
}

class SuperWipeEngine(
    private val tabs: MutableList<TabInstance>,
    private val storageService: StorageService,
    private val securityController: SecurityController,
    private val loopbackProxyServer: LoopbackProxyServer,
    private val onNewSessionNeeded: () -> Unit,
    private val onWipeCompleted: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun execute(reason: WipeReason, terminateProcess: Boolean = false, wipeClipboard: Boolean = true) {
        AmnosLog.d("SuperWipeEngine", "SUPER WIPE TRIGGERED | Reason: $reason | Terminate: $terminateProcess")

        // Phase 0: Cryptographic Kill Switch
        AmnosLog.d("SuperWipeEngine", "Phase 0: Cryptographic Kill Switch")
        KeyManager.obliterateKey()

        // Phase 1: WebView Teardown
        AmnosLog.d("SuperWipeEngine", "Phase 1: WebView Teardown")
        tabs.forEach { tab ->
            try {
                tab.webView.surgicalTeardown()
            } catch (e: Exception) {
                AmnosLog.e("SuperWipeEngine", "Error during surgical teardown", e)
            }
        }
        tabs.clear()

        // Phase 2 & 5: Storage Sanitization & Service Worker Purge
        AmnosLog.d("SuperWipeEngine", "Phase 2 & 5: Storage & SW Sanitization")
        if (wipeClipboard) {
            storageService.wipeClipboard()
        }
        storageService.clearVolatileDownloads()
        storageService.superPurge(
            onWebViewsDestroyed = {}, // We already destroyed them synchronously above
            logCallback = securityController::logInternal
        )

        // Phase 3: Memory Invalidation
        AmnosLog.d("SuperWipeEngine", "Phase 3: Memory Invalidation")
        securityController.obliterate()
        System.gc()
        System.runFinalization()

        // Phase 4: Network Rotation
        AmnosLog.d("SuperWipeEngine", "Phase 4: Network Rotation")
        loopbackProxyServer.stop()
        DnsManager.destroyAndRebuild()

        // Note: Phase 6 (UI Zeroing) is handled by the UI layer listening to onWipeCompleted

        // Phase 7: Heap Hardening (Best Effort)
        AmnosLog.d("SuperWipeEngine", "Phase 7: Heap Hardening")
        hardenHeap()

        onNewSessionNeeded()
        onWipeCompleted()

        // Phase 8: Process Kill
        if (terminateProcess) {
            AmnosLog.w("SuperWipeEngine", "Phase 8: Delayed Process Termination initiated (200ms drain window)")
            mainHandler.postDelayed({
                AmnosLog.w("SuperWipeEngine", "EXECUTING HARD PROCESS KILL")
                Process.killProcess(Process.myPid())
            }, 200)
        }
    }

    private fun hardenHeap() {
        try {
            // Allocate an 8MB array to scrub reusable heap memory segments
            val buffer = ByteArray(8 * 1024 * 1024)
            for (i in buffer.indices step 4096) {
                buffer[i] = 0 // Touch pages
            }
        } catch (e: OutOfMemoryError) {
            AmnosLog.w("SuperWipeEngine", "OOM during heap hardening, skipped")
        }
    }
}

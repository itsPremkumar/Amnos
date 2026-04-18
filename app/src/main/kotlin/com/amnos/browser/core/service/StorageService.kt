package com.amnos.browser.core.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.amnos.browser.core.session.AmnosLog

class StorageService(
    private val context: Context,
    private val webViewDataSuffix: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // MODULAR AGENTS
    val vault = SecureVault(context, webViewDataSuffix)
    private val webPurge = WebStoragePurgeAgent(context)
    private val diskNuke = ForensicFileSystemNuke()

    fun clearVolatileDownloads() { /* No-op in Pure RAM */ }

    fun wipeClipboard() {
        com.amnos.browser.core.security.ClipboardSentinel.wipe(context)
    }

    fun superPurge(onWebViewsDestroyed: () -> Unit, logCallback: ((String, String) -> Unit)? = null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { superPurge(onWebViewsDestroyed, logCallback) }
            return
        }

        AmnosLog.w("StorageService", "INITIATING CLUSTERED PURGE SEQUENCE...")
        
        webPurge.purge {
            logCallback?.invoke("[Storage:Web]", "WebStorage and Cookies purged")
            onWebViewsDestroyed()
            
            // Final Physical Nuke
            diskNuke.execute(context.dataDir, webViewDataSuffix) { tag, msg ->
                logCallback?.invoke(tag, msg)
            }
        }
    }

    fun purgeGlobalStorage(logCallback: ((String, String) -> Unit)? = null) {
        superPurge({}, logCallback)
    }
}

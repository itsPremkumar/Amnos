package com.amnos.browser.core.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.amnos.browser.core.session.AmnosLog
import java.io.File

class StorageService(
    private val context: Context,
    private val webViewDataSuffix: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Returns a diagnostic-only identifier. Disk-backed downloads are disabled in Pure RAM mode.
     */
    fun getVolatileDownloadPath(): String = "memory://downloads-disabled"

    fun downloadEphemeralFile(@Suppress("UNUSED_PARAMETER") url: String, @Suppress("UNUSED_PARAMETER") userAgent: String) {
        AmnosLog.w(
            "StorageService",
            "Download blocked in Pure RAM mode. Persistent file output is disabled for this session."
        )
    }

    /**
     * No-op because Amnos no longer writes downloads to disk.
     */
    fun clearVolatileDownloads() {
        AmnosLog.v("StorageService", "Volatile download wipe skipped: disk-backed downloads are disabled.")
    }

    /**
     * Wipes the system clipboard for privacy using the modular Sentinel.
     */
    fun wipeClipboard() {
        com.amnos.browser.core.security.ClipboardSentinel.wipe(context)
    }

    /**
     * Purges all persistent WebView data, cookies, and storage.
     */
    fun superPurge(onWebViewsDestroyed: () -> Unit, logCallback: ((String, String) -> Unit)? = null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { superPurge(onWebViewsDestroyed, logCallback) }
            return
        }

        AmnosLog.d("StorageService", "INITIATING GLOBAL FORENSIC PURGE...")

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies { removed ->
            if (removed) {
                AmnosLog.i("StorageService", "WIPE: Cookies purged successfully")
                logCallback?.invoke("[Storage:Cookies]", "Cookies purged")
            }
            cookieManager.flush()
            
            // Continue purge after cookies...
            continuePurge(onWebViewsDestroyed, logCallback)
        }
    }

    private fun continuePurge(onWebViewsDestroyed: () -> Unit, logCallback: ((String, String) -> Unit)?) {
        logCallback?.invoke("[Storage:Web]", "WebStorage data purging initiated")
        AmnosLog.d("StorageService", "WIPE: Clearing WebStorage data")
        WebStorage.getInstance().deleteAllData()

        logCallback?.invoke("[Storage:DB]", "WebViewDatabase auth/forms purging initiated")
        AmnosLog.d("StorageService", "WIPE: Clearing WebViewDatabase auth and forms")
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()

        try {
            AmnosLog.d("StorageService", "WIPE: Clearing ClientCertPreferences")
            android.webkit.WebView.clearClientCertPreferences(null)
        } catch (ignored: Throwable) {
            // Logically fine if unavailable
        }

        // Callback indicates webViews are fully destroyed synchronously in Engine
        onWebViewsDestroyed()

        // PHYSICAL NUKE: Ensure all disk traces are hard-deleted
        nukePhysicalWebViewData(logCallback)
    }

    /**
     * Legacy global storage purge
     */
    fun purgeGlobalStorage(logCallback: ((String, String) -> Unit)? = null) {
        superPurge({}, logCallback)
    }

    private fun nukePhysicalWebViewData(logCallback: ((String, String) -> Unit)? = null) {
        try {
            val dataDir = context.dataDir
            val webViewDirPrefix = "app_webview"
            
            val targetDirs = dataDir.listFiles()
                ?.filter { file ->
                    file.isDirectory && (
                        file.name == webViewDirPrefix ||
                        file.name == "${webViewDirPrefix}_$webViewDataSuffix" ||
                        file.name.startsWith("${webViewDirPrefix}_amnos_") ||
                        file.name == "cache" ||
                        file.name == "code_cache"
                    )
                }
                .orEmpty()

            if (targetDirs.isEmpty()) {
                AmnosLog.v("StorageService", "PHYSICAL NUKE: No isolated WebView directories found.")
                return
            }

            targetDirs.forEach { targetDir ->
                val path = targetDir.absolutePath
                AmnosLog.i("StorageService", "PHYSICAL NUKE: Deleting session directory: $path")
                deleteRecursive(targetDir)
                logCallback?.invoke("[Storage:Physical]", "Session directory nuked: $path")
            }
        } catch (e: Exception) {
            AmnosLog.e("StorageService", "Physical nuke failed", e)
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}

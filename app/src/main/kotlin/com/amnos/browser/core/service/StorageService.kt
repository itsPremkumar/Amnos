package com.amnos.browser.core.service

import android.content.Context
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import com.amnos.browser.core.network.DnsManager
import com.amnos.browser.core.session.AmnosLog
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class StorageService(private val context: Context) {
    private val downloadClient by lazy { DnsManager.secureClient(blockIpv6 = true) }

    private val volatileDownloadDir: File by lazy {
        File(context.cacheDir, "volatile_downloads").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Returns the path for ephemeral downloads.
     */
    fun getVolatileDownloadPath(): String {
        return volatileDownloadDir.absolutePath
    }

    fun downloadEphemeralFile(url: String, userAgent: String) {
        Thread {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .header("DNT", "1")
                    .header("Sec-GPC", "1")
                    .build()

                downloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AmnosLog.w("StorageService", "Ephemeral download failed: HTTP ${response.code}")
                        return@use
                    }

                    val guessedName = URLUtil.guessFileName(
                        url,
                        response.header("Content-Disposition"),
                        response.body?.contentType()?.toString()
                    )
                    val safeName = guessedName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val outFile = File(volatileDownloadDir, "${UUID.randomUUID()}_$safeName")

                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    AmnosLog.i("StorageService", "Ephemeral download saved to ${outFile.absolutePath}")
                }
            } catch (error: Exception) {
                AmnosLog.e("StorageService", "Failed to store ephemeral download", error)
            }
        }.start()
    }

    /**
     * Wipes the ephemeral download directory.
     */
    fun clearVolatileDownloads() {
        Thread {
            try {
                AmnosLog.d("StorageService", "Wiping ephemeral downloads in background...")
                deleteRecursive(volatileDownloadDir)
                volatileDownloadDir.mkdirs() // Recreate for next session
            } catch (e: Exception) {
                AmnosLog.e("StorageService", "Background wipe failed", e)
            }
        }.start()
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
    fun purgeGlobalStorage(logCallback: ((String, String) -> Unit)? = null) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(false)
        cookieManager.removeAllCookies { removed ->
            if (removed) {
                logCallback?.invoke("[Storage:Cookies]", "Cookies purged")
            }
        }
        cookieManager.flush()

        WebStorage.getInstance().deleteAllData()
        val webViewDB = WebViewDatabase.getInstance(context)
        webViewDB.clearHttpAuthUsernamePassword()
        @Suppress("DEPRECATION")
        webViewDB.clearFormData()
        try {
            android.webkit.WebView.clearClientCertPreferences(null)
        } catch (ignored: Throwable) {
            // Logically fine if unavailable
        }

        // PHYSICAL NUKE: Ensure all disk traces are hard-deleted
        nukePhysicalWebViewData(logCallback)
    }

    /**
     * Physically deletes the WebView session directory from the file system.
     */
    private fun nukePhysicalWebViewData(logCallback: ((String, String) -> Unit)? = null) {
        try {
            val dataDir = context.dataDir
            val webViewDirPrefix = "app_webview_"
            val suffix = "amnos_session"
            val targetDir = File(dataDir, webViewDirPrefix + suffix)
            
            if (targetDir.exists()) {
                AmnosLog.d("StorageService", "PHYSICAL NUKE: Deleting session directory: ${targetDir.absolutePath}")
                deleteRecursive(targetDir)
                logCallback?.invoke("[Storage:Physical]", "Session directory nuked")
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

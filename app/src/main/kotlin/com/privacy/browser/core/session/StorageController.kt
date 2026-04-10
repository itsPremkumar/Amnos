package com.privacy.browser.core.session

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import java.io.File

class StorageController(private val context: Context) {

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

    /**
     * Wipes the ephemeral download directory.
     */
    fun clearVolatileDownloads() {
        Log.d("StorageController", "Wiping ephemeral downloads...")
        deleteRecursive(volatileDownloadDir)
        volatileDownloadDir.mkdirs() // Recreate for next session
    }

    /**
     * Wipes the system clipboard for privacy using the modular Sentinel.
     */
    fun wipeClipboard() {
        com.privacy.browser.core.security.ClipboardSentinel.wipe(context)
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}

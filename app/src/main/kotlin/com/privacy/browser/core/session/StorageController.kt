package com.privacy.browser.core.session

import android.content.Context
import android.webkit.URLUtil
import com.privacy.browser.core.network.DnsManager
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class StorageController(private val context: Context) {
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
                        AmnosLog.w("StorageController", "Ephemeral download failed: HTTP ${response.code}")
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

                    AmnosLog.i("StorageController", "Ephemeral download saved to ${outFile.absolutePath}")
                }
            } catch (error: Exception) {
                AmnosLog.e("StorageController", "Failed to store ephemeral download", error)
            }
        }.start()
    }

    /**
     * Wipes the ephemeral download directory.
     */
    fun clearVolatileDownloads() {
        AmnosLog.d("StorageController", "Wiping ephemeral downloads...")
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

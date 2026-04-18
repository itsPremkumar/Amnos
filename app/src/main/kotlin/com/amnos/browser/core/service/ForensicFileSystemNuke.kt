package com.amnos.browser.core.service

import com.amnos.browser.core.session.AmnosLog
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom

class ForensicFileSystemNuke {
    
    private val random = SecureRandom()

    fun execute(dataDir: File, suffix: String, onLogged: (String, String) -> Unit) {
        try {
            val webViewDirPrefix = "app_webview"
            val targetDirs = dataDir.listFiles()?.filter { file ->
                file.isDirectory && (
                    file.name == webViewDirPrefix ||
                    file.name == "${webViewDirPrefix}_$suffix" ||
                    file.name.startsWith("${webViewDirPrefix}_amnos_") ||
                    file.name == "cache" ||
                    file.name == "code_cache"
                )
            }.orEmpty()

            targetDirs.forEach { dir ->
                AmnosLog.w("FileSystemNuke", "CRITICAL: Securely scrubbing session directory -> ${dir.absolutePath}")
                secureDeleteRecursive(dir)
                onLogged("[Storage:Nuke]", "Directory securely obliterated: ${dir.name}")
            }
        } catch (e: Exception) {
            AmnosLog.e("FileSystemNuke", "Forensic nuke failed", e)
        }
    }

    private fun secureDeleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { secureDeleteRecursive(it) }
        } else {
            secureOverwrite(file)
        }
        file.delete()
    }

    private fun secureOverwrite(file: File) {
        try {
            if (!file.exists() || file.isDirectory) return
            
            val length = file.length()
            if (length <= 0) return

            RandomAccessFile(file, "rws").use { raf ->
                val buffer = ByteArray(8192)
                var totalWritten = 0L
                while (totalWritten < length) {
                    random.nextBytes(buffer)
                    val toWrite = minOf(buffer.size.toLong(), length - totalWritten).toInt()
                    raf.write(buffer, 0, toWrite)
                    totalWritten += toWrite
                }
                // Force sync and clear
                raf.setLength(0)
            }
        } catch (e: Exception) {
            // If overwrite fails, we still want to try to delete the file
            AmnosLog.w("FileSystemNuke", "Overwrite failed for ${file.name}, proceeding with unlink.")
        }
    }
}

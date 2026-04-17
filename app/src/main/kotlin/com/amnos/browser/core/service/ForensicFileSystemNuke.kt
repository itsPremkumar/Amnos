package com.amnos.browser.core.service

import com.amnos.browser.core.session.AmnosLog
import java.io.File

class ForensicFileSystemNuke {
    
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
                AmnosLog.w("FileSystemNuke", "CRITICAL: Nuking session directory -> ${dir.absolutePath}")
                deleteRecursive(dir)
                onLogged("[Storage:Nuke]", "Directory obliterated: ${dir.name}")
            }
        } catch (e: Exception) {
            AmnosLog.e("FileSystemNuke", "Forensic nuke failed", e)
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}

package com.privacy.browser.core.adblock

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class AdBlocker(context: Context) {
    private val blockedDomains = mutableSetOf<String>()

    init {
        loadFilterList(context)
    }

    private fun loadFilterList(context: Context) {
        try {
            val inputStream = context.assets.open("blocklist.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("!")) {
                    blockedDomains.add(trimmed)
                }
            }
            reader.close()
            Log.d("AdBlocker", "Loaded ${blockedDomains.size} blocked domains")
        } catch (e: Exception) {
            Log.e("AdBlocker", "Error loading blocklist", e)
        }
    }

    fun shouldBlock(url: String): Boolean {
        val host = try {
            URL(url).host.removePrefix("www.")
        } catch (e: Exception) {
            return false
        }

        var currentHost = host
        while (currentHost.contains(".")) {
            if (blockedDomains.contains(currentHost)) {
                Log.d("AdBlocker", "Blocked: $url (matched $currentHost)")
                return true
            }
            currentHost = currentHost.substringAfter(".", "")
            if (currentHost.isEmpty()) break
        }
        
        return false
    }
}

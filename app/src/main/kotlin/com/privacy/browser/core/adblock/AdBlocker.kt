package com.privacy.browser.core.adblock

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
                normalizeRule(line)?.let { blockedDomains.add(it) }
            }
            reader.close()
            Log.d("AdBlocker", "Loaded ${blockedDomains.size} blocked domains")
        } catch (e: Exception) {
            Log.e("AdBlocker", "Error loading blocklist", e)
        }
    }

    private fun normalizeRule(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("!") || trimmed.startsWith("[")) return null

        val withoutOptions = trimmed.substringBefore('$')
        val domain = when {
            withoutOptions.startsWith("||") -> withoutOptions.removePrefix("||").substringBefore('^')
            withoutOptions.startsWith("|http") -> withoutOptions.removePrefix("|").toHttpUrlOrNull()?.host
            withoutOptions.contains("://") -> withoutOptions.toHttpUrlOrNull()?.host
            else -> withoutOptions.substringBefore('^').substringBefore('/')
        }

        return domain
            ?.removePrefix("www.")
            ?.removePrefix(".")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
    }

    fun shouldBlock(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host?.removePrefix("www.")?.lowercase() ?: return false

        var currentHost = host
        while (currentHost.isNotEmpty()) {
            if (blockedDomains.contains(currentHost)) {
                Log.d("AdBlocker", "Blocked: $url (matched $currentHost)")
                return true
            }
            currentHost = currentHost.substringAfter('.', "")
            if (currentHost == host) {
                break
            }
            if (!currentHost.contains('.')) {
                if (blockedDomains.contains(currentHost)) {
                    return true
                }
            }
        }
        return false
    }
}

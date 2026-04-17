package com.amnos.browser.core.adblock

import android.content.Context
import com.amnos.browser.core.session.AmnosLog
import okio.buffer
import okio.source
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStreamReader
import java.util.regex.Pattern

class FilterRegistry(private val context: Context) {
    val knownAdPatterns = listOf(
        ".*\\/ads?\\/.*", ".*\\/advert.*", ".*\\/banner.*", ".*\\/tracking.*",
        ".*\\/analytics.*", ".*\\/telemetry.*", ".*\\/pixel.*", ".*\\/beacon.*",
        ".*ad[sx]?[0-9]*\\.", ".*tracker[0-9]*\\.", ".*analytics[0-9]*\\.", ".*pixel[0-9]*\\."
    )

    val adKeywords = listOf(
        "/ad/", "/ads/", "/adv/", "/banner/", "/banners/", "/track/", "/tracking/", "/tracker/",
        "/analytics/", "/analytic/", "/telemetry/", "/pixel/", "/pixels/", "/beacon/", "/beacons/",
        "/impression/", "/impressions/", "/click/", "/clicks/", "/conversion/", "/conversions/",
        "pagead", "doubleclick", "googleads", "googlesyndication", "facebook.net/tr", "facebook.com/tr",
        "?utm_", "&utm_", "?fbclid=", "&fbclid=", "?gclid=", "&gclid="
    )

    @Volatile
    var blockedDomains: Set<String> = emptySet()
        private set

    val compiledPatterns: List<Pattern> by lazy {
        knownAdPatterns.mapNotNull { pattern ->
            try {
                Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            } catch (e: Exception) {
                AmnosLog.e("FilterRegistry", "Error compiling pattern: $pattern", e)
                null
            }
        }
    }

    init {
        loadAllLists()
    }

    fun loadAllLists() {
        try {
            val domains = linkedSetOf<String>()
            loadList("blocklist.txt", domains)
            loadList("blocklist_comprehensive.txt", domains)
            blockedDomains = domains
            AmnosLog.d("FilterRegistry", "Total identifiable blocked domains: ${blockedDomains.size}")
        } catch (e: Exception) {
            AmnosLog.e("FilterRegistry", "Failed to load blocklists", e)
        }
    }

    private fun loadList(filename: String, target: MutableSet<String>) {
        try {
            context.assets.open(filename).use { inputStream ->
                InputStreamReader(inputStream).buffered().use { reader ->
                    reader.forEachLine { line ->
                        normalizeRule(line)?.let { target.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            AmnosLog.v("FilterRegistry", "Optional list $filename not found or unreadable.")
        }
    }

    private fun normalizeRule(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[") || trimmed.startsWith("#")) return null

        val withoutOptions = trimmed.substringBefore('$')
        val domain = when {
            withoutOptions.startsWith("||") -> withoutOptions.removePrefix("||").substringBefore('^')
            withoutOptions.startsWith("|http") -> withoutOptions.removePrefix("|").toHttpUrlOrNull()?.host
            withoutOptions.contains("://") -> withoutOptions.toHttpUrlOrNull()?.host
            else -> withoutOptions.substringBefore('^').substringBefore('/')
        }

        return domain?.removePrefix("www.")?.removePrefix(".")?.lowercase()?.takeIf { it.isNotBlank() }
    }
}

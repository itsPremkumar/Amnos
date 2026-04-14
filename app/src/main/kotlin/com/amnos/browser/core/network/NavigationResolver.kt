package com.amnos.browser.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class NavigationTargetType {
    URL,
    SEARCH
}

data class ResolvedNavigation(
    val input: String,
    val targetType: NavigationTargetType,
    val transformedUrl: String,
    val sanitizedUrl: String,
    val displayText: String
)

object NavigationResolver {
    private const val SEARCH_ENGINE_HOST = "duckduckgo.com"

    fun resolve(input: String): ResolvedNavigation? {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) return null

        val isUrlCandidate = (trimmedInput.startsWith("http://", ignoreCase = true) ||
            trimmedInput.startsWith("https://", ignoreCase = true) ||
            (trimmedInput.contains(".") && !trimmedInput.contains(" ") && trimmedInput.length > 3)) &&
            !trimmedInput.startsWith("javascript:", ignoreCase = true) &&
            !trimmedInput.startsWith("file:", ignoreCase = true) &&
            !trimmedInput.startsWith("data:", ignoreCase = true) &&
            !trimmedInput.startsWith("content:", ignoreCase = true)

        val transformedUrl: String
        val targetType: NavigationTargetType

        if (isUrlCandidate) {
            val normalizedUrl = when {
                trimmedInput.startsWith("http://", ignoreCase = true) -> trimmedInput
                trimmedInput.startsWith("https://", ignoreCase = true) -> trimmedInput
                else -> "https://$trimmedInput"
            }

            if (hasFortifiedHost(normalizedUrl)) {
                transformedUrl = normalizedUrl
                targetType = NavigationTargetType.URL
            } else {
                transformedUrl = buildSearchUrl(trimmedInput)
                targetType = NavigationTargetType.SEARCH
            }
        } else {
            transformedUrl = buildSearchUrl(trimmedInput)
            targetType = NavigationTargetType.SEARCH
        }

        return ResolvedNavigation(
            input = trimmedInput,
            targetType = targetType,
            transformedUrl = transformedUrl,
            sanitizedUrl = UrlSanitizer.sanitize(transformedUrl),
            displayText = trimmedInput
        )
    }

    private fun buildSearchUrl(query: String): String {
        return okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host(SEARCH_ENGINE_HOST)
            .addPathSegment("")
            .addQueryParameter("q", query)
            .addQueryParameter("ia", "web")
            .build()
            .toString()
    }

    private fun hasFortifiedHost(url: String): Boolean {
        val parsedUrl = url.toHttpUrlOrNull() ?: return false
        val host = parsedUrl.host.lowercase()

        if (host == "localhost" || isIpv4Address(host) || host.contains(":")) {
            return true
        }

        val labels = host.split(".")
        if (labels.size < 2) {
            return false
        }
        if (labels.any { it.isBlank() || it.startsWith('-') || it.endsWith('-') }) {
            return false
        }

        val tld = labels.last()
        return tld.matches(Regex("[a-z]{2,63}")) || tld.matches(Regex("xn--[a-z0-9-]{2,59}"))
    }

    private fun isIpv4Address(host: String): Boolean {
        val octets = host.split(".")
        return octets.size == 4 && octets.all { octet ->
            val value = octet.toIntOrNull() ?: return@all false
            value in 0..255
        }
    }
}

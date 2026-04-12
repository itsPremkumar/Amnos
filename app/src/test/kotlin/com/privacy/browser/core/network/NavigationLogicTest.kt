package com.privacy.browser.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

class NavigationLogicTest {

    private fun simulateNavigate(input: String): String {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) return ""

        // This matches the logic I added to BrowserViewModel.kt
        val isUrl = (trimmedInput.startsWith("http://") || trimmedInput.startsWith("https://")) ||
                    (trimmedInput.contains(".") && !trimmedInput.contains(" ") && trimmedInput.length > 3)

        val destinationUrl = if (isUrl) {
            val withScheme = if (trimmedInput.startsWith("http")) trimmedInput else "https://$trimmedInput"
            // This is the new fortification logic I added
            if (!withScheme.contains("/") && !withScheme.contains(".") && !withScheme.contains(":") && !withScheme.contains("localhost")) {
                "https://duckduckgo.com/?q=${URLEncoder.encode(trimmedInput, "UTF-8")}&ia=web"
            } else {
                withScheme
            }
        } else {
            "https://duckduckgo.com/?q=${URLEncoder.encode(trimmedInput, "UTF-8")}&ia=web"
        }

        return UrlSanitizer.sanitize(destinationUrl)
    }

    @Test
    fun verifyDuckDuckSearch() {
        val result = simulateNavigate("duckduck")
        // Should be a search URL, not https://duckduck
        assertEquals("https://duckduckgo.com/?q=duckduck&ia=web", result)
    }

    @Test
    fun verifyGoogleUrl() {
        val result = simulateNavigate("google.com")
        // Should be a direct URL
        assertEquals("https://google.com", result)
    }

    @Test
    fun verifyHttpUrl() {
        val result = simulateNavigate("http://example.com")
        // Should be kept as is (UrlSanitizer might have views on http vs https but here we check routing)
        assertTrue(result.contains("example.com"))
    }

    @Test
    fun verifyComplexSearch() {
        val result = simulateNavigate("what is the weather")
        assertEquals("https://duckduckgo.com/?q=what+is+the+weather&ia=web", result)
    }

    @Test
    fun verifyTruncationIssue() {
        // This tests if UrlSanitizer accidentally truncates the search URL
        val rawSearch = "https://duckduckgo.com/?q=duckduck&ia=web"
        val output = UrlSanitizer.sanitize(rawSearch)
        assertEquals(rawSearch, output)
    }
}

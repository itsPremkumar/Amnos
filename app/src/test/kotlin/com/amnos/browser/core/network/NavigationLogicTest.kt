package com.amnos.browser.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLogicTest {

    @Test
    fun verifyDuckDuckSearch() {
        val result = NavigationResolver.resolve("duckduck")

        assertEquals(NavigationTargetType.SEARCH, result?.targetType)
        assertEquals("https://duckduckgo.com/?q=duckduck&ia=web", result?.sanitizedUrl)
    }

    @Test
    fun verifyGoogleUrl() {
        val result = NavigationResolver.resolve("google.com")

        assertEquals(NavigationTargetType.URL, result?.targetType)
        assertEquals("https://google.com", result?.sanitizedUrl)
    }

    @Test
    fun verifyHttpUrl() {
        val result = NavigationResolver.resolve("http://example.com")

        assertEquals(NavigationTargetType.URL, result?.targetType)
        assertTrue(result?.sanitizedUrl?.contains("example.com") == true)
    }

    @Test
    fun verifyComplexSearch() {
        val result = NavigationResolver.resolve("what is the weather")

        assertEquals(NavigationTargetType.SEARCH, result?.targetType)
        assertEquals("https://duckduckgo.com/?q=what%20is%20the%20weather&ia=web", result?.sanitizedUrl)
    }

    @Test
    fun explicitSchemeWithoutValidTldFallsBackToSearch() {
        val result = NavigationResolver.resolve("https://duckduck")

        assertEquals(NavigationTargetType.SEARCH, result?.targetType)
        assertEquals("https://duckduckgo.com/?q=https%3A%2F%2Fduckduck&ia=web", result?.sanitizedUrl)
    }

    @Test
    fun trackingParametersAreRemovedFromDirectUrls() {
        val result = NavigationResolver.resolve("example.com/?utm_source=newsletter&id=42")

        assertEquals(NavigationTargetType.URL, result?.targetType)
        assertEquals("https://example.com/?id=42", result?.sanitizedUrl)
    }
}

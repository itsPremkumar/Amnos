package com.privacy.browser.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlSanitizerTest {

    @Test
    fun removesKnownTrackingParametersAndKeepsUserParameters() {
        val sanitized = UrlSanitizer.sanitize(
            "https://example.com/page?utm_source=newsletter&utm_campaign=spring&id=42&fbclid=abc123"
        )

        assertEquals("https://example.com/page?id=42", sanitized)
    }

    @Test
    fun leavesUrlsWithoutTrackingParametersUnchanged() {
        val url = "https://example.com/search?q=privacy&lang=en"

        assertEquals(url, UrlSanitizer.sanitize(url))
    }
}

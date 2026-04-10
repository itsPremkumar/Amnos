package com.privacy.browser.core.network

import com.privacy.browser.core.security.PrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSecurityManagerTest {

    private val manager = NetworkSecurityManager { PrivacyPolicy() }

    @Test
    fun rejectsUnsupportedSchemesInNavigationInput() {
        assertEquals(null, manager.sanitizeNavigationUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertEquals(null, manager.sanitizeNavigationUrl("mailto:test@example.com"))
    }

    @Test
    fun computesStableSiteKeysAndCrossSiteBoundaries() {
        assertEquals("example.com", manager.siteKeyForUrl("https://sub.example.com/path"))
        assertTrue(manager.isCrossSiteNavigation("https://example.com", "https://another.org"))
        assertFalse(manager.isCrossSiteNavigation("https://a.example.com", "https://b.example.com"))
    }
}

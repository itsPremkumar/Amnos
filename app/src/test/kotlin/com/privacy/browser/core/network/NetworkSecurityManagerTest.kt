package com.privacy.browser.core.network

import android.content.Context
import com.privacy.browser.core.adblock.AdBlocker
import com.privacy.browser.core.security.PrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NetworkSecurityManagerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val adBlocker = AdBlocker(context)
    private val manager = NetworkSecurityManager(adBlocker) { PrivacyPolicy(removeTrackingParameters = true) }

    @Test
    fun rejectsUnsupportedSchemesInNavigationInput() {
        assertEquals(null, manager.sanitizeNavigationUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertEquals(null, manager.sanitizeNavigationUrl("mailto:test@example.com"))
    }

    @Test
    fun upgradesHttpAndStripsTrackingParametersDuringNavigationSanitization() {
        assertEquals(
            "https://example.com/path?id=7",
            manager.sanitizeNavigationUrl("http://example.com/path?utm_source=news&id=7")
        )
    }

    @Test
    fun computesStableSiteKeysAndCrossSiteBoundaries() {
        assertEquals("example.com", manager.siteKeyForUrl("https://sub.example.com/path"))
        assertTrue(manager.isCrossSiteNavigation("https://example.com", "https://another.org"))
        assertFalse(manager.isCrossSiteNavigation("https://a.example.com", "https://b.example.com"))
    }

    @Test
    fun flagsPrivateNetworksAndAllowsPublicHosts() {
        assertTrue(manager.isLocalNetworkHost("192.168.1.10"))
        assertTrue(manager.isLocalNetworkHost("localhost"))
        assertFalse(manager.isLocalNetworkHost("example.com"))
    }
}

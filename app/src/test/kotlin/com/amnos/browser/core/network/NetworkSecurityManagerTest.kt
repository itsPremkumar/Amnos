package com.amnos.browser.core.network

import android.content.Context
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.security.PrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NetworkSecurityManagerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val adBlocker = AdBlocker(context)
    private val manager = NetworkSecurityManager(adBlocker) { PrivacyPolicy(filterRemoveTrackingParams = true) }

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
    fun identifiesAndProcessesSearchQueries() {
        // Single word (like 'youtue') -> Search
        assertEquals(
            "https://duckduckgo.com/?q=youtue",
            manager.sanitizeNavigationUrl("youtue")
        )

        // Multiple words (with spaces) -> Search
        assertEquals(
            "https://duckduckgo.com/?q=how+to+cook",
            manager.sanitizeNavigationUrl("how to cook")
        )

        // Single word with trailing slash (common user behavior) -> Search
        assertEquals(
            "https://duckduckgo.com/?q=youtube%2F",
            manager.sanitizeNavigationUrl("youtube/")
        )
    }

    @Test
    fun preservesValidUrlsAndIpAddresses() {
        // Standard domain -> Direct
        assertEquals("https://google.com", manager.sanitizeNavigationUrl("google.com"))
        
        // IP Address -> Direct
        assertEquals("https://192.168.1.1", manager.sanitizeNavigationUrl("192.168.1.1"))
        
        // Localhost -> Direct
        assertEquals("https://localhost", manager.sanitizeNavigationUrl("localhost"))
        
        // Subdomain -> Direct
        assertEquals("https://news.bbc.co.uk", manager.sanitizeNavigationUrl("news.bbc.co.uk"))
    }

    @Test
    fun computesStableSiteKeysAndCrossSiteBoundaries() {
        assertEquals("example.com", manager.siteKeyForUrl("https://sub.example.com/path"))
        assertTrue(manager.isCrossSiteNavigation("https://example.com", "https://another.org"))
        assertFalse(manager.isCrossSiteNavigation("https://a.example.com", "https://b.example.com"))
    }

    @Test
    fun repro_paranoidModeBlocksLegitimateNavigation() {
        // SETUP: Paranoid mode active
        val paranoidPolicy = PrivacyPolicy(networkFirewallLevel = com.amnos.browser.core.security.FirewallLevel.PARANOID)
        val paranoidManager = NetworkSecurityManager(adBlocker) { paranoidPolicy }
        
        val wikipediaUrl = "https://en.wikipedia.org/wiki/Privacy"
        val request = createMockRequest(wikipediaUrl, isMainFrame = true)
        
        // EVALUATE
        val decision = paranoidManager.evaluateRequest(request, null)
        
        // ASSERT: It is BLOCKED (reproducing the bug where unknown sites fail)
        assertTrue("Paranoid mode MUST block unknown domains", decision.isBlocked)
        assertEquals(BlockReason.SECURITY_THREAT, decision.blockReason)
    }

    @Test
    fun verify_balancedModeAllowsLegitimateNavigation() {
        // SETUP: Balanced mode (proposed solution)
        val balancedPolicy = PrivacyPolicy(networkFirewallLevel = com.amnos.browser.core.security.FirewallLevel.BALANCED)
        val balancedManager = NetworkSecurityManager(adBlocker) { balancedPolicy }
        
        val wikipediaUrl = "https://en.wikipedia.org/wiki/Privacy"
        val request = createMockRequest(wikipediaUrl, isMainFrame = true)
        
        // EVALUATE
        val decision = balancedManager.evaluateRequest(request, null)
        
        // ASSERT: It is ALLOWED
        assertFalse("Balanced mode SHOULD allow legitimate navigation", decision.isBlocked)
        assertNull(decision.blockReason)
    }

    private fun createMockRequest(url: String, isMainFrame: Boolean): android.webkit.WebResourceRequest {
        return object : android.webkit.WebResourceRequest {
            override fun getUrl(): android.net.Uri = android.net.Uri.parse(url)
            override fun isForMainFrame(): Boolean = isMainFrame
            override fun isRedirect(): Boolean = false
            override fun hasGesture(): Boolean = false
            override fun getMethod(): String = "GET"
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
    }
}

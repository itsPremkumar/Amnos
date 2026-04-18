package com.amnos.browser.core.network

import android.content.Context
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.security.PrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PrivacyPolicyLogicTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val adBlocker = AdBlocker(context)

    @Test
    fun blockUnsafeMethods_blocksPostRequests() {
        val policy = PrivacyPolicy(filterBlockUnsafeMethods = true)
        val manager = NetworkSecurityManager(adBlocker) { policy }
        
        val url = "https://example.com"
        val getRequest = createMockRequest(url, "GET")
        val postRequest = createMockRequest(url, "POST")
        
        assertFalse("GET should be allowed", manager.evaluateRequest(getRequest, null).isBlocked)
        assertTrue("POST should be blocked", manager.evaluateRequest(postRequest, null).isBlocked)
        assertEquals(BlockReason.UNSAFE_METHOD, manager.evaluateRequest(postRequest, null).blockReason)
    }

    @Test
    fun blockLocalNetwork_blocksLocalhost() {
        val policyWithBlock = PrivacyPolicy(networkBlockLocalNetwork = true)
        val managerWithBlock = NetworkSecurityManager(adBlocker) { policyWithBlock }
        
        val policyWithoutBlock = PrivacyPolicy(networkBlockLocalNetwork = false)
        val managerWithoutBlock = NetworkSecurityManager(adBlocker) { policyWithoutBlock }
        
        val localUrl = "https://localhost"
        val request = createMockRequest(localUrl, "GET")
        
        assertTrue("Localhost should be blocked when policy is active", managerWithBlock.evaluateRequest(request, null).isBlocked)
        assertFalse("Localhost should be allowed when policy is disabled", managerWithoutBlock.evaluateRequest(request, null).isBlocked)
    }

    private fun createMockRequest(url: String, method: String): android.webkit.WebResourceRequest {
        return object : android.webkit.WebResourceRequest {
            override fun getUrl(): android.net.Uri = android.net.Uri.parse(url)
            override fun isForMainFrame(): Boolean = true
            override fun isRedirect(): Boolean = false
            override fun hasGesture(): Boolean = false
            override fun getMethod(): String = method
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
    }
}

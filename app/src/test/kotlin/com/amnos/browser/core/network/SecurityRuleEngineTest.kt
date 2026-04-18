package com.amnos.browser.core.network

import android.net.Uri
import android.webkit.WebResourceRequest
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.security.FirewallLevel
import com.amnos.browser.core.security.PrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SecurityRuleEngineTest {

    private val adBlocker = AdBlocker(RuntimeEnvironment.getApplication())
    private val engine = SecurityRuleEngine(adBlocker)

    private val paranoidPolicy = PrivacyPolicy(
        networkFirewallLevel = FirewallLevel.PARANOID,
        filterBlockThirdPartyRequests = true,
        filterBlockTrackers = true
    )

    @Test
    fun allowsYouTubeVideoPlaybackUnderStrictPolicy() {
        val youtubeUrl = "https://rr4---sn-gwpa-pmhl.googlevideo.com/videoplayback?expire=1"
        val request = createMockRequest(youtubeUrl, isMainFrame = false)
        
        val decision = engine.evaluateRequest(request, "www.youtube.com", paranoidPolicy)
        
        // Assert it is ALLOWED (null BlockReason means allowed)
        assertNull("YouTube media should not be blocked even in PARANOID mode", decision.blockReason)
    }

    @Test
    fun allowsYouTubeApiUnderStrictPolicy() {
        val apiUrl = "https://youtubei.googleapis.com/youtubei/v1/player?key=abc"
        val request = createMockRequest(apiUrl, isMainFrame = false)
        
        val decision = engine.evaluateRequest(request, "www.youtube.com", paranoidPolicy)
        
        assertNull("YouTube API should not be blocked", decision.blockReason)
    }

    @Test
    fun blocksUnrelatedThirdPartyUnderStrictPolicy() {
        val trackerUrl = "https://tracking.example.com/pixel.gif"
        val request = createMockRequest(trackerUrl, isMainFrame = false)
        
        val decision = engine.evaluateRequest(request, "www.youtube.com", paranoidPolicy)
        
        assertEquals(BlockReason.THIRD_PARTY, decision.blockReason)
    }

    private fun createMockRequest(url: String, isMainFrame: Boolean): WebResourceRequest {
        return object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse(url)
            override fun isForMainFrame(): Boolean = isMainFrame
            override fun isRedirect(): Boolean = false
            override fun hasGesture(): Boolean = false
            override fun getMethod(): String = "GET"
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
    }
}

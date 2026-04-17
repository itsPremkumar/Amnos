package com.amnos.browser.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPolicyManagerTest {

    @Test
    fun allowsKnownMediaCompatibilityDomains() {
        assertTrue(DomainPolicyManager.isAllowed("https://www.youtube.com/watch?v=abc"))
        assertTrue(DomainPolicyManager.isAllowed("https://ytimg.com/vi/abc/default.jpg"))
        assertTrue(DomainPolicyManager.isAllowed("https://youtubei.googleapis.com/v1/player"))
        assertTrue(DomainPolicyManager.isAllowed("https://jnn-pa.googleapis.com/log"))
        assertTrue(DomainPolicyManager.isAllowed("https://player.vimeo.com/video/123"))
        assertTrue(DomainPolicyManager.isAllowed("https://vod-progressive.akamaized.net/exp=1"))
    }

    @Test
    fun rejectsUnknownDomains() {
        assertFalse(DomainPolicyManager.isAllowed("https://example.invalid"))
    }
}

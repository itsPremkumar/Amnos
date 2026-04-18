package com.amnos.browser.core.fingerprint

import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.PrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintManagerTest {

    @Test
    fun profileGenerationIsDeterministicPerSessionAndTab() {
        val policy = PrivacyPolicy(hardwareFingerprintLevel = FingerprintProtectionLevel.STRICT)
        val first = FingerprintManager.generateCoherentProfile("session-1", "tab-1", policy)
        val second = FingerprintManager.generateCoherentProfile("session-1", "tab-1", policy)

        assertEquals(first, second)
    }

    @Test
    fun differentTabsReceiveDistinctTabScopedNoise() {
        val policy = PrivacyPolicy(hardwareFingerprintLevel = FingerprintProtectionLevel.STRICT)
        val first = FingerprintManager.generateCoherentProfile("session-1", "tab-1", policy)
        val second = FingerprintManager.generateCoherentProfile("session-1", "tab-2", policy)

        assertNotEquals(first.tabId, second.tabId)
        assertNotEquals(first.noiseSeed, second.noiseSeed)
    }

    @Test
    fun generatedProfilesStayWithinAndroidMobileShape() {
        val policy = PrivacyPolicy(hardwareFingerprintLevel = FingerprintProtectionLevel.BALANCED)
        val profile = FingerprintManager.generateCoherentProfile(
            "session-android",
            "tab-android",
            policy
        )

        assertTrue(profile.userAgent.contains("Android"))
        assertTrue(profile.platform.contains("Linux"))
        assertTrue(profile.screen.width > 0)
        assertTrue(profile.screen.height > 0)
    }

    @Test
    fun strictProfilesNormalizeHardwareAndTimezone() {
        val policy = PrivacyPolicy(hardwareFingerprintLevel = FingerprintProtectionLevel.STRICT)
        val profile = FingerprintManager.generateCoherentProfile(
            "session-strict",
            "tab-strict",
            policy
        )

        assertEquals(8, profile.hardwareConcurrency)
        assertEquals(8, profile.deviceMemory)
        assertEquals("UTC", profile.timeZone)
        assertEquals(0, profile.timezoneOffsetMinutes)
    }
}

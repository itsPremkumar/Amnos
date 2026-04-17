package com.amnos.browser.core.fingerprint

import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

data class DeviceProfile(
    val sessionId: String,
    val tabId: String,
    val userAgent: String,
    val platform: String,
    val languages: List<String>,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val timeZone: String,
    val timezoneOffsetMinutes: Int,
    val screen: ScreenSpecs,
    val gpuVendor: String,
    val gpuRenderer: String,
    val noiseSeed: Int
) {
    val acceptLanguageHeader: String
        get() = languages.joinToString(separator = ",")
}

data class ScreenSpecs(
    val width: Int,
    val height: Int,
    val availWidth: Int,
    val availHeight: Int,
    val colorDepth: Int,
    val pixelDepth: Int,
    val devicePixelRatio: Double
)

object FingerprintManager {
    private val secureRandom = SecureRandom()

    fun newSessionId(): String = UUID.randomUUID().toString()

    fun newTabId(): String = UUID.randomUUID().toString()

    fun newUnlockPin(): String = secureRandom.nextInt(10_000).toString().padStart(4, '0')

    fun generateCoherentProfile(
        sessionId: String,
        tabId: String,
        policy: PrivacyPolicy
    ): DeviceProfile {
        val level = policy.fingerprintProtectionLevel
        val sessionRandom = seededRandom(sessionId)
        val tabRandom = seededRandom(sessionId, tabId)

        val template = when {
            policy.uaTemplate.isNotEmpty() && policy.uaTemplate != "RANDOM" -> {
                when (policy.uaTemplate.uppercase()) {
                    "PIXEL_8" -> FingerprintRegistry.androidTemplates[0]
                    "S23" -> FingerprintRegistry.androidTemplates[1]
                    "ONEPLUS" -> FingerprintRegistry.androidTemplates[2]
                    else -> FingerprintRegistry.androidTemplates.first()
                }
            }
            level == FingerprintProtectionLevel.BALANCED -> FingerprintRegistry.androidTemplates[abs(sessionRandom.nextInt()) % FingerprintRegistry.androidTemplates.size]
            level == FingerprintProtectionLevel.STRICT -> FingerprintRegistry.androidTemplates[abs(tabRandom.nextInt()) % 2]
            else -> FingerprintRegistry.androidTemplates.first()
        }
        val locale = when (level) {
            FingerprintProtectionLevel.BALANCED -> FingerprintRegistry.balancedLocalePresets[abs(tabRandom.nextInt()) % FingerprintRegistry.balancedLocalePresets.size]
            FingerprintProtectionLevel.STRICT -> FingerprintRegistry.strictLocalePreset
            FingerprintProtectionLevel.DISABLED -> FingerprintRegistry.balancedLocalePresets.first()
            else -> FingerprintRegistry.strictLocalePreset
        }
        val userAgent = when (level) {
            FingerprintProtectionLevel.BALANCED -> template.userAgents[abs(tabRandom.nextInt()) % template.userAgents.size]
            FingerprintProtectionLevel.STRICT, FingerprintProtectionLevel.DISABLED -> template.userAgents.first()
            else -> template.userAgents.first()
        }

        AmnosLog.i("FingerprintManager", "Identity Generated (Tab: ${tabId.take(8)}) -> UA: ${userAgent.take(30)}... | GPU: ${template.gpuRenderer}")
        AmnosLog.v("FingerprintManager", "Profile Details: ${template.screen.width}x${template.screen.height}, TZ: ${locale.timeZone}")

        return DeviceProfile(
            sessionId = sessionId,
            tabId = tabId,
            userAgent = userAgent,
            platform = template.platform,
            languages = locale.languages,
            hardwareConcurrency = if (level == FingerprintProtectionLevel.STRICT) 8 else template.hardwareConcurrency,
            deviceMemory = if (level == FingerprintProtectionLevel.STRICT) 8 else template.deviceMemory,
            timeZone = locale.timeZone,
            timezoneOffsetMinutes = locale.timezoneOffsetMinutes,
            screen = template.screen,
            gpuVendor = template.gpuVendor,
            gpuRenderer = template.gpuRenderer,
            noiseSeed = abs(tabRandom.nextInt()) + 1
        )
    }

    private fun seededRandom(vararg parts: String): Random {
        var seed = 1125899906842597L
        parts.forEach { part ->
            part.forEach { ch ->
                seed = (seed * 31L) + ch.code
            }
        }
        return Random(seed)
    }
}

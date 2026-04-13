package com.amnos.browser.core.fingerprint

import com.amnos.browser.core.security.FingerprintProtectionLevel
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

    fun newSessionId(): String = UUID.randomUUID().toString()

    fun newTabId(): String = UUID.randomUUID().toString()

    fun generateCoherentProfile(
        sessionId: String,
        tabId: String,
        level: FingerprintProtectionLevel
    ): DeviceProfile {
        val sessionRandom = seededRandom(sessionId)
        val tabRandom = seededRandom(sessionId, tabId)

        val template = when (level) {
            FingerprintProtectionLevel.BALANCED -> FingerprintRegistry.androidTemplates[abs(sessionRandom.nextInt()) % FingerprintRegistry.androidTemplates.size]
            FingerprintProtectionLevel.STRICT -> FingerprintRegistry.androidTemplates[abs(tabRandom.nextInt()) % 2]
            FingerprintProtectionLevel.DISABLED -> FingerprintRegistry.androidTemplates.first()
        }
        val locale = when (level) {
            FingerprintProtectionLevel.BALANCED -> FingerprintRegistry.balancedLocalePresets[abs(tabRandom.nextInt()) % FingerprintRegistry.balancedLocalePresets.size]
            FingerprintProtectionLevel.STRICT -> FingerprintRegistry.strictLocalePreset
            FingerprintProtectionLevel.DISABLED -> FingerprintRegistry.balancedLocalePresets.first()
        }
        val userAgent = when (level) {
            FingerprintProtectionLevel.BALANCED -> template.userAgents[abs(tabRandom.nextInt()) % template.userAgents.size]
            FingerprintProtectionLevel.STRICT, FingerprintProtectionLevel.DISABLED -> template.userAgents.first()
        }

        return DeviceProfile(
            sessionId = sessionId,
            tabId = tabId,
            userAgent = userAgent,
            platform = template.platform,
            languages = locale.languages,
            hardwareConcurrency = 8,
            deviceMemory = 8,
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

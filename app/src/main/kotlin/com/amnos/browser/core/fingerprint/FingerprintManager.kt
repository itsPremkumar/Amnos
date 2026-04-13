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

private data class LocalePreset(
    val languages: List<String>,
    val timeZone: String,
    val timezoneOffsetMinutes: Int
)

private data class DeviceTemplate(
    val userAgents: List<String>,
    val platform: String,
    val screen: ScreenSpecs,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val gpuVendor: String,
    val gpuRenderer: String
)

object FingerprintManager {
    private val balancedLocalePresets = listOf(
        LocalePreset(listOf("en-US", "en"), "America/New_York", 300),
        LocalePreset(listOf("en-GB", "en"), "Europe/London", 0),
        LocalePreset(listOf("de-DE", "de"), "Europe/Berlin", -60),
        LocalePreset(listOf("fr-FR", "fr"), "Europe/Paris", -60)
    )

    private val strictLocalePreset = LocalePreset(
        languages = listOf("en-US", "en"),
        timeZone = "UTC",
        timezoneOffsetMinutes = 0
    )

    private val androidTemplates = listOf(
        DeviceTemplate(
            userAgents = listOf(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.80 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"
            ),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "Google Inc. (Qualcomm)",
            gpuRenderer = "ANGLE (Qualcomm, Adreno 740, OpenGL ES 3.2)"
        ),
        DeviceTemplate(
            userAgents = listOf(
                "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.105 Mobile Safari/537.36"
            ),
            platform = "Linux armv8l",
            screen = ScreenSpecs(384, 854, 384, 810, 24, 24, 3.0),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 740"
        ),
        DeviceTemplate(
            userAgents = listOf(
                "Mozilla/5.0 (Linux; Android 13; CPH2487) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.105 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; IN2023) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"
            ),
            platform = "Linux armv8l",
            screen = ScreenSpecs(393, 873, 393, 829, 24, 24, 2.75),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "ARM",
            gpuRenderer = "Mali-G710"
        )
    )

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
            FingerprintProtectionLevel.BALANCED -> androidTemplates[abs(sessionRandom.nextInt()) % androidTemplates.size]
            FingerprintProtectionLevel.STRICT -> androidTemplates[abs(tabRandom.nextInt()) % 2]
            FingerprintProtectionLevel.DISABLED -> androidTemplates.first()
        }
        val locale = when (level) {
            FingerprintProtectionLevel.BALANCED -> balancedLocalePresets[abs(tabRandom.nextInt()) % balancedLocalePresets.size]
            FingerprintProtectionLevel.STRICT -> strictLocalePreset
            FingerprintProtectionLevel.DISABLED -> balancedLocalePresets.first()
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

package com.amnos.browser.core.fingerprint

import com.amnos.browser.core.session.AmnosLog
import kotlin.math.abs
import kotlin.random.Random

data class LocalePreset(
    val languages: List<String>,
    val timeZone: String,
    val timezoneOffsetMinutes: Int
)

data class DeviceTemplate(
    val userAgents: List<String>,
    val platform: String,
    val screen: ScreenSpecs,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val gpuVendor: String,
    val gpuRenderer: String
)

object FingerprintRegistry {
    val balancedLocalePresets = listOf(
        LocalePreset(listOf("en-US", "en"), "America/New_York", 300),
        LocalePreset(listOf("en-GB", "en"), "Europe/London", 0),
        LocalePreset(listOf("de-DE", "de"), "Europe/Berlin", -60),
        LocalePreset(listOf("fr-FR", "fr"), "Europe/Paris", -60)
    )

    val strictLocalePreset = LocalePreset(
        languages = listOf("en-US", "en"),
        timeZone = "UTC",
        timezoneOffsetMinutes = 0
    )

    val androidTemplates = listOf(
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.80 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "Google Inc. (Qualcomm)",
            gpuRenderer = "ANGLE (Qualcomm, Adreno 740, OpenGL ES 3.2)"
        )
    )

    // GENERATIVE POOLS
    private val gpuPool = listOf(
        Pair("Qualcomm", "Adreno (TM) 740"),
        Pair("Qualcomm", "Adreno (TM) 730"),
        Pair("Qualcomm", "Adreno (TM) 660"),
        Pair("ARM", "Mali-G715"),
        Pair("ARM", "Mali-G710"),
        Pair("ARM", "Mali-G78"),
        Pair("Google Inc. (Qualcomm)", "ANGLE (Qualcomm, Adreno 740, OpenGL ES 3.2)")
    )

    private val screenPool = listOf(
        ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625), // 20:9
        ScreenSpecs(384, 854, 384, 810, 24, 24, 3.0),   // 20:9
        ScreenSpecs(360, 800, 360, 760, 24, 24, 3.0),   // 20:9
        ScreenSpecs(393, 873, 393, 829, 24, 24, 2.75)   // 20:9
    )

    fun generateDynamicTemplate(seed: Int): DeviceTemplate {
        val rand = Random(seed)
        val gpu = gpuPool[abs(rand.nextInt()) % gpuPool.size]
        val screen = screenPool[abs(rand.nextInt()) % screenPool.size]
        val ram = listOf(6, 8, 12, 16)[abs(rand.nextInt()) % 4]
        val cores = listOf(8, 12)[abs(rand.nextInt()) % 2]
        
        // Base UA that will be jittered by FingerprintManager
        val baseUa = "Mozilla/5.0 (Linux; Android ${12 + rand.nextInt(3)}; SM-G${900 + rand.nextInt(100)}B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"

        return DeviceTemplate(
            userAgents = listOf(baseUa),
            platform = "Linux armv8l",
            screen = screen,
            hardwareConcurrency = cores,
            deviceMemory = ram,
            gpuVendor = gpu.first,
            gpuRenderer = gpu.second
        )
    }
}

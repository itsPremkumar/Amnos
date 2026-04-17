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
        LocalePreset(listOf("fr-FR", "fr"), "Europe/Paris", -60),
        LocalePreset(listOf("ja-JP", "ja"), "Asia/Tokyo", -540),
        LocalePreset(listOf("ko-KR", "ko"), "Asia/Seoul", -540)
    )

    val strictLocalePreset = LocalePreset(
        languages = listOf("en-US", "en"),
        timeZone = "UTC",
        timezoneOffsetMinutes = 0
    )

    // MOBILE-ONLY IDENTITY LIBRARY
    val androidTemplates = listOf(
        // 1. Google Pixel 8 Pro
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 12,
            gpuVendor = "Google Inc. (Qualcomm)",
            gpuRenderer = "ANGLE (Qualcomm, Adreno 740, OpenGL ES 3.2)"
        ),
        // 2. Samsung Galaxy S24 Ultra
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.113 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 12,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 750"
        ),
        // 3. Samsung Galaxy S23
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(384, 854, 384, 810, 24, 24, 3.0),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 740"
        ),
        // 4. OnePlus 12
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; CPH2573) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 16,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 750"
        ),
        // 5. Google Pixel 7a
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 13; Pixel 7a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.105 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "Google Inc. (Qualcomm)",
            gpuRenderer = "Adreno (TM) 730"
        ),
        // 6. Nothing Phone (2)
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; A065) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(393, 873, 393, 829, 24, 24, 2.75),
            hardwareConcurrency = 8,
            deviceMemory = 12,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 730"
        ),
        // 7. Xiaomi 14 Pro
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; 2311TRN52C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
            hardwareConcurrency = 8,
            deviceMemory = 16,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 750"
        ),
        // 8. Sony Xperia 1 V
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 13; XQ-DQ72) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.105 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(384, 854, 384, 810, 24, 24, 2.8125),
            hardwareConcurrency = 8,
            deviceMemory = 12,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 740"
        ),
        // 9. Generic Hardened Android (Invisible)
        DeviceTemplate(
            userAgents = listOf("Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"),
            platform = "Linux armv8l",
            screen = ScreenSpecs(360, 800, 360, 760, 24, 24, 3.0),
            hardwareConcurrency = 8,
            deviceMemory = 8,
            gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 660"
        )
    )

    // GENERATIVE POOLS (MOBILE ONLY)
    private val gpuPool = listOf(
        Pair("Qualcomm", "Adreno (TM) 750"),
        Pair("Qualcomm", "Adreno (TM) 740"),
        Pair("Qualcomm", "Adreno (TM) 730"),
        Pair("ARM", "Mali-G715"),
        Pair("ARM", "Mali-G710"),
        Pair("Google Inc. (Qualcomm)", "ANGLE (Qualcomm, Adreno 740, OpenGL ES 3.2)")
    )

    private val screenPool = listOf(
        ScreenSpecs(412, 915, 412, 872, 24, 24, 2.625),
        ScreenSpecs(384, 854, 384, 810, 24, 24, 3.0),
        ScreenSpecs(360, 800, 360, 760, 24, 24, 3.0),
        ScreenSpecs(393, 873, 393, 829, 24, 24, 2.75)
    )

    fun generateDynamicTemplate(seed: Int): DeviceTemplate {
        val rand = Random(seed)
        val gpu = gpuPool[abs(rand.nextInt()) % gpuPool.size]
        val screen = screenPool[abs(rand.nextInt()) % screenPool.size]
        val ram = listOf(6, 8, 12, 16)[abs(rand.nextInt()) % 4]
        val cores = 8 // Mobile standard
        
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

package com.amnos.browser.core.fingerprint

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
}

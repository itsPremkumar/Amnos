package com.privacy.browser.core.fingerprint

data class DeviceProfile(
    val userAgent: String,
    val platform: String,
    val languages: List<String>,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val timeZone: String,
    val screen: ScreenSpecs,
    val gpuVendor: String,
    val gpuRenderer: String,
    val noiseSeed: Int
)

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
    
    fun generateCoherentProfile(): DeviceProfile {
        return when ((0..2).random()) {
            0 -> createAndroidProfile()
            1 -> createWindowsProfile()
            else -> createMacProfile()
        }
    }

    private fun createAndroidProfile() = DeviceProfile(
        userAgent = "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36",
        platform = "Linux armv8l",
        languages = listOf("en-US", "en"),
        hardwareConcurrency = 8,
        deviceMemory = 8,
        timeZone = "UTC",
        screen = ScreenSpecs(390, 844, 390, 844, 24, 24, 3.0),
        gpuVendor = "Qualcomm",
        gpuRenderer = "Adreno (TM) 740",
        noiseSeed = (0..1000).random()
    )

    private fun createWindowsProfile() = DeviceProfile(
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
        platform = "Win32",
        languages = listOf("en-US", "en"),
        hardwareConcurrency = 12,
        deviceMemory = 16,
        timeZone = "America/New_York",
        screen = ScreenSpecs(1920, 1080, 1920, 1040, 24, 24, 1.0),
        gpuVendor = "Google Inc. (Intel)",
        gpuRenderer = "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0)",
        noiseSeed = (0..1000).random()
    )

    private fun createMacProfile() = DeviceProfile(
        userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
        platform = "MacIntel",
        languages = listOf("en-US", "en"),
        hardwareConcurrency = 8,
        deviceMemory = 16,
        timeZone = "Europe/London",
        screen = ScreenSpecs(1440, 900, 1440, 850, 24, 24, 2.0),
        gpuVendor = "Apple Inc.",
        gpuRenderer = "Apple M2",
        noiseSeed = (0..1000).random()
    )
}

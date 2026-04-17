package com.amnos.browser.core.fingerprint

data class ScreenSpecs(
    val width: Int,
    val height: Int,
    val availWidth: Int,
    val availHeight: Int,
    val colorDepth: Int = 24,
    val pixelDepth: Int = 24,
    val devicePixelRatio: Double
)

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
    val noiseSeed: Int,
    
    // Generative Client Hints
    val clientHints: ClientHints = ClientHints(userAgent)
) {
    val acceptLanguageHeader: String
        get() = languages.joinToString(",") { if (it.contains("-")) "$it,${it.split("-")[0]};q=0.9" else "$it;q=0.9" }
}

data class ClientHints(
    val brand: String,
    val version: String,
    val platform: String,
    val mobile: Boolean = true,
    val model: String = ""
) {
    constructor(ua: String) : this(
        brand = "Google Chrome",
        version = extractVersion(ua),
        platform = "Android",
        mobile = true,
        model = extractModel(ua)
    )

    companion object {
        private fun extractVersion(ua: String): String {
            val match = Regex("Chrome/([0-9]+)").find(ua)
            return match?.groupValues?.get(1) ?: "123"
        }
        
        private fun extractModel(ua: String): String {
            val match = Regex("\\(([^;]+); ([^;]+); ([^\\)]+)\\)").find(ua)
            return match?.groupValues?.get(3) ?: ""
        }
    }
}

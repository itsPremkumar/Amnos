package com.privacy.browser.core.session

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

data class SessionConfig(
    val sessionId: String = UUID.randomUUID().toString(),
    val userAgent: String,
    val platform: String,
    val languages: List<String>,
    val hardwareConcurrency: Int,
    val deviceMemory: Int,
    val timeZone: String,
    val screen: ScreenConfig,
    val noiseSeed: Int = (0..1000).random()
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("sessionId", sessionId)
        root.put("userAgent", userAgent)
        root.put("platform", platform)
        
        val langs = JSONArray()
        languages.forEach { langs.put(it) }
        root.put("languages", langs)
        
        root.put("hardwareConcurrency", hardwareConcurrency)
        root.put("deviceMemory", deviceMemory)
        root.put("timeZone", timeZone)
        root.put("noiseSeed", noiseSeed)
        
        val screenObj = JSONObject()
        screenObj.put("width", screen.width)
        screenObj.put("height", screen.height)
        screenObj.put("availWidth", screen.availWidth)
        screenObj.put("availHeight", screen.availHeight)
        screenObj.put("colorDepth", screen.colorDepth)
        screenObj.put("pixelDepth", screen.pixelDepth)
        root.put("screen", screenObj)
        
        return root.toString()
    }

    companion object {
        fun generateRandom(): SessionConfig {
            val userAgents = listOf(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Firefox/118.0",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
            )
            val platforms = listOf("Win32", "MacIntel", "Linux x86_64")
            val languagesList = listOf(listOf("en-US", "en"), listOf("fr-FR", "fr"), listOf("de-DE", "de"))
            val timeZones = listOf("UTC", "America/New_York", "Europe/London", "Asia/Tokyo")

            return SessionConfig(
                userAgent = userAgents.random(),
                platform = platforms.random(),
                languages = languagesList.random(),
                hardwareConcurrency = listOf(2, 4, 8, 12, 16).random(),
                deviceMemory = listOf(2, 4, 8, 16).random(),
                timeZone = timeZones.random(),
                screen = ScreenConfig(
                    width = (1280..2560).random(),
                    height = (720..1440).random(),
                    availWidth = (1280..2560).random(),
                    availHeight = (700..1400).random(),
                    colorDepth = 24,
                    pixelDepth = 24
                )
            )
        }
    }
}

data class ScreenConfig(
    val width: Int,
    val height: Int,
    val availWidth: Int,
    val availHeight: Int,
    val colorDepth: Int,
    val pixelDepth: Int
)

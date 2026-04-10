package com.privacy.browser.core.fingerprint

import com.privacy.browser.core.fingerprint.DeviceProfile
import org.json.JSONArray
import org.json.JSONObject

class ScriptInjector(private val profile: DeviceProfile) {

    private fun generateConfigJson(): String {
        val root = JSONObject()
        root.put("userAgent", profile.userAgent)
        root.put("platform", profile.platform)
        
        val langs = JSONArray()
        profile.languages.forEach { langs.put(it) }
        root.put("languages", langs)
        
        root.put("hardwareConcurrency", profile.hardwareConcurrency)
        root.put("deviceMemory", profile.deviceMemory)
        root.put("timeZone", profile.timeZone)
        root.put("noiseSeed", profile.noiseSeed)
        
        val screenObj = JSONObject()
        screenObj.put("width", profile.screen.width)
        screenObj.put("height", profile.screen.height)
        screenObj.put("availWidth", profile.screen.availWidth)
        screenObj.put("availHeight", profile.screen.availHeight)
        screenObj.put("colorDepth", profile.screen.colorDepth)
        screenObj.put("pixelDepth", profile.screen.pixelDepth)
        screenObj.put("devicePixelRatio", profile.screen.devicePixelRatio)
        root.put("screen", screenObj)
        
        // WebGL Specifics
        root.put("gpuVendor", profile.gpuVendor)
        root.put("gpuRenderer", profile.gpuRenderer)
        
        return root.toString()
    }

    fun wrapScript(baseScript: String): String {
        val configJson = generateConfigJson()
        return """
            (function() {
                window._privacyConfig = $configJson;
                $baseScript
            })();
        """.trimIndent()
    }
}

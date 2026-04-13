package com.amnos.browser.core.fingerprint

import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.WebGlMode
import org.json.JSONArray
import org.json.JSONObject

class ScriptInjector(
    private val profile: DeviceProfile,
    private val policy: PrivacyPolicy
) {

    private fun generateConfigJson(): String {
        val root = JSONObject()
        root.put("sessionId", profile.sessionId)
        root.put("tabId", profile.tabId)
        root.put("userAgent", profile.userAgent)
        root.put("platform", profile.platform)

        val langs = JSONArray()
        profile.languages.forEach { langs.put(it) }
        root.put("languages", langs)

        root.put("hardwareConcurrency", profile.hardwareConcurrency)
        root.put("deviceMemory", profile.deviceMemory)
        root.put("timeZone", profile.timeZone)
        root.put("timezoneOffsetMinutes", profile.timezoneOffsetMinutes)
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

        root.put("gpuVendor", profile.gpuVendor)
        root.put("gpuRenderer", profile.gpuRenderer)

        val policyObj = JSONObject()
        policyObj.put("blockInlineScripts", policy.blockInlineScripts)
        policyObj.put("blockWebSockets", policy.blockWebSockets)
        policyObj.put("allowFirstPartyWebSockets", policy.allowFirstPartyWebSockets)
        policyObj.put("blockWebRtc", policy.blockWebRtc)
        policyObj.put("blockDnsPrefetch", policy.blockDnsPrefetch)
        policyObj.put("blockPreconnect", policy.blockPreconnect)
        policyObj.put("blockEval", policy.blockEval)
        policyObj.put("blockServiceWorkers", policy.blockServiceWorkers)
        policyObj.put("webGlDisabled", policy.webGlMode == WebGlMode.DISABLED)
        policyObj.put("strictFirstPartyIsolation", policy.strictFirstPartyIsolation)
        policyObj.put("fingerprintLevel", policy.fingerprintProtectionLevel.name)
        policyObj.put(
            "timingResolutionMs",
            if (policy.fingerprintProtectionLevel == FingerprintProtectionLevel.STRICT) 100 else 16
        )
        policyObj.put(
            "timingJitterMs",
            if (policy.fingerprintProtectionLevel == FingerprintProtectionLevel.STRICT) 12 else 3
        )
        root.put("policy", policyObj)

        return root.toString()
    }

    fun wrapScript(baseScript: String): String {
        val configJson = generateConfigJson()
        return """
            (() => {
                window._privacyConfig = $configJson;
                $baseScript
            })();
        """.trimIndent()
    }
}

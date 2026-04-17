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
        policyObj.put("blockInlineScripts", policy.filterBlockInlineScripts)
        policyObj.put("blockWebSockets", policy.filterBlockWebSockets)
        policyObj.put("allowFirstPartyWebSockets", false)
        policyObj.put("blockWebRtc", policy.networkBlockWebRtc)
        policyObj.put("blockDnsPrefetch", policy.networkBlockDnsPrefetch)
        policyObj.put("blockPreconnect", policy.networkBlockPreconnect)
        policyObj.put("blockEval", policy.filterBlockEval)
        policyObj.put("blockServiceWorkers", policy.filterBlockServiceWorkers)
        policyObj.put("webGlDisabled", policy.hardwareWebGlMode == WebGlMode.DISABLED)
        policyObj.put("strictFirstPartyIsolation", policy.filterStrictFirstPartyIsolation)
        policyObj.put("fingerprintLevel", policy.hardwareFingerprintLevel.name)
        policyObj.put("firewallLevel", policy.networkFirewallLevel.name)
        policyObj.put(
            "timingResolutionMs",
            when (policy.networkFirewallLevel) {
                com.amnos.browser.core.security.FirewallLevel.PARANOID -> 200
                else -> if (policy.hardwareFingerprintLevel == FingerprintProtectionLevel.STRICT) 100 else 16
            }
        )
        policyObj.put(
            "timingJitterMs",
            when (policy.networkFirewallLevel) {
                com.amnos.browser.core.security.FirewallLevel.PARANOID -> 50
                else -> if (policy.hardwareFingerprintLevel == FingerprintProtectionLevel.STRICT) 12 else 3
            }
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

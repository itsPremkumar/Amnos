package com.amnos.browser.core.fingerprint

import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

object FingerprintManager {
    private val secureRandom = SecureRandom()

    fun newSessionId(): String = UUID.randomUUID().toString()
    fun newTabId(): String = UUID.randomUUID().toString()
    fun newUnlockPin(): String = secureRandom.nextInt(10_000).toString().padStart(4, '0')

    fun generateCoherentProfile(
        sessionId: String,
        tabId: String,
        policy: PrivacyPolicy
    ): DeviceProfile {
        val level = policy.hardwareFingerprintLevel
        val sessionRandom = seededRandom(sessionId)
        val tabSeed = abs(seededRandom(sessionId, tabId).nextInt())

        val template = when {
            policy.identityUaTemplate.uppercase() == "DYNAMIC" -> {
                FingerprintRegistry.generateDynamicTemplate(tabSeed)
            }
            policy.identityUaTemplate.isNotEmpty() && policy.identityUaTemplate != "RANDOM" -> {
                when (policy.identityUaTemplate.uppercase()) {
                    "PIXEL_8" -> FingerprintRegistry.androidTemplates[0]
                    else -> FingerprintRegistry.androidTemplates.first()
                }
            }
            level == FingerprintProtectionLevel.BALANCED -> FingerprintRegistry.androidTemplates[abs(sessionRandom.nextInt()) % FingerprintRegistry.androidTemplates.size]
            else -> FingerprintRegistry.androidTemplates.first()
        }

        val locale = when (level) {
            FingerprintProtectionLevel.BALANCED -> FingerprintRegistry.balancedLocalePresets[abs(tabSeed) % FingerprintRegistry.balancedLocalePresets.size]
            FingerprintProtectionLevel.STRICT -> FingerprintRegistry.strictLocalePreset
            else -> FingerprintRegistry.balancedLocalePresets.first()
        }

        val baseUa = template.userAgents.first()
        val finalUserAgent = jitterUserAgent(baseUa, tabSeed)

        AmnosLog.i("FingerprintManager", "IDENTITY DYNAMICS: [${policy.identityUaTemplate}] -> Profile synthesized for Tab ${tabId.take(4)}")

        return DeviceProfile(
            sessionId = sessionId,
            tabId = tabId,
            userAgent = finalUserAgent,
            platform = template.platform,
            languages = locale.languages,
            hardwareConcurrency = template.hardwareConcurrency,
            deviceMemory = template.deviceMemory,
            timeZone = locale.timeZone,
            timezoneOffsetMinutes = locale.timezoneOffsetMinutes,
            screen = template.screen,
            gpuVendor = template.gpuVendor,
            gpuRenderer = template.gpuRenderer,
            noiseSeed = abs(tabSeed) + 1
        )
    }

    private fun jitterUserAgent(base: String, seed: Int): String {
        val rand = Random(seed)
        // Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36
        
        return base.replace(Regex("Chrome/[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
            val major = 120 + rand.nextInt(5)
            val patch = 4000 + rand.nextInt(1000)
            val build = 10 + rand.nextInt(90)
            "Chrome/$major.0.$patch.$build"
        }.replace(Regex("AppleWebKit/[0-9]+\\.[0-9]+")) {
            "AppleWebKit/537.${30 + rand.nextInt(10)}"
        }
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

package com.amnos.browser.ui.screens.browser.logic

import androidx.compose.runtime.MutableState
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.JavaScriptMode
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.WebGlMode
import com.amnos.browser.core.session.SessionManager

class PolicyEnforcementController(
    private val sessionManager: SessionManager,
    private val policy: MutableState<PrivacyPolicy>,
    private val jsMode: MutableState<JavaScriptMode>,
    private val webGl: MutableState<Boolean>,
    private val fpLevel: MutableState<FingerprintProtectionLevel>,
    private val firewall: MutableState<com.amnos.browser.core.security.FirewallLevel>,
    private val sandbox: MutableState<Boolean>,
    private val onPolicyUpdated: () -> Unit
) {
    fun setJavaScriptMode(mode: JavaScriptMode) {
        sessionManager.setJavaScriptMode(mode)
        onPolicyUpdated()
    }

    fun toggleWebGL(enabled: Boolean) {
        sessionManager.setWebGlEnabled(enabled)
        onPolicyUpdated()
    }

    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) {
        sessionManager.setFingerprintProtectionLevel(level)
        onPolicyUpdated()
    }

    fun setFirewallLevel(level: com.amnos.browser.core.security.FirewallLevel) {
        sessionManager.updatePrivacyPolicy { it.copy(networkFirewallLevel = level) }
        onPolicyUpdated()
    }

    fun toggleSandboxEnabled(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(purgeSandboxEnabled = enabled) }
        onPolicyUpdated()
    }

    fun toggleGenericPolicy(updater: (PrivacyPolicy) -> PrivacyPolicy) {
        sessionManager.updatePrivacyPolicy(updater)
        onPolicyUpdated()
    }

    fun syncUIState() {
        val p = sessionManager.privacyPolicy
        policy.value = p
        jsMode.value = p.hardwareJavascriptMode
        webGl.value = p.hardwareWebGlMode == WebGlMode.SPOOF
        fpLevel.value = p.hardwareFingerprintLevel
        firewall.value = p.networkFirewallLevel
        sandbox.value = p.purgeSandboxEnabled
    }
}

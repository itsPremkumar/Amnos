package com.amnos.browser.ui.screens.browser.logic

import com.amnos.browser.BuildConfig
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.security.JavaScriptMode
import com.amnos.browser.core.security.WebGlMode
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.ui.screens.browser.BrowserViewModel

class SecurityHandler(
    private val viewModel: BrowserViewModel,
    private val sessionManager: SessionManager
) {
    fun setJavaScriptMode(mode: JavaScriptMode) {
        AmnosLog.i("SecurityHandler", "Policy Update: JavaScript mode set to $mode")
        sessionManager.setJavaScriptMode(mode)
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun toggleWebGL(enabled: Boolean) {
        AmnosLog.i("SecurityHandler", "Policy Update: WebGL toggled to $enabled")
        sessionManager.setWebGlEnabled(enabled)
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun toggleHttpsOnly(enabled: Boolean) {
        AmnosLog.i("SecurityHandler", "Policy Update: HTTPS-only toggled to $enabled")
        sessionManager.updatePrivacyPolicy { it.copy(httpsOnlyEnabled = enabled) }
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun toggleThirdPartyBlocking(enabled: Boolean) {
        AmnosLog.i("SecurityHandler", "Policy Update: Third-party blocking toggled to $enabled")
        sessionManager.updatePrivacyPolicy {
            it.copy(
                blockThirdPartyRequests = enabled,
                blockThirdPartyScripts = enabled
            )
        }
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun toggleInlineScriptBlocking(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy {
            it.copy(
                blockInlineScripts = enabled,
                blockEval = enabled,
                javascriptMode = if (enabled && it.javascriptMode == JavaScriptMode.FULL) {
                    JavaScriptMode.RESTRICTED
                } else {
                    it.javascriptMode
                }
            )
        }
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun toggleResetIdentityOnRefresh(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(resetIdentityOnRefresh = enabled) }
        viewModel.refreshPolicyState()
    }

    fun toggleStrictFirstPartyIsolation(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(strictFirstPartyIsolation = enabled) }
        viewModel.refreshPolicyState()
    }

    fun toggleWebSockets(enabled: Boolean) {
        sessionManager.updatePrivacyPolicy { it.copy(blockWebSockets = enabled) }
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun toggleRemoteDebugging(enabled: Boolean) {
        if (!BuildConfig.DEBUG) {
            sessionManager.securityController.logInternal(
                "[Diagnostics:RemoteDebugging]",
                "Ignored remote debugging toggle outside debug builds.",
                "WARN"
            )
            return
        }
        sessionManager.updatePrivacyPolicy { it.copy(enableRemoteDebugging = enabled) }
        viewModel.refreshPolicyState()
        try {
            android.webkit.WebView.setWebContentsDebuggingEnabled(enabled)
            AmnosLog.d("SecurityHandler", "Remote debugging dynamically set to: $enabled")
        } catch (e: Exception) {
            AmnosLog.e("SecurityHandler", "Failed to set remote debugging dynamically", e)
        }
    }

    fun toggleForceRelaxSecurity(enabled: Boolean) {
        if (!BuildConfig.DEBUG) {
            sessionManager.securityController.logInternal(
                "[Diagnostics:RelaxedMode]",
                "Ignored relaxed security toggle outside debug builds.",
                "WARN"
            )
            return
        }
        sessionManager.updatePrivacyPolicy { it.copy(forceRelaxSecurityForDebug = enabled) }
        viewModel.refreshPolicyState()
        viewModel.reload()
    }

    fun setFingerprintProtectionLevel(level: FingerprintProtectionLevel) {
        AmnosLog.i("SecurityHandler", "Policy Update: Fingerprint protection level set to $level")
        sessionManager.setFingerprintProtectionLevel(level)
        viewModel.refreshPolicyState()
        viewModel.recreateCurrentTab()
    }
}

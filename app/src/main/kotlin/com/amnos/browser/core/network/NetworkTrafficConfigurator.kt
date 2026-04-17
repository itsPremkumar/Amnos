package com.amnos.browser.core.network

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.session.AmnosLog

class NetworkTrafficConfigurator(
    private val context: Context,
    private val loopbackProxyServer: LoopbackProxyServer,
    private val securityController: SecurityController
) {
    @SuppressLint("RequiresFeature")
    fun configure(policy: PrivacyPolicy) {
        if (policy.forceRelaxSecurityForDebug) {
            AmnosLog.w("NetworkTrafficConfig", "TOTAL PROXY BYPASS - Diagnostics mode active")
            loopbackProxyServer.stop()
            clearProxyOverride()
            return
        }

        if (!policy.networkEnforceLoopbackProxy || !WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            loopbackProxyServer.stop()
            clearProxyOverride()
            securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
            return
        }

        loopbackProxyServer.stop()
        val port = loopbackProxyServer.start()
        val proxyConfig = ProxyConfig.Builder()
            .removeImplicitRules()
            .addProxyRule("http://127.0.0.1:$port", ProxyConfig.MATCH_ALL_SCHEMES)
            .build()

        ProxyController.getInstance().setProxyOverride(
            proxyConfig,
            ContextCompat.getMainExecutor(context)
        ) {
            securityController.updateProxyStatus(active = true, dohGlobal = true, port = port)
        }
    }

    @SuppressLint("RequiresFeature")
    fun clearProxyOverride() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
            return
        }

        ProxyController.getInstance().clearProxyOverride(ContextCompat.getMainExecutor(context)) {
            securityController.updateProxyStatus(active = false, dohGlobal = false, port = null)
        }
    }
}

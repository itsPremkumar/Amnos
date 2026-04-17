package com.amnos.browser.core.webview.guard

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.amnos.browser.core.network.BlockReason
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.model.RequestDisposition
import com.amnos.browser.core.security.PrivacyPolicy

class ResourceGuard(
    private val networkSecurityManager: NetworkSecurityManager,
    private val securityController: SecurityController,
    private val deviceProfile: DeviceProfile,
    private val policyProvider: () -> PrivacyPolicy,
    private val onTrackerBlocked: () -> Unit
) {
    fun intercept(view: WebView?, request: WebResourceRequest?, currentHost: String?): WebResourceResponse? {
        request ?: return null
        val url = request.url.toString()
        if (url.startsWith("about:", ignoreCase = true) || url.startsWith("data:", ignoreCase = true) || url.startsWith("blob:", ignoreCase = true)) {
            return null
        }

        val decision = networkSecurityManager.evaluateRequest(request, currentHost)

        if (decision.isBlocked) {
            securityController.logRequest(
                url = decision.sanitizedUrl,
                method = request.method,
                type = securityController.mapKind(decision.kind),
                disposition = RequestDisposition.BLOCKED,
                thirdParty = decision.thirdParty,
                reason = decision.blockReason?.let { networkSecurityManager.blockReasonLabel(it) }
            )
            if (decision.blockReason == BlockReason.TRACKER ||
                decision.blockReason == BlockReason.THIRD_PARTY ||
                decision.blockReason == BlockReason.THIRD_PARTY_SCRIPT
            ) {
                onTrackerBlocked()
            }
            return networkSecurityManager.createBlockedResponse(decision.blockReason!!, request.isForMainFrame)
        }

        try {
            val proxiedResponse = networkSecurityManager.fetchResponse(request, decision, deviceProfile, currentHost)
            if (proxiedResponse != null) {
                securityController.logRequest(
                    url = decision.sanitizedUrl,
                    method = request.method,
                    type = securityController.mapKind(decision.kind),
                    disposition = RequestDisposition.ALLOWED,
                    thirdParty = decision.thirdParty
                )
                
                if (!policyProvider().debugBlockForensicLogging) {
                    AmnosLog.v("ResourceGuard", "Interception SUCCESS (Proxied): ${request.method} ${decision.sanitizedUrl}")
                }
                return proxiedResponse
            }
        } catch (e: Exception) {
            AmnosLog.e("ResourceGuard", "Interception FAILURE: Network fetch error for ${decision.sanitizedUrl}", e)
        }

        AmnosLog.v("ResourceGuard", "Interception FALLTHROUGH to System Network: ${decision.sanitizedUrl}")
        securityController.logRequest(
            url = decision.sanitizedUrl,
            method = request.method,
            type = securityController.mapKind(decision.kind),
            disposition = RequestDisposition.PASSTHROUGH,
            thirdParty = decision.thirdParty,
            reason = if (request.method.equals("GET", ignoreCase = true) || request.method.equals("HEAD", ignoreCase = true)) {
                "proxy_fallback"
            } else {
                networkSecurityManager.blockReasonLabel(BlockReason.UNSAFE_METHOD)
            }
        )
        return null
    }
}

package com.amnos.browser.core.session

import androidx.compose.runtime.mutableStateOf
import com.amnos.browser.core.network.RequestKind
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.model.*

class SecurityController {
    val monitor = TrafficMonitor()
    val privacyLog = PrivacyAuditLog()
    val forensicLog = ForensicAuditLog()

    // Bridged status properties for UI compatibility
    val requestLog: List<RequestEntry> get() = privacyLog.requestLog
    val activeConnections: List<ConnectionEntry> get() = monitor.activeConnections
    val proxyStatus get() = monitor.proxyStatus
    val dohStatus get() = monitor.dohStatus
    val webRtcStatus get() = monitor.webRtcStatus
    val webSocketStatus get() = monitor.webSocketStatus
    val webRtcAttemptCount get() = monitor.webRtcAttemptCount
    val webSocketAttemptCount get() = monitor.webSocketAttemptCount
    val fingerprintLevel = mutableStateOf(FingerprintProtectionLevel.STRICT)
    val warningMessage = mutableStateOf("Strong privacy protections enabled. Network anonymity is not guaranteed.")
    val internalLogs get() = forensicLog.internalLogs

    fun logInternal(tag: String, message: String, level: String = "INFO") {
        forensicLog.log(tag, message, level)
    }

    fun logRequest(
        url: String,
        method: String,
        type: RequestType,
        disposition: RequestDisposition,
        thirdParty: Boolean = false,
        reason: String? = null
    ) {
        privacyLog.logRequest(url, method, type, disposition, thirdParty, reason)
    }

    fun updateProxyStatus(active: Boolean, dohGlobal: Boolean, port: Int?) {
        monitor.updateProxyStatus(active, dohGlobal, port)
    }

    fun setFingerprintLevel(level: FingerprintProtectionLevel) {
        fingerprintLevel.value = level
    }

    fun setForensicLoggingBlocked(blocked: Boolean) {
        forensicLog.setMirroringEnabled(!blocked)
    }

    fun recordWebRtcAttempt(detail: String, blocked: Boolean) {
        monitor.recordWebRtcAttempt(blocked)
        logRequest(
            url = detail,
            method = "JS",
            type = RequestType.OTHER,
            disposition = if (blocked) RequestDisposition.BLOCKED else RequestDisposition.ALLOWED,
            reason = "webrtc"
        )
    }

    fun recordWebSocketAttempt(detail: String, blocked: Boolean) {
        monitor.recordWebSocketAttempt(blocked)
        logRequest(
            url = detail,
            method = "JS",
            type = RequestType.WEBSOCKET,
            disposition = if (blocked) RequestDisposition.BLOCKED else RequestDisposition.ALLOWED,
            reason = "websocket"
        )
    }

    fun addConnection(id: String, host: String, port: Int, type: String) {
        monitor.addConnection(id, host, port, type)
    }

    fun removeConnection(id: String) {
        monitor.removeConnection(id)
    }

    fun mapKind(kind: RequestKind): RequestType = privacyLog.mapKind(kind)

    fun blockedCount(): Int = privacyLog.getBlockedCount()

    fun trackerBlockCount(): Int = privacyLog.getTrackerBlockCount()

    fun obliterate() {
        monitor.clear()
        privacyLog.clear()
        forensicLog.clear()
        warningMessage.value = "Strong privacy protections enabled. Network anonymity is not guaranteed."
    }
}

package com.privacy.browser.core.session

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.privacy.browser.core.network.RequestKind
import com.privacy.browser.core.security.FingerprintProtectionLevel

class SecurityController {

    private val _requestLog = mutableStateListOf<RequestEntry>()
    val requestLog: List<RequestEntry> = _requestLog
    private val _activeConnections = mutableStateListOf<ConnectionEntry>()
    val activeConnections: List<ConnectionEntry> = _activeConnections

    val proxyStatus = mutableStateOf("Inactive")
    val dohStatus = mutableStateOf("Partial")
    val webRtcStatus = mutableStateOf("Blocked")
    val webSocketStatus = mutableStateOf("Blocked")
    val fingerprintLevel = mutableStateOf(FingerprintProtectionLevel.STRICT)
    val webRtcAttemptCount = mutableIntStateOf(0)
    val webSocketAttemptCount = mutableIntStateOf(0)
    val warningMessage = mutableStateOf("Strong privacy protections enabled. Network anonymity is not guaranteed.")

    val internalLogs = mutableStateListOf<InternalLogEntry>()

    data class InternalLogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val tag: String,
        val message: String,
        val level: String = "INFO"
    )

    fun logInternal(tag: String, message: String, level: String = "INFO") {
        internalLogs.add(0, InternalLogEntry(tag = tag, message = message, level = level))
        if (internalLogs.size > 200) {
            internalLogs.removeAt(internalLogs.size - 1)
        }
        android.util.Log.println(
            when (level) {
                "DEBUG" -> android.util.Log.DEBUG
                "WARN" -> android.util.Log.WARN
                "ERROR" -> android.util.Log.ERROR
                else -> android.util.Log.INFO
            },
            tag,
            message
        )
    }

    data class RequestEntry(
        val url: String,
        val method: String,
        val type: RequestType,
        val disposition: RequestDisposition,
        val thirdParty: Boolean = false,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ConnectionEntry(
        val id: String,
        val host: String,
        val port: Int,
        val type: String,
        val openedAt: Long = System.currentTimeMillis()
    )

    enum class RequestType {
        DOCUMENT, SCRIPT, STYLESHEET, IMAGE, MEDIA, FONT, XHR, WEBSOCKET, DOWNLOAD, SERVICE_WORKER, OTHER
    }

    enum class RequestDisposition {
        ALLOWED,
        BLOCKED,
        PASSTHROUGH
    }

    fun logRequest(
        url: String,
        method: String,
        type: RequestType,
        disposition: RequestDisposition,
        thirdParty: Boolean = false,
        reason: String? = null
    ) {
        if (_requestLog.size >= 100) {
            _requestLog.removeAt(0)
        }
        _requestLog.add(
            RequestEntry(
                url = url,
                method = method,
                type = type,
                disposition = disposition,
                thirdParty = thirdParty,
                reason = reason
            )
        )
    }

    fun updateProxyStatus(active: Boolean, dohGlobal: Boolean, port: Int?) {
        proxyStatus.value = if (active && port != null) "Loopback proxy active on 127.0.0.1:$port" else "Inactive"
        dohStatus.value = if (active && dohGlobal) "Global via loopback proxy" else "Partial via request proxying"
    }

    fun setFingerprintLevel(level: FingerprintProtectionLevel) {
        fingerprintLevel.value = level
    }

    fun recordWebRtcAttempt(detail: String, blocked: Boolean) {
        webRtcAttemptCount.intValue += 1
        webRtcStatus.value = if (blocked) "Blocked and spoofed" else "Observed"
        logRequest(
            url = detail,
            method = "JS",
            type = RequestType.OTHER,
            disposition = if (blocked) RequestDisposition.BLOCKED else RequestDisposition.ALLOWED,
            reason = "webrtc"
        )
    }

    fun recordWebSocketAttempt(detail: String, blocked: Boolean) {
        webSocketAttemptCount.intValue += 1
        webSocketStatus.value = if (blocked) "Blocked" else "Allowed"
        logRequest(
            url = detail,
            method = "JS",
            type = RequestType.WEBSOCKET,
            disposition = if (blocked) RequestDisposition.BLOCKED else RequestDisposition.ALLOWED,
            reason = "websocket"
        )
    }

    fun addConnection(id: String, host: String, port: Int, type: String) {
        _activeConnections.removeAll { it.id == id }
        _activeConnections.add(ConnectionEntry(id = id, host = host, port = port, type = type))
    }

    fun removeConnection(id: String) {
        _activeConnections.removeAll { it.id == id }
    }

    fun mapKind(kind: RequestKind): RequestType = when (kind) {
        RequestKind.DOCUMENT -> RequestType.DOCUMENT
        RequestKind.SCRIPT -> RequestType.SCRIPT
        RequestKind.STYLESHEET -> RequestType.STYLESHEET
        RequestKind.IMAGE -> RequestType.IMAGE
        RequestKind.MEDIA -> RequestType.MEDIA
        RequestKind.FONT -> RequestType.FONT
        RequestKind.XHR -> RequestType.XHR
        RequestKind.WEBSOCKET -> RequestType.WEBSOCKET
        RequestKind.DOWNLOAD -> RequestType.DOWNLOAD
        RequestKind.SERVICE_WORKER -> RequestType.SERVICE_WORKER
        RequestKind.OTHER -> RequestType.OTHER
    }

    fun blockedCount(): Int {
        return requestLog.count { it.disposition == RequestDisposition.BLOCKED }
    }

    fun trackerBlockCount(): Int {
        return requestLog.count {
            it.disposition == RequestDisposition.BLOCKED &&
                (it.reason == "tracker" || it.reason == "third_party" || it.reason == "third_party_script")
        }
    }

    fun clearLog() {
        _requestLog.clear()
        _activeConnections.clear()
        internalLogs.clear()
        webRtcAttemptCount.intValue = 0
        webSocketAttemptCount.intValue = 0
    }
}

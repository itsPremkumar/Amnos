package com.amnos.browser.core.session

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.amnos.browser.BuildConfig
import com.amnos.browser.core.network.RequestKind
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.model.*

class SecurityController {

    private val _requestLog = mutableStateListOf<RequestEntry>()
    val requestLog: List<RequestEntry> = _requestLog
    private val _activeConnections = mutableStateListOf<ConnectionEntry>()
    val activeConnections: List<ConnectionEntry> = _activeConnections

    // Synchronize access to mutable state collections
    private val lock = Any()

    // Cached counters to avoid thread-unsafe iteration during UI updates
    private val _blockedCount = mutableIntStateOf(0)
    private val _trackerBlockCount = mutableIntStateOf(0)

    val proxyStatus = mutableStateOf("Inactive")
    val dohStatus = mutableStateOf("Partial")
    val webRtcStatus = mutableStateOf("Blocked")
    val webSocketStatus = mutableStateOf("Blocked")
    val fingerprintLevel = mutableStateOf(FingerprintProtectionLevel.STRICT)
    val webRtcAttemptCount = mutableIntStateOf(0)
    val webSocketAttemptCount = mutableIntStateOf(0)
    val warningMessage = mutableStateOf("Strong privacy protections enabled. Network anonymity is not guaranteed.")

    val internalLogs = mutableStateListOf<InternalLogEntry>()
    @Volatile
    private var mirrorToSystemLog = !BuildConfig.SECURITY_BLOCK_FORENSIC_LOGGING

    fun logInternal(tag: String, message: String, level: String = "INFO") {
        synchronized(lock) {
            internalLogs.add(0, InternalLogEntry(tag = tag, message = message, level = level))
            while (internalLogs.size > 10000) {
                internalLogs.removeAt(internalLogs.size - 1)
            }
        }

        if (mirrorToSystemLog) {
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
    }

    fun logRequest(
        url: String,
        method: String,
        type: RequestType,
        disposition: RequestDisposition,
        thirdParty: Boolean = false,
        reason: String? = null
    ) {
        synchronized(lock) {
            while (_requestLog.size >= 10000) {
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
            
            if (disposition == RequestDisposition.BLOCKED) {
                _blockedCount.intValue++
                // Atomically update tracker count without iterating requestLog
                if (reason == "tracker" || reason == "third_party" || reason == "third_party_script" || reason == "webrtc") {
                    _trackerBlockCount.intValue++
                }
            }
        }
    }

    fun updateProxyStatus(active: Boolean, dohGlobal: Boolean, port: Int?) {
        proxyStatus.value = if (active && port != null) "Loopback proxy active on 127.0.0.1:$port" else "Inactive"
        dohStatus.value = if (active && dohGlobal) "Global via loopback proxy" else "Partial via request proxying"
    }

    fun setFingerprintLevel(level: FingerprintProtectionLevel) {
        fingerprintLevel.value = level
    }

    fun setForensicLoggingBlocked(blocked: Boolean) {
        mirrorToSystemLog = !blocked
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
        synchronized(lock) {
            _activeConnections.removeAll { it.id == id }
            _activeConnections.add(ConnectionEntry(id = id, host = host, port = port, type = type))
        }
    }

    fun removeConnection(id: String) {
        synchronized(lock) {
            _activeConnections.removeAll { it.id == id }
        }
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

    fun blockedCount(): Int = _blockedCount.intValue

    /**
     * Retrieves the tracker block count from an atomic counter.
     * This avoids ConcurrentModificationException by NOT iterating the request log.
     */
    fun trackerBlockCount(): Int = _trackerBlockCount.intValue

    fun clearLog() {
        synchronized(lock) {
            _requestLog.clear()
            _activeConnections.clear()
            internalLogs.clear()
            webRtcAttemptCount.intValue = 0
            webSocketAttemptCount.intValue = 0
            _blockedCount.intValue = 0
            _trackerBlockCount.intValue = 0
        }
    }
}

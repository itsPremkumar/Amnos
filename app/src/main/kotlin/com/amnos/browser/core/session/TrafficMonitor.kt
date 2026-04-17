package com.amnos.browser.core.session

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.amnos.browser.core.model.ConnectionEntry

class TrafficMonitor {
    private val _activeConnections = mutableStateListOf<ConnectionEntry>()
    val activeConnections: List<ConnectionEntry> = _activeConnections

    val proxyStatus = mutableStateOf("Inactive")
    val dohStatus = mutableStateOf("Partial")
    val webRtcStatus = mutableStateOf("Blocked")
    val webSocketStatus = mutableStateOf("Blocked")
    val webRtcAttemptCount = mutableIntStateOf(0)
    val webSocketAttemptCount = mutableIntStateOf(0)

    private val lock = Any()

    fun updateProxyStatus(active: Boolean, dohGlobal: Boolean, port: Int?) {
        proxyStatus.value = if (active && port != null) "Loopback proxy active on 127.0.0.1:$port" else "Inactive"
        dohStatus.value = if (active && dohGlobal) "Global via loopback proxy" else "Partial via request proxying"
    }

    fun recordWebRtcAttempt(blocked: Boolean) {
        webRtcAttemptCount.intValue += 1
        webRtcStatus.value = if (blocked) "Blocked and spoofed" else "Observed"
    }

    fun recordWebSocketAttempt(blocked: Boolean) {
        webSocketAttemptCount.intValue += 1
        webSocketStatus.value = if (blocked) "Blocked" else "Allowed"
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

    fun clear() {
        synchronized(lock) {
            _activeConnections.clear()
            webRtcAttemptCount.intValue = 0
            webSocketAttemptCount.intValue = 0
            proxyStatus.value = "Inactive"
            dohStatus.value = "Partial"
            webRtcStatus.value = "Blocked"
            webSocketStatus.value = "Blocked"
        }
    }
}

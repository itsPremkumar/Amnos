package com.privacy.browser.core.session

import androidx.compose.runtime.mutableStateListOf

class SecurityController {
    
    // Volatile RAM-only log of recently blocked/active requests
    private val _requestLog = mutableStateListOf<RequestEntry>()
    val requestLog: List<RequestEntry> = _requestLog

    data class RequestEntry(
        val url: String,
        val type: RequestType,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class RequestType {
        XHR, DOCUMENT, WEBSOCKET, TRACKER, SYSTEM
    }

    fun logRequest(url: String, type: RequestType) {
        if (_requestLog.size > 50) {
            _requestLog.removeAt(0) // Keep only last 50
        }
        _requestLog.add(RequestEntry(url, type))
    }

    fun clearLog() {
        _requestLog.clear()
    }
}

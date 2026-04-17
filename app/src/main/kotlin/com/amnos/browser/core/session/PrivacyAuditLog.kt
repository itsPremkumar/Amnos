package com.amnos.browser.core.session

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import com.amnos.browser.core.model.RequestDisposition
import com.amnos.browser.core.model.RequestEntry
import com.amnos.browser.core.model.RequestType
import com.amnos.browser.core.network.RequestKind

class PrivacyAuditLog {
    private val _requestLog = mutableStateListOf<RequestEntry>()
    val requestLog: List<RequestEntry> = _requestLog

    private val _blockedCount = mutableIntStateOf(0)
    private val _trackerBlockCount = mutableIntStateOf(0)

    private val lock = Any()

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
                if (reason == "tracker" || reason == "third_party" || reason == "third_party_script" || reason == "webrtc" || reason == "firewall_rule") {
                    _trackerBlockCount.intValue++
                }
            }
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

    fun getBlockedCount(): Int = _blockedCount.intValue
    fun getTrackerBlockCount(): Int = _trackerBlockCount.intValue

    fun clear() {
        synchronized(lock) {
            _requestLog.clear()
            _blockedCount.intValue = 0
            _trackerBlockCount.intValue = 0
        }
    }
}

package com.privacy.browser.core.session

import androidx.compose.runtime.mutableStateListOf
import com.privacy.browser.core.network.RequestKind

class SecurityController {

    private val _requestLog = mutableStateListOf<RequestEntry>()
    val requestLog: List<RequestEntry> = _requestLog

    data class RequestEntry(
        val url: String,
        val method: String,
        val type: RequestType,
        val disposition: RequestDisposition,
        val thirdParty: Boolean = false,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis()
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
    }
}

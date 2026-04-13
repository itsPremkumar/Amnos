package com.amnos.browser.core.model

data class InternalLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: String = "INFO"
)

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

package com.amnos.browser.core.network

enum class RequestKind {
    DOCUMENT,
    SCRIPT,
    STYLESHEET,
    IMAGE,
    MEDIA,
    FONT,
    XHR,
    WEBSOCKET,
    DOWNLOAD,
    SERVICE_WORKER,
    OTHER
}

enum class BlockReason {
    HTTPS_ONLY,
    TRACKER,
    THIRD_PARTY,
    THIRD_PARTY_SCRIPT,
    WEBSOCKET,
    UNSUPPORTED_SCHEME,
    LOCAL_NETWORK,
    UNSAFE_METHOD,
    SECURITY_THREAT
}

data class RequestDecision(
    val sanitizedUrl: String,
    val kind: RequestKind,
    val blockReason: BlockReason? = null,
    val thirdParty: Boolean = false
) {
    val isBlocked: Boolean
        get() = blockReason != null
}

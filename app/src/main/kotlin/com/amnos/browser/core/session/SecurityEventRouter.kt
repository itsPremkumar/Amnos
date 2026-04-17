package com.amnos.browser.core.session

import com.amnos.browser.core.security.ClipboardVault
import com.amnos.browser.core.session.AmnosLog
import org.json.JSONObject

class SecurityEventRouter(
    private val securityController: SecurityController
) {
    fun route(rawMessage: String, onKeyboardRequested: ((Boolean) -> Unit)?) {
        try {
            val payload = JSONObject(rawMessage)
            when (payload.optString("type")) {
                "keyboard_event" -> {
                    val action = payload.optString("action")
                    onKeyboardRequested?.invoke(action == "show")
                }
                "clipboard_copy" -> {
                    val text = payload.optString("text")
                    if (text.isNotBlank()) {
                        ClipboardVault.write(text)
                    }
                }
                "webrtc" -> {
                    securityController.recordWebRtcAttempt(
                        detail = payload.optString("detail", "webrtc"),
                        blocked = payload.optBoolean("blocked", true)
                    )
                }
                "websocket" -> {
                    val detail = payload.optString("url", payload.optString("detail", "websocket"))
                    val blocked = payload.optBoolean("blocked", true)
                    securityController.recordWebSocketAttempt(detail, blocked)

                    val socketId = payload.optString("id")
                    when (payload.optString("state")) {
                        "open" -> securityController.addConnection(
                            id = socketId,
                            host = payload.optString("host"),
                            port = payload.optInt("port", 443),
                            type = "WEBSOCKET"
                        )
                        "close", "blocked" -> securityController.removeConnection(socketId)
                    }
                }
                "spoof" -> {
                    val property = payload.optString("property", "unknown")
                    val detail = payload.optString("detail", "value modified")
                    securityController.logInternal("FingerprintShield", "SHIELDED: Browser property [$property] accessed. ($detail)", "DEBUG")
                }
                "tamper_detected" -> {
                   securityController.logInternal("SecurityIntegrity", "CRITICAL: WebView Script Tampering Detected!", "ERROR")
                }
            }
        } catch (error: Exception) {
            AmnosLog.w("SecurityEventRouter", "Failed to parse security event: $rawMessage", error)
        }
    }
}

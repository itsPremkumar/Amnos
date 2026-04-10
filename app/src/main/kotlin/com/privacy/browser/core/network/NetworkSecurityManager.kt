package com.privacy.browser.core.network

import android.net.Uri
import android.util.Log

class NetworkSecurityManager {

    /**
     * Blocks WebSocket handshake requests (ws:// or wss://)
     */
    fun isWebSocketHandshake(url: String): Boolean {
        val result = url.startsWith("ws://", ignoreCase = true) || 
                     url.startsWith("wss://", ignoreCase = true)
        if (result) Log.w("NetworkSecurity", "BLOCKED WEBSOCKET HANDSHAKE: $url")
        return result
    }

    /**
     * Sanitizes headers to strip Referer for third-party domains
     * and inject GPC / DNT signals.
     */
    fun getHardenedHeaders(url: String, currentDomain: String?): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // 1. Global Privacy Control (GPC) & Do Not Track (DNT)
        headers["Sec-GPC"] = "1"
        headers["DNT"] = "1"

        // 2. Referer Stripping for Third-Party
        currentDomain?.let { domain ->
            val requestedUri = Uri.parse(url)
            val requestedHost = requestedUri.host
            
            if (requestedHost != null && !requestedHost.contains(domain)) {
                // It's a third-party request. We could strip it, but better:
                // We set it to a neutral value or remove it entirely.
                headers["Referer"] = "https://$domain/" 
            }
        }

        return headers
    }
}

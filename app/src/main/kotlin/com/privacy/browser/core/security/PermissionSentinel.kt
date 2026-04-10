package com.privacy.browser.core.security

import android.util.Log
import android.webkit.PermissionRequest

/**
 * Amnos Permission Sentinel
 * Automatically denies all intrusive hardware permissions (Camera, Microphone, Location, etc.)
 * to ensure zero hardware exposure without triggering system-level prompts.
 */
object PermissionSentinel {

    fun handlePermissionRequest(request: PermissionRequest?) {
        request?.resources?.forEach { resource ->
            Log.w("PermissionSentinel", "SILENTLY DENIED hardware permission: $resource")
        }
        
        // Block all intrusive hardware resources
        request?.deny()
    }
}

package com.amnos.browser.core.security

import android.webkit.PermissionRequest
import com.amnos.browser.core.session.AmnosLog

/**
 * Amnos Permission Sentinel
 * Automatically denies all intrusive hardware permissions (Camera, Microphone, Location, etc.)
 * to ensure zero hardware exposure without triggering system-level prompts.
 */
object PermissionSentinel {

    fun handlePermissionRequest(request: PermissionRequest?) {
        request?.resources?.forEach { resource ->
            AmnosLog.w("PermissionSentinel", "SILENTLY DENIED hardware permission: $resource")
        }
        
        // Block all intrusive hardware resources
        request?.deny()
    }
}

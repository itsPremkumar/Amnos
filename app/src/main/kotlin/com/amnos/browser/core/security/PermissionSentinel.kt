package com.amnos.browser.core.security

import android.webkit.PermissionRequest
import com.amnos.browser.core.session.AmnosLog

/**
 * Amnos Permission Sentinel
 * Automatically denies all intrusive hardware permissions (Camera, Microphone, Location, etc.)
 * to ensure zero hardware exposure without triggering system-level prompts.
 */
object PermissionSentinel {
    private val highRiskResources = setOf(
        PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        PermissionRequest.RESOURCE_VIDEO_CAPTURE,
        PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID,
        PermissionRequest.RESOURCE_MIDI_SYSEX
    )

    fun handlePermissionRequest(request: PermissionRequest?) {
        request?.resources?.forEach { resource ->
            val riskClass = if (resource in highRiskResources) "hardware" else "web"
            AmnosLog.w("PermissionSentinel", "BLOCK: $riskClass resource [$resource] denied to prevent fingerprinting and device access.")
        }

        // Hard-block all intrusive hardware resources
        request?.deny()
    }
}

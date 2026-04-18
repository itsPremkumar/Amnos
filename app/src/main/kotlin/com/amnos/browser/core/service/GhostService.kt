package com.amnos.browser.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager

/**
 * GhostService: The final sentinel of the Amnos sandbox.
 * This service triggers a Super Wipe if the user manually removes the app from the task switcher.
 */
class GhostService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        val sessionManager = SessionManager.getInstance(this)
        if (!sessionManager.privacyPolicy.stealthAbsoluteCloaking) return

        AmnosLog.w("GhostService", "TASK REMOVAL DETECTED: Swiped from recents. Engaging Emergency Wipe.")
        
        try {
            // Trigger emergency process termination and wipe
            sessionManager.killAll(terminateProcess = true)
        } catch (e: Exception) {
            // Process might already be dying
        }
        
        stopSelf()
    }
}

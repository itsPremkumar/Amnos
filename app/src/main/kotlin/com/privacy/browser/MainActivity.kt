package com.privacy.browser

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.privacy.browser.core.session.SessionManager
import com.privacy.browser.ui.screens.browser.BrowserScreen
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.PrivacyBrowserTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Global Crash Logger
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AmnosCrash", "CRITICAL FAILURE in thread ${thread.name}", throwable)
            // Try to log to a file or something more persistent?
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // WebView Suffix must be set BEFORE any WebView is created, including via state restoration in super.onCreate
        try {
            android.webkit.WebView.setDataDirectorySuffix("amnos_session")
            Log.d("MainActivity", "WebView data directory suffix set successfully")
        } catch (e: Exception) {
            Log.w("MainActivity", "WebView suffix already set or failed: ${e.message}")
        }

        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: Initializing Amnos UI")

        try {
            val prefs = getSharedPreferences("amnos_debug_prefs", MODE_PRIVATE)
            val developerDebugEnabled = prefs.getBoolean("enable_remote_debugging", false)
            val debugEnabled = BuildConfig.DEBUG || developerDebugEnabled
            
            WebView.setWebContentsDebuggingEnabled(debugEnabled)
            Log.d("MainActivity", "Web debugging: ${if (debugEnabled) "enabled" else "disabled"} (developerMode=$developerDebugEnabled)")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to set web debugging state", e)
        }

        // Security flags will be initialized after SessionManager creation

        try {
            Log.d("MainActivity", "Creating SessionManager")
            sessionManager = SessionManager(this)
            
            // Apply centralized security flags from Command Center (PrivacyPolicy)
            updateSecurityFlags(sessionManager.privacyPolicy)

            Log.d("MainActivity", "Creating BrowserViewModel")
            viewModel = BrowserViewModel(sessionManager)
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization failed during component creation", e)
            throw e 
        }

        Log.d("MainActivity", "Setting content")
        setContent {
            PrivacyBrowserTheme {
                Surface {
                    BrowserScreen(viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            Log.d("MainActivity", "Idle wipe triggered: application moved to background.")
            // Removing terminateProcess = true to prevent accidental "crashes" on startup/transitions
            sessionManager.killAll(terminateProcess = false)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN && !isChangingConfigurations) {
            Log.d("MainActivity", "Memory pressure wipe triggered.")
            sessionManager.killAll(terminateProcess = false)
        }
    }

    private fun updateSecurityFlags(policy: com.privacy.browser.core.security.PrivacyPolicy) {
        if (policy.blockScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            Log.d("MainActivity", "Screenshot protection ENABLED (via policy)")
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Log.d("MainActivity", "Screenshot protection DISABLED (via policy)")
        }
    }
}

package com.privacy.browser

import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.privacy.browser.core.session.AmnosLog
import com.privacy.browser.core.session.SessionManager
import com.privacy.browser.ui.screens.browser.BrowserScreen
import com.privacy.browser.ui.screens.browser.BrowserViewModel
import com.privacy.browser.ui.theme.PrivacyBrowserTheme

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AmnosLog.e("AmnosCrash", "CRITICAL FAILURE in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            android.webkit.WebView.setDataDirectorySuffix("amnos_session")
            AmnosLog.d("MainActivity", "WebView data directory suffix set successfully")
        } catch (e: Exception) {
            AmnosLog.w("MainActivity", "WebView suffix already set or failed: ${e.message}")
        }

        super.onCreate(savedInstanceState)
        AmnosLog.d("MainActivity", "onCreate: Initializing Amnos UI")

        try {
            val prefs = getSharedPreferences("amnos_debug_prefs", MODE_PRIVATE)
            val developerDebugEnabled = prefs.getBoolean("enable_remote_debugging", false)
            val debugEnabled = BuildConfig.DEBUG && developerDebugEnabled

            WebView.setWebContentsDebuggingEnabled(debugEnabled)
            AmnosLog.d(
                "MainActivity",
                "Web debugging: ${if (debugEnabled) "enabled" else "disabled"} (debugBuild=${BuildConfig.DEBUG}, developerMode=$developerDebugEnabled)"
            )
        } catch (e: Exception) {
            AmnosLog.e("MainActivity", "Failed to set web debugging state", e)
        }

        try {
            AmnosLog.d("MainActivity", "Creating SessionManager")
            sessionManager = SessionManager(this)

            if (com.privacy.browser.core.security.RootDetector.isRooted(this)) {
                sessionManager.securityController.logInternal(
                    "SystemHealth",
                    "SECURITY WARNING: Root access detected. Device integrity is compromised. Entering Strict Privacy Mode.",
                    "ERROR"
                )
                sessionManager.setFingerprintProtectionLevel(com.privacy.browser.core.security.FingerprintProtectionLevel.STRICT)
            } else {
                sessionManager.securityController.logInternal("SystemHealth", "Device integrity verified: Normal environment.", "INFO")
            }

            updateSecurityFlags(sessionManager.privacyPolicy)

            AmnosLog.d("MainActivity", "Creating BrowserViewModel")
            viewModel = BrowserViewModel(sessionManager)
        } catch (e: Exception) {
            AmnosLog.e("MainActivity", "Initialization failed during component creation", e)
            throw e 
        }

        AmnosLog.d("MainActivity", "Setting content")
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
        if (::sessionManager.isInitialized && !isChangingConfigurations) {
            AmnosLog.d("MainActivity", "Idle wipe triggered: application moved to background.")
            sessionManager.killAll(terminateProcess = false)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::sessionManager.isInitialized && level >= TRIM_MEMORY_UI_HIDDEN && !isChangingConfigurations) {
            AmnosLog.d("MainActivity", "Memory pressure wipe triggered.")
            sessionManager.killAll(terminateProcess = false)
        }
    }

    private fun updateSecurityFlags(policy: com.privacy.browser.core.security.PrivacyPolicy) {
        if (policy.blockScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            AmnosLog.d("MainActivity", "Screenshot protection ENABLED (via policy)")
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            AmnosLog.d("MainActivity", "Screenshot protection DISABLED (via policy)")
        }
    }
}

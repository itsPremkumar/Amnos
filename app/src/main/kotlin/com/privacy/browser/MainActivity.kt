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
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: Initializing Amnos")

        // 1. RAM-Only Session Isolation
        try {
            // We only set the suffix if it hasn't been set before in this process.
            // Using a try-catch for IllegalStateException specifically.
            android.webkit.WebView.setDataDirectorySuffix("amnos_" + UUID.randomUUID().toString().take(8))
            Log.d("MainActivity", "WebView data directory suffix set successfully")
        } catch (e: IllegalStateException) {
            Log.w("MainActivity", "WebView suffix already set or WebView already initialized", e)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to set data suffix", e)
        }

        try {
            WebView.setWebContentsDebuggingEnabled(true) // Enabled for debugging as requested
            Log.d("MainActivity", "Web debugging enabled")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to enable web debugging", e)
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        try {
            Log.d("MainActivity", "Creating SessionManager")
            sessionManager = SessionManager(this)
            Log.d("MainActivity", "Creating BrowserViewModel")
            viewModel = BrowserViewModel(sessionManager)
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization failed during component creation", e)
            throw e // Re-throw to be caught by global handler
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
}

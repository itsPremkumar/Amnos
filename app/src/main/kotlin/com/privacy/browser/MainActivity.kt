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
        super.onCreate(savedInstanceState)

        // 1. RAM-Only Session Isolation
        try {
            WebView.setDataDirectorySuffix(UUID.randomUUID().toString())
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to set data suffix", e)
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        sessionManager = SessionManager(this)
        viewModel = BrowserViewModel(sessionManager)

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
        // 2. Dead Man's Switch: Auto-Wipe on Background
        // In "God-Tier" mode, we wipe the session as soon as the app is no longer visible
        // to prevent data leaks if the phone is suddenly locked or put away.
        Log.d("MainActivity", "Idle Wipe triggered: Application moved to background.")
        sessionManager.killAll()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            Log.d("MainActivity", "Memory pressure wipe triggered.")
            sessionManager.killAll()
        }
    }
}

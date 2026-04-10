package com.privacy.browser

import android.os.Bundle
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

        // 1. RAM-Only Session Isolation: 
        // Set a randomized data directory suffix for this process.
        // This ensures the WebView's internal state (cache, cookies, databases)
        // are placed in a disposable, isolated folder for this session only.
        try {
            WebView.setDataDirectorySuffix(UUID.randomUUID().toString())
        } catch (e: Exception) {
            // Already set or error, we skip but logs show isolation is the priority
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
}

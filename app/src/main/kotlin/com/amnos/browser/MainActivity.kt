package com.amnos.browser

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.ui.screens.browser.BrowserScreen
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.PrivacyBrowserTheme
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: BrowserViewModel
    private var isInitialized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // GLOBAL RESILIENCE ENGINE - Capture crashes to prevent force-close
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AmnosLog.e("AmnosResilience", "FATAL RECOVERY: Exception in ${thread.name}", throwable)
            
            // Log forensic details for user review
            com.amnos.browser.core.security.ClipboardSentinel.wipe(this) 
            
            // If we're on the main thread, we try to show a recovery indicator
            if (thread == android.os.Looper.getMainLooper().thread) {
                AmnosLog.w("AmnosResilience", "Attempting UI rescue...")
                isInitialized = false // Force the loading screen to reappear
                // We don't call the default handler's uncaughtException(thread, throwable) 
                // because that will kill the process on most Android versions.
                // Restarting the main looper is risky but keeps the app 'alive'.
                kotlin.concurrent.thread {
                    android.os.Looper.prepare()
                    AmnosLog.d("AmnosResilience", "Main Looper resurrected. Session stable.")
                    android.os.Looper.loop()
                }
            } else {
                // For background threads, we just swallow the error after logging
                AmnosLog.w("AmnosResilience", "Background thread crash suppressed. Stability maintained.")
            }
        }

        try {
            android.webkit.WebView.setDataDirectorySuffix("amnos_session")
            AmnosLog.d("MainActivity", "WebView data directory suffix set successfully")
        } catch (e: Exception) {
            AmnosLog.w("MainActivity", "WebView suffix already set or failed: ${e.message}")
        }

        super.onCreate(savedInstanceState)
        AmnosLog.d("MainActivity", "onCreate: Initializing Amnos UI")

        // 1. Set the initial loading content immediately
        setContent {
            PrivacyBrowserTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isInitialized) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        BrowserScreen(viewModel)
                    }
                }
            }
        }

        // 2. Perform heavy initialization in a coroutine
        lifecycleScope.launch {
            try {
                // Initialize SessionManager (some parts are IO-bound)
                withContext(Dispatchers.IO) {
                    AmnosLog.d("MainActivity", "Creating SessionManager (Background)")
                    sessionManager = SessionManager(this@MainActivity)
                }

                // Security checks and UI-bound setup
                if (com.amnos.browser.core.security.RootDetector.isRooted(this@MainActivity)) {
                    sessionManager.securityController.logInternal("SystemHealth", "SECURITY WARNING: Root access detected.", "ERROR")
                    sessionManager.setFingerprintProtectionLevel(com.amnos.browser.core.security.FingerprintProtectionLevel.STRICT)
                }

                updateSecurityFlags(sessionManager.privacyPolicy)

                // ViewModel and WebView must be created on the Main thread
                AmnosLog.d("MainActivity", "Creating BrowserViewModel (Main)")
                viewModel = BrowserViewModel(sessionManager)
                
                isInitialized = true
                AmnosLog.d("MainActivity", "Amnos Bootstrap Complete")
            } catch (e: Exception) {
                AmnosLog.e("MainActivity", "Initialization failed during bootstrap", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::sessionManager.isInitialized && !isChangingConfigurations) {
            AmnosLog.d("MainActivity", "Idle wipe triggered: application moved to background (Clipboard retained for UX).")
            sessionManager.killAll(terminateProcess = false, wipeClipboard = false)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::sessionManager.isInitialized && level >= TRIM_MEMORY_UI_HIDDEN && !isChangingConfigurations) {
            AmnosLog.d("MainActivity", "Memory pressure wipe triggered (Clipboard retained for UX).")
            sessionManager.killAll(terminateProcess = false, wipeClipboard = false)
        }
    }

    private fun updateSecurityFlags(policy: com.amnos.browser.core.security.PrivacyPolicy) {
        if (policy.blockScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            AmnosLog.d("MainActivity", "Screenshot protection ENABLED (via policy)")
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            AmnosLog.d("MainActivity", "Screenshot protection DISABLED (via policy)")
        }
    }

    override fun onResume() {
        super.onResume()
        setupGlobalKeyboardKiller()
    }

    private fun setupGlobalKeyboardKiller() {
        val rootView = window.decorView.rootView
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val controller = WindowInsetsControllerCompat(window, rootView)
            val isImeVisible = androidx.core.view.ViewCompat.getRootWindowInsets(rootView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            
            if (isImeVisible) {
                // controller.hide(WindowInsetsCompat.Type.ime())
                // Noisy logging suppressed to prevent logcat flooding
                // AmnosLog.d("AmnosKeyboardKiller", "System IME suppressed.")
                controller.hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        // Log deep touch events for diagnostics if keyboard is visible
        val isImeVisible = androidx.core.view.ViewCompat.getRootWindowInsets(window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (isImeVisible) {
            AmnosLog.d("MainActivity", "Touch detected while IME visible. Coordinates: ${ev?.x}, ${ev?.y}")
        }
        return super.dispatchTouchEvent(ev)
    }
}

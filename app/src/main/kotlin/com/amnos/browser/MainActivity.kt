package com.amnos.browser

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.KeyManager
import com.amnos.browser.core.session.AmnosLog
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import com.amnos.browser.core.session.RuntimeSecurityConfig
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.ui.screens.browser.BrowserScreen
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import com.amnos.browser.ui.theme.PrivacyBrowserTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    companion object {
        private const val BACKGROUND_WIPE_GRACE_MS = 10_000L
    }

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: BrowserViewModel
    private var isInitialized by mutableStateOf(false)
    private var previousUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var keyboardGuardRootView: View? = null
    private var keyboardGuardListener: android.view.ViewTreeObserver.OnGlobalLayoutListener? = null
    private var pendingLaunchRequest: String? = null
    private val lifecycleHandler = Handler(Looper.getMainLooper())
    private val delayedGhostWipe = Runnable {
        if (::sessionManager.isInitialized && !isChangingConfigurations) {
            AmnosLog.d("MainActivity", "Ghost wipe grace period elapsed.")
            sessionManager.killAll(terminateProcess = false, wipeClipboard = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installCrashHandler()

        try {
            // Start Ghost Sentinel Service for Task Removal Detection (if enabled)
            val sessionManagerForStartup = SessionManager.getInstance(this, RuntimeSecurityConfig.webViewProfileSuffix)
            if (sessionManagerForStartup.privacyPolicy.absoluteCloakingEnabled) {
                startService(Intent(this, com.amnos.browser.core.service.GhostService::class.java))
            }

            android.webkit.WebView.setDataDirectorySuffix(RuntimeSecurityConfig.webViewProfileSuffix)
            AmnosLog.d("MainActivity", "WebView data directory suffix set successfully")
        } catch (e: Exception) {
            AmnosLog.w("MainActivity", "WebView suffix already set or failed: ${e.message}")
        }

        super.onCreate(savedInstanceState)
        pendingLaunchRequest = extractNavigationRequest(intent)
        AmnosLog.d("MainActivity", "onCreate: Initializing Amnos UI")

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

        lifecycleScope.launch {
            try {
                // Phase 1 INTEGRITY CHECK
                KeyManager.checkIntegrity(this@MainActivity)
                
                withContext(Dispatchers.IO) {
                    AmnosLog.d("MainActivity", "Initializing SessionManager (Singleton)")
                    sessionManager = SessionManager.getInstance(this@MainActivity, RuntimeSecurityConfig.webViewProfileSuffix)
                }

                if (com.amnos.browser.core.security.RootDetector.isRooted(this@MainActivity)) {
                    sessionManager.securityController.logInternal("SystemHealth", "SECURITY WARNING: Root access detected.", "ERROR")
                    sessionManager.setFingerprintProtectionLevel(com.amnos.browser.core.security.FingerprintProtectionLevel.STRICT)
                }

                updateSecurityFlags(sessionManager.privacyPolicy)
                android.webkit.WebView.setWebContentsDebuggingEnabled(sessionManager.privacyPolicy.enableRemoteDebugging)

                AmnosLog.d("MainActivity", "Creating BrowserViewModel (Main)")
                viewModel = BrowserViewModel(sessionManager)

                sessionManager.registerWipeListener {
                    if (sessionManager.privacyPolicy.absoluteCloakingEnabled) {
                        AmnosLog.d("MainActivity", "Session wipe triggered. Ghosting activity.")
                        finishAffinity() // Clear all activities from the Task Manager
                    }
                }

                isInitialized = true
                investigateAccessibilityServices()
                consumePendingLaunchRequest()
                AmnosLog.d("MainActivity", "Amnos Bootstrap Complete")
            } catch (e: Exception) {
                AmnosLog.e("MainActivity", "Initialization failed during bootstrap", e)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        AmnosLog.d("MainActivity", "onStart: UI visible")
    }

    override fun onStop() {
        super.onStop()
        AmnosLog.d("MainActivity", "onStop: UI hidden")
        if (::sessionManager.isInitialized && !isChangingConfigurations) {
            scheduleGhostWipe("Application backgrounded (Pure RAM mode enabled).")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::sessionManager.isInitialized && level >= TRIM_MEMORY_UI_HIDDEN && !isChangingConfigurations) {
            scheduleGhostWipe("Memory pressure UI-hidden event received.")
        }
    }

    private fun updateSecurityFlags(policy: PrivacyPolicy) {
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
        AmnosLog.d("MainActivity", "onResume: Session active")
        cancelPendingGhostWipe()
        setupGlobalKeyboardKiller()
    }

    override fun onPause() {
        super.onPause()
        AmnosLog.d("MainActivity", "onPause: Session backgrounded")
        teardownGlobalKeyboardKiller()
    }

    override fun onDestroy() {
        cancelPendingGhostWipe()
        teardownGlobalKeyboardKiller()
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(previousUncaughtExceptionHandler)
        }
        super.onDestroy()
    }

    private fun setupGlobalKeyboardKiller() {
        if (keyboardGuardListener != null) {
            return
        }

        val rootView = window.decorView.rootView
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val controller = WindowInsetsControllerCompat(window, rootView)
            val isImeVisible = androidx.core.view.ViewCompat.getRootWindowInsets(rootView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true

            if (isImeVisible) {
                controller.hide(WindowInsetsCompat.Type.ime())
            }
        }

        keyboardGuardRootView = rootView
        keyboardGuardListener = listener
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun teardownGlobalKeyboardKiller() {
        val rootView = keyboardGuardRootView
        val listener = keyboardGuardListener
        if (rootView != null && listener != null && rootView.viewTreeObserver.isAlive) {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
        keyboardGuardRootView = null
        keyboardGuardListener = null
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (::sessionManager.isInitialized && !sessionManager.privacyPolicy.blockForensicLogging) {
            val isImeVisible = androidx.core.view.ViewCompat.getRootWindowInsets(window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (isImeVisible) {
                AmnosLog.d("MainActivity", "DEBUG TOUCH while IME visible: ${ev?.x}, ${ev?.y}")
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun installCrashHandler() {
        previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val request = extractNavigationRequest(intent) ?: return
        pendingLaunchRequest = request
        AmnosLog.d("MainActivity", "Received external navigation request")
        consumePendingLaunchRequest()
    }

    private fun consumePendingLaunchRequest() {
        val request = pendingLaunchRequest ?: return
        if (!isInitialized || !::viewModel.isInitialized) {
            return
        }

        pendingLaunchRequest = null
        viewModel.navigate(request)
    }

    private fun investigateAccessibilityServices() {
        val am = getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        val active = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (active.isNotEmpty()) {
            val names = active.joinToString { it.resolveInfo.serviceInfo.name }
            sessionManager.securityController.logInternal("SystemHealth", "SANDBOX WARNING: Active Accessibility Services detected: $names. These services can scrape screen content.", "WARN")
        }
    }

    private fun extractNavigationRequest(intent: Intent?): String? {
        intent ?: return null
        
        // V2 SANDBOX GATING: If in PARANOID mode, block ALL inbound intents by default
        if (::sessionManager.isInitialized && sessionManager.privacyPolicy.sandboxMode == com.amnos.browser.core.security.AmnosSandboxMode.PARANOID) {
            AmnosLog.w("MainActivity", "INBOUND INTENT BLOCKED: Paranoid Sandbox Mode is ACTIVE.")
            return null
        }

        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val rawString = intent.dataString?.takeIf { it.isNotBlank() } ?: return null
                try {
                    // INBOUND INTENT JAIL: Strip tracking queries and fragments from external apps
                    val uri = android.net.Uri.parse(rawString)
                    if (uri.scheme?.lowercase() in listOf("http", "https")) {
                        android.net.Uri.Builder()
                            .scheme(uri.scheme)
                            .authority(uri.authority)
                            .path(uri.path)
                            .build()
                            .toString()
                    } else {
                        null // Drop non-http/https intents completely
                    }
                } catch (e: Exception) {
                    null
                }
            }
            Intent.ACTION_WEB_SEARCH -> intent.getStringExtra(SearchManager.QUERY)?.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun scheduleGhostWipe(reason: String) {
        cancelPendingGhostWipe()
        AmnosLog.d("MainActivity", "Ghost wipe scheduled in ${BACKGROUND_WIPE_GRACE_MS}ms. Reason: $reason")
        lifecycleHandler.postDelayed(delayedGhostWipe, BACKGROUND_WIPE_GRACE_MS)
    }

    private fun cancelPendingGhostWipe() {
        lifecycleHandler.removeCallbacks(delayedGhostWipe)
    }

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
        runCatching {
            if (::sessionManager.isInitialized) {
                sessionManager.killAll(terminateProcess = false, wipeClipboard = true)
            } else {
                com.amnos.browser.core.security.ClipboardSentinel.wipe(this)
            }
        }
        AmnosLog.e("Amnos", "Fatal crash in ${thread.name}. Delegating to system handler.", throwable)
        previousUncaughtExceptionHandler?.uncaughtException(thread, throwable) ?: run {
            finishAffinity()
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}

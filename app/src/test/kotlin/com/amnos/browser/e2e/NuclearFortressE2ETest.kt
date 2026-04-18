package com.amnos.browser.e2e

import android.content.Context
import android.os.Build
import com.amnos.browser.core.security.FirewallLevel
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.security.FingerprintProtectionLevel
import com.amnos.browser.core.session.SessionManager
import com.amnos.browser.core.network.BlockReason
import com.amnos.browser.ui.screens.browser.BrowserViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.amnos.browser.core.webview.AmnosWebView

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE], manifest = Config.NONE)
class NuclearFortressE2ETest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: BrowserViewModel
    private var lastLoadedUrl: String? = null

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        // --- ADVANCED: Reset SessionManager Singleton via Reflection for Test Isolation ---
        val instanceField = SessionManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)

        // --- PURE SIMULATED WEBVIEW FACTORY ---
        // We provide a pure interface implementation to avoid ANY native WebView dependencies
        val testFactory: (Context) -> AmnosWebView = { ctx ->
            object : AmnosWebView {
                private var currentUrl: String? = null
                private var mWebViewClient: WebViewClient = WebViewClient()
                private var mWebChromeClient: WebChromeClient? = null
                
                override fun getWebViewClient(): WebViewClient = mWebViewClient
                override fun setWebViewClient(client: WebViewClient) { mWebViewClient = client }
                override fun getWebChromeClient(): WebChromeClient? = mWebChromeClient
                override fun setWebChromeClient(client: WebChromeClient?) { mWebChromeClient = client }
                
                override fun injectInput(text: String) {}
                override fun injectBackspace() {}
                override fun injectSearch() {}
                
                override fun getContext(): Context = ctx
                override fun getUrl(): String? = currentUrl
                override fun loadUrl(url: String) { currentUrl = url; lastLoadedUrl = url }
                override fun loadUrl(url: String, headers: Map<String, String>) { currentUrl = url; lastLoadedUrl = url }
                override fun stopLoading() {}
                override fun reload() {}
                override fun clearHistory() {}
                override fun canGoBack(): Boolean = false
                override fun canGoForward(): Boolean = false
                override fun goBack() {}
                override fun goForward() {}
                override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean = true
                override fun pauseTimers() {}
                override fun resumeTimers() {}
                override fun destroy() {}
                override fun asView(): android.view.View = android.view.View(ctx)
                
                override fun applyHardening(p: DeviceProfile, pol: PrivacyPolicy, s: String, e: (String) -> Unit) {}
                override fun updateRuntimePolicy(p: DeviceProfile, pol: PrivacyPolicy, s: String, e: (String) -> Unit) {}
                override fun surgicalTeardown() {}
                override fun clearVolatileState() {}
            }
        }

        sessionManager = SessionManager.getInstance(context, "E2E_Test_Profile", testFactory)
        viewModel = BrowserViewModel(sessionManager)
    }

    @Test
    fun fullNuclearFortressLifecycle_EndToEnd() {
        // --- SCENARIO 1: IDENTITY GENERATION ---
        val initialSessionId = sessionManager.sessionId
        assertNotNull("SessionId must be initialized", initialSessionId)
        
        val tab = viewModel.currentTab.value
        assertNotNull("Default tab must be created on init", tab)
        assertEquals(FingerprintProtectionLevel.STRICT, viewModel.privacyPolicy.value.hardwareFingerprintLevel)

        // --- SCENARIO 2: SMART NAVIGATION & SANITIZATION (YOU-TUE FIX) ---
        // Test that 'youtue' redirects to search
        viewModel.navigate("youtue")
        assertEquals("https://duckduckgo.com/?q=youtue", lastLoadedUrl)
        
        // Test standard link hydration
        viewModel.navigate("google.com")
        assertEquals("https://google.com", lastLoadedUrl)

        // --- SCENARIO 3: FIREWALL ENFORCEMENT ---
        viewModel.setFirewallLevel(FirewallLevel.PARANOID)
        
        // Navigation to an unknown domain should be blocked
        val blockedUrl = "https://untrusted-forensic-tracking.org"
        val success = sessionManager.loadUrl(tab!!, blockedUrl)
        assertFalse("Firewall must block unknown domains in PARANOID", success)
        
        // Switch back to BALANCED mode
        viewModel.setFirewallLevel(FirewallLevel.BALANCED)
        val successBalanced = sessionManager.loadUrl(tab, "https://wikipedia.org")
        assertTrue("Firewall should allow domains in BALANCED", successBalanced)
        assertEquals("https://wikipedia.org", lastLoadedUrl)

        // --- SCENARIO 4: SITE ISOLATION ---
        sessionManager.loadUrl(tab, "https://google.com")
        val isIsolated = sessionManager.shouldRecreateForTopLevelNavigation(tab, "https://facebook.com")
        assertTrue("Cross-site navigation must trigger isolation", isIsolated)

        // --- SCENARIO 5: FORENSIC WIPE (THE BURN) ---
        val preWipeSessionId = sessionManager.sessionId
        
        // Trigger a background wipe simulation
        viewModel.killSwitch()
        
        // Verify session rotation
        val postWipeSessionId = sessionManager.sessionId
        assertNotEquals("Session ID must be rotated after a forensic wipe", preWipeSessionId, postWipeSessionId)
        
        // Verify UI reset
        assertEquals("", viewModel.urlInput.value)
        assertNull("Tabs must be cleared after wipe", viewModel.currentTab.value)
    }
}

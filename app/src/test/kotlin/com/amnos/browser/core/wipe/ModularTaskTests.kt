package com.amnos.browser.core.wipe

import android.content.Context
import android.os.Build
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.session.TabManager
import com.amnos.browser.core.wipe.tasks.CryptographicTask
import com.amnos.browser.core.wipe.tasks.WebViewTeardownTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ModularTaskTests {

    private lateinit var context: Context

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `test CryptographicTask execution`() = runBlocking {
        ShadowLog.clear()
        val task = CryptographicTask(context)
        val result = task.execute()
        
        assertTrue("Task should succeed", result.isSuccess)
        val logs = ShadowLog.getLogsForTag("WipeTasks")
        assertTrue("Should log key obliteration", logs.any { it.msg.contains("Keys obliterated") })
    }

    @Test
    fun `test WebViewTeardownTask execution`() = runBlocking {
        ShadowLog.clear()
        val adBlocker = AdBlocker(context)
        val nsm = NetworkSecurityManager(adBlocker) { PrivacyPolicy() }
        val tabManager = TabManager(context, adBlocker, nsm, SecurityController(), { null!! })
        val task = WebViewTeardownTask(tabManager)
        val result = task.execute()
        
        assertTrue("Task should succeed", result.isSuccess)
        val logs = ShadowLog.getLogsForTag("WipeTasks")
        assertTrue("Should log tab clearing", logs.any { it.msg.contains("Tabs cleared") })
    }
}

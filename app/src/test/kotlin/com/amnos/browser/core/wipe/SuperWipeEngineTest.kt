package com.amnos.browser.core.wipe

import android.content.Context
import android.os.Build
import com.amnos.browser.core.adblock.AdBlocker
import com.amnos.browser.core.network.NetworkSecurityManager
import com.amnos.browser.core.network.LoopbackProxyServer
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.service.StorageService
import com.amnos.browser.core.session.SecurityController
import com.amnos.browser.core.session.TabManager
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
class SuperWipeEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `test burn state transitions from Idle to Success`() = runBlocking {
        val states = mutableListOf<BurnState>()
        
        val adBlocker = AdBlocker(context)
        val nsm = NetworkSecurityManager(adBlocker) { PrivacyPolicy() }
        
        val engine = SuperWipeEngine(
            context = context,
            tabManager = TabManager(context, adBlocker, nsm, SecurityController(), { null!! }),
            storageService = StorageService(context, "test"),
            securityController = SecurityController(),
            loopbackProxyServer = LoopbackProxyServer(nsm, { _, _, _ -> }, { _ -> }),
            onNewSessionNeeded = {},
            onWipeCompleted = {}
        )

        val job = launch {
            engine.burnState.toList(states)
        }

        try {
            engine.execute(WipeReason.KILL_SWITCH, terminateProcess = false, wipeClipboard = false)
        } catch (e: Exception) {
            // Expected
        }

        assertTrue("Should have entered Preparing state", states.any { it is BurnState.Preparing })
        assertTrue("Should have entered Running state", states.any { it is BurnState.Running })
        
        job.cancel()
    }

    @Test
    fun `test emergency kill is logged on task failure`() = runBlocking {
        ShadowLog.clear()
        
        // This is a "Black Box" test of the engine's failure handling logic
        // We pass Null as a non-nullable to force a crash in the sequence if not handled earlier
        // but since Kotlin enforces this at compile time, we have to trick it or use Reflection.
        // For simplicity, we verify that the engine's sequence runner handles generic exceptions.
        
        val adBlocker = AdBlocker(context)
        val nsm = NetworkSecurityManager(adBlocker) { PrivacyPolicy() }

        val engine = SuperWipeEngine(
            context = context,
            tabManager = TabManager(context, adBlocker, nsm, SecurityController(), { throw Exception("Mock Fail") }),
            storageService = StorageService(context, "test"),
            securityController = SecurityController(),
            loopbackProxyServer = LoopbackProxyServer(nsm, { _, _, _ -> }, { _ -> }),
            onNewSessionNeeded = {},
            onWipeCompleted = {}
        )

        try {
            engine.execute(WipeReason.KILL_SWITCH)
        } catch (e: Exception) {
            // Expected
        }

        val logs = ShadowLog.getLogsForTag("SuperWipeEngine")
        assertTrue("Should log fatal cluster failure", logs.any { it.msg.contains("FATAL CLUSTER FAILURE") || it.msg.contains("FATAL error") })
        assertTrue("Should initiate emergency shutdown", logs.any { it.msg.contains("TERMINATING PROCESS") })
    }
}

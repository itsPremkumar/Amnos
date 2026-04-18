package com.amnos.browser.core.wipe.tasks

import com.amnos.browser.core.network.DnsManager
import com.amnos.browser.core.network.LoopbackProxyServer
import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.wipe.WipeTask

/**
 * Task responsible for rotating the network identity, restarting the proxy server,
 * and refreshing DNS resolution.
 * This is Phase 4 of the purge sequence.
 */
class NetworkRotationTask(private val loopbackProxyServer: LoopbackProxyServer) : WipeTask {
    override val name: String = "Network Identity Rotation"

    override suspend fun execute(): Result<Unit> = runCatching {
        AmnosLog.d("WipeTasks", "Stopping proxy and clearing DNS cache...")
        loopbackProxyServer.stop()
        DnsManager.destroyAndRebuild()
        AmnosLog.i("WipeTasks", "✔ Network identity rotated.")
    }
}

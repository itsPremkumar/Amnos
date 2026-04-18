package com.amnos.browser.core.wipe.tasks

import com.amnos.browser.core.session.AmnosLog
import com.amnos.browser.core.session.TabManager
import com.amnos.browser.core.wipe.WipeTask

/**
 * Task responsible for destroying all active WebViews and clearing the tab list.
 * This is Phase 1 of the purge sequence.
 */
class WebViewTeardownTask(private val tabManager: TabManager) : WipeTask {
    override val name: String = "WebView Teardown"

    override suspend fun execute(): Result<Unit> = runCatching {
        val count = tabManager.getTabs().size
        AmnosLog.d("WipeTasks", "Tearing down $count active tabs...")
        tabManager.clearAll()
        AmnosLog.i("WipeTasks", "✔ Tabs cleared.")
    }
}

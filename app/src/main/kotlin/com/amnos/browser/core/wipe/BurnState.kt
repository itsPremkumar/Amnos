package com.amnos.browser.core.wipe

/**
 * Represents the lifecycle and progress of a Burn Session (Purge Sequence).
 */
sealed class BurnState {
    object Idle : BurnState()
    object Preparing : BurnState()
    
    /**
     * Currently executing a specific wipe phase.
     */
    data class Running(val taskName: String) : BurnState()
    
    object Completing : BurnState()
    object Success : BurnState()
    
    /**
     * A task failed during execution.
     * @param error The exception that caused the failure.
     * @param taskName The name of the task that failed.
     */
    data class Failed(val error: Throwable, val taskName: String) : BurnState()
}

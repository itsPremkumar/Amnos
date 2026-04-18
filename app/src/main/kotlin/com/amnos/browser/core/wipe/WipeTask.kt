package com.amnos.browser.core.wipe

/**
 * Standard interface for a modular purge operation.
 * Each implementation represents one phase of the "Burn" sequence.
 */
interface WipeTask {
    /**
     * Human-readable name of the task, used for logging and UI status.
     */
    val name: String

    /**
     * Executes the specific wipe logic.
     * @return Result.Success if completed, or Result.Failure if an error occurred.
     */
    suspend fun execute(): Result<Unit>
}

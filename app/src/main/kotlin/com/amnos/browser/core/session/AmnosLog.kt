package com.amnos.browser.core.session

import com.amnos.browser.BuildConfig
import android.util.Log

object AmnosLog {
    @Volatile
    private var controllerProvider: (() -> SecurityController?)? = null
    @Volatile
    private var systemLoggingAllowed: Boolean = !BuildConfig.DEBUG_BLOCK_FORENSIC_LOGGING

    fun attach(provider: () -> SecurityController?) {
        controllerProvider = provider
    }

    fun detach() {
        controllerProvider = null
    }

    fun v(tag: String, message: String) = log(tag, message, "VERBOSE")

    fun d(tag: String, message: String) = log(tag, message, "DEBUG")

    fun i(tag: String, message: String) = log(tag, message, "INFO")

    fun w(tag: String, message: String, throwable: Throwable? = null) = log(tag, message, "WARN", throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) = log(tag, message, "ERROR", throwable)

    fun setSystemLoggingAllowed(allowed: Boolean) {
        systemLoggingAllowed = allowed
    }

    private fun log(tag: String, message: String, level: String, throwable: Throwable? = null) {
        val threadInfo = "[${Thread.currentThread().name}]"
        val finalMessage = if (throwable != null) {
            "$message (${throwable.javaClass.simpleName}: ${throwable.message ?: "no message"})"
        } else {
            message
        }

        try {
            controllerProvider?.invoke()?.let { controller ->
                controller.logInternal(tag, "$threadInfo $finalMessage", level)
            } ?: run {
                printFallback(tag, "$threadInfo $finalMessage", level, throwable)
            }
        } catch (e: Throwable) {
            printFallback(tag, "$threadInfo $finalMessage (LOG_FAILURE: ${e.message})", level, throwable)
        }
    }

    private fun printFallback(tag: String, message: String, level: String, throwable: Throwable?) {
        // AMNOS EMERGENCY BYPASS: Always allow ERROR/FATAL logs to reach system console 
        // regardless of policy to ensure crashes are visible for diagnostics.
        val isEmergency = level == "ERROR" || message.contains("FATAL", true) || message.contains("CRITICAL", true)
        
        if (!systemLoggingAllowed && !isEmergency) {
            return
        }

        try {
            val priority = when (level) {
                "VERBOSE" -> Log.VERBOSE
                "DEBUG" -> Log.DEBUG
                "WARN" -> Log.WARN
                "ERROR" -> Log.ERROR
                else -> Log.INFO
            }
            Log.println(priority, tag, message)
            if (throwable != null && priority >= Log.WARN) {
                Log.println(priority, tag, Log.getStackTraceString(throwable))
            }
        } catch (_: Throwable) {
            val line = "[$level][$tag] $message"
            if (level == "ERROR" || level == "WARN") {
                System.err.println(line)
            } else {
                System.out.println(line)
            }
        }
    }
}

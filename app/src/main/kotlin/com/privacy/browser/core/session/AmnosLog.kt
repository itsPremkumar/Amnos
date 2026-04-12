package com.privacy.browser.core.session

import android.util.Log

object AmnosLog {
    @Volatile
    private var controllerProvider: (() -> SecurityController?)? = null

    fun attach(provider: () -> SecurityController?) {
        controllerProvider = provider
    }

    fun detach() {
        controllerProvider = null
    }

    fun d(tag: String, message: String) = log(tag, message, "DEBUG")

    fun i(tag: String, message: String) = log(tag, message, "INFO")

    fun w(tag: String, message: String, throwable: Throwable? = null) = log(tag, message, "WARN", throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) = log(tag, message, "ERROR", throwable)

    private fun log(tag: String, message: String, level: String, throwable: Throwable? = null) {
        val finalMessage = if (throwable != null) {
            "$message (${throwable.javaClass.simpleName}: ${throwable.message ?: "no message"})"
        } else {
            message
        }

        try {
            controllerProvider?.invoke()?.logInternal(tag, finalMessage, level) ?: run {
                printFallback(tag, finalMessage, level, throwable)
            }
        } catch (e: Throwable) {
            // Defensive catch to ensure logging never crashes the app
            printFallback(tag, "$finalMessage (LOG_FAILURE: ${e.message})", level, throwable)
        }
    }

    private fun printFallback(tag: String, message: String, level: String, throwable: Throwable?) {
        try {
            val priority = when (level) {
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

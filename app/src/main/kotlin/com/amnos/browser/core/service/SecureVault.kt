package com.amnos.browser.core.service

import android.content.Context
import android.content.SharedPreferences
import com.amnos.browser.core.security.KeyManager

class SecureVault(
    private val context: android.content.Context,
    private val suffix: String
) {
    private val memoryMap = java.util.concurrent.ConcurrentHashMap<String, Any>()

    fun putString(key: String, value: String) {
        memoryMap[key] = value
    }

    fun getString(key: String, default: String? = null): String? {
        return memoryMap[key] as? String ?: default
    }

    fun clear() {
        memoryMap.clear()
    }
}

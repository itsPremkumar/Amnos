package com.amnos.browser.core.service

import android.content.Context
import android.content.SharedPreferences
import com.amnos.browser.core.security.KeyManager

class SecureVault(
    private val context: Context,
    private val suffix: String
) {
    val prefs: SharedPreferences by lazy {
        KeyManager.getEncryptedSharedPreferences(context, "amnos_secure_prefs_$suffix")
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

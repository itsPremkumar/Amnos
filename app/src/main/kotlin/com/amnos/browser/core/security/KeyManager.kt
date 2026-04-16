package com.amnos.browser.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.amnos.browser.core.session.AmnosLog
import java.security.KeyStore
import javax.crypto.KeyGenerator

object KeyManager {

    private const val SESSION_KEY_ALIAS = "Amnos_Session_Master_Key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Generates a new AES-256-GCM key inside the Android Keystore.
     * Where available, this key is strictly bound to the StrongBox TEE.
     */
    fun generateSessionKey(context: Context) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            // If a key already exists (e.g. from an improper shutdown), destroy it first to guarantee ephemeral zero-reuse state
            if (keyStore.containsAlias(SESSION_KEY_ALIAS)) {
                AmnosLog.w("KeyManager", "Stale session key found. Destroying before regenerating.")
                obliterateKey()
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

            val specBuilder = KeyGenParameterSpec.Builder(
                SESSION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            // Enforce hardware protection if available
            val hasStrongBox = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)
            if (hasStrongBox) {
                try {
                    specBuilder.setIsStrongBoxBacked(true)
                } catch (e: Exception) {
                    AmnosLog.w("KeyManager", "StrongBox requested but unavailable during key generation", e)
                }
            }

            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
            AmnosLog.i("KeyManager", "Session Master Key generated securely" + if (hasStrongBox) " [StrongBox Enforced]" else "")

        } catch (e: Exception) {
            AmnosLog.e("KeyManager", "FATAL: Failed to generate Session Master Key", e)
        }
    }

    /**
     * The Cryptographic Kill Switch.
     * Instantly deletes the key material from hardware. Downstream ciphertext becomes permanently undecryptable.
     */
    fun obliterateKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(SESSION_KEY_ALIAS)) {
                keyStore.deleteEntry(SESSION_KEY_ALIAS)
                AmnosLog.w("KeyManager", "CRITICAL: Session Master Key destroyed (Cryptographic Kill Switch engaged)")
            }
        } catch (e: Exception) {
            AmnosLog.e("KeyManager", "WARNING: Exception during Keystore obliteration", e)
        }
    }

    /**
     * Resolves Jetpack's EncryptedSharedPreferences leveraging the SessionMasterKey.
     */
    fun getEncryptedSharedPreferences(context: Context, filename: String): SharedPreferences {
        try {
            // Reconstruct the MasterKey builder using the strictly defined alias
            val masterKey = MasterKey.Builder(context, SESSION_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                filename,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            AmnosLog.e("KeyManager", "Failed to construct EncryptedSharedPreferences. Falling back to ram-only dummy if possible, or failing.", e)
            throw RuntimeException("Secure storage unavailable", e)
        }
    }
}

package com.pravor.notessharing.data.kaya

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class KayaCredentials(
    val username: String,
    val password: String,
    val savedAt: Long = System.currentTimeMillis()
)

interface KayaCredentialStore {
    fun saveCredentials(userId: String, username: String, password: String): Boolean
    fun getCredentials(userId: String): KayaCredentials?
    fun hasCredentials(userId: String): Boolean
    fun clearCredentials(userId: String)
}

class AndroidKeyStoreKayaCredentialStore(private val context: Context) : KayaCredentialStore {

    companion object {
        private const val TAG = "KayaCredentialStore"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_PREFIX = "kaya_credentials_"
        private const val KEY_ALIAS_PREFIX = "kaya_keystore_key_"

        private const val PREF_KEY_USERNAME = "username"
        private const val PREF_KEY_ENC_PASSWORD = "enc_password"
        private const val PREF_KEY_SAVED_AT = "saved_at"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    private fun getAlias(userId: String): String = "$KEY_ALIAS_PREFIX$userId"

    private fun getPrefs(userId: String) =
        context.getSharedPreferences("$PREFS_PREFIX$userId", Context.MODE_PRIVATE)

    @Synchronized
    private fun getOrCreateSecretKey(alias: String): SecretKey? {
        return try {
            if (keyStore.containsAlias(alias)) {
                val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve or generate AndroidKeyStore key", e)
            null
        }
    }

    override fun saveCredentials(userId: String, username: String, password: String): Boolean {
        if (userId.isBlank() || username.isBlank() || password.isBlank()) {
            return false
        }

        val alias = getAlias(userId)
        val secretKey = getOrCreateSecretKey(alias) ?: return false

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val payload = "$ivBase64:$cipherBase64"

            getPrefs(userId).edit()
                .putString(PREF_KEY_USERNAME, username.trim())
                .putString(PREF_KEY_ENC_PASSWORD, payload)
                .putLong(PREF_KEY_SAVED_AT, System.currentTimeMillis())
                .apply()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely save credentials for user $userId", e)
            false
        }
    }

    override fun getCredentials(userId: String): KayaCredentials? {
        if (userId.isBlank()) return null
        val prefs = getPrefs(userId)
        val username = prefs.getString(PREF_KEY_USERNAME, null) ?: return null
        val encPayload = prefs.getString(PREF_KEY_ENC_PASSWORD, null) ?: return null
        val savedAt = prefs.getLong(PREF_KEY_SAVED_AT, 0L)

        val parts = encPayload.split(":")
        if (parts.size != 2) return null

        val alias = getAlias(userId)
        val secretKey = getOrCreateSecretKey(alias) ?: return null

        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val password = String(decryptedBytes, Charsets.UTF_8)

            KayaCredentials(
                username = username,
                password = password,
                savedAt = savedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt credentials for user $userId", e)
            null
        }
    }

    override fun hasCredentials(userId: String): Boolean {
        if (userId.isBlank()) return false
        val prefs = getPrefs(userId)
        return !prefs.getString(PREF_KEY_USERNAME, null).isNullOrBlank() &&
                !prefs.getString(PREF_KEY_ENC_PASSWORD, null).isNullOrBlank()
    }

    override fun clearCredentials(userId: String) {
        if (userId.isBlank()) return
        try {
            getPrefs(userId).edit().clear().apply()
            val alias = getAlias(userId)
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to completely clear credentials/key for user $userId", e)
        }
    }
}

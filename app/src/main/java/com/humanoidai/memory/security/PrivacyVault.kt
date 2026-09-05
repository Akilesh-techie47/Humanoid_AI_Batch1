package com.humanoidai.memory.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Advanced security vault for Humanoid AI.
 * Handles AES-256-GCM encryption for embeddings and SQLCipher key management.
 */
class PrivacyVault(private val context: Context) {

    companion object {
        private const val MASTER_KEY_ALIAS  = "humanoid_ai_master_key"
        private const val DATA_KEY_ALIAS    = "humanoid_ai_data_key"
        private const val DB_KEY_ALIAS      = "humanoid_db_key_v2"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val ALGORITHM         = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE        = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING           = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION    = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH    = 128
        private const val PREFS_FILE        = "humanoid_secure_prefs"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Retrieves the database passphrase. 
     * Uses Base64 to safely store/retrieve random bytes as a String.
     */
    fun getDatabasePassphrase(): ByteArray {
        val stored = securePrefs.getString(DB_KEY_ALIAS, null)
        if (stored != null) {
            return Base64.decode(stored, Base64.NO_WRAP)
        }
        val passphrase = generateRandomBytes(32)
        securePrefs.edit()
            .putString(DB_KEY_ALIAS, Base64.encodeToString(passphrase, Base64.NO_WRAP))
            .apply()
        return passphrase
    }

    fun encrypt(plaintext: String): String {
        val key = getOrCreateAesKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String): String {
        val key = getOrCreateAesKey()
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun encryptEmbedding(embedding: FloatArray): String {
        return encrypt(embedding.joinToString(","))
    }

    fun decryptEmbedding(encrypted: String): FloatArray {
        return decrypt(encrypted).split(",").map { it.toFloat() }.toFloatArray()
    }

    fun wipeAll() {
        securePrefs.edit().clear().apply()
        deleteAesKey()
    }

    private fun getOrCreateAesKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        if (keyStore.containsAlias(DATA_KEY_ALIAS)) {
            val entry = keyStore.getEntry(DATA_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }
        
        val keyGen = KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(DATA_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return keyGen.generateKey()
    }

    private fun deleteAesKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        keyStore.deleteEntry(DATA_KEY_ALIAS)
    }

    private fun generateRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes
    }
}

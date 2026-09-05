package com.humanoidai.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Secure password hashing utility using PBKDF2.
 */
object PasswordHasher {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hashPassword(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    fun verifyPassword(password: CharArray, salt: String, expectedHash: String): Boolean {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val actualHash = hashPassword(password, saltBytes)
        return actualHash == expectedHash
    }
}

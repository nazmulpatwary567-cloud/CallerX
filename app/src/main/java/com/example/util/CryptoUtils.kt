package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

    private val secureRandom = SecureRandom()

    fun generateSaltBase64(): String {
        val bytes = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hashPinSha256(pin: String, saltBase64: String): String {
        val saltBytes = Base64.getDecoder().decode(saltBase64)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashed)
    }

    fun verifyPinSha256(pin: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val candidate = hashPinSha256(pin, saltBase64).toByteArray(Charsets.UTF_8)
        val expected = expectedHashBase64.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidate, expected)
    }

    fun deriveAesKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Format: [4-byte magic "CLRX"][16-byte salt][12-byte IV][AES-256-GCM ciphertext+tag]
     */
    fun encryptAes256Gcm(plaintext: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveAesKey(password, salt)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val magic = "CLRX".toByteArray(Charsets.US_ASCII)
        return ByteArray(magic.size + salt.size + iv.size + ciphertext.size).apply {
            var offset = 0
            System.arraycopy(magic, 0, this, offset, magic.size); offset += magic.size
            System.arraycopy(salt, 0, this, offset, salt.size); offset += salt.size
            System.arraycopy(iv, 0, this, offset, iv.size); offset += iv.size
            System.arraycopy(ciphertext, 0, this, offset, ciphertext.size)
        }
    }

    fun decryptAes256Gcm(payload: ByteArray, password: String): ByteArray {
        val magicLen = 4
        require(payload.size > magicLen + SALT_BYTES + IV_BYTES + 16) {
            "Invalid encrypted CallerX backup file"
        }
        val magic = String(payload, 0, magicLen, Charsets.US_ASCII)
        require(magic == "CLRX") { "Unsupported backup file signature" }

        var offset = magicLen
        val salt = payload.copyOfRange(offset, offset + SALT_BYTES); offset += SALT_BYTES
        val iv = payload.copyOfRange(offset, offset + IV_BYTES); offset += IV_BYTES
        val ciphertext = payload.copyOfRange(offset, payload.size)

        val key = deriveAesKey(password, salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}

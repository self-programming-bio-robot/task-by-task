package dev.zhdanov.apps.composeApp.services

import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class JvmWorkspaceCryptoService : WorkspaceCryptoService {
    private val random = SecureRandom()
    private val base64 = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    override fun generateDataKey(): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    override fun generateSalt(): String = base64.encodeToString(ByteArray(SALT_BYTES).also(random::nextBytes))

    override fun wrapDataKey(pin: String, salt: String, iterations: Int, dataKey: ByteArray): String {
        return encryptBytes(dataKey, deriveKey(pin, salt, iterations))
    }

    override fun unwrapDataKey(pin: String, salt: String, iterations: Int, wrappedDataKey: String): ByteArray {
        return try {
            decryptBytes(wrappedDataKey, deriveKey(pin, salt, iterations))
        } catch (error: AEADBadTagException) {
            throw InvalidWorkspacePinException()
        } catch (error: IllegalArgumentException) {
            throw InvalidWorkspacePinException()
        } catch (error: InvalidKeySpecException) {
            throw InvalidWorkspacePinException()
        }
    }

    override fun encryptText(plainText: String, dataKey: ByteArray): String {
        return encryptBytes(plainText.encodeToByteArray(), SecretKeySpec(dataKey, "AES"))
    }

    override fun decryptText(cipherText: String, dataKey: ByteArray): String {
        return decryptBytes(cipherText, SecretKeySpec(dataKey, "AES")).decodeToString()
    }

    override fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    private fun deriveKey(pin: String, salt: String, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), decoder.decode(salt), iterations, KEY_BITS)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    private fun encryptBytes(plainBytes: ByteArray, key: SecretKeySpec): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainBytes)
        return "$PREFIX${base64.encodeToString(iv)}:${base64.encodeToString(encrypted)}"
    }

    private fun decryptBytes(envelope: String, key: SecretKeySpec): ByteArray {
        require(envelope.startsWith(PREFIX)) { "Unsupported encrypted value" }
        val parts = envelope.removePrefix(PREFIX).split(':')
        require(parts.size == 2) { "Malformed encrypted value" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, decoder.decode(parts[0])))
        return cipher.doFinal(decoder.decode(parts[1]))
    }

    companion object {
        private const val PREFIX = "enc:v1:"
        private const val KEY_BYTES = 32
        private const val KEY_BITS = KEY_BYTES * 8
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}

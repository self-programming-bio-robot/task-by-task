package dev.zhdanov.apps.composeApp.services

interface WorkspaceCryptoService {
    fun generateDataKey(): ByteArray
    fun generateSalt(): String
    fun wrapDataKey(pin: String, salt: String, iterations: Int, dataKey: ByteArray): String
    fun unwrapDataKey(pin: String, salt: String, iterations: Int, wrappedDataKey: String): ByteArray
    fun encryptText(plainText: String, dataKey: ByteArray): String
    fun decryptText(cipherText: String, dataKey: ByteArray): String
    fun isEncrypted(value: String): Boolean
}

class WorkspaceLockedException : IllegalStateException("Workspace is locked")
class InvalidWorkspacePinException : IllegalStateException("Invalid workspace PIN")

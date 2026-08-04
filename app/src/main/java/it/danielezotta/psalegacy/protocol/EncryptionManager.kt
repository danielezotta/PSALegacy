package it.danielezotta.psalegacy.protocol

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Keys are derived from the VIN once (on first incoming message) before worker threads use them;
 * publish via volatile vinBytes to establish a happens-before edge.
 * Do not call generateKeys concurrently with encrypt/decrypt.
 */
class EncryptionManager {
    val keyMessage = ByteArray(16)
    val keyChallenge = ByteArray(16)
    @Volatile private var vinBytes: ByteArray? = null
    var enabled = true
    val hasVin: Boolean get() = vinBytes != null

    fun generateKeys(vin: ByteArray): Boolean {
        if (vin.size != ProtocolConstants.VIN_SIZE) return false
        vinBytes = vin
        val keys = ProtocolConstants.KEYS
        for (i in 0 until 16) {
            val a = vin[16 - (i % 8)].toInt()
            val b = vin[16 - ((i + 1) % 8)].toInt()
            val v = a + (b % 10)
            keyMessage[i] = (v xor keys[17 + i].toInt()).toByte()
            keyChallenge[i] = (v xor keys[65 + i].toInt()).toByte()
        }
        return true
    }

    fun encrypt(data: ByteArray): ByteArray {
        val padded = padToBlock(data)
        if (!enabled || vinBytes == null) return padded
        return aes(padded, keyMessage, Cipher.ENCRYPT_MODE)
    }

    fun decrypt(data: ByteArray): ByteArray {
        if (enabled && vinBytes != null) {
            return aes(data, keyMessage, Cipher.DECRYPT_MODE)
        }
        return data
    }

    fun encryptChallenge(data: ByteArray): ByteArray = aes(data, keyChallenge, Cipher.ENCRYPT_MODE)

    fun decryptChallenge(data: ByteArray): ByteArray = aes(data, keyChallenge, Cipher.DECRYPT_MODE)

    private fun padToBlock(data: ByteArray): ByteArray {
        val remainder = data.size % 16
        if (remainder == 0) return data
        return data.copyOf(data.size + (16 - remainder))
    }

    private fun aes(data: ByteArray, key: ByteArray, mode: Int): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }
}

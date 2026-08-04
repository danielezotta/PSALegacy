package it.danielezotta.psalegacy.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder.BIG_ENDIAN

class FrameCodec(private val encryption: EncryptionManager) {

    fun encode(clearBytes: ByteArray): ByteArray {
        val encrypted = encryption.encrypt(clearBytes)
        val buffer = ByteBuffer.allocate(ProtocolConstants.WIRE_HEADER_SIZE + encrypted.size)
            .order(BIG_ENDIAN)
        buffer.putShort(ProtocolConstants.SMART_APP_START_IDENTIFIER)
        require(encrypted.size <= 0xFFFF) { "Encrypted payload too large: ${encrypted.size}" }
        buffer.putShort(encrypted.size.toShort())
        buffer.put(encrypted)
        buffer.put(checksumByte(buffer.array()))
        return buffer.array()
    }

    fun decode(frame: ByteArray): ByteArray {
        if (frame.size < ProtocolConstants.WIRE_HEADER_SIZE) {
            throw ProtocolException("Frame too short")
        }
        val buffer = ByteBuffer.wrap(frame).order(BIG_ENDIAN)
        if (buffer.short != ProtocolConstants.SMART_APP_START_IDENTIFIER) {
            throw ProtocolException("Bad frame start marker")
        }
        val length = buffer.short.toInt() and 0xFFFF
        val total = ProtocolConstants.WIRE_HEADER_SIZE + length
        if (frame.size != total) {
            throw ProtocolException("Bad frame length: expected $total got ${frame.size}")
        }
        if (!isValidChecksum(frame)) {
            throw ProtocolException("Bad frame checksum")
        }
        val encrypted = ByteArray(length)
        buffer.get(encrypted)
        // Returns the FULL AES-decrypted, zero-padded block (multiple of 16).
        // MessageCodec.decode reads exactly CLEAR_HEADER_SIZE + length bytes from the
        // front and ignores trailing padding — matching the original app.
        return try {
            encryption.decrypt(encrypted)
        } catch (e: Exception) {
            throw ProtocolException("Frame decrypt failed: ${e.message}")
        }
    }
}

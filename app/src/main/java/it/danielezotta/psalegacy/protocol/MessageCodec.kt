package it.danielezotta.psalegacy.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder.BIG_ENDIAN

object MessageCodec {

    fun encode(clear: ClearMessage): ByteArray {
        val payload = clear.payload.encode()
        val buffer = ByteBuffer.allocate(ProtocolConstants.CLEAR_HEADER_SIZE + payload.size)
            .order(BIG_ENDIAN)
        buffer.putShort(ProtocolConstants.CLEAR_START_IDENTIFIER)
        buffer.putShort(payload.size.toShort())
        buffer.putShort(ProtocolConstants.PROTOCOL_VERSION)
        buffer.putShort(clear.messageId)
        buffer.putShort(clear.sequence)
        buffer.put(payload)
        buffer.put(checksumByte(buffer.array()))
        return buffer.array()
    }

    fun decode(bytes: ByteArray): ClearMessage {
        if (bytes.size < ProtocolConstants.CLEAR_HEADER_SIZE) {
            throw ProtocolException("Clear message too short")
        }
        val buffer = ByteBuffer.wrap(bytes).order(BIG_ENDIAN)
        if (buffer.short != ProtocolConstants.CLEAR_START_IDENTIFIER) {
            throw ProtocolException("Bad clear start marker")
        }
        val length = buffer.short.toInt() and 0xFFFF
        val total = ProtocolConstants.CLEAR_HEADER_SIZE + length
        if (bytes.size < total) {
            throw ProtocolException("Bad clear length: expected >= $total got ${bytes.size}")
        }
        if (!isValidChecksum(bytes.copyOfRange(0, total))) {
            throw ProtocolException("Bad clear checksum")
        }
        if (buffer.short != ProtocolConstants.PROTOCOL_VERSION) {
            throw ProtocolException("Unsupported protocol version")
        }
        val messageId = buffer.short
        val sequence = buffer.short
        val payloadBytes = ByteArray(length)
        buffer.get(payloadBytes)
        val payload = Payload.decode(messageId, payloadBytes)
        return ClearMessage(messageId, sequence, payload)
    }
}

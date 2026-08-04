package it.danielezotta.psalegacy.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FrameCodecTest {

    @Test
    fun `frame round-trip preserves clear message`() {
        val encryption = EncryptionManager()
        encryption.generateKeys("TESTVIN0000000001".toByteArray(Charsets.US_ASCII))
        val codec = FrameCodec(encryption)
        val clear = ClearMessage(
            ProtocolConstants.MSG_AUTH_RESPONSE, 2,
            Payload.AuthResponsePayload(ByteArray(32) { it.toByte() }, "TESTVIN0000000001".toByteArray())
        )
        val frame = codec.encode(MessageCodec.encode(clear))
        val decoded = MessageCodec.decode(codec.decode(frame))
        assertEquals(ProtocolConstants.MSG_AUTH_RESPONSE, decoded.messageId)
        assertEquals(2.toShort(), decoded.sequence)
        val decodedPayload = decoded.payload as Payload.AuthResponsePayload
        val originalPayload = clear.payload as Payload.AuthResponsePayload
        assertArrayEquals(originalPayload.signedChallenge, decodedPayload.signedChallenge)
        assertArrayEquals(originalPayload.vin, decodedPayload.vin)
    }

    @Test
    fun `encrypted frame is longer than plaintext`() {
        val encryption = EncryptionManager()
        encryption.generateKeys("TESTVIN0000000001".toByteArray(Charsets.US_ASCII))
        val codec = FrameCodec(encryption)
        val clearBytes = MessageCodec.encode(
            ClearMessage(ProtocolConstants.MSG_ACTIVATION, 1, Payload.ActivationRequestPayload(7))
        )
        val frame = codec.encode(clearBytes)
        assert(frame.size > clearBytes.size)
    }

    @Test
    fun `corrupted frame is rejected`() {
        val encryption = EncryptionManager()
        encryption.generateKeys("TESTVIN0000000001".toByteArray(Charsets.US_ASCII))
        val codec = FrameCodec(encryption)
        val clearBytes = MessageCodec.encode(
            ClearMessage(ProtocolConstants.MSG_ACTIVATION, 1, Payload.ActivationRequestPayload(7))
        )
        val frame = codec.encode(clearBytes).toMutableList()
        frame[5] = 0x7F.toByte()
        assertThrows(ProtocolException::class.java) { codec.decode(frame.toByteArray()) }
    }
}

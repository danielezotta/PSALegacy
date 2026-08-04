package it.danielezotta.psalegacy.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MessageCodecTest {

    private fun roundTrip(msg: ClearMessage): ClearMessage =
        MessageCodec.decode(MessageCodec.encode(msg))

    @Test
    fun `activation request round-trips`() {
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_ACTIVATION, 1,
            Payload.ActivationRequestPayload(7)))
        assertEquals(ProtocolConstants.MSG_ACTIVATION, decoded.messageId)
        assertEquals(1.toShort(), decoded.sequence)
        assertEquals(7, (decoded.payload as Payload.ActivationRequestPayload).activationRequest.toInt())
    }

    @Test
    fun `auth response round-trips`() {
        val signed = ByteArray(32) { it.toByte() }
        val vin = "TESTVIN0000000001".toByteArray(Charsets.US_ASCII)
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_AUTH_RESPONSE, 3,
            Payload.AuthResponsePayload(signed, vin)))
        assertArrayEquals(signed, (decoded.payload as Payload.AuthResponsePayload).signedChallenge)
        assertArrayEquals(vin, (decoded.payload as Payload.AuthResponsePayload).vin)
    }

    @Test
    fun `trip data ack round-trips`() {
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_TRIP_DATA_ACK, 5,
            Payload.TripDataAckPayload(1)))
        assertEquals(1, (decoded.payload as Payload.TripDataAckPayload).value.toInt())
    }

    @Test
    fun `activation ack round-trips`() {
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_ACTIVATION_ACK, 9,
            Payload.ActivationAckPayload(1, 42)))
        assertEquals(42, (decoded.payload as Payload.ActivationAckPayload).tripCount.toInt() and 0xFFFF)
        assertEquals(true, (decoded.payload as Payload.ActivationAckPayload).activationResult.toInt() and 1 == 1)
    }

    @Test
    fun `error response round-trips`() {
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_ERROR_RESPONSE, 0,
            Payload.ErrorResponsePayload(1)))
        assertEquals(1, (decoded.payload as Payload.ErrorResponsePayload).value.toInt())
    }

    @Test
    fun `auth request round-trips`() {
        val challenge = ByteArray(32) { (it * 2).toByte() }
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_AUTH_REQUEST, 7,
            Payload.AuthRequestPayload(challenge, 48, 5)))
        val payload = decoded.payload as Payload.AuthRequestPayload
        assertArrayEquals(challenge, payload.challenge)
        assertEquals(48, payload.btelType.toInt())
        assertEquals(5, payload.serviceStatus.toInt())
    }

    @Test
    fun `trip data round-trips raw bytes`() {
        val raw = ByteArray(113) { (it + 1).toByte() }
        val decoded = roundTrip(ClearMessage(ProtocolConstants.MSG_TRIP_DATA, 9,
            Payload.TripDataPayload(raw)))
        assertArrayEquals(raw, (decoded.payload as Payload.TripDataPayload).raw)
    }

    @Test
    fun `truncated fixed payload throws ProtocolException`() {
        val bytes = MessageCodec.encode(ClearMessage(ProtocolConstants.MSG_AUTH_REQUEST, 1,
            Payload.AuthRequestPayload(ByteArray(32), 0, 0))).toMutableList()
        // shorten by removing 5 bytes from the end so length field no longer matches
        repeat(5) { bytes.removeAt(bytes.size - 1) }
        assertThrows(ProtocolException::class.java) { MessageCodec.decode(bytes.toByteArray()) }
    }

    @Test
    fun `bad version is rejected`() {
        val bytes = MessageCodec.encode(ClearMessage(ProtocolConstants.MSG_ACTIVATION, 1,
            Payload.ActivationRequestPayload(7))).toMutableList()
        bytes[4] = 0x02; bytes[5] = 0x00 // corrupt version field only
        bytes[bytes.size - 1] = checksumByte(bytes.toByteArray()) // fix checksum so it passes
        assertThrows(ProtocolException::class.java) { MessageCodec.decode(bytes.toByteArray()) }
    }

    @Test
    fun `corrupted checksum is rejected`() {
        val bytes = MessageCodec.encode(ClearMessage(ProtocolConstants.MSG_ACTIVATION, 1,
            Payload.ActivationRequestPayload(7))).toMutableList()
        bytes[10] = 0x7F.toByte() // corrupt payload byte, length/size unchanged
        assertThrows(ProtocolException::class.java) { MessageCodec.decode(bytes.toByteArray()) }
    }
}

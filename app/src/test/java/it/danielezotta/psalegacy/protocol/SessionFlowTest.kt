package it.danielezotta.psalegacy.protocol

import it.danielezotta.psalegacy.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end regression test at the wire level: an ENCRYPTED AuthRequest frame
 * coming from the car must be decryptable immediately after SessionMachine.start().
 *
 * Regression: VIN keys were generated lazily inside onClearMessage(), which runs
 * AFTER FrameCodec.decode() — so the very first (encrypted) frame from the car
 * could never be decrypted and was always dropped, killing the connection.
 */
class SessionFlowTest {

    private val vin = "TESTVIN0000000001"

    private class Recorder : SessionEvents {
        var authenticated = false
        override fun onConnected() {}
        override fun onAuthenticated(btelType: Short, serviceStatus: Short) { authenticated = true }
        override fun onActivationAck(tripCount: Int, serviceActive: Boolean) {}
        override fun onTrip(trip: Trip) {}
        override fun onError(message: String) {}
        override fun onDisconnected() {}
    }

    @Test
    fun `first encrypted frame from car is decrypted and answered`() {
        // Car side: builds an encrypted AuthRequest frame with VIN-derived keys.
        val carEncryption = EncryptionManager()
        carEncryption.generateKeys(vin.toByteArray(Charsets.US_ASCII))
        val carCodec = FrameCodec(carEncryption)
        val challenge = ByteArray(32) { it.toByte() }
        val authRequest = ClearMessage(
            ProtocolConstants.MSG_AUTH_REQUEST, 0,
            Payload.AuthRequestPayload(challenge, 16, 1)
        )
        val frame = carCodec.encode(MessageCodec.encode(authRequest))

        // Phone side: connection accepted, session started — then the frame arrives.
        val rec = Recorder()
        val machine = SessionMachine(vin, rec)
        machine.start()

        val phoneCodec = FrameCodec(machine.encryption)
        val clearBytes = phoneCodec.decode(frame) // must not throw / must really decrypt
        val clear = MessageCodec.decode(clearBytes)
        assertEquals(ProtocolConstants.MSG_AUTH_REQUEST, clear.messageId)

        val response = machine.onClearMessage(clear)
        assertNotNull(response)
        assertEquals(ProtocolConstants.MSG_AUTH_RESPONSE, response!!.messageId)
        assertTrue(rec.authenticated)

        // Car side must be able to decrypt the phone's AuthResponse.
        val responseFrame = phoneCodec.encode(MessageCodec.encode(response))
        val responseClear = MessageCodec.decode(carCodec.decode(responseFrame))
        assertEquals(ProtocolConstants.MSG_AUTH_RESPONSE, responseClear.messageId)
        val payload = responseClear.payload as Payload.AuthResponsePayload
        assertEquals(vin, String(payload.vin, Charsets.US_ASCII))
        assertTrue(challenge.contentEquals(carEncryption.decryptChallenge(payload.signedChallenge)))
    }

    @Test
    fun `keys exist right after start before any message arrives`() {
        val rec = Recorder()
        val machine = SessionMachine(vin, rec)
        machine.start()
        assertTrue(machine.encryption.hasVin)
    }
}

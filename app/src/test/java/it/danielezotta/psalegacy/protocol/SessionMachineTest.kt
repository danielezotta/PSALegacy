package it.danielezotta.psalegacy.protocol

import it.danielezotta.psalegacy.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMachineTest {

    private val vin = "TESTVIN0000000001"

    private class Recorder : SessionEvents {
        val trips = mutableListOf<Trip>()
        var authBtel: Short? = null
        var ackTripCount: Int? = null
        var ackActive: Boolean? = null
        var errors = mutableListOf<String>()
        var connected = false
        var disconnected = false

        override fun onConnected() { connected = true }
        override fun onAuthenticated(btelType: Short, serviceStatus: Short) { authBtel = btelType }
        override fun onActivationAck(tripCount: Int, serviceActive: Boolean) {
            ackTripCount = tripCount; ackActive = serviceActive
        }
        override fun onTrip(trip: Trip) { trips.add(trip) }
        override fun onError(message: String) { errors.add(message) }
        override fun onDisconnected() { disconnected = true }
    }

    private fun machine(rec: Recorder): SessionMachine {
        val m = SessionMachine(vin, rec)
        m.start()
        return m
    }

    private fun authRequest(seq: Short): ClearMessage = ClearMessage(
        ProtocolConstants.MSG_AUTH_REQUEST, seq,
        Payload.AuthRequestPayload(ByteArray(32) { 1 }, 16, 1)
    )

    @Test
    fun `auth request produces auth response with vin`() {
        val rec = Recorder()
        val m = machine(rec)
        val response = m.onClearMessage(authRequest(3))
        assertEquals(ProtocolConstants.MSG_AUTH_RESPONSE, response!!.messageId)
        assertEquals(3.toShort(), response.sequence)
        val payload = response.payload as Payload.AuthResponsePayload
        assertEquals(vin, String(payload.vin, Charsets.US_ASCII))
        assertEquals(16, rec.authBtel!!.toInt())
        assertEquals(SessionMachine.State.AUTHENTICATED, m.state)
    }

    @Test
    fun `activation ack echoes trip count and active state`() {
        val rec = Recorder()
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        val activation = m.buildActivation()!!
        assertEquals(ProtocolConstants.MSG_ACTIVATION, activation.messageId)
        assertEquals(SessionMachine.State.WAITING_ACK, m.state)
        val ack = ClearMessage(ProtocolConstants.MSG_ACTIVATION_ACK, activation.sequence,
            Payload.ActivationAckPayload(1, 12))
        assertNull(m.onClearMessage(ack))
        assertEquals(12, rec.ackTripCount)
        assertEquals(true, rec.ackActive)
        assertEquals(SessionMachine.State.AUTHENTICATED, m.state)
    }

    @Test
    fun `trip data triggers onTrip and returns ack`() {
        val rec = Recorder()
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        val payload = java.nio.ByteBuffer.allocate(113).order(java.nio.ByteOrder.BIG_ENDIAN)
        payload.putInt(1); payload.putLong(1_500_000_000L); payload.putInt(12345)
        payload.put(0.toByte()); payload.putInt(1); payload.putInt(2); payload.putShort(0)
        payload.putLong(1_500_003_600L); payload.putInt(12346); payload.put(0.toByte())
        payload.putInt(3); payload.putInt(4); payload.putShort(0); payload.putInt(0)
        payload.putInt(0); payload.putInt(0); payload.putInt(0); payload.putShort(0)
        payload.put(0.toByte()); payload.put(50.toByte()); payload.putShort(0)
        payload.put(0.toByte()); payload.putShort(0); payload.putShort(0)
        payload.put(ByteArray(32)); payload.putShort(0); payload.putShort(0)
        val ack = m.onClearMessage(ClearMessage(ProtocolConstants.MSG_TRIP_DATA, 9,
            Payload.TripDataPayload(payload.array())))
        assertEquals(ProtocolConstants.MSG_TRIP_DATA_ACK, ack!!.messageId)
        assertEquals(0, (ack.payload as Payload.TripDataAckPayload).value.toInt())
        assertEquals(1, rec.trips.size)
        assertEquals(9.toShort(), ack.sequence)
    }

    @Test
    fun `error message from car is surfaced`() {
        val rec = Recorder()
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        m.onClearMessage(ClearMessage(ProtocolConstants.MSG_ERROR_RESPONSE, 1,
            Payload.ErrorResponsePayload(1)))
        assertTrue(rec.errors.any { it.contains("error") })
    }

    @Test
    fun `buildActivation before auth returns null and keeps state`() {
        val rec = Recorder()
        val m = machine(rec)
        assertNull(m.buildActivation())
        assertEquals(SessionMachine.State.CONNECTED, m.state)
    }

    @Test
    fun `activation ack with wrong sequence is ignored`() {
        val rec = Recorder()
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        val activation = m.buildActivation()!!
        assertNull(m.onClearMessage(ClearMessage(ProtocolConstants.MSG_ACTIVATION_ACK,
            ((activation.sequence.toInt() + 1) and 0xFFFF).toShort(),
            Payload.ActivationAckPayload(1, 12))))
        assertEquals(SessionMachine.State.WAITING_ACK, m.state)
        assertEquals(null, rec.ackTripCount)
    }

    @Test
    fun `activation ack outside waiting state is ignored`() {
        val rec = Recorder()
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        assertNull(m.onClearMessage(ClearMessage(ProtocolConstants.MSG_ACTIVATION_ACK, 1,
            Payload.ActivationAckPayload(1, 12))))
        assertEquals(SessionMachine.State.AUTHENTICATED, m.state)
        assertEquals(null, rec.ackTripCount)
    }

    @Test
    fun `connClosed fires onDisconnected and resets to listening`() {
        val rec = Recorder()
        val m = machine(rec)
        m.connClosed()
        assertTrue(rec.disconnected)
        assertEquals(SessionMachine.State.LISTENING, m.state)
    }
}

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
        val synced = mutableListOf<Pair<Int, Int>>()

        override fun onConnected() { connected = true }
        override fun onAuthenticated(btelType: Short, serviceStatus: Short) { authBtel = btelType }
        override fun onActivationAck(tripCount: Int, serviceActive: Boolean) {
            ackTripCount = tripCount; ackActive = serviceActive
        }
        override fun onTrip(trip: Trip) { trips.add(trip) }
        override fun onError(message: String) { errors.add(message) }
        override fun onDisconnected() { disconnected = true }
        override fun onTripsSynced(received: Int, expected: Int) { synced.add(received to expected) }
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
    fun `activation request asks the head unit to keep stored trips`() {
        val rec = Recorder()
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        val activation = m.buildActivation()!!
        val flags = (activation.payload as Payload.ActivationRequestPayload).activationRequest.toInt()
        assertEquals(ProtocolConstants.ACTIVATION_REQUEST_DEFAULT.toInt(), flags)
        assertEquals(0, flags and ProtocolConstants.ACTIVATION_FLAG_CLEAR_TRIPS)
        assertEquals(ProtocolConstants.ACTIVATION_FLAG_ACTIVATE, flags and ProtocolConstants.ACTIVATION_FLAG_ACTIVATE)
        assertEquals(
            ProtocolConstants.ACTIVATION_FLAG_POSITION_RECORDING,
            flags and ProtocolConstants.ACTIVATION_FLAG_POSITION_RECORDING
        )
    }

    @Test
    fun `activation ack echoes trip count and active state`() {        val rec = Recorder()
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

    private fun tripMessage(seq: Short): ClearMessage {
        val payload = java.nio.ByteBuffer.allocate(113).order(java.nio.ByteOrder.BIG_ENDIAN)
        payload.putInt(1); payload.putLong(1_500_000_000L); payload.putInt(12345)
        payload.put(0.toByte()); payload.putInt(1); payload.putInt(2); payload.putShort(0)
        payload.putLong(1_500_003_600L); payload.putInt(12346); payload.put(0.toByte())
        payload.putInt(3); payload.putInt(4); payload.putShort(0); payload.putInt(0)
        payload.putInt(0); payload.putInt(0); payload.putInt(0); payload.putShort(0)
        payload.put(0.toByte()); payload.put(50.toByte()); payload.putShort(0)
        payload.put(0.toByte()); payload.putShort(0); payload.putShort(0)
        payload.put(ByteArray(32)); payload.putShort(0); payload.putShort(0)
        return ClearMessage(ProtocolConstants.MSG_TRIP_DATA, seq, Payload.TripDataPayload(payload.array()))
    }

    private fun activatedMachine(rec: Recorder, tripCount: Short): SessionMachine {
        val m = machine(rec)
        m.onClearMessage(authRequest(1))
        val activation = m.buildActivation()!!
        m.onClearMessage(ClearMessage(ProtocolConstants.MSG_ACTIVATION_ACK, activation.sequence,
            Payload.ActivationAckPayload(1, tripCount)))
        return m
    }

    @Test
    fun `all expected trips received fires onTripsSynced once`() {
        val rec = Recorder()
        val m = activatedMachine(rec, 3)
        m.onClearMessage(tripMessage(1))
        m.onClearMessage(tripMessage(2))
        assertEquals(emptyList<Pair<Int, Int>>(), rec.synced)
        m.onClearMessage(tripMessage(3))
        assertEquals(listOf(3 to 3), rec.synced)
        m.connClosed()
        assertEquals(listOf(3 to 3), rec.synced)
    }

    @Test
    fun `disconnect with missing trips reports partial sync`() {
        val rec = Recorder()
        val m = activatedMachine(rec, 3)
        m.onClearMessage(tripMessage(1))
        m.connClosed()
        assertEquals(listOf(1 to 3), rec.synced)
    }

    @Test
    fun `zero trip count never fires onTripsSynced`() {
        val rec = Recorder()
        val m = activatedMachine(rec, 0)
        m.connClosed()
        assertEquals(emptyList<Pair<Int, Int>>(), rec.synced)
    }

    @Test
    fun `extra trips after sync do not fire onTripsSynced again`() {
        val rec = Recorder()
        val m = activatedMachine(rec, 2)
        m.onClearMessage(tripMessage(1))
        m.onClearMessage(tripMessage(2))
        assertEquals(listOf(2 to 2), rec.synced)
        m.onClearMessage(tripMessage(3))
        m.connClosed()
        assertEquals(listOf(2 to 2), rec.synced)
    }
}

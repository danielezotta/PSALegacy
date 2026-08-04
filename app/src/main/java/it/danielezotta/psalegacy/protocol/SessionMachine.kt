package it.danielezotta.psalegacy.protocol

import it.danielezotta.psalegacy.model.Trip

class SessionMachine(
    private val vin: String,
    private val events: SessionEvents,
    val encryption: EncryptionManager = EncryptionManager()
) {
    enum class State { LISTENING, CONNECTED, AUTHENTICATED, WAITING_ACK }

    @Volatile
    var state: State = State.LISTENING
        private set

    @Volatile
    private var sequence: Short = 0

    fun start() {
        if (state == State.LISTENING) {
            // Keys must exist BEFORE the first frame is decrypted: the car's
            // AuthRequest arrives already encrypted with the VIN-derived key
            // (same as the original MessageManager.messageReceived()).
            generateKeysIfNeeded()
            state = State.CONNECTED
            events.onConnected()
        }
    }

    fun onClearMessage(msg: ClearMessage): ClearMessage? {
        generateKeysIfNeeded()
        return when (msg.messageId) {
            ProtocolConstants.MSG_ERROR_RESPONSE -> {
                val value = (msg.payload as Payload.ErrorResponsePayload).value
                events.onError("Car sent error code $value")
                null
            }
            ProtocolConstants.MSG_AUTH_REQUEST -> handleAuthRequest(msg)
            ProtocolConstants.MSG_ACTIVATION_ACK -> handleActivationAck(msg)
            ProtocolConstants.MSG_TRIP_DATA -> handleTrip(msg)
            else -> {
                events.onError("Unexpected message id ${msg.messageId}")
                null
            }
        }
    }

    fun buildActivation(): ClearMessage? {
        if (state != State.AUTHENTICATED) return null
        sequence = ((sequence + 1) and 0xFFFF).toShort()
        state = State.WAITING_ACK
        return ClearMessage(
            ProtocolConstants.MSG_ACTIVATION,
            sequence,
            Payload.ActivationRequestPayload(ProtocolConstants.ACTIVATION_REQUEST_DEFAULT)
        )
    }

    fun connClosed() {
        if (state != State.LISTENING) {
            state = State.LISTENING
            sequence = 0
            events.onDisconnected()
        }
    }

    private fun generateKeysIfNeeded() {
        if (!encryption.hasVin && vin.length == ProtocolConstants.VIN_SIZE) {
            encryption.generateKeys(vin.toByteArray(Charsets.US_ASCII))
        }
    }

    private fun handleAuthRequest(msg: ClearMessage): ClearMessage {
        val req = msg.payload as Payload.AuthRequestPayload
        val signedChallenge = encryption.encryptChallenge(req.challenge)
        events.onAuthenticated(req.btelType, req.serviceStatus)
        state = State.AUTHENTICATED
        return ClearMessage(
            ProtocolConstants.MSG_AUTH_RESPONSE,
            msg.sequence,
            Payload.AuthResponsePayload(
                signedChallenge,
                vin.toByteArray(Charsets.US_ASCII)
            )
        )
    }

    private fun handleActivationAck(msg: ClearMessage): ClearMessage? {
        if (state != State.WAITING_ACK || msg.sequence != sequence) return null
        val ack = msg.payload as Payload.ActivationAckPayload
        state = State.AUTHENTICATED
        val tripCount = ack.tripCount.toInt() and 0xFFFF
        val active = ack.activationResult.toInt() and 1 == 1
        events.onActivationAck(tripCount, active)
        return null
    }

    private fun handleTrip(msg: ClearMessage): ClearMessage? {
        val raw = (msg.payload as Payload.TripDataPayload).raw
        return try {
            val trip: Trip = TripDecoder.decode(raw, vin)
            events.onTrip(trip)
            ClearMessage(
                ProtocolConstants.MSG_TRIP_DATA_ACK,
                msg.sequence,
                Payload.TripDataAckPayload(TripProcessingResult.SUCCESS)
            )
        } catch (e: Exception) {
            events.onError("Trip decode failed: ${e.message}")
            ClearMessage(
                ProtocolConstants.MSG_TRIP_DATA_ACK,
                msg.sequence,
                Payload.TripDataAckPayload(TripProcessingResult.DATA_VALIDATION_FAILURE)
            )
        }
    }
}

package it.danielezotta.psalegacy.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder.BIG_ENDIAN

class ProtocolException(message: String) : Exception(message)

sealed class Payload {
    abstract fun encode(): ByteArray

    class ErrorResponsePayload(val value: Short) : Payload() {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(2).order(BIG_ENDIAN)
            buf.putShort(value)
            return buf.array()
        }
    }

    class ActivationRequestPayload(val activationRequest: Short) : Payload() {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(2).order(BIG_ENDIAN)
            buf.putShort(activationRequest)
            return buf.array()
        }
    }

    class ActivationAckPayload(val activationResult: Short, val tripCount: Short) : Payload() {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(4).order(BIG_ENDIAN)
            buf.putShort(activationResult)
            buf.putShort(tripCount)
            return buf.array()
        }
    }

    class AuthRequestPayload(
        val challenge: ByteArray,
        val btelType: Short,
        val serviceStatus: Short
    ) : Payload() {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(ProtocolConstants.CHALLENGE_SIZE + 4).order(BIG_ENDIAN)
            buf.put(challenge)
            buf.putShort(btelType)
            buf.putShort(serviceStatus)
            return buf.array()
        }
    }

    class AuthResponsePayload(val signedChallenge: ByteArray, val vin: ByteArray) : Payload() {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(ProtocolConstants.CHALLENGE_SIZE + ProtocolConstants.VIN_SIZE)
                .order(BIG_ENDIAN)
            buf.put(signedChallenge)
            buf.put(vin)
            return buf.array()
        }
    }

    class TripDataPayload(val raw: ByteArray) : Payload() {
        override fun encode(): ByteArray = raw
    }

    class TripDataAckPayload(val value: Short) : Payload() {
        override fun encode(): ByteArray {
            val buf = ByteBuffer.allocate(2).order(BIG_ENDIAN)
            buf.putShort(value)
            return buf.array()
        }
    }

    companion object {
        fun decode(messageId: Short, bytes: ByteArray): Payload {
            fun requireLength(expected: Int) {
                if (bytes.size != expected) {
                    throw ProtocolException(
                        "Invalid payload length ${bytes.size} for message $messageId, expected $expected"
                    )
                }
            }
            val buf = ByteBuffer.wrap(bytes).order(BIG_ENDIAN)
            return when (messageId) {
                ProtocolConstants.MSG_ERROR_RESPONSE -> {
                    requireLength(2)
                    ErrorResponsePayload(buf.short)
                }
                ProtocolConstants.MSG_ACTIVATION -> {
                    requireLength(2)
                    ActivationRequestPayload(buf.short)
                }
                ProtocolConstants.MSG_ACTIVATION_ACK -> {
                    requireLength(4)
                    ActivationAckPayload(buf.short, buf.short)
                }
                ProtocolConstants.MSG_AUTH_REQUEST -> {
                    requireLength(ProtocolConstants.CHALLENGE_SIZE + 4)
                    val challenge = ByteArray(ProtocolConstants.CHALLENGE_SIZE)
                    buf.get(challenge)
                    AuthRequestPayload(challenge, buf.short, buf.short)
                }
                ProtocolConstants.MSG_AUTH_RESPONSE -> {
                    requireLength(ProtocolConstants.CHALLENGE_SIZE + ProtocolConstants.VIN_SIZE)
                    val signed = ByteArray(ProtocolConstants.CHALLENGE_SIZE)
                    val vin = ByteArray(ProtocolConstants.VIN_SIZE)
                    buf.get(signed)
                    buf.get(vin)
                    AuthResponsePayload(signed, vin)
                }
                ProtocolConstants.MSG_TRIP_DATA -> TripDataPayload(bytes.copyOf())
                ProtocolConstants.MSG_TRIP_DATA_ACK -> {
                    requireLength(2)
                    TripDataAckPayload(buf.short)
                }
                else -> throw ProtocolException("Unknown message id $messageId")
            }
        }
    }
}

data class ClearMessage(
    val messageId: Short,
    val sequence: Short,
    val payload: Payload
)

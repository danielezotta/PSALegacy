package it.danielezotta.psalegacy.protocol

import java.util.UUID

object ProtocolConstants {
    const val SMART_APP_START_IDENTIFIER: Short = 0x5341
    const val CLEAR_START_IDENTIFIER: Short = 0x7361
    const val PROTOCOL_VERSION: Short = 0x0100
    const val WIRE_HEADER_SIZE = 5
    const val CLEAR_HEADER_SIZE = 11

    const val MSG_ERROR_RESPONSE: Short = 0
    const val MSG_ACTIVATION: Short = 1
    const val MSG_ACTIVATION_ACK: Short = 2
    const val MSG_AUTH_REQUEST: Short = 3
    const val MSG_AUTH_RESPONSE: Short = 4
    const val MSG_TRIP_DATA: Short = 5
    const val MSG_TRIP_DATA_ACK: Short = 6

    fun messageName(id: Short): String = when (id) {
        MSG_ERROR_RESPONSE -> "ERROR_RESPONSE"
        MSG_ACTIVATION -> "ACTIVATION_REQUEST"
        MSG_ACTIVATION_ACK -> "ACTIVATION_ACK"
        MSG_AUTH_REQUEST -> "AUTH_REQUEST"
        MSG_AUTH_RESPONSE -> "AUTH_RESPONSE"
        MSG_TRIP_DATA -> "TRIP_DATA"
        MSG_TRIP_DATA_ACK -> "TRIP_DATA_ACK"
        else -> "UNKNOWN($id)"
    }

    const val VIN_SIZE = 17
    const val CHALLENGE_SIZE = 32
    const val MISC_INDICATORS_SIZE = 32

    const val ACTIVATION_REQUEST_DEFAULT: Short = 7

    const val PENDING_ACTIVATION_DELAY_MS = 5000L
    const val ACTIVATION_ACK_TIMEOUT_MS = 2000L

    val SMARTAPP_UUID: UUID = UUID.fromString("f7cc5d80-61eb-11e1-b86c-0800200c9a66")
    val PEUGEOT_BRAND_UUID: UUID = UUID.fromString("9c551414-03eb-11e7-93ae-92361f002671")
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val KEYS_SOURCE: ByteArray = byteArrayOf(
        -11, -83, -63, -115, 10, -102, -73, -105, -91, -119, -81, -103, 107, 95, 120, 19,
        115, -107, -115, -64, 125, -38, 10, 118, 3, -84, -45, -82, -54, 40, 92, -112,
        112, 98, -54, -10, 121, -5, 77, -1, -105, 9, 5, -77, 41, -33, 50, -107,
        97, 85, 41, -83, 125, 11, 93, 112, -111, 105, -6, 33, -50, -119, -79, 8,
        63, 101, -51, 86, 121, -5, 77, 126, -104, 109, -20, 65, 34, -39, -75, 88, 52
    )

    val KEYS: ByteArray get() = KEYS_SOURCE.copyOf()
}

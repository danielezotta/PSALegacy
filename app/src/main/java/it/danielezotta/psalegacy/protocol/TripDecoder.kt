package it.danielezotta.psalegacy.protocol

import it.danielezotta.psalegacy.model.Trip
import java.nio.ByteBuffer
import java.nio.ByteOrder.BIG_ENDIAN
import java.util.Calendar

object TripDecoder {

    private const val UINT16_NONE = 65535
    private const val UINT8_NONE = 255

    fun decode(raw: ByteArray, vin: String): Trip {
        if (raw.size < 113) {
            throw ProtocolException("Trip payload too short: ${raw.size}")
        }
        val buf = ByteBuffer.wrap(raw).order(BIG_ENDIAN)

        val tripNumber = buf.int.toLong() and 0xFFFFFFFFL
        val startSec = buf.long
        val startMileageRaw = buf.int
        val startPqi = buf.get().toInt() and 0xFF
        val startLat = buf.int
        val startLon = buf.int
        val startAlt = buf.short.toInt()
        val endSec = buf.long
        val endMileageRaw = buf.int
        val endPqi = buf.get().toInt() and 0xFF
        val endLat = buf.int
        val endLon = buf.int
        val endAlt = buf.short.toInt()
        val destLat = buf.int
        val destLon = buf.int
        val fuelRaw = buf.int
        val distMaintRaw = buf.int
        val daysMaintRaw = buf.short.toInt() and 0xFFFF
        val maintPassed = buf.get()
        val fuelLevelRaw = buf.get().toInt() and 0xFF
        val fuelAutonomyRaw = buf.short.toInt() and 0xFFFF
        val otherEnergyType = buf.get().toInt() and 0xFF
        val otherEnergyLevel = buf.short.toInt() and 0xFFFF
        val otherEnergyAutonomyRaw = buf.short.toInt() and 0xFFFF
        val misc = ByteArray(ProtocolConstants.MISC_INDICATORS_SIZE)
        buf.get(misc)
        val endAddrLen = buf.short.toInt() and 0xFFFF
        val endAddr = if (endAddrLen > 0) {
            if (endAddrLen > buf.remaining()) {
                throw ProtocolException("End address length $endAddrLen exceeds remaining ${buf.remaining()}")
            }
            val b = ByteArray(endAddrLen); buf.get(b); String(b, Charsets.UTF_8)
        } else null
        val destAddrLen = buf.short.toInt() and 0xFFFF
        val destAddr = if (destAddrLen > 0) {
            if (destAddrLen > buf.remaining()) {
                throw ProtocolException("Destination address length $destAddrLen exceeds remaining ${buf.remaining()}")
            }
            val b = ByteArray(destAddrLen); buf.get(b); String(b, Charsets.UTF_8)
        } else null

        val startMileage = if (startMileageRaw.toLong() and 0xFFFFFFFFL != 0xFFFFFFFFL) {
            (startMileageRaw.toLong() and 0xFFFFFFFFL) / 10.0f
        } else -1.0f
        val endMileage = if (endMileageRaw.toLong() and 0xFFFFFFFFL != 0xFFFFFFFFL) {
            (endMileageRaw.toLong() and 0xFFFFFFFFL) / 10.0f
        } else -1.0f

        val startMs = gmtMillis(startSec)
        val endMs = gmtMillis(endSec)

        return Trip(
            vin = vin,
            tripNumber = tripNumber,
            startEpochMs = startMs,
            endEpochMs = endMs,
            startMileageKm = startMileage,
            endMileageKm = endMileage,
            distanceKm = if (startMileage >= 0 && endMileage >= 0) endMileage - startMileage else -1.0f,
            travelTimeMs = endMs - startMs,
            fuelConsumptionL = if (fuelRaw.toLong() and 0xFFFFFFFFL != 0xFFFFFFFFL) {
                (fuelRaw.toLong() and 0xFFFFFFFFL) / 1_000_000.0f
            } else -1.0f,
            fuelLevel = if (fuelLevelRaw != UINT8_NONE) fuelLevelRaw else -1,
            fuelAutonomyKm = if (fuelAutonomyRaw != UINT16_NONE) fuelAutonomyRaw else -1,
            otherEnergyType = otherEnergyType,
            otherEnergyLevel = otherEnergyLevel,
            otherEnergyAutonomyKm = if (otherEnergyAutonomyRaw != UINT16_NONE) otherEnergyAutonomyRaw else -1,
            startLat = coord(startLat),
            startLon = coord(startLon),
            startAltitude = startAlt,
            endLat = coord(endLat),
            endLon = coord(endLon),
            endAltitude = endAlt,
            destinationLat = coord(destLat),
            destinationLon = coord(destLon),
            distanceToNextMaintenance = if (distMaintRaw.toLong() and 0xFFFFFFFFL != 0xFFFFFFFFL) {
                (distMaintRaw.toLong() and 0xFFFFFFFFL).toInt()
            } else -1,
            daysUntilNextMaintenance = if (daysMaintRaw != UINT16_NONE) daysMaintRaw else -1,
            maintenancePassed = maintPassed.toInt() == 1,
            endAddress = endAddr,
            destinationAddress = destAddr,
            alertCodes = AlertDecoder.decodeAlertCodes(misc)
        )
    }

    private fun coord(v: Int): Float =
        if (v == -1) Float.MAX_VALUE else v / 100000.0f

    // Matches the original app's getRealGMTDateInMs: seconds are converted using the
    // timezone offset at DECODE time (not the trip's own offset). Kept identical to the
    // original behavior even though a DST change between recording and decoding can
    // shift the displayed time by an hour.
    private fun gmtMillis(seconds: Long): Long {
        val cal = Calendar.getInstance()
        val tzOffset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
        return seconds * 1000L - tzOffset
    }
}

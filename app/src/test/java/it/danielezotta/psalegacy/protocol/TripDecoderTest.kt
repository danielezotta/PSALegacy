package it.danielezotta.psalegacy.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class TripDecoderTest {

    private fun buildPayload(
        tripNumber: Int = 5,
        startSec: Long = 1_500_000_000L,
        startMileage: Int = 12345,
        endMileage: Int = 12545,
        startLat: Int = 4567890,
        startLon: Int = 9765432,
        endLat: Int = 4568000,
        endLon: Int = 9766000,
        fuelConsumption: Int = 1500000,
        fuelLevel: Int = 60,
        misc: ByteArray = ByteArray(32) { 0 }
    ): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(109 + 2 + 0 + 2 + 0)
            .order(java.nio.ByteOrder.BIG_ENDIAN)
        buf.putInt(tripNumber)
        buf.putLong(startSec)
        buf.putInt(startMileage)
        buf.put(0)
        buf.putInt(startLat)
        buf.putInt(startLon)
        buf.putShort(100)
        buf.putLong(startSec + 3600L)
        buf.putInt(endMileage)
        buf.put(0)
        buf.putInt(endLat)
        buf.putInt(endLon)
        buf.putShort(120)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(fuelConsumption)
        buf.putInt(20000)
        buf.putShort(30)
        buf.put(0)
        buf.put(fuelLevel.toByte())
        buf.putShort(500)
        buf.put(0)
        buf.putShort(0)
        buf.putShort(0)
        buf.put(misc)
        buf.putShort(0)
        buf.putShort(0)
        return buf.array()
    }

    @Test
    fun `trip decodes with correct scaling`() {
        val trip = TripDecoder.decode(buildPayload(), "TESTVIN0000000001")
        assertEquals(5L, trip.tripNumber)
        assertEquals(1234.5f, trip.startMileageKm, 0.001f)
        assertEquals(1254.5f, trip.endMileageKm, 0.001f)
        assertEquals(20.0f, trip.distanceKm, 0.001f)
        assertEquals(3600_000L, trip.travelTimeMs)
        assertEquals(1.5f, trip.fuelConsumptionL, 0.0001f)
        assertEquals(60, trip.fuelLevel)
        assertEquals(500, trip.fuelAutonomyKm)
        assertEquals(45.6789f, trip.startLat, 0.0001f)
        assertEquals(97.65432f, trip.startLon, 0.0001f)
        assertEquals(20000, trip.distanceToNextMaintenance)
        assertEquals(30, trip.daysUntilNextMaintenance)
        assertEquals(false, trip.maintenancePassed)
    }

    @Test
    fun `sentinel values map to no-data`() {
        val buf = java.nio.ByteBuffer.allocate(113).order(java.nio.ByteOrder.BIG_ENDIAN)
        buf.putInt(-1); buf.putLong(0); buf.putInt(-1); buf.put(0.toByte())
        buf.putInt(-1); buf.putInt(-1); buf.putShort(0); buf.putLong(0)
        buf.putInt(-1); buf.put(0.toByte()); buf.putInt(-1); buf.putInt(-1); buf.putShort(0)
        buf.putInt(-1); buf.putInt(-1); buf.putInt(-1); buf.putInt(-1); buf.putShort(-1)
        buf.put((-1).toByte()); buf.put((-1).toByte()); buf.putShort(-1); buf.put(0.toByte())
        buf.putShort(-1); buf.putShort(-1)
        buf.put(ByteArray(32)); buf.putShort(0); buf.putShort(0)
        val trip = TripDecoder.decode(buf.array(), "TESTVIN0000000001")
        assertEquals(-1.0f, trip.startMileageKm, 0.001f)
        assertEquals(-1.0f, trip.endMileageKm, 0.001f)
        assertEquals(-1.0f, trip.fuelConsumptionL, 0.0001f)
        assertEquals(Float.MAX_VALUE, trip.startLat, 0.001f)
        assertEquals(Float.MAX_VALUE, trip.startLon, 0.001f)
        assertEquals(-1, trip.fuelLevel)
        assertEquals(-1, trip.fuelAutonomyKm)
        assertEquals(-1, trip.otherEnergyAutonomyKm)
        assertEquals(-1, trip.distanceToNextMaintenance)
        assertEquals(-1, trip.daysUntilNextMaintenance)
    }

    @Test
    fun `alert codes are extracted from misc indicators`() {
        val misc = ByteArray(32)
        misc[0] = 0b0000_0011.toByte()
        misc[1] = 0b1000_0000.toByte()
        val trip = TripDecoder.decode(buildPayload(misc = misc), "TESTVIN0000000001")
        assertEquals(listOf(0, 1, 15), trip.alertCodes)
    }
}

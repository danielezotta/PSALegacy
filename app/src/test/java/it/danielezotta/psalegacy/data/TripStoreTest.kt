package it.danielezotta.psalegacy.data

import it.danielezotta.psalegacy.model.Trip
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class TripStoreTest {

    private fun sample(): Trip = Trip(
        vin = "TESTVIN0000000001",
        tripNumber = 5,
        startEpochMs = 1_500_000_000_000L,
        endEpochMs = 1_500_003_600_000L,
        startMileageKm = 1234.5f,
        endMileageKm = 1254.5f,
        distanceKm = 20.0f,
        travelTimeMs = 3_600_000L,
        fuelConsumptionL = 1.5f,
        fuelLevel = 60,
        fuelAutonomyKm = 500,
        otherEnergyType = 0,
        otherEnergyLevel = 0,
        otherEnergyAutonomyKm = -1,
        startLat = 45.6789f,
        startLon = 97.65432f,
        startAltitude = 100,
        endLat = 45.68f,
        endLon = 97.66f,
        endAltitude = 120,
        destinationLat = 0f,
        destinationLon = 0f,
        distanceToNextMaintenance = 20000,
        daysUntilNextMaintenance = 30,
        maintenancePassed = false,
        endAddress = "Paris",
        destinationAddress = null,
        alertCodes = listOf(0, 1)
    )

    @Test
    fun `trip serializes and deserializes losslessly`() {
        val json = TripStore.toJson(sample())
        val roundTrip = TripStore.fromJson(json)
        assertEquals(sample(), roundTrip)
    }

    @Test
    fun `list serializes to JSON array and back`() {
        val list = listOf(sample(), sample().copy(tripNumber = 6, fuelLevel = 40))
        val array = TripStore.toJsonArray(list)
        assertEquals(2, array.length())
        assertEquals(list, TripStore.fromJsonArray(array))
    }

    @Test
    fun `vin is read back from the stored object`() {
        val json = TripStore.toJson(sample())
        assertEquals("TESTVIN0000000001", json.getString("vin"))
    }

    @Test
    fun `mergeTrips upserts by trip number and keeps other vins`() {
        val otherVin = sample().copy(vin = "OTHER0000000000001", tripNumber = 1)
        val existing = listOf(sample(), otherVin)
        val incoming = listOf(sample().copy(tripNumber = 5, fuelLevel = 42), sample().copy(tripNumber = 7))
        val merged = TripStore.mergeTrips(existing, incoming, "TESTVIN0000000001")
        assertEquals(3, merged.size)
        assertEquals(42, merged.first { it.vin == "TESTVIN0000000001" && it.tripNumber == 5L }.fuelLevel)
        assertEquals(1L, merged.first { it.vin == "OTHER0000000000001" }.tripNumber)
    }

    @Test
    fun `mergeTrips preserves both vins on distinct trip numbers`() {
        val a = sample()
        val b = sample().copy(vin = "OTHER0000000000001", tripNumber = 99)
        val merged = TripStore.mergeTrips(listOf(a), listOf(b), "TESTVIN0000000001")
        assertEquals(listOf("OTHER0000000000001", "TESTVIN0000000001"), merged.map { it.vin }.sorted())
    }
}

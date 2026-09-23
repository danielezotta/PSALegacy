package it.danielezotta.psalegacy.ui

import it.danielezotta.psalegacy.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripFiltersTest {

    private val now = 1_786_000_000_000L
    private val day = 86_400_000L

    private fun trip(
        ageDays: Long = 0,
        km: Float = 18f,
        fuel: Float = 1.2f,
        alerts: List<Int> = emptyList(),
        level: Int = 50
    ) = Trip(
        vin = "VF3HZBHZ2JS123456", tripNumber = 1, startEpochMs = now - ageDays * day,
        endEpochMs = now - ageDays * day + 60_000, startMileageKm = 0f, endMileageKm = km,
        distanceKm = km, travelTimeMs = 60_000, fuelConsumptionL = fuel, fuelLevel = level,
        fuelAutonomyKm = 400, otherEnergyType = 0, otherEnergyLevel = 65535, otherEnergyAutonomyKm = -1,
        startLat = Float.MAX_VALUE, startLon = Float.MAX_VALUE, startAltitude = -1,
        endLat = Float.MAX_VALUE, endLon = Float.MAX_VALUE, endAltitude = -1,
        destinationLat = Float.MAX_VALUE, destinationLon = Float.MAX_VALUE,
        distanceToNextMaintenance = -1, daysUntilNextMaintenance = -1, maintenancePassed = false,
        endAddress = null, destinationAddress = null, alertCodes = alerts
    )

    @Test
    fun `default filters match everything and count zero`() {
        val f = TripFilters()
        assertEquals(0, f.activeCount)
        assertTrue(f.matches(trip(ageDays = 400, km = -1f, fuel = -1f), now))
    }

    @Test
    fun `period filter excludes older trips`() {
        val f = TripFilters(period = PeriodFilter.DAYS_7)
        assertTrue(f.matches(trip(ageDays = 6), now))
        assertFalse(f.matches(trip(ageDays = 8), now))
        assertEquals(1, f.activeCount)
    }

    @Test
    fun `distance buckets split at 20 and 80 km`() {
        assertTrue(TripFilters(distance = DistanceFilter.SHORT).matches(trip(km = 19.9f), now))
        assertFalse(TripFilters(distance = DistanceFilter.SHORT).matches(trip(km = 20f), now))
        assertTrue(TripFilters(distance = DistanceFilter.MID).matches(trip(km = 80f), now))
        assertTrue(TripFilters(distance = DistanceFilter.LONG).matches(trip(km = 142.3f), now))
        assertFalse(TripFilters(distance = DistanceFilter.LONG).matches(trip(km = 80f), now))
    }

    @Test
    fun `alert filter checks alert codes`() {
        assertTrue(TripFilters(alerts = AlertFilter.WITH).matches(trip(alerts = listOf(26)), now))
        assertFalse(TripFilters(alerts = AlertFilter.WITH).matches(trip(), now))
        assertTrue(TripFilters(alerts = AlertFilter.WITHOUT).matches(trip(), now))
    }

    @Test
    fun `consumption filter uses liters per 100 km and skips unknown`() {
        // 1.2 L / 18 km = 6.67 L/100
        assertTrue(TripFilters(consumption = ConsumptionFilter.MID).matches(trip(), now))
        assertFalse(TripFilters(consumption = ConsumptionFilter.LOW).matches(trip(), now))
        assertTrue(TripFilters(consumption = ConsumptionFilter.HIGH).matches(trip(km = 100f, fuel = 7.6f), now))
        assertFalse(TripFilters(consumption = ConsumptionFilter.LOW).matches(trip(fuel = -1f), now))
    }

    @Test
    fun `totals only count trips with distance and fuel`() {
        val totals = TripTotals.of(listOf(trip(km = 18f, fuel = 1.2f), trip(km = 10f, fuel = -1f)))
        assertEquals(1, totals.counted)
        assertEquals(18f, totals.distanceKm, 0.001f)
        assertEquals(1.2f, totals.fuelL, 0.001f)
    }

    @Test
    fun `vin validation rejects I O Q and wrong length`() {
        assertTrue(MainViewModel.isValidVin("VF3HZBHZ2JS123456"))
        assertFalse(MainViewModel.isValidVin("VF3HZBHZ2JS12345"))
        assertFalse(MainViewModel.isValidVin("VF3HZBHZ2JS12345O"))
    }

    @Test
    fun `refuel is the latest rise in fuel level between consecutive trips`() {
        val trips = listOf(
            trip(ageDays = 10, level = 30),
            trip(ageDays = 9, level = 80), // refuel before this trip
            trip(ageDays = 5, level = 60),
            trip(ageDays = 4, level = 20),
            trip(ageDays = 3, level = 90), // latest refuel
            trip(ageDays = 2, level = 88),
            trip(ageDays = 1, level = -1) // level not reported: ignored
        )
        val refuel = Refuel.latest(trips)!!
        assertEquals(now - 3 * day, refuel.startEpochMs)
        assertEquals(20, refuel.levelBefore)
        assertEquals(90, refuel.levelAfter)
    }

    @Test
    fun `small level rises are gauge noise, not refuels`() {
        assertEquals(null, Refuel.latest(listOf(trip(ageDays = 2, level = 40), trip(ageDays = 1, level = 43))))
        assertEquals(null, Refuel.latest(emptyList()))
    }

    @Test
    fun `since refuel keeps trips from the refuel on, or all when none detected`() {
        val f = TripFilters(period = PeriodFilter.SINCE_REFUEL)
        val cutoff = now - 3 * day
        assertTrue(f.matches(trip(ageDays = 3), now, cutoff))
        assertTrue(f.matches(trip(ageDays = 1), now, cutoff))
        assertFalse(f.matches(trip(ageDays = 4), now, cutoff))
        assertTrue(f.matches(trip(ageDays = 400), now, null))
    }
}

package it.danielezotta.psalegacy.ui

import it.danielezotta.psalegacy.model.Trip

enum class PeriodFilter(val label: String) {
    ALL("All"), SINCE_REFUEL("Since refuel"), DAYS_7("7 d"), DAYS_30("30 d")
}
enum class DistanceFilter(val label: String) { ALL("All"), SHORT("<20"), MID("20–80"), LONG(">80") }
enum class AlertFilter(val label: String) { ALL("All"), WITH("With"), WITHOUT("Without") }
enum class ConsumptionFilter(val label: String) { ALL("All"), LOW("<6"), MID("6–7"), HIGH(">7") }

/** Trip list filters from the Trips "Filters" sheet. */
data class TripFilters(
    val period: PeriodFilter = PeriodFilter.ALL,
    val distance: DistanceFilter = DistanceFilter.ALL,
    val alerts: AlertFilter = AlertFilter.ALL,
    val consumption: ConsumptionFilter = ConsumptionFilter.ALL
) {
    val activeCount: Int
        get() = listOf(
            period != PeriodFilter.ALL,
            distance != DistanceFilter.ALL,
            alerts != AlertFilter.ALL,
            consumption != ConsumptionFilter.ALL
        ).count { it }

    /**
     * @param refuelStartMs start of the first trip after the latest detected refuel
     *   ([Refuel.latest]); with no refuel detected, "since refuel" keeps every trip.
     */
    fun matches(trip: Trip, nowMs: Long, refuelStartMs: Long? = null): Boolean {
        val age = nowMs - trip.startEpochMs
        when (period) {
            PeriodFilter.ALL -> Unit
            PeriodFilter.SINCE_REFUEL -> if (refuelStartMs != null && trip.startEpochMs < refuelStartMs) return false
            PeriodFilter.DAYS_7 -> if (age > 7 * DAY_MS) return false
            PeriodFilter.DAYS_30 -> if (age > 30 * DAY_MS) return false
        }
        val km = trip.distanceKm
        when (distance) {
            DistanceFilter.ALL -> Unit
            DistanceFilter.SHORT -> if (km < 0f || km >= 20f) return false
            DistanceFilter.MID -> if (km < 20f || km > 80f) return false
            DistanceFilter.LONG -> if (km <= 80f) return false
        }
        when (alerts) {
            AlertFilter.ALL -> Unit
            AlertFilter.WITH -> if (trip.alertCodes.isEmpty()) return false
            AlertFilter.WITHOUT -> if (trip.alertCodes.isNotEmpty()) return false
        }
        val l100 = consumptionL100(trip)
        return when (consumption) {
            ConsumptionFilter.ALL -> true
            ConsumptionFilter.LOW -> l100 != null && l100 < 6f
            ConsumptionFilter.MID -> l100 != null && l100 >= 6f && l100 <= 7f
            ConsumptionFilter.HIGH -> l100 != null && l100 > 7f
        }
    }

    companion object {
        private const val DAY_MS = 86_400_000L

        fun consumptionL100(trip: Trip): Float? =
            if (trip.distanceKm > 0f && trip.fuelConsumptionL >= 0f) trip.fuelConsumptionL / trip.distanceKm * 100f
            else null
    }
}

/** Totals over trips that report both distance and fuel (so the average stays consistent). */
data class TripTotals(val distanceKm: Float, val fuelL: Float, val counted: Int) {
    companion object {
        fun of(trips: List<Trip>): TripTotals {
            val valid = trips.filter { it.distanceKm > 0f && it.fuelConsumptionL >= 0f }
            return TripTotals(
                distanceKm = valid.sumOf { it.distanceKm.toDouble() }.toFloat(),
                fuelL = valid.sumOf { it.fuelConsumptionL.toDouble() }.toFloat(),
                counted = valid.size
            )
        }
    }
}

/**
 * A refuel inferred from the trip log: the car reports its fuel level at the end of
 * each trip, so a level that rose between two consecutive trips means fuel was added
 * before the later one ([firstTrip]).
 */
data class Refuel(val firstTrip: Trip, val levelBefore: Int, val levelAfter: Int) {
    val startEpochMs: Long get() = firstTrip.startEpochMs

    companion object {
        /** Rise in fuel level (%) that counts as a refuel, above gauge noise on a parked car. */
        const val MIN_RISE_PERCENT = 5

        fun latest(trips: List<Trip>): Refuel? {
            val withLevel = trips.filter { it.fuelLevel in 0..100 }.sortedBy { it.startEpochMs }
            for (i in withLevel.indices.reversed()) {
                if (i == 0) break
                val before = withLevel[i - 1].fuelLevel
                val after = withLevel[i].fuelLevel
                if (after - before >= MIN_RISE_PERCENT) return Refuel(withLevel[i], before, after)
            }
            return null
        }
    }
}

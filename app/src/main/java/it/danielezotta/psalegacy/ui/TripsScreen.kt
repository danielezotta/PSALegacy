package it.danielezotta.psalegacy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.protocol.AlertCatalog
import it.danielezotta.psalegacy.ui.components.Badge
import it.danielezotta.psalegacy.ui.components.ButtonVariant
import it.danielezotta.psalegacy.ui.components.Caption
import it.danielezotta.psalegacy.ui.components.Eyebrow
import it.danielezotta.psalegacy.ui.components.FilterChip
import it.danielezotta.psalegacy.ui.components.LinkButton
import it.danielezotta.psalegacy.ui.components.ListCard
import it.danielezotta.psalegacy.ui.components.ListDivider
import it.danielezotta.psalegacy.ui.components.NumText
import it.danielezotta.psalegacy.ui.components.Pill
import it.danielezotta.psalegacy.ui.components.PillTone
import it.danielezotta.psalegacy.ui.components.PsaButton
import it.danielezotta.psalegacy.ui.components.PsaCard
import it.danielezotta.psalegacy.ui.components.PsaIcons
import it.danielezotta.psalegacy.ui.components.PsaSheet
import it.danielezotta.psalegacy.ui.components.Radius
import it.danielezotta.psalegacy.ui.components.SectionTitle
import it.danielezotta.psalegacy.ui.components.SeverityDot
import it.danielezotta.psalegacy.ui.components.StatGrid
import it.danielezotta.psalegacy.ui.components.valueWithUnit
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType

@Composable
fun TripsScreen(viewModel: MainViewModel) {
    val trips by viewModel.trips.collectAsState()
    val filters by viewModel.tripFilters.collectAsState()
    val now = System.currentTimeMillis()
    val sorted = trips.sortedByDescending { it.startEpochMs }
    val refuel = remember(trips) { Refuel.latest(trips) }
    val visible = sorted.filter { filters.matches(it, now, refuel?.startEpochMs) }
    val totals = TripTotals.of(visible)
    val active = filters.activeCount

    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var openTripKey by rememberSaveable { mutableStateOf<String?>(null) }

    ScreenColumn(tight = true) {
        val hasTotals = visible.isNotEmpty() && totals.counted > 0
        StatGrid(
            "Distance" to valueWithUnit(if (hasTotals) Format.kmValue(totals.distanceKm) else "—", "km"),
            "Fuel" to valueWithUnit(if (hasTotals) Format.litersValue(totals.fuelL) else "—", "L"),
            "Average" to valueWithUnit(
                if (hasTotals) Format.lPer100Value(totals.distanceKm, totals.fuelL) else "—", "L/100"
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            FiltersButton(active) { filtersOpen = true }
            Spacer(Modifier.weight(1f))
            Caption(
                if (active > 0) "${visible.size} of ${sorted.size} trips"
                else "${sorted.size} ${if (sorted.size == 1) "trip" else "trips"}",
                maxLines = 1
            )
            if (active > 0) LinkButton("Reset", viewModel::resetTripFilters)
        }

        if (visible.isEmpty()) {
            val filteredOut = sorted.isNotEmpty()
            PsaCard {
                Icon(PsaIcons.Route, contentDescription = null, tint = PsaTheme.colors.text)
                SectionTitle(if (filteredOut) "No results" else "No trips")
                Caption(
                    if (filteredOut) "No trips match these filters. Change them in Filters or tap Reset."
                    else "The head unit sends trips when a journey ends. Start listening and trigger the connection from the car."
                )
            }
        } else {
            ListCard {
                visible.forEachIndexed { index, trip ->
                    if (index > 0) ListDivider()
                    TripRow(trip, now) { openTripKey = trip.key }
                }
            }
        }
    }

    if (filtersOpen) {
        FiltersSheet(
            filters = filters,
            refuel = refuel,
            matching = visible.size,
            onChange = viewModel::setTripFilters,
            onReset = viewModel::resetTripFilters,
            onDismiss = { filtersOpen = false }
        )
    }

    val openTrip = trips.firstOrNull { it.key == openTripKey }
    if (openTrip != null) {
        TripSheet(openTrip, now) { openTripKey = null }
    }
}

private val Trip.key: String get() = "$vin:$tripNumber"

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    val c = PsaTheme.colors
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(Radius.sm)
            .background(c.surface)
            .border(1.dp, c.line, Radius.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(PsaIcons.Filter, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Filters", style = PsaType.chip)
        if (activeCount > 0) Badge(activeCount)
    }
}

@Composable
private fun TripRow(trip: Trip, now: Long, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(Format.dateLabel(trip.startEpochMs, now), style = PsaType.body, maxLines = 1)
            Caption(
                "${trip.tripNumber} · ${Format.duration(trip.travelTimeMs)} · " +
                    "${Format.lPer100Value(trip.distanceKm, trip.fuelConsumptionL)} L/100",
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NumText("${Format.kmValue(trip.distanceKm)} km")
            val n = trip.alertCodes.size
            if (n > 0) Pill("$n ${if (n == 1) "alert" else "alerts"}", PillTone.Accent)
            else Pill("no alerts", PillTone.Muted)
        }
    }
}

@Composable
private fun FiltersSheet(
    filters: TripFilters,
    refuel: Refuel?,
    matching: Int,
    onChange: (TripFilters) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    PsaSheet(
        title = "Filters",
        onDismiss = onDismiss,
        footer = {
            PsaButton(
                "Show $matching ${if (matching == 1) "trip" else "trips"}",
                onDismiss,
                Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterGroup("Period", PeriodFilter.entries, filters.period, { it.label }) {
                onChange(filters.copy(period = it))
            }
            if (filters.period == PeriodFilter.SINCE_REFUEL) {
                Caption(
                    if (refuel == null) "No refuel detected yet: showing all trips."
                    else "Last refuel before ${Format.dateLabel(refuel.startEpochMs, System.currentTimeMillis())} · " +
                        "fuel ${refuel.levelBefore} % → ${refuel.levelAfter} %"
                )
            }
            FilterGroup("Distance km", DistanceFilter.entries, filters.distance, { it.label }) {
                onChange(filters.copy(distance = it))
            }
            FilterGroup("Alerts", AlertFilter.entries, filters.alerts, { it.label }) {
                onChange(filters.copy(alerts = it))
            }
            FilterGroup("Consumption L/100", ConsumptionFilter.entries, filters.consumption, { it.label }) {
                onChange(filters.copy(consumption = it))
            }
            LinkButton("Reset all", onReset)
        }
    }
}

@Composable
private fun <T> FilterGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(title)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (option in options) {
                FilterChip(label(option), option == selected) { onSelect(option) }
            }
        }
    }
}

@Composable
private fun TripSheet(trip: Trip, now: Long, onDismiss: () -> Unit) {
    PsaSheet(
        title = "Trip ${trip.tripNumber}",
        onDismiss = onDismiss,
        footer = {
            PsaButton("Close", onDismiss, Modifier.fillMaxWidth(), ButtonVariant.Secondary)
        }
    ) {
        Column {
            Eyebrow("Trip ${trip.tripNumber}")
            Caption(Format.dateLabel(trip.startEpochMs, now))
        }
        StatGrid(
            "Distance" to valueWithUnit(Format.kmValue(trip.distanceKm), "km"),
            "Duration" to valueWithUnit(Format.duration(trip.travelTimeMs), null),
            "Fuel" to valueWithUnit(Format.litersValue(trip.fuelConsumptionL), "L"),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        ListCard(Modifier.padding(bottom = 4.dp)) {
            val rows = tripDetailRows(trip)
            rows.forEachIndexed { index, (label, value, numeric) ->
                if (index > 0) ListDivider()
                DetailRow(label, value, numeric)
            }
        }
        Eyebrow("Alerts")
        ListCard {
            if (trip.alertCodes.isEmpty()) {
                DetailRow("No alerts", "Empty 256-bit indicator mask", numeric = false, captionBelow = true)
            } else {
                trip.alertCodes.forEachIndexed { index, code ->
                    if (index > 0) ListDivider()
                    AlertRow(code, detail = listOfNotNull(code.toString(), AlertCatalog.criticity(code)).joinToString(" · "))
                }
            }
        }
    }
}

private data class DetailItem(val label: String, val value: String, val numeric: Boolean)

private fun tripDetailRows(trip: Trip): List<DetailItem> = buildList {
    add(
        DetailItem(
            "Mileage",
            "${Format.kmValue(trip.startMileageKm)} → ${Format.kmValue(trip.endMileageKm)} km",
            true
        )
    )
    add(DetailItem("End address", trip.endAddress ?: "—", false))
    add(DetailItem("Destination", trip.destinationAddress ?: "—", false))
    add(
        DetailItem(
            "Fuel level at trip end",
            "${Format.intValue(trip.fuelLevel)} % · range ${Format.intValue(trip.fuelAutonomyKm)} km",
            true
        )
    )
    Format.coord(trip.startLat, trip.startLon)?.let { coord ->
        add(DetailItem("Start GPS", coord + Format.altAndPqi(trip.startAltitude, trip.startPqi)?.let { " · $it" }.orEmpty(), true))
    }
    Format.coord(trip.endLat, trip.endLon)?.let { coord ->
        add(DetailItem("End GPS", coord + Format.altAndPqi(trip.endAltitude, trip.endPqi)?.let { " · $it" }.orEmpty(), true))
    }
    Format.coord(trip.destinationLat, trip.destinationLon)?.let { add(DetailItem("Destination GPS", it, true)) }
    val evLevel = if (trip.otherEnergyLevel != 65535) trip.otherEnergyLevel else -1
    if (trip.otherEnergyType != 0 && (evLevel >= 0 || trip.otherEnergyAutonomyKm >= 0)) {
        add(
            DetailItem(
                "Additional energy",
                "${Format.intValue(evLevel)} % · range ${Format.intValue(trip.otherEnergyAutonomyKm)} km",
                true
            )
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, numeric: Boolean, captionBelow: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (captionBelow) {
            Text(label, style = PsaType.body)
            Caption(value)
        } else {
            Caption(label)
            if (numeric) {
                Text(value, style = PsaType.num, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(value, style = PsaType.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Vehicle alert row: severity dot, localized title, detail caption. Shared with the Vehicle tab. */
@Composable
fun AlertRow(code: Int, detail: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SeverityDot(AlertCatalog.criticity(code))
        Column(Modifier.weight(1f)) {
            Text(
                AlertCatalog.title(code) ?: "Alert $code",
                style = PsaType.body, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Caption(detail)
        }
    }
}

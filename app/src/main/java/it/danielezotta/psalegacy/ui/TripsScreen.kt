package it.danielezotta.psalegacy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.protocol.AlertCatalog

@Composable
fun TripsScreen(viewModel: MainViewModel) {
    val trips by viewModel.trips.collectAsState()
    val sorted = trips.sortedByDescending { it.startEpochMs }
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize()) {
        Text(
            "${sorted.size} trips",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
            item { StatsRow(sorted) }
            if (sorted.isEmpty()) {
                item {
                    Text(
                        "No data yet. Connect the car to capture trips.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            items(sorted, key = { "${it.vin}:${it.tripNumber}" }) { trip -> TripRow(trip, now) }
        }
    }
}

@Composable
private fun StatsRow(trips: List<Trip>) {
    val hasKm = trips.any { it.distanceKm >= 0 }
    val hasFuel = trips.any { it.fuelConsumptionL >= 0 }
    val totalKm = trips.filter { it.distanceKm >= 0 }.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalFuel = trips.filter { it.fuelConsumptionL >= 0 }.sumOf { it.fuelConsumptionL.toDouble() }.toFloat()
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Distance", if (hasKm) Format.kmInt(totalKm) else "—", Modifier.weight(1f))
        StatCard("Fuel", if (hasFuel) Format.liters(totalFuel) else "—", Modifier.weight(1f))
        StatCard("Avg", Format.lPer100(totalKm, totalFuel), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TripRow(trip: Trip, now: Long) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        Format.dateLabel(trip.startEpochMs, now),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Trip ${trip.tripNumber} · ${Format.duration(trip.travelTimeMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Format.km(trip.distanceKm),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "${Format.liters(trip.fuelConsumptionL)} · ${Format.lPer100(trip.distanceKm, trip.fuelConsumptionL)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${Format.km(trip.startMileageKm)} → ${Format.km(trip.endMileageKm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (trip.alertCodes.isEmpty()) {
                    Text(
                        "no alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "▲ ${trip.alertCodes.joinToString(", ") { AlertCatalog.title(it) ?: "alert $it" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

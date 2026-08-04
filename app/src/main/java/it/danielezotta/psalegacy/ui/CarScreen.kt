package it.danielezotta.psalegacy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.protocol.AlertCatalog

@Composable
fun CarScreen(viewModel: MainViewModel) {
    val trips by viewModel.trips.collectAsState()
    val latest = trips.maxByOrNull { it.startEpochMs }
    val now = System.currentTimeMillis()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (latest == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No data yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Connect the car and complete a journey — fuel and odometer data arrive after the trip ends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            HeroCard(latest.endMileageKm, latest.endEpochMs, now)
            FuelCard(latest.fuelLevel, latest.fuelAutonomyKm)
            MaintenanceCard(
                latest.distanceToNextMaintenance,
                latest.daysUntilNextMaintenance,
                latest.maintenancePassed
            )
            val evLevel = if (latest.otherEnergyLevel != 65535) latest.otherEnergyLevel else -1
            val evAutonomy = latest.otherEnergyAutonomyKm
            if (latest.otherEnergyType != 0 && (evLevel >= 0 || evAutonomy >= 0)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("EV", style = MaterialTheme.typography.titleSmall)
                        Text(
                            buildList {
                                if (evLevel >= 0) add("level $evLevel%")
                                if (evAutonomy >= 0) add("range ${Format.kmInt(evAutonomy.toFloat())}")
                            }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            AlertsCard(latest.alertCodes)
        }
    }
}

@Composable
private fun HeroCard(endMileageKm: Float, endEpochMs: Long, now: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "ODOMETER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                Format.kmInt(endMileageKm),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "updated ${Format.ago(endEpochMs, now)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FuelCard(fuelLevel: Int, fuelAutonomyKm: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fuel level", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (fuelLevel < 0) "—" else "$fuelLevel%",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (fuelLevel >= 0) {
                LinearProgressIndicator(
                    progress = { fuelLevel.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (fuelAutonomyKm >= 0) {
                Text(
                    "Range: ${Format.kmInt(fuelAutonomyKm.toFloat())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MaintenanceCard(distanceKm: Int, days: Int, passed: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Next maintenance", style = MaterialTheme.typography.titleSmall)
            val parts = if (passed) {
                listOf("service due")
            } else {
                buildList {
                    if (distanceKm >= 0) add("${Format.km(distanceKm.toFloat())} left")
                    if (days >= 0) add("$days days left")
                }
            }
            Text(
                parts.joinToString(" · ").ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (passed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlertsCard(alertCodes: List<Int>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Active alerts",
                style = MaterialTheme.typography.titleSmall,
                color = if (alertCodes.isEmpty()) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error
            )
            val alertText = if (alertCodes.isEmpty()) {
                "None"
            } else {
                alertCodes.joinToString("\n") { code ->
                    "${AlertCatalog.title(code) ?: "Alert $code"} ($code)"
                }
            }
            Text(
                alertText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (alertCodes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

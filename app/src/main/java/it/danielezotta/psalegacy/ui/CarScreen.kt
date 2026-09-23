package it.danielezotta.psalegacy.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.R
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.model.VehicleInfo
import it.danielezotta.psalegacy.model.VehicleModel
import it.danielezotta.psalegacy.protocol.AlertCatalog
import it.danielezotta.psalegacy.ui.components.Caption
import it.danielezotta.psalegacy.ui.components.Eyebrow
import it.danielezotta.psalegacy.ui.components.Field
import it.danielezotta.psalegacy.ui.components.ListCard
import it.danielezotta.psalegacy.ui.components.ListDivider
import it.danielezotta.psalegacy.ui.components.NumText
import it.danielezotta.psalegacy.ui.components.Pill
import it.danielezotta.psalegacy.ui.components.PillTone
import it.danielezotta.psalegacy.ui.components.ProgressTrack
import it.danielezotta.psalegacy.ui.components.PsaCard
import it.danielezotta.psalegacy.ui.components.PsaIcons
import it.danielezotta.psalegacy.ui.components.Radius
import it.danielezotta.psalegacy.ui.components.SectionTitle
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType

/** Transparent car cutouts based on the source photos credited in the README. */
private val VehicleModel.image
    get() = when (this) {
        VehicleModel.P208 -> R.drawable.car_208_cutout
        VehicleModel.P2008 -> R.drawable.car_2008_cutout
        VehicleModel.P308 -> R.drawable.car_308_cutout
        VehicleModel.P3008 -> R.drawable.car_3008_cutout
        VehicleModel.P5008 -> R.drawable.car_5008_cutout
        VehicleModel.P508 -> R.drawable.car_508_cutout
    }

@Composable
fun CarScreen(viewModel: MainViewModel) {
    val trips by viewModel.trips.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    val latest = trips.maxByOrNull { it.startEpochMs }
    val now = System.currentTimeMillis()

    ScreenColumn {
        VehiclePhoto(vehicle)
        OdometerCard(latest, vehicle, now)
        FuelCard(latest)
        if (latest != null) EnergyCard(latest)
        MaintenanceCard(latest)
        AlertsSection(latest)
    }
}

@Composable
private fun VehiclePhoto(vehicle: VehicleInfo) {
    val c = PsaTheme.colors
    val frame = Modifier
        .fillMaxWidth()
        .aspectRatio(1280f / 800f)
        .clip(Radius.lg)
        .background(c.elevated)
    val model = vehicle.model
    if (model != null) {
        Image(
            painter = painterResource(model.image),
            contentDescription = vehicle.name,
            contentScale = ContentScale.Fit,
            modifier = frame
        )
    } else {
        Box(frame, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(PsaIcons.Car, contentDescription = null, tint = c.text2, modifier = Modifier.size(48.dp))
                Caption("Model not recognised from the VIN. Pick it in Settings")
            }
        }
    }
}

@Composable
private fun OdometerCard(latest: Trip?, vehicle: VehicleInfo, now: Long) {
    val c = PsaTheme.colors
    PsaCard {
        Column {
            Eyebrow("Odometer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    latest?.let { Format.kmValue(it.endMileageKm) } ?: "—",
                    style = PsaType.odometer, color = c.text, maxLines = 1, softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
                Text("km", style = PsaType.num, color = c.text2, modifier = Modifier.alignByBaseline())
            }
            Caption(latest?.let { "updated ${Format.ago(it.endEpochMs, now)}" } ?: "Waiting for data from the vehicle")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOfNotNull(vehicle.name, vehicle.modelYear?.toString(), "SMARTAPPS V1")
                .forEach { Pill(it, PillTone.Muted) }
        }
    }
}

@Composable
private fun CardHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = PsaTheme.colors.text2, modifier = Modifier.size(18.dp))
        SectionTitle(title)
    }
}

@Composable
private fun FuelCard(latest: Trip?) {
    val level = latest?.fuelLevel ?: -1
    PsaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardHeader(PsaIcons.Fuel, "Fuel", Modifier.weight(1f))
            NumText(if (level >= 0) "$level %" else "—")
        }
        ProgressTrack(if (level >= 0) level / 100f else 0f)
        Row {
            Field("Range", latest?.let { "${Format.intValue(it.fuelAutonomyKm)} km" } ?: "—", Modifier.weight(1f))
            Field(
                "Consumption",
                latest?.let { "${Format.lPer100Value(it.distanceKm, it.fuelConsumptionL)} L/100" } ?: "—",
                alignEnd = true
            )
        }
    }
}

@Composable
private fun EnergyCard(latest: Trip) {
    val level = if (latest.otherEnergyLevel != 65535) latest.otherEnergyLevel else -1
    val range = latest.otherEnergyAutonomyKm
    if (latest.otherEnergyType == 0 || (level < 0 && range < 0)) return
    PsaCard {
        CardHeader(PsaIcons.Bolt, "Additional energy")
        Row {
            Field("Level", if (level >= 0) "$level %" else "—", Modifier.weight(1f))
            Field("Range", if (range >= 0) "${Format.intValue(range)} km" else "—", alignEnd = true)
        }
    }
}

@Composable
private fun MaintenanceCard(latest: Trip?) {
    val km = latest?.distanceToNextMaintenance ?: -1
    val days = latest?.daysUntilNextMaintenance ?: -1
    PsaCard {
        CardHeader(PsaIcons.Tool, "Next service")
        Row {
            Field("Distance", if (km >= 0) "${Format.intValue(km)} km" else "—", Modifier.weight(1f))
            Field("Days", if (days in 0 until 65535) "$days d" else "—", alignEnd = true)
        }
        if (latest?.maintenancePassed == true) Pill("Service overdue", PillTone.Warn)
    }
}

@Composable
private fun AlertsSection(latest: Trip?) {
    val codes = latest?.alertCodes.orEmpty().distinct()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Vehicle alerts", Modifier.weight(1f))
            Pill(codes.size.toString(), PillTone.Muted)
        }
        when {
            latest == null -> PsaCard {
                Caption("No data. Complete a trip to read status and alerts.")
            }

            codes.isEmpty() -> PsaCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(PsaIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("No alerts", style = PsaType.body)
                        Caption("Empty 256-bit indicator mask on the last trip.")
                    }
                }
            }

            else -> ListCard {
                codes.forEachIndexed { index, code ->
                    if (index > 0) ListDivider()
                    AlertRow(code, listOfNotNull("code $code", AlertCatalog.criticity(code)).joinToString(" · "))
                }
            }
        }
    }
}

package it.danielezotta.psalegacy.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import it.danielezotta.psalegacy.AppState.AutoStart
import it.danielezotta.psalegacy.model.VehicleModel
import it.danielezotta.psalegacy.model.VinDecoder
import it.danielezotta.psalegacy.ui.components.Caption
import it.danielezotta.psalegacy.ui.components.ListCard
import it.danielezotta.psalegacy.ui.components.ListDivider
import it.danielezotta.psalegacy.ui.components.ListRow
import it.danielezotta.psalegacy.ui.components.PsaIcons
import it.danielezotta.psalegacy.ui.components.PsaSheet
import it.danielezotta.psalegacy.ui.components.Radius
import it.danielezotta.psalegacy.ui.components.SectionTitle
import it.danielezotta.psalegacy.ui.theme.AppTheme
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType
import it.danielezotta.psalegacy.ui.theme.colors

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val theme by viewModel.theme.collectAsState()
    ScreenColumn {
        VehicleSection(viewModel)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Theme")
            Caption("Three themes. Applied immediately to every tab.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTheme.entries.forEach { t ->
                ThemeChip(t, selected = t == theme) {
                    viewModel.setTheme(t)
                    viewModel.showToast("${t.label} theme")
                }
            }
        }

        AutoStartSection(viewModel)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("About")
            ListCard {
                InfoRow("PSA Legacy", "SMARTAPPS V1 · offline · MIT")
                ListDivider()
                InfoRow("Protocol", "AES-128-ECB · keys derived from the VIN")
                ListDivider()
                InfoRow("Network", "No INTERNET permission")
            }
        }
    }
}

private val AutoStart.label
    get() = when (this) {
        AutoStart.OFF -> "Off"
        AutoStart.CAR_CONNECTED -> "When the car connects"
        AutoStart.ALWAYS -> "Always"
    }

private val AutoStart.caption
    get() = when (this) {
        AutoStart.OFF -> "Start listening from the Connect tab"
        AutoStart.CAR_CONNECTED -> "Listen while the car's Bluetooth is connected"
        AutoStart.ALWAYS -> "Listen from boot. Never misses the car"
    }

@Composable
private fun AutoStartSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val mode by viewModel.autoStart.collectAsState()
    val carAddress by viewModel.carAddress.collectAsState()
    var batteryExempt by remember { mutableStateOf(viewModel.isIgnoringBatteryOptimizations()) }
    var pickingCar by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        // The exemption is granted in a system dialog; re-check when we come back.
        batteryExempt = viewModel.isIgnoringBatteryOptimizations()
        onPauseOrDispose { }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Auto-start")
        Caption("Start listening without opening the app.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AutoStart.entries.forEach { m ->
            ChoiceChip(m.label, m.caption, selected = m == mode, onClick = {
                viewModel.setAutoStart(m)
                viewModel.showToast("Auto-start: ${m.label.lowercase()}")
            })
        }
    }

    if (mode == AutoStart.OFF) return

    ListCard {
        if (mode == AutoStart.CAR_CONNECTED) {
            val carName = remember(carAddress) {
                viewModel.pairedDevices().find { it.address == carAddress }?.name
            }
            ListRow(icon = PsaIcons.Car, onClick = { pickingCar = true }) {
                Column(Modifier.weight(1f)) {
                    Text(carName ?: carAddress.ifEmpty { "Any device" }, style = PsaType.body)
                    Caption(
                        if (carAddress.isEmpty()) "Learned on the first car connection. Tap to pick"
                        else "$carAddress · tap to change",
                        maxLines = 1
                    )
                }
            }
            ListDivider()
        }
        ListRow(
            icon = if (batteryExempt) PsaIcons.Check else PsaIcons.Alert,
            onClick = if (batteryExempt) null else {
                {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
        ) {
            Column(Modifier.weight(1f)) {
                Text("Battery optimisation", style = PsaType.body)
                Caption(
                    if (batteryExempt) "Disabled. Android won't block auto-start"
                    else "Enabled. Android may block auto-start, tap to disable"
                )
            }
        }
    }

    if (pickingCar) {
        CarPickerSheet(
            devices = remember { viewModel.pairedDevices() },
            selected = carAddress,
            onPick = {
                viewModel.setCarAddress(it)
                pickingCar = false
            },
            onDismiss = { pickingCar = false }
        )
    }
}

@Composable
private fun VehicleSection(viewModel: MainViewModel) {
    val vin by viewModel.vin.collectAsState()
    val override by viewModel.vehicleModel.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()
    var picking by remember { mutableStateOf(false) }
    val fromVin = VinDecoder.decode(vin).name?.let { "Detected from the VIN: $it" } ?: "Not recognised from the VIN"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Vehicle")
        Caption("Sets the photo and name on the Car tab.")
    }
    ListCard {
        ListRow(icon = PsaIcons.Car, onClick = { picking = true }) {
            Column(Modifier.weight(1f)) {
                Text(vehicle.name ?: "Unknown model", style = PsaType.body)
                Caption(if (override == null) "$fromVin · tap to change" else "Set manually · tap to change", maxLines = 1)
            }
        }
    }

    if (picking) {
        PsaSheet("Vehicle", { picking = false }) {
            ChoiceChip("Automatic", fromVin, selected = override == null, onClick = {
                viewModel.setVehicleModel(null)
                picking = false
            })
            VehicleModel.entries.forEach { m ->
                ChoiceChip("Peugeot ${m.label}", "Use the ${m.label} photo", selected = m == override, onClick = {
                    viewModel.setVehicleModel(m)
                    picking = false
                })
            }
        }
    }
}

@Composable
private fun CarPickerSheet(
    devices: List<MainViewModel.PairedDevice>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PsaSheet("Car", onDismiss) {
        Caption("Paired devices. Auto-start follows this one's Bluetooth link.")
        if (devices.isEmpty()) {
            Caption("No paired devices, or Bluetooth permission missing.")
        }
        devices.forEach { d ->
            ChoiceChip(d.name, d.address, selected = d.address == selected, onClick = { onPick(d.address) })
        }
        ChoiceChip(
            "Any device",
            "Learn the car on its next connection",
            selected = selected.isEmpty(),
            onClick = { onPick("") }
        )
    }
}

@Composable
private fun InfoRow(title: String, caption: String) {
    ListRow {
        Column(Modifier.weight(1f)) {
            Text(title, style = PsaType.body)
            Caption(caption, maxLines = 1)
        }
    }
}

@Composable
private fun ThemeChip(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val preview = theme.colors()
    ChoiceChip(theme.label, theme.caption, selected, onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Swatch(preview.bg)
            Swatch(preview.surface)
            Swatch(preview.accent)
        }
    }
}

@Composable
private fun ChoiceChip(
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    val c = PsaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(Radius.md)
            .background(c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.line, Radius.md)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = PsaType.body)
            Caption(caption)
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(14.dp)
            .clip(shape)
            .background(color)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), shape)
    )
}

package it.danielezotta.psalegacy.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.ui.theme.StatusAmber
import it.danielezotta.psalegacy.ui.theme.StatusGreen

@Composable
fun ConnectScreen(viewModel: MainViewModel) {
    val vin by viewModel.vin.collectAsState()
    val state by viewModel.connState.collectAsState()
    val isListening = state is AppState.ConnState.Listening || state is AppState.ConnState.Connected
    val useBrand by viewModel.useBrandUuid.collectAsState()
    val useSpp by viewModel.useSppUuid.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vehicle VIN", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = vin,
                    onValueChange = viewModel::setVin,
                    label = { Text("17-char VIN") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        StatusCard(state)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SwitchRow(
                    title = "Peugeot brand UUID",
                    subtitle = "Also listen on the brand UUID",
                    checked = useBrand,
                    onCheckedChange = viewModel::setUseBrandUuid
                )
                SwitchRow(
                    title = "SPP UUID",
                    subtitle = "Fallback serial port profile",
                    checked = useSpp,
                    onCheckedChange = viewModel::setUseSppUuid
                )
            }
        }

        Button(
            onClick = viewModel::toggleListening,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (isListening) "Stop listening" else "Start listening",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Text(
            "1. Pair the phone with the car in Bluetooth settings.\n" +
                "2. Press \"Start listening\".\n" +
                "3. Open the connected-apps / MyPeugeot screen in the car to connect.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusCard(state: AppState.ConnState) {
    val listening = state is AppState.ConnState.Listening
    val dotColor: Color
    val label: String
    when (state) {
        is AppState.ConnState.Idle -> {
            dotColor = MaterialTheme.colorScheme.outline
            label = "Idle"
        }

        is AppState.ConnState.Listening -> {
            dotColor = StatusAmber
            label = "Listening — waiting for the car…"
        }

        is AppState.ConnState.Connected -> {
            dotColor = StatusGreen
            label = "Connected"
        }

        is AppState.ConnState.Error -> {
            dotColor = MaterialTheme.colorScheme.error
            label = "Error: ${state.message}"
        }
    }
    val pulse by if (listening) {
        rememberInfiniteTransition(label = "statusPulse").animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "statusPulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .alpha(if (listening) pulse else 1f)
            )
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

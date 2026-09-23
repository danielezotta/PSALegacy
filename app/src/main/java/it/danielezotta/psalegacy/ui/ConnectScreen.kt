package it.danielezotta.psalegacy.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.protocol.ProtocolConstants
import it.danielezotta.psalegacy.ui.components.ButtonVariant
import it.danielezotta.psalegacy.ui.components.Caption
import it.danielezotta.psalegacy.ui.components.Eyebrow
import it.danielezotta.psalegacy.ui.components.ListCard
import it.danielezotta.psalegacy.ui.components.ListDivider
import it.danielezotta.psalegacy.ui.components.ListRow
import it.danielezotta.psalegacy.ui.components.PsaButton
import it.danielezotta.psalegacy.ui.components.PsaCard
import it.danielezotta.psalegacy.ui.components.PsaIcons
import it.danielezotta.psalegacy.ui.components.PsaSwitch
import it.danielezotta.psalegacy.ui.components.Radius
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType

private const val VIN_HINT = "17 characters · e.g. VF3HZBHZ2JS123456"
private const val VIN_EMPTY_ERROR = "Enter the 17-character VIN (windscreen or registration document)"
private const val VIN_FORMAT_ERROR = "The VIN must be 17 alphanumeric characters, without I, O or Q"

@Composable
fun ConnectScreen(viewModel: MainViewModel) {
    val vin by viewModel.vin.collectAsState()
    val state by viewModel.connState.collectAsState()
    val isListening = state is AppState.ConnState.Listening || state is AppState.ConnState.Connected
    val useBrand by viewModel.useBrandUuid.collectAsState()
    val useSpp by viewModel.useSppUuid.collectAsState()

    var vinError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    fun validationError(value: String): String? = when {
        value.isEmpty() -> VIN_EMPTY_ERROR
        !MainViewModel.isValidVin(value) -> VIN_FORMAT_ERROR
        else -> null
    }

    ScreenColumn {
        PsaCard {
            Eyebrow("Vehicle VIN")
            Caption(
                vinError ?: VIN_HINT,
                color = if (vinError != null) PsaTheme.colors.dangerInk else PsaTheme.colors.text2,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            VinInput(
                value = vin,
                isError = vinError != null,
                focusRequester = focusRequester,
                onValueChange = { raw ->
                    val cleaned = raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
                        .take(ProtocolConstants.VIN_SIZE)
                    viewModel.setVin(cleaned)
                    vinError = null
                },
                onBlur = { vinError = validationError(vin) }
            )
        }

        StatusCard(state)

        ListCard {
            ListRow(icon = PsaIcons.Bluetooth) {
                Column(Modifier.weight(1f)) {
                    Text("Peugeot brand UUID", style = PsaType.body)
                    Caption("Also listen on the brand UUID ${ProtocolConstants.PEUGEOT_BRAND_UUID.toString().take(8)}-…")
                }
                PsaSwitch(useBrand, {
                    viewModel.setUseBrandUuid(it)
                    viewModel.showToast(if (it) "Brand UUID enabled" else "Brand UUID disabled")
                }, "Peugeot brand UUID")
            }
            ListDivider()
            ListRow(icon = PsaIcons.Bluetooth) {
                Column(Modifier.weight(1f)) {
                    Text("SPP UUID", style = PsaType.body)
                    Caption("Serial fallback ${ProtocolConstants.SPP_UUID.toString().take(8)}-…")
                }
                PsaSwitch(useSpp, {
                    viewModel.setUseSppUuid(it)
                    viewModel.showToast(if (it) "SPP enabled" else "SPP disabled")
                }, "SPP UUID")
            }
        }

        PsaButton(
            text = if (isListening) "Stop listening" else "Start listening",
            icon = if (isListening) PsaIcons.Stop else PsaIcons.Play,
            variant = ButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!isListening) {
                    val error = validationError(vin)
                    if (error != null) {
                        vinError = error
                        focusRequester.requestFocus()
                        return@PsaButton
                    }
                }
                viewModel.toggleListening()
                if (isListening) viewModel.showToast("Listening stopped")
            }
        )

        PsaCard {
            Eyebrow("How it works")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Step(1, "Pair the phone with the car in the system Bluetooth settings.")
                Step(2, "Enter the 17-character VIN and start listening.")
                Step(3, "On the head unit, open MyPeugeot / Connected Apps to connect.")
            }
        }

        PsaCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(PsaIcons.Info, null, tint = PsaTheme.colors.text2, modifier = Modifier.size(18.dp))
                Caption(
                    "SMARTAPPS V1 (Altran) · UUID ${ProtocolConstants.SMARTAPP_UUID} · no network, no cloud.",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun VinInput(
    value: String,
    isError: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit
) {
    val c = PsaTheme.colors
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        focused -> c.accentInk
        isError -> c.danger
        else -> c.line
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = PsaType.input.copy(color = c.text),
        cursorBrush = SolidColor(c.accentInk),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (wasFocused && !it.isFocused) onBlur()
                wasFocused = it.isFocused
            }
            .semantics {
                contentDescription = "Vehicle VIN"
                if (isError) error("Invalid VIN")
            },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(Radius.sm)
                    .background(c.bg)
                    .border(if (focused) 2.dp else 1.dp, borderColor, Radius.sm)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) { inner() }
        }
    )
}

@Composable
private fun StatusCard(state: AppState.ConnState) {
    val c = PsaTheme.colors
    val (dotColor: Color, label: String, message: String) = when (state) {
        is AppState.ConnState.Idle -> Triple(c.text2, "Idle", "Enter the VIN and start listening")
        is AppState.ConnState.Listening -> Triple(c.listening, "Listening", "Waiting for the head unit to connect")
        is AppState.ConnState.Connected -> Triple(c.ok, "Connected", "SMARTAPPS V1 session active")
        is AppState.ConnState.Error -> Triple(c.danger, "Error", state.message.ifBlank { "Connection error" })
    }
    val pulse by if (state is AppState.ConnState.Listening) {
        rememberInfiniteTransition(label = "statusPulse").animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "statusPulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(c.surface)
            .border(1.dp, c.line, Radius.md)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(12.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(dotColor)
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = PsaType.body)
            Caption(message)
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    val c = PsaTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(c.elevated),
            contentAlignment = Alignment.Center
        ) {
            Text(n.toString(), style = PsaType.badge, color = c.text2)
        }
        Caption(text, modifier = Modifier.weight(1f))
    }
}

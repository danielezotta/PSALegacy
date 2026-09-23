package it.danielezotta.psalegacy.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.ui.components.ButtonVariant
import it.danielezotta.psalegacy.ui.components.Caption
import it.danielezotta.psalegacy.ui.components.ConfirmDialog
import it.danielezotta.psalegacy.ui.components.Eyebrow
import it.danielezotta.psalegacy.ui.components.PsaButton
import it.danielezotta.psalegacy.ui.components.PsaIcons
import it.danielezotta.psalegacy.ui.components.Radius
import it.danielezotta.psalegacy.ui.components.minWidth
import it.danielezotta.psalegacy.ui.theme.PsaColors
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RawLogScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val c = PsaTheme.colors
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    // Keep pinned to the newest entry.
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Eyebrow("Wire traffic")
            Caption("Cleartext SMARTAPPS V1 frames, for UUID diagnostics.")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PsaButton(
                "Copy", icon = PsaIcons.Copy, variant = ButtonVariant.Secondary, compact = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (logs.isEmpty()) {
                        viewModel.showToast("No log to copy")
                    } else {
                        clipboard.setText(AnnotatedString(viewModel.logText()))
                        viewModel.showToast("Log copied")
                    }
                }
            )
            PsaButton(
                "Share", icon = PsaIcons.Share, variant = ButtonVariant.Secondary, compact = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (logs.isEmpty()) {
                        viewModel.showToast("No log to share")
                    } else {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "PSA Legacy raw log")
                            putExtra(Intent.EXTRA_TEXT, viewModel.logText())
                        }
                        context.startActivity(Intent.createChooser(send, "Share raw log"))
                    }
                }
            )
            PsaButton(
                "Clear", icon = PsaIcons.Trash, variant = ButtonVariant.Secondary, compact = true,
                modifier = Modifier.weight(1f),
                onClick = { confirmClear = true }
            )
        }

        val panel = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(Radius.md)
            .background(c.surface)
            .border(1.dp, c.line, Radius.md)
            .semantics { contentDescription = "Protocol log" }
        if (logs.isEmpty()) {
            Column(panel.padding(12.dp)) {
                Caption("No frames yet. Start listening to see the traffic.")
            }
        } else {
            LazyColumn(panel, state = listState, contentPadding = PaddingValues(12.dp)) {
                items(logs) { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(timeFormat.format(Date(entry.timestampMs)), style = PsaType.log, color = c.text2, softWrap = false)
                        Text(
                            entry.tag.name,
                            style = PsaType.log.copy(fontWeight = FontWeight.SemiBold),
                            color = tagColor(c, entry.tag),
                            softWrap = false,
                            modifier = Modifier.minWidth(44.dp)
                        )
                        Text(entry.message, style = PsaType.log, color = c.text, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear the log?",
            message = "The recorded wire traffic will be erased. This can't be undone.",
            confirmLabel = "Clear",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.clearLog()
                confirmClear = false
                viewModel.showToast("Log cleared")
            },
            onDismiss = { confirmClear = false }
        )
    }
}

private fun tagColor(c: PsaColors, tag: AppState.LogTag): Color = when (tag) {
    AppState.LogTag.RX -> c.accentInk
    AppState.LogTag.TX -> c.okInk
    AppState.LogTag.ERR -> c.dangerInk
    AppState.LogTag.STATE -> c.warnInk
    AppState.LogTag.INFO -> c.text2
}

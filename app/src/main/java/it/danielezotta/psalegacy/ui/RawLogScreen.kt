package it.danielezotta.psalegacy.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.danielezotta.psalegacy.AppState
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

    // Keep pinned to the newest entry.
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Raw log (${logs.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { clipboard.setText(AnnotatedString(viewModel.logText())) }) {
                Text("Copy")
            }
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "PSA Legacy raw log")
                    putExtra(Intent.EXTRA_TEXT, viewModel.logText())
                }
                context.startActivity(Intent.createChooser(send, "Share raw log"))
            }) {
                Text("Share")
            }
            TextButton(onClick = viewModel::clearLog) {
                Text("Clear")
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(logs) { entry ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp)) {
                    Text(
                        text = timeFormat.format(Date(entry.timestampMs)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " ${entry.tag.name.padEnd(5)} ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = tagColor(entry.tag)
                    )
                    Text(
                        text = entry.message,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun tagColor(tag: AppState.LogTag): Color = when (tag) {
    AppState.LogTag.RX -> Color(0xFF1565C0)
    AppState.LogTag.TX -> Color(0xFF2E9E4F)
    AppState.LogTag.ERR -> MaterialTheme.colorScheme.error
    AppState.LogTag.STATE -> Color(0xFF6A1B9A)
    AppState.LogTag.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
}

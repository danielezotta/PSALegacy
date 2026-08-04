package it.danielezotta.psalegacy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import it.danielezotta.psalegacy.R
import it.danielezotta.psalegacy.ui.theme.AppTheme
import it.danielezotta.psalegacy.ui.theme.BrandAccent
import it.danielezotta.psalegacy.ui.theme.BrandBackground
import it.danielezotta.psalegacy.ui.theme.ClusterAccent
import it.danielezotta.psalegacy.ui.theme.ClusterBackground
import it.danielezotta.psalegacy.ui.theme.Purple40

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val theme by viewModel.theme.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                AppTheme.entries.forEach { t ->
                    ThemeRow(t, selected = t == theme, onClick = { viewModel.setTheme(t) })
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text(
                    "PSA Legacy — standalone SMARTAPPS V1 client.\nNo cloud. All data stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val swatches = when (theme) {
        AppTheme.BRAND -> listOf(BrandBackground, BrandAccent)
        AppTheme.LIGHT -> listOf(Color(0xFFFAF8FF), Purple40)
        AppTheme.CLUSTER -> listOf(ClusterBackground, ClusterAccent)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row {
            swatches.forEachIndexed { index, color ->
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .background(color)
                        .then(if (index > 0) Modifier.offset(x = (-8).dp) else Modifier)
                )
            }
        }
        Text(theme.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.settings_theme_selected),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

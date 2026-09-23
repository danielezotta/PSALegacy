package it.danielezotta.psalegacy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Scrolling screen body (`.screen`, or `.screen-tight` when [tight]). */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    tight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (tight) 12.dp else 16.dp,
                bottom = if (tight) 24.dp else 32.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (tight) 16.dp else 24.dp),
        content = content
    )
}

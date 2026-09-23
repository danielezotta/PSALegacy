package it.danielezotta.psalegacy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType
import kotlinx.coroutines.delay

private val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

/** Bottom sheet with a title row and close action (`.sheet`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsaSheet(
    title: String,
    onDismiss: () -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = PsaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = c.surface,
        contentColor = c.text,
        scrimColor = c.scrim,
        tonalElevation = 0.dp,
        // No outline modifier: ModalBottomSheet applies `modifier` to a container that
        // doesn't follow the sheet's offset, so a border draws as a stray rectangle.
        dragHandle = null
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(title, Modifier.weight(1f))
                IconAction(PsaIcons.Close, "Close", onDismiss)
            }
            content()
            if (footer != null) {
                Column(
                    Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = footer
                )
            }
        }
    }
}

/** Centered confirmation dialog (`.dialog`). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = PsaTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(Radius.md)
                .background(c.surface)
                .border(1.dp, c.line, Radius.md)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(title)
            Caption(message)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PsaButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), ButtonVariant.Danger)
                PsaButton(dismissLabel, onDismiss, Modifier.fillMaxWidth(), ButtonVariant.Secondary)
            }
        }
    }
}

data class ToastMessage(val text: String, val id: Long = System.nanoTime())

/** Transient confirmation shown above the tab bar for 2.4 s (`.toast`). */
@Composable
fun PsaToast(message: ToastMessage?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = PsaTheme.colors
    // Keep the last text around so the fade-out still has something to draw.
    var shown by remember { mutableStateOf(message) }
    if (message != null) shown = message
    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(2400)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(Motion.BASE, easing = Motion.EaseEnter)),
        exit = fadeOut(tween(Motion.FAST, easing = Motion.EaseExit)),
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(Radius.sm)
                .background(c.elevated)
                .border(1.dp, c.line, Radius.sm)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(PsaIcons.Check, contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
            Text(shown?.text.orEmpty(), style = PsaType.body, color = c.text, modifier = Modifier.weight(1f))
        }
    }
}

package it.danielezotta.psalegacy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.danielezotta.psalegacy.ui.theme.PsaColors
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType

// ---- Tokens ----------------------------------------------------------------

object Radius {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
}

object Motion {
    const val FAST = 160
    const val BASE = 220
    const val SLOW = 320
    val EaseEnter = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val EaseExit = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

// ---- Text ------------------------------------------------------------------

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, style = PsaType.eyebrow, color = PsaTheme.colors.text2)
}

@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PsaTheme.colors.text2,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null
) {
    Text(
        text, modifier = modifier, style = PsaType.caption, color = color,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = textAlign
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = PsaType.sectionTitle, color = PsaTheme.colors.text)
}

/** Monospaced data value that never wraps (`.num.od-nowrap`). */
@Composable
fun NumText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text, modifier = modifier, style = PsaType.num, color = PsaTheme.colors.text,
        maxLines = 1, softWrap = false, textAlign = textAlign
    )
}

/** "18.1 km" with the unit rendered as the design's muted `<small>`. */
@Composable
fun valueWithUnit(value: String, unit: String?): AnnotatedString {
    val c = PsaTheme.colors
    return buildAnnotatedString {
        append(value)
        if (unit != null && value != "—") {
            append(" ")
            withStyle(
                SpanStyle(
                    fontFamily = PsaType.unitSmall.fontFamily,
                    fontWeight = PsaType.unitSmall.fontWeight,
                    fontSize = PsaType.unitSmall.fontSize,
                    color = c.text2
                )
            ) { append(unit) }
        }
    }
}

// ---- Containers ------------------------------------------------------------

@Composable
fun PsaCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = PsaTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(c.surface)
            .border(1.dp, c.line, Radius.md)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/** Bordered container whose rows are separated by hairlines; insert [ListDivider] between rows. */
@Composable
fun ListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = PsaTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(c.surface)
            .border(1.dp, c.line, Radius.md),
        content = content
    )
}

@Composable
fun ListDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(PsaTheme.colors.line))
}

/** `.row-btn`: 56dp min row with 12dp gaps, optional leading icon. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    val c = PsaTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.text2)
        content()
    }
}

/** Label over a value, the design's `.od-field`. */
@Composable
fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    numeric: Boolean = true
) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Caption(label)
        if (numeric) {
            NumText(value)
        } else {
            Text(value, style = PsaType.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---- Buttons ---------------------------------------------------------------

enum class ButtonVariant { Primary, Secondary, Danger }

@Composable
fun PsaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val c = PsaTheme.colors
    val (bg, fg) = when (variant) {
        ButtonVariant.Primary -> c.accentFill to c.onAccent
        ButtonVariant.Secondary -> c.surface to c.text
        ButtonVariant.Danger -> c.danger to Color.White
    }
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 48.dp)
            .clip(Radius.sm)
            .background(bg)
            .then(if (variant == ButtonVariant.Secondary) Modifier.border(1.dp, c.line, Radius.sm) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = PsaType.button, color = fg, maxLines = 1, softWrap = false)
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(Radius.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, style = PsaType.chip, color = PsaTheme.colors.accentInk)
    }
}

@Composable
fun IconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(Radius.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

// ---- Switch ----------------------------------------------------------------

@Composable
fun PsaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, contentDescription: String) {
    val c = PsaTheme.colors
    val spec = tween<Dp>(Motion.BASE, easing = Motion.EaseEnter)
    val thumbX by animateDpAsState(if (checked) 20.dp else 0.dp, spec, label = "switchThumb")
    val track by animateColorAsState(
        if (checked) c.accentFill else c.line, tween(Motion.BASE, easing = Motion.EaseEnter), label = "switchTrack"
    )
    val thumb by animateColorAsState(
        if (checked) c.onAccent else c.text, tween(Motion.BASE, easing = Motion.EaseEnter), label = "switchThumbColor"
    )
    Box(
        Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription }
            .padding(4.dp)
    ) {
        Box(Modifier.offset(x = thumbX).size(24.dp).clip(CircleShape).background(thumb))
    }
}

// ---- Pills, chips, badges --------------------------------------------------

enum class PillTone { Accent, Ok, Warn, Muted }

@Composable
fun Pill(text: String, tone: PillTone = PillTone.Accent, modifier: Modifier = Modifier) {
    val c = PsaTheme.colors
    val (bg, fg) = pillColors(c, tone)
    Box(
        modifier
            .heightIn(min = 26.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = PsaType.pill, color = fg, maxLines = 1, softWrap = false)
    }
}

private fun pillColors(c: PsaColors, tone: PillTone): Pair<Color, Color> = when (tone) {
    PillTone.Accent -> c.accentSoft to c.accentInk
    PillTone.Ok -> c.ok.copy(alpha = 0.2f) to c.okInk
    PillTone.Warn -> c.warn.copy(alpha = 0.2f) to c.warnInk
    PillTone.Muted -> c.elevated to c.text2
}

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = PsaTheme.colors
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (selected) c.accentSoft else c.surface)
            .border(1.dp, if (selected) c.accent else c.line, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = PsaType.chip, color = if (selected) c.accentInk else c.text, maxLines = 1)
    }
}

@Composable
fun Badge(count: Int) {
    val c = PsaTheme.colors
    Box(
        Modifier
            .height(20.dp)
            .widthIn(min = 20.dp)
            .clip(CircleShape)
            .background(c.accentFill)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(count.toString(), style = PsaType.badge, color = c.onAccent)
    }
}

/** Severity dot for vehicle alerts: HIGH → danger, MEDIUM/unknown → warn. */
@Composable
fun SeverityDot(criticity: String?) {
    val c = PsaTheme.colors
    Box(
        Modifier
            .padding(top = 7.dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(if (criticity == "HIGH") c.danger else c.warn)
    )
}

// ---- Stats -----------------------------------------------------------------

@Composable
fun StatCard(label: String, value: AnnotatedString, modifier: Modifier = Modifier) {
    val c = PsaTheme.colors
    Column(
        modifier
            .clip(Radius.md)
            .background(c.surface)
            .border(1.dp, c.line, Radius.md)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label.uppercase(), style = PsaType.statKey, color = c.text2, maxLines = 1)
        Text(
            value, style = PsaType.statValue, color = c.text,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun StatGrid(vararg stats: Pair<String, AnnotatedString>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((label, value) in stats) StatCard(label, value, Modifier.weight(1f))
    }
}

/** Filled track for fuel level (`.progress-track` / `.progress-fill`). */
@Composable
fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier) {
    val c = PsaTheme.colors
    val animated by androidx.compose.animation.core.animateFloatAsState(
        fraction.coerceIn(0f, 1f), tween(Motion.BASE, easing = Motion.EaseEnter), label = "progress"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(c.elevated)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .background(c.accent)
        )
    }
}

/** Min-width helper used where the design reserves space (e.g. log tags). */
fun Modifier.minWidth(width: Dp) = this.defaultMinSize(minWidth = width)

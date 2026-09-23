package it.danielezotta.psalegacy.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

enum class AppTheme(val label: String, val caption: String) {
    BRAND("Brand", "Graphite + Peugeot red"),
    LIGHT("Light", "Material 3 light"),
    CLUSTER("Cluster", "Dashboard + cyan")
}

fun AppTheme.colors(): PsaColors = when (this) {
    AppTheme.BRAND -> BrandColors
    AppTheme.LIGHT -> LightColors
    AppTheme.CLUSTER -> ClusterColors
}

object PsaTheme {
    val colors: PsaColors
        @Composable @ReadOnlyComposable get() = LocalPsaColors.current
}

@Composable
fun PsaLegacyTheme(
    theme: AppTheme = AppTheme.BRAND,
    content: @Composable () -> Unit
) {
    val c = theme.colors()
    // Material components (sheets, ripples) still read the Material scheme, so
    // mirror the design tokens into it.
    val scheme = if (c.isLight) {
        lightColorScheme(
            primary = c.accentFill, onPrimary = c.onAccent,
            secondary = c.accent, tertiary = c.warn,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.elevated, onSurfaceVariant = c.text2,
            surfaceContainerLow = c.surface,
            outline = c.line, error = c.danger, scrim = c.scrim
        )
    } else {
        darkColorScheme(
            primary = c.accentFill, onPrimary = c.onAccent,
            secondary = c.accent, tertiary = c.warn,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.elevated, onSurfaceVariant = c.text2,
            surfaceContainerLow = c.surface,
            outline = c.line, error = c.danger, scrim = c.scrim
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(bodyLarge = PsaType.body, bodyMedium = PsaType.body)
    ) {
        CompositionLocalProvider(
            LocalPsaColors provides c,
            LocalContentColor provides c.text,
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = c.accent,
                backgroundColor = c.accent.copy(alpha = 0.3f)
            ),
            content = content
        )
    }
}

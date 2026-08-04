package it.danielezotta.psalegacy.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(val label: String) {
    BRAND("Brand"),
    LIGHT("Light"),
    CLUSTER("Cluster")
}

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val BrandColorScheme = darkColorScheme(
    primary = BrandAccent,
    onPrimary = Color.White,
    secondary = BrandAccent,
    tertiary = BrandAccent,
    background = BrandBackground,
    onBackground = BrandText,
    surface = BrandSurface,
    onSurface = BrandText,
    surfaceVariant = BrandSurface,
    onSurfaceVariant = BrandMuted,
    outline = Color(0xFF3A404C),
    error = Color(0xFFFF6B5E)
)

private val ClusterColorScheme = darkColorScheme(
    primary = ClusterAccent,
    onPrimary = Color(0xFF0A0E14),
    secondary = ClusterAccent,
    tertiary = ClusterWarning,
    background = ClusterBackground,
    onBackground = ClusterText,
    surface = ClusterSurface,
    onSurface = ClusterText,
    surfaceVariant = ClusterSurface,
    onSurfaceVariant = ClusterMuted,
    outline = Color(0xFF1E2A3C),
    error = Color(0xFFFF6B5E)
)

@Composable
fun PsaLegacyTheme(
    theme: AppTheme = AppTheme.BRAND,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.BRAND -> BrandColorScheme
        AppTheme.CLUSTER -> ClusterColorScheme
        // "Light" is a fixed theme choice (like Brand/Cluster): it must render
        // light even when the system is in dark mode.
        AppTheme.LIGHT -> if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(LocalContext.current)
        } else {
            LightColorScheme
        }
    }
    val typography = when (theme) {
        AppTheme.BRAND -> BrandTypography
        AppTheme.CLUSTER -> ClusterTypography
        AppTheme.LIGHT -> Typography
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

package it.danielezotta.psalegacy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Design tokens shared by the three themes (see design handoff: --color-*). */
@Immutable
data class PsaColors(
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val line: Color,
    val text: Color,
    val text2: Color,
    val accent: Color,
    val accentInk: Color,
    val accentFill: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val warn: Color,
    val warnInk: Color,
    val danger: Color,
    val dangerInk: Color,
    val ok: Color,
    val okInk: Color,
    val scrim: Color,
    val isLight: Boolean
) {
    /** Amber pulse used by the "listening" status dot in every theme. */
    val listening: Color get() = StatusAmber
}

val StatusAmber = Color(0xFFF5A623)

// Brand — graphite + Peugeot red
val BrandColors = PsaColors(
    bg = Color(0xFF16181D),
    surface = Color(0xFF1F232B),
    elevated = Color(0xFF2A303B),
    line = Color(0xFF333A47),
    text = Color(0xFFF2F2F2),
    text2 = Color(0xFF9AA0AA),
    accent = Color(0xFFE63329),
    accentInk = Color(0xFFFF6A61),
    accentFill = Color(0xFFD32B22),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFF3A1F1E),
    warn = Color(0xFFFFB450),
    warnInk = Color(0xFFFFB450),
    danger = Color(0xFFFF5C5C),
    dangerInk = Color(0xFFFF5C5C),
    ok = Color(0xFF2E9E4F),
    okInk = Color(0xFF4FBF70),
    scrim = Color(0x9E06080C),
    isLight = false
)

// Light — Material 3 light with Peugeot red
val LightColors = PsaColors(
    bg = Color(0xFFF6F7FA),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFECEFF5),
    line = Color(0xFFD7DDE8),
    text = Color(0xFF12151C),
    text2 = Color(0xFF4A5568),
    accent = Color(0xFFE63329),
    accentInk = Color(0xFFC42B22),
    accentFill = Color(0xFFD32B22),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFFBE3E1),
    warn = Color(0xFF8A5A00),
    warnInk = Color(0xFF8A5A00),
    danger = Color(0xFFB32E2E),
    dangerInk = Color(0xFFB32E2E),
    ok = Color(0xFF0A6B52),
    okInk = Color(0xFF0A6B52),
    scrim = Color(0x6B12151C),
    isLight = true
)

// Cluster — instrument cluster, cyan glow
val ClusterColors = PsaColors(
    bg = Color(0xFF0A0E14),
    surface = Color(0xFF0F1622),
    elevated = Color(0xFF17202E),
    line = Color(0xFF243042),
    text = Color(0xFFE8F0F8),
    text2 = Color(0xFF7A8798),
    accent = Color(0xFF3FD2F8),
    accentInk = Color(0xFF3FD2F8),
    accentFill = Color(0xFF0E7A99),
    onAccent = Color(0xFF041018),
    accentSoft = Color(0xFF0C2A36),
    warn = Color(0xFFFFB450),
    warnInk = Color(0xFFFFB450),
    danger = Color(0xFFFF5C5C),
    dangerInk = Color(0xFFFF5C5C),
    ok = Color(0xFF2E9E4F),
    okInk = Color(0xFF4FBF70),
    scrim = Color(0xA802050A),
    isLight = false
)

/** Original Peugeot navy of the wordmark, used on light backgrounds. */
val PeugeotNavy = Color(0xFF002355)

val LocalPsaColors = staticCompositionLocalOf { BrandColors }

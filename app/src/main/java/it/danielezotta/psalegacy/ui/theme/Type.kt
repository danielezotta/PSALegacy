package it.danielezotta.psalegacy.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.danielezotta.psalegacy.R

/** Weight 650 is used throughout the design for labels, buttons and chips. */
val Weight650 = FontWeight(650)

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val SoraFamily = FontFamily(
    variable(R.font.sora_variable, FontWeight.SemiBold),
    variable(R.font.sora_variable, FontWeight.Bold)
)

val PlexSansFamily = FontFamily(
    variable(R.font.ibm_plex_sans_variable, FontWeight.Normal),
    variable(R.font.ibm_plex_sans_variable, FontWeight.Medium),
    variable(R.font.ibm_plex_sans_variable, FontWeight.SemiBold),
    variable(R.font.ibm_plex_sans_variable, Weight650),
    variable(R.font.ibm_plex_sans_variable, FontWeight.Bold)
)

val PlexMonoFamily = FontFamily(
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold)
)

/** Type scale from the design handoff (display = Sora, body = Plex Sans, data = Plex Mono). */
object PsaType {
    val appbarTitle = TextStyle(
        fontFamily = SoraFamily, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 1.15.em, letterSpacing = (-0.02).em
    )
    val sectionTitle = TextStyle(
        fontFamily = SoraFamily, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 1.15.em, letterSpacing = (-0.02).em
    )
    val body = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 1.6.em
    )
    val caption = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 1.45.em
    )
    val eyebrow = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = Weight650,
        fontSize = 12.sp, lineHeight = 1.6.em, letterSpacing = 0.08.em
    )
    val statKey = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = Weight650,
        fontSize = 11.sp, lineHeight = 1.6.em, letterSpacing = 0.05.em
    )
    val statValue = TextStyle(
        fontFamily = PlexMonoFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 1.6.em
    )
    val unitSmall = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp
    )
    val num = TextStyle(
        fontFamily = PlexMonoFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 1.6.em
    )
    val odometer = TextStyle(
        fontFamily = PlexMonoFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp, lineHeight = 1.em, letterSpacing = (-0.02).em
    )
    val button = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = Weight650,
        fontSize = 16.sp, lineHeight = 1.4.em
    )
    val chip = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = Weight650,
        fontSize = 14.sp, lineHeight = 1.4.em
    )
    val pill = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = Weight650,
        fontSize = 12.sp, lineHeight = 1.4.em
    )
    val badge = TextStyle(
        fontFamily = PlexMonoFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 1.2.em
    )
    val tabLabel = TextStyle(
        fontFamily = PlexSansFamily, fontWeight = Weight650,
        fontSize = 10.sp, lineHeight = 1.4.em, letterSpacing = 0.01.em
    )
    val log = TextStyle(
        fontFamily = PlexMonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 1.55.em
    )
    val input = TextStyle(
        fontFamily = PlexMonoFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, letterSpacing = 0.06.em
    )
}

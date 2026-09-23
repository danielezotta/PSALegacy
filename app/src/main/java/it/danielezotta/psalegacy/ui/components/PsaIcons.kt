package it.danielezotta.psalegacy.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icon set from the design (24×24 viewBox, 1.75 stroke, round caps/joins).
 * Drawn in black and tinted by [androidx.compose.material3.Icon].
 */
object PsaIcons {
    val Bluetooth = icon("M7 8l10 8-5 4V4l5 4-10 8")
    val Route = icon(circle(6f, 19f, 2f), circle(18f, 5f, 2f), "M8 19h5a4 4 0 0 0 0-8H9a4 4 0 0 1 0-8h5")
    val Car = icon(
        "M5 11 6.5 6.5A2 2 0 0 1 8.4 5h7.2a2 2 0 0 1 1.9 1.5L19 11",
        "M4 11h16v6H4z",
        circle(7.5f, 17.5f, 1.5f), circle(16.5f, 17.5f, 1.5f)
    )
    val Log = icon("M5 5h14v14H5z", "M8 9h8", "M8 12h8", "M8 15h5")
    val Settings = icon(
        circle(12f, 12f, 3f), "M12 3v2", "M12 19v2", "M3 12h2", "M19 12h2",
        "M5.6 5.6l1.4 1.4", "M17 17l1.4 1.4", "M5.6 18.4l1.4-1.4", "M17 7l1.4-1.4"
    )
    val Play = icon("M8 5.5v13l11-6.5-11-6.5Z")
    val Stop = icon("M7 7h10v10H7z")
    val Check = icon(circle(12f, 12f, 8f), "M8.5 12.5l2.5 2.5 4.5-5")
    val Close = icon(circle(12f, 12f, 8f), "M9.5 9.5l5 5", "M14.5 9.5l-5 5")
    val Alert = icon("M12 4 3 20h18L12 4Z", "M12 10v4", "M12 17h.01")
    val Info = icon(circle(12f, 12f, 8f), "M12 11v5", "M12 8h.01")
    val Filter = icon("M4 6h16", "M7 12h10", "M10 18h4")
    val Fuel = icon(
        "M5 20V5a2 2 0 0 1 2-2h5a2 2 0 0 1 2 2v15", "M4 20h11",
        "M14 9h3a2 2 0 0 1 2 2v5a2 2 0 0 0 2 2", "M14 12h3"
    )
    val Tool = icon("M14.7 6.3a4 4 0 0 0-5 5L4 17l3 3 5.7-5.7a4 4 0 0 0 5-5L15 12l-3-3 2.7-2.7Z")
    val Copy = icon(
        "M11 9h7a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z",
        "M5 15V5a2 2 0 0 1 2-2h8"
    )
    val Share = icon(
        circle(6f, 12f, 2f), circle(17f, 6f, 2f), circle(17f, 18f, 2f),
        "M8 11l7-4", "M8 13l7 4"
    )
    val Trash = icon(
        "M4 7h16", "M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2",
        "M6 7v13a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7"
    )
    val Bolt = icon("M13 3l-8 10h6l-1 8 8-10h-6l1-8Z")

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"

    private fun icon(vararg paths: String): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            for (d in paths) {
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.75f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
}

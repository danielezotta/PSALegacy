package it.danielezotta.psalegacy.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {

    fun km(value: Float): String =
        if (value < 0) "—" else "${groupDigits("%.1f".format(Locale.US, value))} km"

    fun kmInt(value: Float): String =
        if (value < 0) "—" else "${groupDigits("%.0f".format(Locale.US, value))} km"

    fun liters(value: Float): String =
        if (value < 0) "—" else "%.2f L".format(Locale.US, value)

    fun lPer100(distanceKm: Float, fuelL: Float): String =
        if (distanceKm <= 0f || fuelL < 0f) "—"
        else "%.1f L/100km".format(Locale.US, fuelL / distanceKm * 100f)

    fun duration(ms: Long): String {
        if (ms <= 0) return "—"
        val mins = ms / 60_000
        if (mins < 60) return "${mins.coerceAtLeast(1)} min"
        return "${mins / 60} h ${String.format(Locale.US, "%02d", mins % 60)} min"
    }

    fun ago(epochMs: Long, nowMs: Long): String {
        val diff = nowMs - epochMs
        return when {
            diff < 60_000L -> "just now"
            diff < 3_600_000L -> "${diff / 60_000} min ago"
            diff < 86_400_000L -> "${diff / 3_600_000} h ago"
            diff < 3 * 86_400_000L -> "${diff / 86_400_000} d ago"
            else -> "on ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochMs))}"
        }
    }

    fun dateLabel(epochMs: Long, nowMs: Long): String {
        val date = Date(epochMs)
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dateCal = Calendar.getInstance().apply { time = date }
        val sameDay = dateCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) &&
            dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        return if (sameDay) "Today · $time"
        else SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(date)
    }

    private fun groupDigits(plain: String): String {
        val parts = plain.split('.')
        val grouped = parts[0].reversed().chunked(3).joinToString(" ").reversed()
        return if (parts.size == 2) "$grouped.${parts[1]}" else grouped
    }
}

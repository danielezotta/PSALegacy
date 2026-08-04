package it.danielezotta.psalegacy.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test
    fun `km rounds to one decimal with unit and grouping`() {
        assertEquals("22.7 km", Format.km(22.703125f))
        assertEquals("118 373.3 km", Format.km(118373.3f))
        assertEquals("1.0 km", Format.km(1f))
    }

    @Test
    fun `kmInt rounds to whole with unit and grouping`() {
        assertEquals("118 373 km", Format.kmInt(118373.3f))
        assertEquals("390 km", Format.kmInt(390f))
    }

    @Test
    fun `negative values format as dash`() {
        assertEquals("—", Format.km(-1f))
        assertEquals("—", Format.kmInt(-1f))
        assertEquals("—", Format.liters(-1f))
        assertEquals("—", Format.lPer100(-1f, 1.5f))
        assertEquals("—", Format.lPer100(22.7f, -1f))
        assertEquals("—", Format.lPer100(0f, 1.5f))
    }

    @Test
    fun `liters rounds to two decimals with unit`() {
        assertEquals("1.51 L", Format.liters(1.50768f))
    }

    @Test
    fun `consumption computes liters per 100 km`() {
        assertEquals("6.6 L/100km", Format.lPer100(22.7f, 1.50768f))
    }

    @Test
    fun `duration formats minutes and hours`() {
        assertEquals("43 min", Format.duration(2_612_000L))
        assertEquals("1 h 05 min", Format.duration(65 * 60_000L))
        assertEquals("1 min", Format.duration(5_000L))
    }

    @Test
    fun `duration renders dash for missing or negative times`() {
        assertEquals("—", Format.duration(-1L))
        assertEquals("—", Format.duration(0L))
        assertEquals("—", Format.duration(-61 * 60_000L))
    }

    @Test
    fun `ago formats relative times`() {
        val now = 1_000_000_000_000L
        assertEquals("just now", Format.ago(now - 30_000L, now))
        assertEquals("5 min ago", Format.ago(now - 5 * 60_000L, now))
        assertEquals("2 h ago", Format.ago(now - 2 * 3_600_000L, now))
        assertEquals("just now", Format.ago(now + 60_000L, now))
    }

    @Test
    fun `dateLabel marks same-day as today`() {
        val now = 1_786_000_000_000L
        val sameDay = now - 10 * 60_000L
        assertEquals("Today · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
            .format(java.util.Date(sameDay))}", Format.dateLabel(sameDay, now))
    }

    @Test
    fun `ago renders old dates as on date`() {
        val now = 1_000_000_000_000L
        val older = now - 3 * 86_400_000L
        assertTrue(Format.ago(older, now).startsWith("on "))
    }
}

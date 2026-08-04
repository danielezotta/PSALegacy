package it.danielezotta.psalegacy.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecksumTest {

    @Test
    fun `checksum makes total sum divisible by 256`() {
        val data = byteArrayOf(0x53, 0x41, 0x00, 0x10, 0x01, 0x02, 0x03, 0x04)
        val frame = data + checksumByte(data)
        assertTrue(isValidChecksum(frame))
    }

    @Test
    fun `checksum is zero when sum already divides by 256`() {
        val data = byteArrayOf(0, 0, 0, 0, 256.toByte())
        assertEquals(0.toByte(), checksumByte(data))
    }

    @Test
    fun `a frame corrupted in the middle fails validation`() {
        val data = byteArrayOf(0x53, 0x41, 0x00, 0x10, 0x01, 0x02, 0x03, 0x04)
        val frame = (data + checksumByte(data)).toMutableList()
        frame[2] = 0x7F.toByte()
        assertFalse(isValidChecksum(frame.toByteArray()))
    }
}

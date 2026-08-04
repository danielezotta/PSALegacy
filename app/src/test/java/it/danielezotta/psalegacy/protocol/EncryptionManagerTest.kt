package it.danielezotta.psalegacy.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptionManagerTest {

    private val vin = "TESTVIN0000000001".toByteArray(Charsets.US_ASCII)

    @Test
    fun `key derivation matches reference vectors`() {
        val em = EncryptionManager()
        assertTrue(em.generateKeys(vin))
        assertArrayEquals(
            byteArrayOf(0xAC.toByte(), 0xB5.toByte(), 0xF8.toByte(), 0x45, 0xE2.toByte(), 0x32, 0x4E, 0x3A,
                0x95.toByte(), 0xEB.toByte(), 0x96.toByte(), 0xF2.toByte(), 0x10, 0x64, 0xA8.toByte(), 0x49),
            em.keyMessage
        )
        assertArrayEquals(
            byteArrayOf(0x5C, 0xF5.toByte(), 0x6E, 0x41, 0xC3.toByte(), 0x75, 0x46, 0xA1.toByte(),
                0x54, 0xD4.toByte(), 0x79, 0x1A, 0xE1.toByte(), 0x8D.toByte(), 0x60, 0x0D),
            em.keyChallenge
        )
    }

    @Test
    fun `invalid vin length rejects key generation`() {
        assertFalse(EncryptionManager().generateKeys("SHORT".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `encrypt pads to multiple of 16 and round-trips with decrypt`() {
        val em = EncryptionManager()
        em.generateKeys(vin)
        val plain = ByteArray(11) { it.toByte() }
        val encrypted = em.encrypt(plain)
        assertEquals(16, encrypted.size)
        val decrypted = em.decrypt(encrypted)
        assertEquals(16, decrypted.size)
        assertArrayEquals(plain, decrypted.copyOfRange(0, plain.size))
    }

    @Test
    fun `challenge encrypt round-trips`() {
        val em = EncryptionManager()
        em.generateKeys(vin)
        val challenge = ByteArray(32) { (it * 3).toByte() }
        assertArrayEquals(challenge, em.decryptChallenge(em.encryptChallenge(challenge)))
    }

    @Test
    fun `aes known-answer - encrypt 16 zero bytes`() {
        val em = EncryptionManager()
        em.generateKeys(vin)
        assertArrayEquals(
            byteArrayOf(
                0x15.toByte(), 0x85.toByte(), 0x6E.toByte(), 0x4D.toByte(),
                0x63.toByte(), 0x6B.toByte(), 0xFA.toByte(), 0xD1.toByte(),
                0x38.toByte(), 0x2B.toByte(), 0x7B.toByte(), 0x7C.toByte(),
                0xC7.toByte(), 0x51.toByte(), 0x0A.toByte(), 0x6E.toByte()
            ),
            em.encrypt(ByteArray(16))
        )
    }

    @Test
    fun `aes known-answer - challenge encrypt 32 bytes of 1`() {
        val em = EncryptionManager()
        em.generateKeys(vin)
        val input = ByteArray(32) { 1 }
        val expected = byteArrayOf(
            0x29.toByte(), 0x75.toByte(), 0xA6.toByte(), 0x24.toByte(),
            0xEE.toByte(), 0x75.toByte(), 0x4C.toByte(), 0xD4.toByte(),
            0x43.toByte(), 0x26.toByte(), 0x8C.toByte(), 0x98.toByte(),
            0x8B.toByte(), 0xD3.toByte(), 0x3C.toByte(), 0x6F.toByte(),
            0x29.toByte(), 0x75.toByte(), 0xA6.toByte(), 0x24.toByte(),
            0xEE.toByte(), 0x75.toByte(), 0x4C.toByte(), 0xD4.toByte(),
            0x43.toByte(), 0x26.toByte(), 0x8C.toByte(), 0x98.toByte(),
            0x8B.toByte(), 0xD3.toByte(), 0x3C.toByte(), 0x6F.toByte()
        )
        assertArrayEquals(expected, em.encryptChallenge(input))
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected.toLong(), actual.toLong())
    }
}

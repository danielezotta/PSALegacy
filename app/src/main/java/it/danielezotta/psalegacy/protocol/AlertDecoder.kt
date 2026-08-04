package it.danielezotta.psalegacy.protocol

object AlertDecoder {
    fun decodeAlertCodes(miscIndicators: ByteArray): List<Int> {
        val codes = mutableListOf<Int>()
        for (i in miscIndicators.indices) {
            for (bit in 0 until 8) {
                if ((miscIndicators[i].toInt() shr bit) and 1 == 1) {
                    codes.add(i * 8 + bit)
                }
            }
        }
        return codes
    }
}

package it.danielezotta.psalegacy.protocol

fun checksumByte(data: ByteArray): Byte {
    var sum = 0
    for (b in data) sum += b.toInt() and 0xFF
    val mod = sum and 0xFF
    return if (mod == 0) 0 else (256 - mod).toByte()
}

fun isValidChecksum(data: ByteArray): Boolean {
    var sum = 0
    for (b in data) sum += b.toInt() and 0xFF
    return sum and 0xFF == 0
}

package it.danielezotta.psalegacy.util

fun ByteArray.toHexString(): String =
    joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

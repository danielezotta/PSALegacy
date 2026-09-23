package it.danielezotta.psalegacy.data

import android.content.Context
import java.io.File

/**
 * Persists the in-memory raw log to a file so sessions can be examined
 * afterwards (the in-memory log dies with the app process).
 */
object LogStore {
    private const val FILE_NAME = "raw.log"
    const val MAX_LINES = 4000
    private const val MAX_BYTES = 512L * 1024

    private var file: File? = null

    fun init(context: Context) {
        init(File(context.filesDir, FILE_NAME))
    }

    internal fun init(file: File) {
        this.file = file
    }

    @Synchronized
    fun append(timestampMs: Long, tag: String, message: String) {
        val f = file ?: return
        f.appendText("$timestampMs $tag $message\n")
        if (f.length() > MAX_BYTES) trim(f)
    }

    @Synchronized
    fun readAll(): String = file?.takeIf { it.exists() }?.readText() ?: ""

    private fun trim(f: File) {
        val lines = f.readLines()
        if (lines.size > MAX_LINES) {
            f.writeText(lines.takeLast(MAX_LINES).joinToString("\n", postfix = "\n"))
        }
    }
}

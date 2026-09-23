package it.danielezotta.psalegacy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LogStoreTest {

    private fun tempFile(): File =
        File.createTempFile("psalegacy-log", ".log").apply { deleteOnExit() }

    @Test
    fun `append writes lines readable via readAll`() {
        val f = tempFile()
        LogStore.init(f)
        LogStore.append(1L, "INFO", "first")
        LogStore.append(2L, "ERR", "second")
        val content = LogStore.readAll()
        assertEquals(listOf("1 INFO first", "2 ERR second"), content.trim().lines())
    }

    @Test
    fun `append without init is a no-op`() {
        LogStore.init(File(""))
        assertEquals("", LogStore.readAll())
    }

    @Test
    fun `old lines are trimmed when the log exceeds the cap`() {
        val f = tempFile()
        LogStore.init(f)
        repeat(LogStore.MAX_LINES + 10) { i ->
            LogStore.append(i.toLong(), "INFO", "line $i " + "x".repeat(130))
        }
        val lines = LogStore.readAll().trim().lines()
        assertEquals(LogStore.MAX_LINES, lines.size)
        assertTrue(lines.first().contains("line 10"))
        assertTrue(lines.last().contains("line ${LogStore.MAX_LINES + 9}"))
    }
}

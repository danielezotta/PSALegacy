package it.danielezotta.psalegacy.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AcceptRetryLoopTest {

    private fun loop(
        open: () -> String?,
        accept: (String) -> String?,
        running: () -> Boolean,
        accepted: MutableList<String> = mutableListOf(),
        errors: MutableList<String> = mutableListOf(),
        closed: MutableList<String> = mutableListOf(),
    ): AcceptRetryLoop<String, String> = AcceptRetryLoop(
        open = open,
        accept = accept,
        close = { closed.add(it) },
        onAccepted = { accepted.add(it) },
        isRunning = running,
        onError = { errors.add(it) },
        retryDelayMs = 0,
        sleep = { }
    )

    @Test
    fun `accept failure reopens the listener and keeps accepting`() {
        val running = AtomicBoolean(true)
        val opened = mutableListOf<String>()
        val accepted = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val l = loop(
            open = {
                when (opened.size) {
                    0 -> "l1".also { opened.add(it) }
                    else -> "l2".also { opened.add(it) }
                }
            },
            accept = { name ->
                when {
                    name == "l1" -> throw IOException("socket reset by audio device")
                    accepted.isEmpty() -> "sock1"
                    else -> {
                        running.set(false)
                        null
                    }
                }
            },
            running = { running.get() },
            accepted = accepted,
            errors = errors,
        )

        l.run()

        assertEquals(listOf("l1", "l2"), opened)
        assertEquals(listOf("sock1"), accepted)
        assertTrue(errors.any { it.contains("accept failed") })
    }

    @Test
    fun `listen failure is retried until it succeeds`() {
        val running = AtomicBoolean(true)
        val accepted = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var attempts = 0

        val l = loop(
            open = {
                attempts++
                if (attempts <= 2) throw IOException("adapter busy") else "l1"
            },
            accept = { if (accepted.isEmpty()) "sock1" else { running.set(false); null } },
            running = { running.get() },
            accepted = accepted,
            errors = errors,
        )

        l.run()

        assertEquals(3, attempts)
        assertEquals(listOf("sock1"), accepted)
    }

    @Test
    fun `accept returning null reopens the listener instead of dying`() {
        val running = AtomicBoolean(true)
        val opened = mutableListOf<String>()
        val accepted = mutableListOf<String>()

        val l = loop(
            open = {
                val name = "l${opened.size + 1}"
                opened.add(name)
                if (opened.size >= 3) running.set(false)
                name
            },
            accept = { null },
            running = { running.get() },
            accepted = accepted,
        )

        l.run()

        assertEquals(listOf("l1", "l2", "l3"), opened)
        assertEquals(emptyList<String>(), accepted)
    }

    @Test
    fun `run does nothing when already stopped`() {
        val running = AtomicBoolean(false)
        var opens = 0

        loop(
            open = { opens++; "l1" },
            accept = { null },
            running = { running.get() },
        ).run()

        assertEquals(0, opens)
    }

    @Test
    fun `closeActive unblocks a pending accept so the loop exits on stop`() {
        val running = AtomicBoolean(true)
        val acceptEntered = CountDownLatch(1)
        val releaseAccept = CountDownLatch(1)
        val closed = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val l = loop(
            open = { "l1" },
            accept = {
                acceptEntered.countDown()
                releaseAccept.await(5, TimeUnit.SECONDS)
                throw IOException("socket closed")
            },
            running = { running.get() },
            closed = closed,
            errors = errors,
        )

        val thread = Thread { l.run() }.also { it.start() }
        assertTrue(acceptEntered.await(5, TimeUnit.SECONDS))
        running.set(false)
        l.closeActive()
        releaseAccept.countDown()
        thread.join(5000)

        assertTrue(!thread.isAlive)
        assertTrue(closed.contains("l1"))
    }
}

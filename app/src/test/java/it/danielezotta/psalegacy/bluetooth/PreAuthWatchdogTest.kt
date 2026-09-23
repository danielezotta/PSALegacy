package it.danielezotta.psalegacy.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PreAuthWatchdogTest {

    @Test
    fun `invokes callback when grace period elapses without cancellation`() {
        val latch = CountDownLatch(1)
        val watchdog = PreAuthWatchdog(graceMs = 50) { latch.countDown() }
        watchdog.start()
        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun `does not invoke callback after cancel`() {
        val expired = AtomicBoolean(false)
        val watchdog = PreAuthWatchdog(graceMs = 50) { expired.set(true) }
        watchdog.start()
        watchdog.cancel()
        Thread.sleep(200)
        assertFalse(expired.get())
    }
}

package it.danielezotta.psalegacy.bluetooth

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Closes the session if no valid protocol data arrives within [graceMs].
 * The car always sends its AuthRequest right after connecting; devices that
 * never speak the protocol (earphones, random connections) are cut off once
 * the grace period elapses, so the app falls back to listening again.
 */
class PreAuthWatchdog(
    private val graceMs: Long,
    private val onExpire: () -> Unit,
) {
    private val cancelled = AtomicBoolean(false)

    private val thread = Thread {
        Thread.sleep(graceMs)
        if (!cancelled.get()) onExpire()
    }.apply {
        isDaemon = true
        name = "preauth-watchdog"
    }

    fun start() {
        thread.start()
    }

    fun cancel() {
        cancelled.set(true)
    }
}

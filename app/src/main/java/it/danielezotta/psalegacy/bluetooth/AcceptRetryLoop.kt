package it.danielezotta.psalegacy.bluetooth

import java.util.concurrent.atomic.AtomicReference

/**
 * Resilient accept loop: never exits while [isRunning] returns true. A transient
 * failure to listen or accept (e.g. the Bluetooth stack resetting the RFCOMM
 * server socket when an audio device connects) closes the listener, waits
 * [retryDelayMs] and re-opens it, instead of killing the accept thread
 * permanently.
 *
 * @param L listener (server socket) type
 * @param S accepted socket type
 */
class AcceptRetryLoop<L, S>(
    private val open: () -> L?,
    private val accept: (L) -> S?,
    private val close: (L) -> Unit,
    private val onAccepted: (S) -> Unit,
    private val isRunning: () -> Boolean,
    private val onError: (String) -> Unit,
    private val retryDelayMs: Long,
    private val sleep: (Long) -> Unit = { ms -> Thread.sleep(ms) },
) {
    private val activeListener = AtomicReference<L?>()

    fun run() {
        while (isRunning()) {
            val listener = try {
                open()
            } catch (e: Exception) {
                onError("listen failed: ${e.message}")
                null
            }
            if (listener == null) {
                if (isRunning()) sleep(retryDelayMs)
                continue
            }
            activeListener.set(listener)
            try {
                while (isRunning()) {
                    val socket = try {
                        accept(listener)
                    } catch (e: Exception) {
                        onError("accept failed: ${e.message}")
                        null
                    }
                    if (socket == null) break
                    try {
                        onAccepted(socket)
                    } catch (e: Exception) {
                        onError("connection handling failed: ${e.message}")
                    }
                }
            } finally {
                activeListener.set(null)
                try {
                    close(listener)
                } catch (_: Exception) {
                }
            }
            if (isRunning()) sleep(retryDelayMs)
        }
    }

    /** Interrupts a blocked [accept] so [run] can exit when [isRunning] flips false. */
    fun closeActive() {
        activeListener.getAndSet(null)?.let { listener ->
            try {
                close(listener)
            } catch (_: Exception) {
            }
        }
    }
}

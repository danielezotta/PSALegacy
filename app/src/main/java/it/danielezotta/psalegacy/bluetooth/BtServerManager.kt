package it.danielezotta.psalegacy.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import it.danielezotta.psalegacy.AppState.LogTag
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * RFCOMM server. Opens one independent accept socket per UUID so a failure on
 * one UUID never tears down the others (unlike MyPeugeot 1.33.x).
 *
 * Every accept loop is resilient: a transient accept/listen failure (typical
 * when audio devices connect or the Bluetooth stack restarts) re-opens the
 * listener instead of killing the accept thread.
 */
class BtServerManager(
    private val adapter: BluetoothAdapter,
    private val onSocket: (BluetoothSocket) -> Unit,
    private val logger: (LogTag, String) -> Unit = { _, _ -> }
) {
    @Volatile
    var isRunning = false
        private set

    private val acceptThreads = CopyOnWriteArrayList<AcceptThread>()

    fun start(uuids: List<UUID>) {
        stop()
        isRunning = true
        for (uuid in uuids) {
            val thread = AcceptThread(uuid)
            acceptThreads.add(thread)
            thread.start()
        }
        logger(LogTag.INFO, "Listening on ${uuids.size} UUID(s): ${uuids.joinToString()}")
    }

    fun stop() {
        isRunning = false
        for (t in acceptThreads) t.close()
        acceptThreads.clear()
    }

    private inner class AcceptThread(private val uuid: UUID) : Thread("accept-$uuid") {
        private var loop: AcceptRetryLoop<BluetoothServerSocket, BluetoothSocket>? = null

        override fun run() {
            val l = AcceptRetryLoop(
                open = {
                    try {
                        adapter.listenUsingRfcommWithServiceRecord("PsaLegacy", uuid)
                    } catch (e: Exception) {
                        logger(LogTag.ERR, "Failed to listen on $uuid: ${e.message}")
                        null
                    }
                },
                accept = { server -> server.accept() },
                close = { server -> try { server.close() } catch (_: IOException) {} },
                onAccepted = { socket ->
                    logger(LogTag.INFO, "Incoming connection on $uuid")
                    onSocket(socket)
                },
                isRunning = { this@BtServerManager.isRunning },
                onError = { message -> logger(LogTag.ERR, "Accept error on $uuid: $message") },
                retryDelayMs = RETRY_DELAY_MS,
            )
            loop = l
            l.run()
        }

        fun close() {
            loop?.closeActive()
        }
    }

    private companion object {
        const val RETRY_DELAY_MS = 1500L
    }
}

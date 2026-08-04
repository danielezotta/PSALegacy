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
        @Volatile
        private var serverSocket: BluetoothServerSocket? = null

        override fun run() {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    "PsaLegacy", uuid
                )
                logger(LogTag.INFO, "RFCOMM listening on $uuid")
            } catch (e: Exception) {
                logger(LogTag.ERR, "Failed to listen on $uuid: ${e.message}")
                return
            }
            while (isRunning) {
                try {
                    val socket = serverSocket?.accept()
                    if (socket == null) {
                        if (isRunning) logger(LogTag.ERR, "accept() returned null on $uuid")
                        return
                    }
                    logger(LogTag.INFO, "Incoming connection on $uuid")
                    if (isRunning) onSocket(socket)
                } catch (e: IOException) {
                    if (isRunning) logger(LogTag.ERR, "Accept error on $uuid: ${e.message}")
                    return
                }
            }
        }

        fun close() {
            try {
                serverSocket?.close()
            } catch (_: IOException) {
            }
            serverSocket = null
        }
    }
}

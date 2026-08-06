package it.danielezotta.psalegacy.bluetooth

import android.bluetooth.BluetoothSocket
import it.danielezotta.psalegacy.AppState.LogTag
import it.danielezotta.psalegacy.protocol.ClearMessage
import it.danielezotta.psalegacy.protocol.FrameCodec
import it.danielezotta.psalegacy.protocol.MessageCodec
import it.danielezotta.psalegacy.protocol.ProtocolConstants
import it.danielezotta.psalegacy.protocol.ProtocolException
import it.danielezotta.psalegacy.protocol.SessionMachine
import it.danielezotta.psalegacy.util.toHexString
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads frames from the socket, feeds them to the [SessionMachine], and writes
 * back any responses. Runs on its own thread (see [run]).
 *
 * Logs every operation: raw wire frames (hex), decrypted clear messages (hex),
 * parsed message summaries and everything transmitted back.
 */
class ConnectionSession(
    private val socket: BluetoothSocket,
    private val machine: SessionMachine,
    private val logger: (LogTag, String) -> Unit = { _, _ -> }
) {
    private companion object {
        const val MAX_FRAME_LENGTH = 0x7FFF
    }

    @Volatile
    private var closed = false

    @Volatile
    private var output: DataOutputStream? = null

    private val writeLock = Any()

    fun run() {
        try {
            val input = socket.inputStream
            val output = DataOutputStream(socket.outputStream)
            this.output = output
            val frameCodec = FrameCodec(machine.encryption)
            // Blocking read with no idle timeout, like the original app: the car
            // owns the connection lifecycle and may send data long after connecting
            // (e.g. the finished trip at engine stop).
            while (true) {
                val frame = readFrame(input) ?: break
                logger(LogTag.RX, "frame (${frame.size} B): ${frame.toHexString()}")
                val clearBytes = try {
                    frameCodec.decode(frame)
                } catch (e: ProtocolException) {
                    logger(LogTag.ERR, "Bad frame ignored: ${e.message}")
                    continue
                }
                logger(LogTag.RX, "clear (${clearBytes.size} B): ${clearBytes.toHexString()}")
                // The clear header is [sa][len][ver][msgid][seq]: parse the fields
                // directly so messages the codec does not know yet (unknown ids from
                // newer/other head-unit firmwares) still get logged with their
                // numeric id instead of being dropped silently.
                val headerId: Short
                val headerSeq: Short
                if (clearBytes.size >= ProtocolConstants.CLEAR_HEADER_SIZE - 1) {
                    headerId = ((clearBytes[6].toInt() and 0xFF) shl 8 or (clearBytes[7].toInt() and 0xFF)).toShort()
                    headerSeq = ((clearBytes[8].toInt() and 0xFF) shl 8 or (clearBytes[9].toInt() and 0xFF)).toShort()
                } else {
                    headerId = -1
                    headerSeq = -1
                }
                val clear = try {
                    MessageCodec.decode(clearBytes)
                } catch (e: ProtocolException) {
                    logger(
                        LogTag.ERR,
                        "Bad message ignored: ${e.message} " +
                            "(id=${ProtocolConstants.messageName(headerId)} raw=$headerId seq=$headerSeq)"
                    )
                    continue
                }
                logger(LogTag.RX, "msg ${ProtocolConstants.messageName(clear.messageId)} seq=${clear.sequence}")
                val response = machine.onClearMessage(clear)
                if (response != null) {
                    send(response)
                }
            }
        } catch (e: IOException) {
            logger(LogTag.INFO, "Connection ended: ${e.message}")
        } finally {
            closed = true
            try {
                socket.close()
            } catch (_: IOException) {
            }
            machine.connClosed()
        }
    }

    fun send(clear: ClearMessage) {
        val out = output ?: run {
            logger(LogTag.ERR, "Send dropped (no output stream): ${ProtocolConstants.messageName(clear.messageId)}")
            return
        }
        synchronized(writeLock) {
            try {
                val codec = FrameCodec(machine.encryption)
                val encoded = MessageCodec.encode(clear)
                val wire = codec.encode(encoded)
                logger(LogTag.TX, "msg ${ProtocolConstants.messageName(clear.messageId)} seq=${clear.sequence}")
                logger(LogTag.TX, "clear (${encoded.size} B): ${encoded.toHexString()}")
                logger(LogTag.TX, "frame (${wire.size} B): ${wire.toHexString()}")
                out.write(wire)
                out.flush()
            } catch (e: IOException) {
                logger(LogTag.ERR, "Send failed: ${e.message}")
            }
        }
    }

    fun sendActivation(): Boolean {
        val msg = machine.buildActivation() ?: run {
            logger(LogTag.ERR, "Activation not sent: session not authenticated")
            return false
        }
        send(msg)
        return true
    }

    fun close() {
        try {
            socket.close()
        } catch (_: IOException) {
        }
    }

    private fun readFrame(input: InputStream): ByteArray? {
        val header = readFully(input, 4) ?: return null
        val length = ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
        if (length > MAX_FRAME_LENGTH) {
            logger(LogTag.ERR, "Protocol error: frame length $length exceeds max, closing connection")
            return null
        }
        val body = readFully(input, length + 1) ?: return null
        return header + body
    }

    private fun readFully(input: InputStream, n: Int): ByteArray? {
        val buffer = ByteArray(n)
        var offset = 0
        while (offset < n) {
            val read = input.read(buffer, offset, n - offset)
            if (read < 0) return null
            offset += read
        }
        return buffer
    }
}

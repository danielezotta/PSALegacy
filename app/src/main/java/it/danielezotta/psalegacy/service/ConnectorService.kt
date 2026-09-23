package it.danielezotta.psalegacy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.MainActivity
import it.danielezotta.psalegacy.R
import it.danielezotta.psalegacy.bluetooth.BtServerManager
import it.danielezotta.psalegacy.bluetooth.ConnectionSession
import it.danielezotta.psalegacy.data.LogStore
import it.danielezotta.psalegacy.data.SettingsStore
import it.danielezotta.psalegacy.data.TripStore
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.protocol.ProtocolConstants
import it.danielezotta.psalegacy.protocol.SessionEvents
import it.danielezotta.psalegacy.protocol.SessionMachine

class ConnectorService : Service() {

    companion object {
        const val CHANNEL_ID = "connector"
        const val NOTIFICATION_ID = 1
        private const val ACTION_START = "it.danielezotta.psalegacy.action.START"
        private const val ACTION_STOP = "it.danielezotta.psalegacy.action.STOP"

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, ConnectorService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ConnectorService::class.java).setAction(ACTION_STOP)
            )
        }
    }

    @Volatile
    private var btServer: BtServerManager? = null

    @Volatile
    private var activeSession: ConnectionSession? = null

    private var activeVin: String = ""

    /** Remote address of [activeSession]; saved as the car once it authenticates. */
    @Volatile
    private var activeAddress: String? = null

    private val events = object : SessionEvents {
        override fun onConnected() {
            AppState.connState.value = AppState.ConnState.Connected
            AppState.appendLog(AppState.LogTag.STATE, "Car connected")
        }

        override fun onAuthenticated(btelType: Short, serviceStatus: Short) {
            AppState.appendLog(
                AppState.LogTag.STATE,
                "Authenticated (btelType=$btelType serviceStatus=$serviceStatus)"
            )
            rememberCar(activeAddress)
            val session = activeSession
            if (session != null) {
                Thread {
                    AppState.appendLog(
                        AppState.LogTag.INFO,
                        "Sending activation in ${ProtocolConstants.PENDING_ACTIVATION_DELAY_MS} ms"
                    )
                    Thread.sleep(ProtocolConstants.PENDING_ACTIVATION_DELAY_MS)
                    session.sendActivation()
                }.start()
            }
        }

        override fun onActivationAck(tripCount: Int, serviceActive: Boolean) {
            AppState.appendLog(
                AppState.LogTag.STATE,
                "Activation ack: serviceActive=$serviceActive tripCount=$tripCount"
            )
            if (tripCount == 0) {
                AppState.appendLog(
                    AppState.LogTag.INFO,
                    "Car has no stored trips. Fuel/odometer data arrives inside trip " +
                        "messages: keep the app listening, data is pushed after a completed journey"
                )
            }
        }

        override fun onTrip(trip: Trip) {
            TripStore.saveTrips(trip.vin, listOf(trip))
            AppState.trips.value = TripStore.loadTrips(trip.vin)
            AppState.appendLog(
                AppState.LogTag.INFO,
                "Trip ${trip.tripNumber}: ${trip.distanceKm} km, ${trip.fuelConsumptionL} L"
            )
        }

        override fun onError(message: String) {
            AppState.appendLog(AppState.LogTag.ERR, message)
        }

        override fun onDisconnected() {
            AppState.connState.value = AppState.ConnState.Listening
            AppState.appendLog(AppState.LogTag.STATE, "Car disconnected; listening again")
        }

        override fun onTripsSynced(received: Int, expected: Int) {
            if (received >= expected) {
                AppState.appendLog(
                    AppState.LogTag.STATE,
                    "All $expected stored trips received"
                )
            } else {
                AppState.appendLog(
                    AppState.LogTag.STATE,
                    "Received $received/$expected stored trips; the rest will be pushed " +
                        "on the next connection"
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        TripStore.init(applicationContext)
        LogStore.init(applicationContext)
        // Started by AutoStartReceiver or a sticky restart: the UI never loaded settings.
        if (AppState.vin.value.isEmpty()) SettingsStore.loadIntoAppState(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Listening..."))
                // Auto-start may fire while already listening; restarting would drop the car.
                if (btServer?.isRunning != true) startListening()
                return START_STICKY
            }
        }
    }

    private fun startListening() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            AppState.appendLog(AppState.LogTag.ERR, "No Bluetooth adapter on this device")
            AppState.connState.value = AppState.ConnState.Error("No Bluetooth adapter")
            return
        }
        if (!adapter.isEnabled) {
            AppState.appendLog(AppState.LogTag.ERR, "Bluetooth is off")
            AppState.connState.value = AppState.ConnState.Error("Bluetooth is off")
            return
        }
        val vin = AppState.vin.value
        if (vin.length != ProtocolConstants.VIN_SIZE) {
            AppState.appendLog(AppState.LogTag.ERR, "VIN must be 17 chars, got ${vin.length}")
            AppState.connState.value = AppState.ConnState.Error("VIN must be 17 chars")
            return
        }
        activeVin = vin
        AppState.connState.value = AppState.ConnState.Listening
        AppState.trips.value = TripStore.loadTrips(vin)
        AppState.appendLog(AppState.LogTag.INFO, "Service started, VIN=$vin")

        val uuids = buildList {
            add(ProtocolConstants.SMARTAPP_UUID)
            if (AppState.useBrandUuid.value) add(ProtocolConstants.PEUGEOT_BRAND_UUID)
            if (AppState.useSppUuid.value) add(ProtocolConstants.SPP_UUID)
        }

        btServer?.stop()
        btServer = BtServerManager(adapter, ::onSocket) { tag, msg -> AppState.appendLog(tag, msg) }
        btServer?.start(uuids)
    }

    private fun onSocket(socket: BluetoothSocket) {
        activeSession?.close()
        val remote = try {
            val name = socket.remoteDevice?.name
            val address = socket.remoteDevice?.address
            listOfNotNull(name, address).joinToString(" / ").ifEmpty { "unknown device" }
        } catch (e: SecurityException) {
            "unknown device (no permission: ${e.message})"
        }
        AppState.appendLog(AppState.LogTag.INFO, "Accepted connection from $remote")
        activeAddress = try { socket.remoteDevice?.address } catch (_: SecurityException) { null }
        val machine = SessionMachine(activeVin, events)
        machine.start()
        val session = ConnectionSession(socket, machine) { tag, msg -> AppState.appendLog(tag, msg) }
        activeSession = session
        Thread { session.run() }.also { it.start() }
    }

    private fun rememberCar(address: String?) {
        if (address == null || address == AppState.carAddress.value) return
        AppState.carAddress.value = address
        SettingsStore.saveCarAddress(applicationContext, address)
        AppState.appendLog(AppState.LogTag.INFO, "Remembered car $address for auto-start")
    }

    private fun stopEverything() {
        AppState.appendLog(AppState.LogTag.INFO, "Service stopped")
        activeSession?.close()
        activeSession = null
        btServer?.stop()
        btServer = null
        AppState.connState.value = AppState.ConnState.Idle
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Car connection", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ConnectorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_car)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}

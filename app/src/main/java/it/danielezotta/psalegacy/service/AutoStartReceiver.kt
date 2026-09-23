package it.danielezotta.psalegacy.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.AppState.AutoStart
import it.danielezotta.psalegacy.data.LogStore
import it.danielezotta.psalegacy.data.SettingsStore

/**
 * Starts [ConnectorService] without user action, according to [AppState.autoStart]:
 *
 * - [AutoStart.CAR_CONNECTED]: on the car's ACL link (hands-free/audio pairing), so the
 *   RFCOMM listener is up before the head unit dials the SmartApp UUID; stopped when the
 *   car's link drops. With no car learned yet, any device's ACL link starts it.
 * - [AutoStart.ALWAYS]: after boot / app update, and on any ACL link in case the service
 *   was killed or Bluetooth was toggled off meanwhile.
 *
 * ACL broadcasts are exempt from both the implicit-broadcast manifest ban and the
 * background foreground-service start restriction (they require BLUETOOTH_CONNECT).
 */
class AutoStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // The process may have been cold-started just for this broadcast.
        LogStore.init(context.applicationContext)
        SettingsStore.loadIntoAppState(context)
        val mode = AppState.autoStart.value
        if (mode == AutoStart.OFF) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (mode == AutoStart.ALWAYS) start(context, "boot")
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val address = intent.device()?.address ?: return
                if (mode == AutoStart.ALWAYS || isCar(address)) start(context, address)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val address = intent.device()?.address ?: return
                if (mode == AutoStart.CAR_CONNECTED && address == AppState.carAddress.value) {
                    AppState.appendLog(AppState.LogTag.INFO, "Auto-stop: car $address disconnected")
                    context.stopService(Intent(context, ConnectorService::class.java))
                }
            }
        }
    }

    private fun isCar(address: String): Boolean {
        val car = AppState.carAddress.value
        return car.isEmpty() || car.equals(address, ignoreCase = true)
    }

    private fun start(context: Context, reason: String) {
        try {
            ConnectorService.start(context)
            AppState.appendLog(AppState.LogTag.INFO, "Auto-start ($reason)")
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException on OEMs that ignore the exemption;
            // excluding the app from battery optimisation lifts the restriction.
            AppState.appendLog(AppState.LogTag.ERR, "Auto-start ($reason) blocked: ${e.message}")
        }
    }

    private fun Intent.device(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
}

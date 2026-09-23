package it.danielezotta.psalegacy

import it.danielezotta.psalegacy.data.LogStore
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.model.VehicleModel
import it.danielezotta.psalegacy.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object AppState {
    sealed interface ConnState {
        data object Idle : ConnState
        data object Listening : ConnState
        data object Connected : ConnState
        data class Error(val message: String) : ConnState
    }

    /** When [it.danielezotta.psalegacy.service.ConnectorService] starts without user action. */
    enum class AutoStart { OFF, CAR_CONNECTED, ALWAYS }

    enum class LogTag { INFO, RX, TX, STATE, ERR }

    data class LogEntry(val timestampMs: Long, val tag: LogTag, val message: String)

    val vin = MutableStateFlow("")
    val useBrandUuid = MutableStateFlow(false)
    val useSppUuid = MutableStateFlow(false)
    val connState = MutableStateFlow<ConnState>(ConnState.Idle)
    val logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val trips = MutableStateFlow<List<Trip>>(emptyList())
    val selectedTab = MutableStateFlow(0)
    val theme = MutableStateFlow(AppTheme.BRAND)
    val autoStart = MutableStateFlow(AutoStart.OFF)

    /** MAC of the car's Bluetooth, learned on the first authenticated session or picked in Settings. */
    val carAddress = MutableStateFlow("")

    /** Model picked in Settings; null means decode it from the VIN. */
    val vehicleModel = MutableStateFlow<VehicleModel?>(null)

    fun appendLog(tag: LogTag, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), tag, message)
        logs.update { (it + entry).takeLast(2000) }
        LogStore.append(entry.timestampMs, tag.name, message)
    }
}

package it.danielezotta.psalegacy

import it.danielezotta.psalegacy.model.Trip
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

    fun appendLog(tag: LogTag, message: String) {
        logs.update { (it + LogEntry(System.currentTimeMillis(), tag, message)).takeLast(2000) }
    }
}

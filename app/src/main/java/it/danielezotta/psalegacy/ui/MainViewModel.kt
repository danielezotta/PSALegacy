package it.danielezotta.psalegacy.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.data.SettingsStore
import it.danielezotta.psalegacy.data.TripStore
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.protocol.ProtocolConstants
import it.danielezotta.psalegacy.service.ConnectorService
import it.danielezotta.psalegacy.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val vin: StateFlow<String> = AppState.vin.asStateFlow()
    val connState: StateFlow<AppState.ConnState> = AppState.connState.asStateFlow()
    val logs: StateFlow<List<AppState.LogEntry>> = AppState.logs.asStateFlow()
    val trips: StateFlow<List<Trip>> = AppState.trips.asStateFlow()
    val useBrandUuid: StateFlow<Boolean> = AppState.useBrandUuid.asStateFlow()
    val useSppUuid: StateFlow<Boolean> = AppState.useSppUuid.asStateFlow()
    val theme: StateFlow<AppTheme> = AppState.theme.asStateFlow()
    val selectedTab: StateFlow<Int> = AppState.selectedTab.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val logTimeFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)

    init {
        SettingsStore.loadIntoAppState(application)
    }

    fun setVin(value: String) {
        AppState.vin.value = value.trim().uppercase()
        SettingsStore.saveVin(getApplication(), AppState.vin.value)
    }

    fun setUseBrandUuid(value: Boolean) {
        AppState.useBrandUuid.value = value
        SettingsStore.saveUseBrandUuid(getApplication(), value)
    }

    fun setUseSppUuid(value: Boolean) {
        AppState.useSppUuid.value = value
        SettingsStore.saveUseSppUuid(getApplication(), value)
    }

    fun setTheme(value: AppTheme) {
        AppState.theme.value = value
        SettingsStore.saveTheme(getApplication(), value)
    }

    fun setSelectedTab(index: Int) {
        AppState.selectedTab.value = index
    }

    fun onToastShown() {
        _toast.value = null
    }

    val isListening: Boolean
        get() = connState.value is AppState.ConnState.Listening ||
            connState.value is AppState.ConnState.Connected

    fun toggleListening() {
        if (isListening) {
            ConnectorService.stop(getApplication())
        } else {
            if (AppState.vin.value.length != ProtocolConstants.VIN_SIZE) {
                _toast.value = "VIN must be exactly 17 characters"
                return
            }
            if (!hasBluetoothPermission()) {
                _toast.value = "Bluetooth permission required"
                return
            }
            ConnectorService.start(getApplication())
        }
    }

    fun loadTrips() {
        val requestedVin = AppState.vin.value
        viewModelScope.launch {
            val trips = withContext(Dispatchers.IO) {
                TripStore.init(getApplication())
                TripStore.loadTrips(requestedVin)
            }
            if (AppState.vin.value == requestedVin) {
                AppState.trips.value = trips
            }
        }
    }

    fun clearLog() {
        AppState.logs.value = emptyList()
    }

    fun logText(): String = AppState.logs.value.joinToString("\n") { entry ->
        "${logTimeFormat.format(java.util.Date(entry.timestampMs))} ${entry.tag.name} ${entry.message}"
    }

    private fun hasBluetoothPermission(): Boolean {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

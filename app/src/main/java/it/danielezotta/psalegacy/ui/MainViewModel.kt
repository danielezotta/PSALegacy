package it.danielezotta.psalegacy.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.data.SettingsStore
import it.danielezotta.psalegacy.data.TripStore
import it.danielezotta.psalegacy.model.Trip
import it.danielezotta.psalegacy.model.VehicleInfo
import it.danielezotta.psalegacy.model.VehicleModel
import it.danielezotta.psalegacy.model.VinDecoder
import it.danielezotta.psalegacy.protocol.ProtocolConstants
import it.danielezotta.psalegacy.service.ConnectorService
import it.danielezotta.psalegacy.ui.components.ToastMessage
import it.danielezotta.psalegacy.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val autoStart: StateFlow<AppState.AutoStart> = AppState.autoStart.asStateFlow()
    val carAddress: StateFlow<String> = AppState.carAddress.asStateFlow()
    val vehicleModel: StateFlow<VehicleModel?> = AppState.vehicleModel.asStateFlow()

    /** Vehicle decoded from the VIN, with the model overridden by the Settings choice if any. */
    val vehicle: StateFlow<VehicleInfo> = combine(AppState.vin, AppState.vehicleModel) { vin, override ->
        val decoded = VinDecoder.decode(vin)
        if (override == null) decoded else decoded.copy(brand = "Peugeot", model = override)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VinDecoder.decode(AppState.vin.value))

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private val _tripFilters = MutableStateFlow(TripFilters())
    val tripFilters: StateFlow<TripFilters> = _tripFilters.asStateFlow()

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

    fun setVehicleModel(value: VehicleModel?) {
        AppState.vehicleModel.value = value
        SettingsStore.saveVehicleModel(getApplication(), value)
    }

    fun setTheme(value: AppTheme) {
        AppState.theme.value = value
        SettingsStore.saveTheme(getApplication(), value)
    }

    fun setAutoStart(value: AppState.AutoStart) {
        AppState.autoStart.value = value
        SettingsStore.saveAutoStart(getApplication(), value)
        // "Always" means now too, not only after the next reboot.
        if (value == AppState.AutoStart.ALWAYS && !isListening &&
            isValidVin(AppState.vin.value) && hasBluetoothPermission()
        ) {
            ConnectorService.start(getApplication())
        }
    }

    fun setCarAddress(address: String) {
        AppState.carAddress.value = address
        SettingsStore.saveCarAddress(getApplication(), address)
    }

    data class PairedDevice(val name: String, val address: String)

    /** Bonded devices to pick the car from; empty without Bluetooth or permission. */
    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<PairedDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices.orEmpty()
                .map { PairedDevice(it.name ?: it.address, it.address) }
                .sortedBy { it.name.lowercase() }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val context = getApplication<Application>()
        return context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
    }

    fun setSelectedTab(index: Int) {
        AppState.selectedTab.value = index
    }

    fun onToastShown() {
        _toast.value = null
    }

    fun showToast(message: String) {
        _toast.value = ToastMessage(message)
    }

    fun setTripFilters(filters: TripFilters) {
        _tripFilters.value = filters
    }

    fun resetTripFilters() {
        _tripFilters.value = TripFilters()
    }

    val isListening: Boolean
        get() = connState.value is AppState.ConnState.Listening ||
            connState.value is AppState.ConnState.Connected

    fun toggleListening() {
        if (isListening) {
            ConnectorService.stop(getApplication())
        } else {
            if (!isValidVin(AppState.vin.value)) {
                showToast("VIN must be 17 characters")
                return
            }
            if (!hasBluetoothPermission()) {
                showToast("Bluetooth permission required")
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

    companion object {
        private val VIN_REGEX = Regex("^[A-HJ-NPR-Z0-9]{${ProtocolConstants.VIN_SIZE}}$")

        /** 17 alphanumerics, excluding I, O and Q (ISO 3779). */
        fun isValidVin(vin: String): Boolean = VIN_REGEX.matches(vin)
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

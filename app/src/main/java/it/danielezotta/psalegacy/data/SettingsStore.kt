package it.danielezotta.psalegacy.data

import android.content.Context
import it.danielezotta.psalegacy.AppState
import it.danielezotta.psalegacy.ui.theme.AppTheme

/**
 * Persists user settings (VIN, listener UUID toggles, theme) so they survive app restarts.
 */
object SettingsStore {
    private const val PREFS_NAME = "peugeot_connector_settings"
    private const val KEY_VIN = "vin"
    private const val KEY_USE_BRAND_UUID = "use_brand_uuid"
    private const val KEY_USE_SPP_UUID = "use_spp_uuid"
    private const val KEY_THEME = "theme"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadIntoAppState(context: Context) {
        val p = prefs(context.applicationContext)
        AppState.vin.value = p.getString(KEY_VIN, "") ?: ""
        AppState.useBrandUuid.value = p.getBoolean(KEY_USE_BRAND_UUID, false)
        AppState.useSppUuid.value = p.getBoolean(KEY_USE_SPP_UUID, false)
        AppState.theme.value = AppTheme.entries.find { it.name == p.getString(KEY_THEME, null) }
            ?: AppTheme.BRAND
    }

    fun saveVin(context: Context, vin: String) {
        prefs(context.applicationContext).edit().putString(KEY_VIN, vin).apply()
    }

    fun saveUseBrandUuid(context: Context, value: Boolean) {
        prefs(context.applicationContext).edit().putBoolean(KEY_USE_BRAND_UUID, value).apply()
    }

    fun saveUseSppUuid(context: Context, value: Boolean) {
        prefs(context.applicationContext).edit().putBoolean(KEY_USE_SPP_UUID, value).apply()
    }

    fun saveTheme(context: Context, theme: AppTheme) {
        prefs(context.applicationContext).edit().putString(KEY_THEME, theme.name).apply()
    }
}

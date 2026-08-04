package it.danielezotta.psalegacy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import it.danielezotta.psalegacy.ui.CarScreen
import it.danielezotta.psalegacy.ui.ConnectScreen
import it.danielezotta.psalegacy.ui.MainViewModel
import it.danielezotta.psalegacy.ui.RawLogScreen
import it.danielezotta.psalegacy.ui.SettingsScreen
import it.danielezotta.psalegacy.ui.TripsScreen
import it.danielezotta.psalegacy.ui.theme.AppTheme
import it.danielezotta.psalegacy.ui.theme.PsaLegacyTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Classic mode (no edge-to-edge): the framework positions the window
        // below the status bar, so content never overlaps it. Edge-to-edge is
        // unreliable on MIUI 14 — Compose receives zero window insets there.
        requestRuntimePermissions()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val theme by viewModel.theme.collectAsState()
            LaunchedEffect(theme) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                when (theme) {
                    AppTheme.BRAND -> {
                        window.statusBarColor = 0xFF16181D.toInt()
                        window.navigationBarColor = 0xFF16181D.toInt()
                        controller.isAppearanceLightStatusBars = false
                        controller.isAppearanceLightNavigationBars = false
                    }

                    AppTheme.CLUSTER -> {
                        window.statusBarColor = 0xFF0A0E14.toInt()
                        window.navigationBarColor = 0xFF0A0E14.toInt()
                        controller.isAppearanceLightStatusBars = false
                        controller.isAppearanceLightNavigationBars = false
                    }

                    AppTheme.LIGHT -> {
                        window.statusBarColor = 0xFFFFFFFF.toInt()
                        window.navigationBarColor = 0xFFFFFFFF.toInt()
                        controller.isAppearanceLightStatusBars = true
                        controller.isAppearanceLightNavigationBars = true
                    }
                }
                window.setBackgroundDrawable(
                    when (theme) {
                        AppTheme.BRAND -> android.graphics.drawable.ColorDrawable(0xFF16181D.toInt())
                        AppTheme.CLUSTER -> android.graphics.drawable.ColorDrawable(0xFF0A0E14.toInt())
                        AppTheme.LIGHT -> null
                    }
                )
            }
            PsaLegacyTheme(theme = theme) {
                MainScreen(viewModel)
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
private fun MainScreen(viewModel: MainViewModel) {
    val selected by viewModel.selectedTab.collectAsState()
    val tabs = listOf(
        Tab(stringResource(R.string.tab_connect), Icons.Filled.Home),
        Tab(stringResource(R.string.tab_trips), Icons.AutoMirrored.Filled.List),
        Tab(stringResource(R.string.tab_car), Icons.Filled.LocationOn),
        Tab(stringResource(R.string.tab_log), Icons.Filled.Info),
        Tab(stringResource(R.string.tab_settings), Icons.Filled.Settings)
    )

    val vin by viewModel.vin.collectAsState()
    LaunchedEffect(vin) {
        if (vin.length == 17) viewModel.loadTrips()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val toast by viewModel.toast.collectAsState()
    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onToastShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // safeDrawing includes the display cutout: on MIUI the status-bar inset
        // (44px) is smaller than the camera punch-hole (80px) — systemBars alone
        // would leave content overlapping the cutout zone.
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val content = when (selected) {
            1 -> TripsScreen(viewModel)
            2 -> CarScreen(viewModel)
            3 -> RawLogScreen(viewModel)
            4 -> SettingsScreen(viewModel)
            else -> ConnectScreen(viewModel)
        }
        Box(Modifier.padding(innerPadding)) {
            content
        }
    }
}

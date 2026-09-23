package it.danielezotta.psalegacy

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import it.danielezotta.psalegacy.ui.CarScreen
import it.danielezotta.psalegacy.ui.ConnectScreen
import it.danielezotta.psalegacy.ui.MainViewModel
import it.danielezotta.psalegacy.ui.RawLogScreen
import it.danielezotta.psalegacy.ui.SettingsScreen
import it.danielezotta.psalegacy.ui.TripsScreen
import it.danielezotta.psalegacy.ui.components.Motion
import it.danielezotta.psalegacy.ui.components.PsaIcons
import it.danielezotta.psalegacy.ui.components.PsaToast
import it.danielezotta.psalegacy.ui.theme.PeugeotNavy
import it.danielezotta.psalegacy.ui.theme.PsaLegacyTheme
import it.danielezotta.psalegacy.ui.theme.PsaTheme
import it.danielezotta.psalegacy.ui.theme.PsaType
import it.danielezotta.psalegacy.ui.theme.colors

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
                // Status bar matches the app bar (bg), navigation bar the tab bar (surface).
                // Ignored on Android 15+, where the app draws behind the system bars.
                val c = theme.colors()
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                @Suppress("DEPRECATION")
                window.statusBarColor = c.bg.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = c.surface.toArgb()
                controller.isAppearanceLightStatusBars = c.isLight
                controller.isAppearanceLightNavigationBars = c.isLight
                window.setBackgroundDrawable(ColorDrawable(c.bg.toArgb()))
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
        Tab(stringResource(R.string.tab_connect), PsaIcons.Bluetooth),
        Tab(stringResource(R.string.tab_trips), PsaIcons.Route),
        Tab(stringResource(R.string.tab_car), PsaIcons.Car),
        Tab(stringResource(R.string.tab_log), PsaIcons.Log),
        Tab(stringResource(R.string.tab_settings), PsaIcons.Settings)
    )

    val vin by viewModel.vin.collectAsState()
    LaunchedEffect(vin) {
        if (vin.length == 17) viewModel.loadTrips()
    }

    val toast by viewModel.toast.collectAsState()
    val c = PsaTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = c.bg,
        contentColor = c.text,
        // safeDrawing includes the display cutout: on MIUI the status-bar inset
        // (44px) is smaller than the camera punch-hole (80px) — systemBars alone
        // would leave content overlapping the cutout zone.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppBar(tabs[selected.coerceIn(tabs.indices)].label) },
        bottomBar = { TabBar(tabs, selected, viewModel::setSelectedTab) }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(Motion.BASE, easing = Motion.EaseEnter)) +
                        slideInVertically(tween(Motion.BASE, easing = Motion.EaseEnter)) { it / 60 })
                        .togetherWith(fadeOut(tween(Motion.FAST, easing = Motion.EaseExit)))
                },
                label = "screen"
            ) { tab ->
                when (tab) {
                    1 -> TripsScreen(viewModel)
                    2 -> CarScreen(viewModel)
                    3 -> RawLogScreen(viewModel)
                    4 -> SettingsScreen(viewModel)
                    else -> ConnectScreen(viewModel)
                }
            }
            PsaToast(toast, viewModel::onToastShown, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun AppBar(title: String) {
    val c = PsaTheme.colors
    Column(
        Modifier
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = PsaType.appbarTitle, color = c.text, maxLines = 1, modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.peugeot_wordmark),
                contentDescription = "Peugeot",
                colorFilter = ColorFilter.tint(if (c.isLight) PeugeotNavy else Color.White),
                modifier = Modifier.height(13.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@Composable
private fun TabBar(tabs: List<Tab>, selected: Int, onSelect: (Int) -> Unit) {
    val c = PsaTheme.colors
    val navLabel = stringResource(R.string.main_navigation)
    Column(
        Modifier
            .background(c.surface)
            // Not safeDrawing: that includes the IME and would lift the tab bar above the keyboard.
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .semantics { contentDescription = navLabel }
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = index == selected
                val tint = if (active) c.accentInk else c.text2
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 58.dp)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                ) {
                    Icon(tab.icon, contentDescription = null, tint = tint)
                    Text(tab.label, style = PsaType.tabLabel, color = tint, maxLines = 1)
                }
            }
        }
    }
}

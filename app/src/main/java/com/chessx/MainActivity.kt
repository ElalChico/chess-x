package com.chessx

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chessx.data.AppLanguage
import com.chessx.data.AppPreferences
import com.chessx.data.BtState
import com.chessx.data.GameHistoryStore
import com.chessx.data.GameRecord
import com.chessx.data.discoverableIntent
import com.chessx.data.requiredPermissions
import com.chessx.ui.GameViewModel
import com.chessx.ui.screens.ConnectScreen
import com.chessx.ui.screens.GameScreen
import com.chessx.ui.screens.HomeScreen
import com.chessx.ui.screens.LobbyScreen
import com.chessx.ui.screens.ReplayScreen
import com.chessx.ui.screens.RivalProfileDialog
import com.chessx.ui.screens.RoomScreen
import com.chessx.ui.screens.SettingsScreen

enum class Screen { HOME, CONNECT, PROFILE, GAME, SETTINGS, REPLAY, ROOM }

private val NavGold = Color(0xFFE2B94E)
private val NavMuted = Color(0xFF8B96A5)
private val NavBg = Color(0xFF10141A)
private val NavLine = Color(0xFF252E39)

class MainActivity : ComponentActivity() {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: AppPreferences

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.all { it }
        if (!granted) android.util.Log.w("Perms", "Some BT permissions denied: $results")
    }

    private val discoverableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        try { vm.bluetooth.hostListen() } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(applicationContext) as T
        }
        vm = ViewModelProvider(this, factory)[GameViewModel::class.java]
        vm.init()
        requestPermissionsIfNeeded()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0B0D10), surface = Color(0xFF13171D), surfaceVariant = Color(0xFF171D25),
                    primary = Color(0xFFE2B94E), onPrimary = Color(0xFF141414),
                    onBackground = Color(0xFFEEF1F5), onSurface = Color(0xFFEEF1F5),
                    onSurfaceVariant = Color(0xFF8B96A5), outline = Color(0xFF2F3947)
                )
            ) {
                Surface(modifier = Modifier.background(Color(0xFF0B0D10))) {
                    ChessApp(vm, prefs, this@MainActivity, permissionLauncher, discoverableLauncher)
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val perms = requiredPermissions()
        val toAsk = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (toAsk.isNotEmpty() && Build.VERSION.SDK_INT >= 23) permissionLauncher.launch(toAsk.toTypedArray())
        try {
            @Suppress("DEPRECATION")
            val a = BluetoothAdapter.getDefaultAdapter()
            if (a != null && !a.isEnabled) { try { @Suppress("DEPRECATION") a.enable() } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { vm.onClearedCleanup() } catch (_: Exception) {}
    }
}

@Composable
fun ChessApp(
    vm: GameViewModel,
    prefs: AppPreferences,
    activity: Activity,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    discoverableLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var isHostMode by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(GameHistoryStore.load(activity)) }
    var replayRecord by remember { mutableStateOf<GameRecord?>(null) }
    var showRivalProfile by remember { mutableStateOf(false) }

    LaunchedEffect(screen) { if (screen == Screen.PROFILE) history = GameHistoryStore.load(activity) }

    val ui by vm.ui.collectAsState()
    val btState by vm.bluetooth.state.collectAsState()
    val devices by vm.bluetooth.devices.collectAsState()
    val btError by vm.bluetooth.lastError.collectAsState()
    val remoteProfile by vm.remoteProfile.collectAsState()
    val isEn = prefs.language == AppLanguage.EN

    LaunchedEffect(prefs.soundEnabled, prefs.soundVolume) {
        try { vm.sound.setEnabled(prefs.soundEnabled); vm.sound.setVolume(prefs.soundVolume) } catch (_: Exception) {}
    }
    LaunchedEffect(prefs.profileName) { vm.setLocalName(prefs.profileName) }

    LaunchedEffect(fullscreen) {
        try {
            val controller = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (fullscreen) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {}
    }

    LaunchedEffect(btState) {
        if (btState == BtState.CONNECTED && screen == Screen.CONNECT) {
            screen = Screen.ROOM
            vm.sendHello()
            vm.sendProfile()
        }
    }
    LaunchedEffect(ui.lobby.gameStarted) {
        if (ui.lobby.gameStarted && (screen == Screen.PROFILE || screen == Screen.ROOM)) screen = Screen.GAME
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0D10))) {
        Box(Modifier.weight(1f)) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    inSession = btState == BtState.CONNECTED,
                    isGuestSession = btState == BtState.CONNECTED && !ui.lobby.isHost,
                    onCreate = {
                        isHostMode = true; vm.setIsHost(true); screen = Screen.CONNECT
                        vm.bluetooth.startDiscovery()
                        try { discoverableLauncher.launch(discoverableIntent()) } catch (_: Exception) {}
                    },
                    onSearch = {
                        isHostMode = false; vm.setIsHost(false); screen = Screen.CONNECT
                        vm.bluetooth.startDiscovery()
                    },
                    soundEnabled = prefs.soundEnabled,
                    onToggleSound = { prefs.setSoundEnabled(!prefs.soundEnabled) }
                )

                Screen.CONNECT -> ConnectScreen(
                    isHostMode = isHostMode, btState = btState, devices = devices, error = btError,
                    myName = ui.localName.ifBlank { prefs.profileName },
                    remoteName = ui.remoteName,
                    remoteProfile = remoteProfile,
                    onViewRivalProfile = { showRivalProfile = true },
                    onChooseRole = { host ->
                        isHostMode = host; vm.setIsHost(host)
                        if (host) {
                            try { discoverableLauncher.launch(discoverableIntent()) } catch (_: Exception) { try { vm.bluetooth.hostListen() } catch (_: Exception) {} }
                        } else {
                            vm.bluetooth.startDiscovery()
                        }
                    },
                    onHostListen = {
                        isHostMode = true; vm.setIsHost(true)
                        try { discoverableLauncher.launch(discoverableIntent()) } catch (_: Exception) { try { vm.bluetooth.hostListen() } catch (_: Exception) {} }
                    },
                    onSearch = {
                        isHostMode = false; vm.setIsHost(false)
                        vm.bluetooth.startDiscovery()
                    },
                    onConnect = { addr -> vm.bluetooth.connectTo(addr) },
                    onBack = { screen = Screen.HOME }
                )

                Screen.PROFILE -> LobbyScreen(
                    lobby = ui.lobby, localName = ui.localName, remoteName = ui.remoteName,
                    photoBase64 = prefs.profilePhotoBase64,
                    history = history,
                    remoteProfile = remoteProfile,
                    connected = btState == BtState.CONNECTED,
                    onViewRivalProfile = { showRivalProfile = true },
                    onConnect = { screen = Screen.CONNECT },
                    onGoRoom = { screen = Screen.ROOM },
                    onNameChange = { prefs.setProfileName(it); vm.setLocalName(it); vm.sendHello(); vm.sendProfile() },
                    onReplay = { replayRecord = it; screen = Screen.REPLAY },
                    onDeleteGame = { rec -> GameHistoryStore.remove(activity, rec.id); history = GameHistoryStore.load(activity) },
                    onClearHistory = { GameHistoryStore.clear(activity); history = emptyList() },
                    onPhotoCropped = { prefs.setProfilePhotoBase64(it); vm.sendProfile() },
                    onBack = { screen = Screen.HOME; vm.disconnect() }
                )

                Screen.ROOM -> RoomScreen(
                    lobby = ui.lobby, localName = ui.localName, remoteName = ui.remoteName,
                    localPhotoBase64 = prefs.profilePhotoBase64,
                    remoteProfile = remoteProfile,
                    onViewRivalProfile = { showRivalProfile = true },
                    onTimePick = { timeMs, inc -> vm.setTimeControl(timeMs, inc); if (ui.lobby.isHost) vm.sendLobbyConfig() },
                    onColorPick = { color -> vm.setLocalColor(color) },
                    onRandomColor = { vm.setRandomColor() },
                    onStartGame = { vm.startGameAsHost() },
                    onBack = { screen = Screen.HOME; vm.disconnect() }
                )

                Screen.REPLAY -> {
                    val rec = replayRecord
                    if (rec != null) ReplayScreen(rec, onBack = { screen = Screen.PROFILE })
                    else LobbyScreen(
                        lobby = ui.lobby, localName = ui.localName, remoteName = ui.remoteName,
                        photoBase64 = prefs.profilePhotoBase64, history = history,
                        remoteProfile = remoteProfile,
                        connected = btState == BtState.CONNECTED,
                        onViewRivalProfile = { showRivalProfile = true },
                        onConnect = { screen = Screen.CONNECT },
                        onGoRoom = { screen = Screen.ROOM },
                        onNameChange = { prefs.setProfileName(it); vm.setLocalName(it); vm.sendHello(); vm.sendProfile() },
                        onReplay = { replayRecord = it; screen = Screen.REPLAY },
                        onDeleteGame = { r -> GameHistoryStore.remove(activity, r.id); history = GameHistoryStore.load(activity) },
                        onClearHistory = { GameHistoryStore.clear(activity); history = emptyList() },
                        onPhotoCropped = { prefs.setProfilePhotoBase64(it); vm.sendProfile() },
                        onBack = { screen = Screen.HOME }
                    )
                }

                Screen.GAME -> GameScreen(
                    ui = ui, isFullscreen = fullscreen, boardTheme = prefs.boardTheme,
                    moveIndicators = prefs.moveIndicators,
                    localPhotoBase64 = prefs.profilePhotoBase64,
                    remoteProfile = remoteProfile,
                    onViewRivalProfile = { showRivalProfile = true },
                    onToggleFullscreen = { fullscreen = !fullscreen },
                    onSquareTap = vm::onSquareTap,
                    onTogglePause = vm::togglePause,
                    onOfferDraw = vm::offerDraw,
                    onAcceptDraw = { vm.respondDraw(true) },
                    onRejectDraw = { vm.respondDraw(false) },
                    onResign = vm::resign,
                    onPromotion = vm::confirmPromotion,
                    onCancelPromotion = vm::cancelPromotion,
                    onToggleSound = { prefs.setSoundEnabled(!prefs.soundEnabled) },
                    onDisconnect = { fullscreen = false; vm.disconnect(); screen = Screen.HOME }
                )

                Screen.SETTINGS -> SettingsScreen(
                    prefs = prefs,
                    onDisconnectBt = { try { vm.disconnect() } catch (_: Exception) {} },
                    onOpenBtSettings = {
                        try { activity.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) } catch (_: Exception) {}
                    },
                    onExit = { activity.finish() }
                )
            }
        }

        if (!(screen == Screen.GAME && fullscreen)) {
            BottomNav(current = when {
                screen == Screen.REPLAY -> Screen.PROFILE
                screen == Screen.ROOM -> Screen.GAME
                else -> screen
            }, isEn = isEn, onSelect = { target ->
                when (target) {
                    Screen.HOME -> { fullscreen = false; screen = Screen.HOME }
                    Screen.CONNECT -> { fullscreen = false; screen = Screen.CONNECT }
                    Screen.PROFILE, Screen.REPLAY -> { fullscreen = false; screen = Screen.PROFILE }
                    Screen.ROOM, Screen.GAME -> screen = when {
                        ui.lobby.gameStarted -> Screen.GAME
                        btState == BtState.CONNECTED -> Screen.ROOM
                        else -> Screen.CONNECT
                    }
                    Screen.SETTINGS -> { fullscreen = false; screen = Screen.SETTINGS }
                }
            })
        }

        if (showRivalProfile && remoteProfile != null) {
            RivalProfileDialog(profile = remoteProfile!!, onDismiss = { showRivalProfile = false })
        }
    }
}

private data class NavEntry(val screen: Screen, val labelEs: String, val labelEn: String, val icon: ImageVector)

@Composable
private fun BottomNav(current: Screen, isEn: Boolean, onSelect: (Screen) -> Unit) {
    val entries = listOf(
        NavEntry(Screen.HOME, "Inicio", "Home", Icons.Filled.Home),
        NavEntry(Screen.CONNECT, "Conectar", "Connect", Icons.Filled.Search),
        NavEntry(Screen.PROFILE, "Perfil", "Profile", Icons.Filled.Person),
        NavEntry(Screen.GAME, "Partida", "Game", Icons.Filled.PlayArrow),
        NavEntry(Screen.SETTINGS, "Ajustes", "Settings", Icons.Filled.Settings)
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(NavLine))
        Row(
            modifier = Modifier.fillMaxWidth().background(NavBg).padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            entries.forEach { entry ->
                val active = current == entry.screen
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0xFF1C232D) else Color.Transparent)
                        .clickable { onSelect(entry.screen) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(entry.icon, contentDescription = null, tint = if (active) NavGold else NavMuted, modifier = Modifier.size(18.dp))
                    Text(if (isEn) entry.labelEn else entry.labelEs, color = if (active) NavGold else NavMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

package com.chessx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.chessx.data.AppLanguage
import com.chessx.data.AppPreferences
import com.chessx.data.BoardTheme
import com.chessx.data.UpdateManager
import com.chessx.data.UpdateState
import com.chessx.data.UpdateCheckResult
import com.chessx.core.ChessEngine
import com.chessx.core.Color as ChessColor
import com.chessx.ui.components.BoardSelection
import com.chessx.ui.components.ChessBoard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF12141A)
private val Card2 = Color(0xFF151C25)
private val Line = Color(0xFF232C38)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)

@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    onDisconnectBt: () -> Unit = {},
    onOpenBtSettings: () -> Unit = {},
    onExit: () -> Unit = {},
    activity: Activity? = null
) {
    val isEn = prefs.language == AppLanguage.EN
    fun t(es: String, en: String) = if (isEn) en else es
    
    val context = LocalContext.current
    val updateManager = remember { UpdateManager.getInstance(context) }
    val updateState: com.chessx.data.UpdateState by updateManager.updateState.collectAsStateWithLifecycle()
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(t("Ajustes", "Settings"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
        Text(t("Todo se guarda al instante en este móvil.", "Everything saves instantly on this phone."), color = Muted, fontSize = 11.5.sp)

        // ============ IDIOMA ============
        Section(t("IDIOMA", "LANGUAGE"))
        SectionBody {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    val sel = prefs.language == lang
                    Chip(lang.label, sel, Modifier.weight(1f)) { prefs.setLanguage(lang) }
                }
            }
        }

        // ============ TABLERO ============
        Section(t("TABLERO", "BOARD"))
        SectionBody {
            Text(t("Tema", "Theme"), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardTheme.entries.forEach { theme ->
                    val sel = prefs.boardTheme == theme
                    Chip(if (isEn) theme.labelEn else theme.labelEs, sel, Modifier.weight(1f)) { prefs.setBoardTheme(theme) }
                }
            }
            ToggleRow(
                t("Indicadores de movimiento", "Move indicators"),
                t("Puntos y anillos con las jugadas legales", "Dots and rings with legal moves"),
                prefs.moveIndicators
            ) { prefs.setMoveIndicators(it) }
            ToggleRow(
                t("Señal de jaque (rey en rojo)", "Check signal (red king)"),
                t("Solo en jaque o jaque mate", "Only on check or checkmate"),
                prefs.checkSignal
            ) { prefs.setCheckSignal(it) }
        }

        // ============ SONIDOS ============
        Section(t("SONIDOS", "SOUND"))
        SectionBody {
            ToggleRow(
                t("Sonidos activados", "Sound enabled"),
                t("Sonidos estándar: movimientos, capturas, jaque y mate", "Standard sound set: moves, captures, check and mate"),
                prefs.soundEnabled
            ) { prefs.setSoundEnabled(it) }
            if (prefs.soundEnabled) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t("Volumen", "Volume"), color = Text1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${prefs.soundVolume}%", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = prefs.soundVolume.toFloat(),
                    onValueChange = { prefs.setSoundVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Line2)
                )
            }
        }

        // ============ RELOJ POR DEFECTO ============
        Section(t("RELOJ POR DEFECTO", "DEFAULT CLOCK"))
        SectionBody {
            var stMin by remember { mutableIntStateOf((if (prefs.defaultTimeMs > 0) prefs.defaultTimeMs / 60000 else 5).toInt().coerceIn(1, 120)) }
            var stInc by remember { mutableIntStateOf((prefs.defaultIncrementMs / 1000).toInt().coerceIn(0, 30)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(t("Minutos", "Minutes"), stMin, 1, 120, Modifier.weight(1f)) { v ->
                    stMin = v; prefs.setDefaultTime(v * 60_000L, stInc * 1000L)
                }
                Stepper(t("Incremento (s)", "Increment (s)"), stInc, 0, 30, Modifier.weight(1f)) { v ->
                    stInc = v; prefs.setDefaultTime(stMin * 60_000L, v * 1000L)
                }
            }
            Text(
                t("Se usa al crear una sala si no eliges otro. 1–120 min · 0–30 s de incremento.",
                  "Used when hosting a room if none is picked. 1–120 min · 0–30 s increment."),
                color = Muted, fontSize = 11.sp, lineHeight = 15.sp
            )
        }

        // ============ PRIVACIDAD Y COMPARTIR ============
        Section(t("COMPARTIR CON EL RIVAL", "SHARING WITH OPPONENT"))
        SectionBody {
            ToggleRow(
                t("Foto de perfil", "Profile photo"),
                t("Tu rival verá tu avatar", "Opponent sees your avatar"),
                prefs.privacy.sharePhoto
            ) { prefs.setPrivacy(prefs.privacy.copy(sharePhoto = it)) }
            ToggleRow(
                t("Historial", "History"),
                t("Partidas recientes y resultados", "Recent games and results"),
                prefs.privacy.shareHistory
            ) { prefs.setPrivacy(prefs.privacy.copy(shareHistory = it)) }
            ToggleRow(
                t("PGN", "PGN"),
                t("Permitir revisar tus partidas", "Allow reviewing your games"),
                prefs.privacy.sharePgn
            ) { prefs.setPrivacy(prefs.privacy.copy(sharePgn = it)) }
        }

        // ============ CONEXIÓN BLUETOOTH ============
        Section(t("CONEXIÓN BLUETOOTH", "BLUETOOTH CONNECTION"))
        SectionBody {
            ActionRow(
                title = t("Liberar conexión", "Release connection"),
                subtitle = t("Cierra el socket Bluetooth de esta app", "Closes this app's Bluetooth socket"),
                danger = true,
                onClick = onDisconnectBt
            )
            ActionRow(
                title = t("Ajustes BT del sistema", "System BT settings"),
                subtitle = t("Desvincula o apaga el Bluetooth en Android", "Unpair or turn Bluetooth off in Android"),
                onClick = onOpenBtSettings
            )
        }

        // ============ ACTUALIZACIONES ============
        Section(t("ACTUALIZACIONES", "UPDATES"))
        SectionBody {
            UpdateSection(
                updateState = updateState,
                updateManager = updateManager,
                activity = activity,
                isChecking = checking,
                isDownloading = downloading,
                onCheckClick = { checking = true },
                onDownloadClick = { downloading = true },
                onOpenReleasesClick = { updateManager.openReleasesPage(activity!!) },
                translate = { es, en -> t(es, en) },
                scope = scope,
                onResetFlags = { checking = false; downloading = false }
            )
        }

        // ============ APLICACIÓN ============
        Section(t("APLICACIÓN", "APP"))
        SectionBody {
            ActionRow(
                title = t("Cerrar aplicación", "Exit app"),
                subtitle = t("Termina la sesión y cierra CHESS-X", "Ends the session and closes CHESS-X"),
                danger = true,
                onClick = onExit
            )
            Text(
                "CHESS-X vX.45 · ${t("Motor FIDE propio ·", "Custom FIDE engine ·")} ${t("conexión SPP segura", "secure SPP link")}",
                color = Muted, fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 14.dp, start = 2.dp, bottom = 6.dp))
}

@Composable
private fun SectionBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Gold else Text1, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Text1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = Color(0xFF173B28))
        )
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(12.dp))
            .border(1.dp, if (danger) Color(0xFF3A2222) else Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) Color(0xFFFF9B9B) else Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Text("→", color = if (danger) Color(0xFFFF9B9B) else Gold, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier.background(Card2, RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallBtn("−") { if (value > min) onChange(value - 1) }
            Text("$value", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
            SmallBtn("+") { if (value < max) onChange(value + 1) }
        }
    }
}

@Composable
private fun SmallBtn(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).background(Color(0xFF10151C), RoundedCornerShape(8.dp)).border(1.dp, Line2, RoundedCornerShape(8.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Text1, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
private fun UpdateSection(
    updateState: com.chessx.data.UpdateState,
    updateManager: com.chessx.data.UpdateManager,
    activity: Activity?,
    isChecking: Boolean,
    isDownloading: Boolean,
    onCheckClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onOpenReleasesClick: () -> Unit,
    translate: (String, String) -> String,
    scope: CoroutineScope,
    onResetFlags: () -> Unit
) {
    val currentVersion = updateManager.getCurrentVersion()
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Versión actual
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(translate("Versión actual", "Current version"), color = Muted, fontSize = 12.sp)
            Text("X.${currentVersion.versionCode}", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        
        Divider(color = Line, thickness = 1.dp)
        
        when (updateState) {
            is UpdateState.Idle -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Card2, RoundedCornerShape(12.dp))
                        .border(1.dp, Line, RoundedCornerShape(12.dp))
                        .clickable { onCheckClick() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(translate("Buscar actualizaciones", "Check for updates"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(translate("Comprueba GitHub Releases por nueva versión", "Checks GitHub Releases for new version"), color = Muted, fontSize = 11.sp)
                    }
                    Text("→", color = Gold, fontWeight = FontWeight.ExtraBold)
                }
            }
            is UpdateState.Checking -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Card2, RoundedCornerShape(12.dp))
                        .border(1.dp, Gold, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(translate("Buscando actualización…", "Checking for update…"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(translate("Consultando GitHub Releases…", "Querying GitHub Releases…"), color = Muted, fontSize = 11.sp)
                    }
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is UpdateState.UpToDate -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF12241B), RoundedCornerShape(12.dp))
                        .border(1.dp, Green, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(translate("¡App actualizada!", "App is up to date!"), color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(translate("Tienes la última versión disponible", "You have the latest version available"), color = Muted, fontSize = 11.sp)
                    }
                    Text("✓", color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }
            is UpdateState.UpdateAvailable -> {
                val result = (updateState as UpdateState.UpdateAvailable).result
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Info de la actualización
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF26200F), RoundedCornerShape(12.dp))
                            .border(1.dp, Gold, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(translate("Actualización disponible", "Update available"), color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("X.${result.remoteVersion.versionCode}", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(result.releaseName, color = Text1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(result.releaseNotes, color = Muted, fontSize = 11.sp, maxLines = 3)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(translate("Tamaño: ${result.formattedSize()}", "Size: ${result.formattedSize()}"), color = Muted, fontSize = 10.sp)
                            if (result.isPrerelease) {
                                Text("BETA", color = Color(0xFFFF9B9B), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier
                                    .background(Color(0xFF3A2222), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    
                    // Botones de acción
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onDownloadClick(); scope.launch { updateManager.downloadAndInstall(result.apkUrl!!) { p -> } } },
                            modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A14), contentColor = Gold),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Gold
                                )
                                Text(translate("Descargar e instalar", "Download & install"), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = onOpenReleasesClick,
                            modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Card2, contentColor = Text1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Line2),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(translate("Ver en GitHub", "View on GitHub"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            is UpdateState.Downloading -> {
                val progress = (updateState as UpdateState.Downloading).progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF26200F), RoundedCornerShape(12.dp))
                        .border(1.dp, Gold, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(translate("Descargando actualización…", "Downloading update…"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${(progress * 100).toInt()}%", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = progress,
                        color = Gold,
                        trackColor = Color(0xFF3A3524),
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
            }
            is UpdateState.Downloaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF12241B), RoundedCornerShape(12.dp))
                        .border(1.dp, Green, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(translate("Descarga completa", "Download complete"), color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(translate("Pulsa para instalar (se abrirá instalador del sistema)", "Tap to install (system installer will open)"), color = Muted, fontSize = 11.sp)
                    Text(translate("Tus partidas y perfil se conservan (mismo keystore)", "Your games & profile are preserved (same keystore)"), color = Gold.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            is UpdateState.Installing -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF26200F), RoundedCornerShape(12.dp))
                        .border(1.dp, Gold, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(translate("Instalando…", "Installing…"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(translate("La app se cerrará y reiniciará con la nueva versión", "App will close and restart with new version"), color = Muted, fontSize = 11.sp)
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center).size(32.dp)
                    )
                }
            }
            is UpdateState.Installed -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF12241B), RoundedCornerShape(12.dp))
                        .border(1.dp, Green, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(translate("¡Actualizado!", "Updated!"), color = Green, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text(translate("Reinicia la app para usar la nueva versión", "Restart app to use new version"), color = Muted, fontSize = 11.sp)
                }
            }
            is UpdateState.Error -> {
                val error = (updateState as UpdateState.Error).message
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A2222), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFF9B9B), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(translate("Error", "Error"), color = Color(0xFFFF9B9B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("✕", color = Color(0xFFFF9B9B), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResetFlags(); updateManager.clearUpdateState() }
                                .background(Color(0xFF3A2222), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        )
                    }
                    Text(error, color = Text1, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onCheckClick() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A2222), contentColor = Color(0xFFFF9B9B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9B9B)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) { Text(translate("Reintentar", "Retry"), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        Button(
                            onClick = onOpenReleasesClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Card2, contentColor = Text1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Line2),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) { Text(translate("Ver en GitHub", "View on GitHub"), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    }
                }
            }
        }
        
        // Nota informativa
        Text(
            translate(
                "💡 El APK debe estar firmado con el MISMO keystore. Tus partidas, perfil y ajustes se conservan automáticamente al actualizar. Solo se borran al desinstalar manualmente, reset de fábrica o reinstalar limpio.",
                "💡 APK must be signed with SAME keystore. Your games, profile & settings are preserved on update. Only lost on manual uninstall, factory reset, or clean reinstall."
            ),
            color = Muted.copy(alpha = 0.7f), fontSize = 9.5.sp, lineHeight = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

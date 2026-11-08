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
import com.chessx.data.AppLanguage
import com.chessx.data.AppPreferences
import com.chessx.data.BoardTheme

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
    onExit: () -> Unit = {}
) {
    val isEn = prefs.language == AppLanguage.EN
    fun t(es: String, en: String) = if (isEn) en else es

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

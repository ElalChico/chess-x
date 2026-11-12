package com.chessx.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.core.Color as ChessColor
import com.chessx.data.LobbyState
import com.chessx.data.PredefinedControls
import com.chessx.data.RemoteProfile
import com.chessx.R

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF12141A)
private val Card2 = Color(0xFF151C25)
private val Line = Color(0xFF232C38)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)
private val Red = Color(0xFFFF6B6B)

/**
 * Sala de partida: permite configurar reloj y color, conectar al rival e iniciar.
 */
@Composable
fun RoomScreen(
    lobby: LobbyState,
    localName: String,
    remoteName: String,
    localPhotoBase64: String = "",
    remoteProfile: RemoteProfile? = null,
    onViewRivalProfile: () -> Unit = {},
    onTimePick: (Long, Long) -> Unit,
    onColorPick: (ChessColor) -> Unit,
    onRandomColor: () -> Unit,
    onConnect: () -> Unit,
    onStartGame: () -> Unit,
    onStartTournament: (Int) -> Unit = {},
    /** Partida suspendida pendiente de retomar (historial especial). */
    suspendedGame: com.chessx.data.SuspendedGame? = null,
    onResumeGame: (com.chessx.data.SuspendedGame) -> Unit = {},
    onBack: () -> Unit
) {
    val isHost = lobby.isHost
    val rivalOk = remoteName.isNotBlank() && lobby.connected
    val clockLabel = if (lobby.timeMs == 0L) "Sin límite" else "${lobby.timeMs / 60000} + ${lobby.incrementMs / 1000}"
    val colorLabel = if (lobby.colorRandom) "Aleatorio" else if (lobby.localColor == ChessColor.WHITE) "Blancas" else "Negras"
    var tournamentMode by remember { mutableStateOf(false) }
    var tournamentGames by remember { mutableIntStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                Text("← Volver al inicio", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF12241B), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3B2B))) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(7.dp).background(Green, CircleShape))
                    Text("SALA", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9BE3BD), letterSpacing = 1.sp)
                }
            }
        }

        // Cabecera: quién es anfitrión y quién invitado
        Text(
            "Sala de partida",
            fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Text1
        )
        Text(
            if (isHost) "Configura el reloj y el color antes de conectar al rival."
            else "Eres el invitado · espera a que el anfitrión inicie la partida.",
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp
        )

        // Ambos jugadores en tarjetas
        RoomPlayerCard(
            name = localName.ifBlank { "Jugador" },
            role = if (isHost) "ANFITRIÓN" else "INVITADO",
            color = lobby.localColor,
            photoBase64 = localPhotoBase64,
            accent = Gold,
            you = true,
            onClick = null
        )
        RoomPlayerCard(
            name = remoteProfile?.name?.ifBlank { remoteName.ifBlank { "Rival" } } ?: remoteName.ifBlank { "Rival" },
            role = if (isHost) "INVITADO" else "ANFITRIÓN",
            color = lobby.remoteColor,
            photoBase64 = remoteProfile?.photoBase64,
            accent = Color(0xFF5B9DF9),
            you = false,
            onClick = { onViewRivalProfile() }
        )

        // ===== Reloj (anfitrión) =====
        SectionLabel("CONTROL DE TIEMPO")
        Column(
            modifier = Modifier.fillMaxWidth().alpha(if (isHost) 1f else 0.55f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PredefinedControls.options.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { opt ->
                        val selected = lobby.timeMs == opt.timeMs && lobby.incrementMs == opt.incrementMs
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(14.dp))
                                .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(14.dp))
                                .clickable { if (isHost) onTimePick(opt.timeMs, opt.incrementMs) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(opt.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) Gold else Text1)
                            Text(if (opt.timeMs == 0L) "Sin límite" else if (opt.incrementMs > 0) "+${opt.incrementMs / 1000}s" else "${opt.timeMs / 60000} min", fontSize = 9.sp, color = Muted)
                        }
                    }
                    if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (isHost) {
                var cMin by remember { mutableIntStateOf((if (lobby.timeMs > 0) lobby.timeMs / 60000 else 5).toInt().coerceIn(1, 120)) }
                var cInc by remember { mutableIntStateOf((lobby.incrementMs / 1000).toInt().coerceIn(0, 30)) }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Card2, RoundedCornerShape(14.dp)).border(1.dp, Line2, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Stepper("Min", cMin, 1, 120, Modifier.weight(1f)) { cMin = it; onTimePick(cMin * 60_000L, cInc * 1000L) }
                    Stepper("+Seg", cInc, 0, 30, Modifier.weight(1f)) { cInc = it; onTimePick(cMin * 60_000L, cInc * 1000L) }
                }
            }
        }

        // ===== Color (anfitrión) =====
        SectionLabel("COLOR")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(if (isHost) 1f else 0.55f)) {
            ColorCard("Blancas", "Mueven primero", ChessColor.WHITE, !lobby.colorRandom && lobby.localColor == ChessColor.WHITE, Modifier.weight(1f)) { if (isHost) onColorPick(ChessColor.WHITE) }
            ColorCard("Negras", "Juegan segundo", ChessColor.BLACK, !lobby.colorRandom && lobby.localColor == ChessColor.BLACK, Modifier.weight(1f)) { if (isHost) onColorPick(ChessColor.BLACK) }
            RandomColorCard("Aleatorio", "Se decide al comenzar", lobby.colorRandom, Modifier.weight(1f)) { if (isHost) onRandomColor() }
        }

        // ===== Checklist =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card2, RoundedCornerShape(12.dp))
                .border(1.dp, Line2, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChecklistItem("Reloj", clockLabel, true)
            ChecklistItem("Color", colorLabel, true)
            ChecklistItem("Rival", if (rivalOk) remoteName.take(10) else "esperando", rivalOk)
        }

        // ===== Torneo (solo anfitrión): N partidas seguidas, colores alternos =====
        if (isHost) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionLabel("MODO TORNEO")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Card, RoundedCornerShape(14.dp))
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Stepper("Partidas", tournamentGames, 2, 6, Modifier.weight(1f)) { tournamentGames = it }
                    Column(Modifier.weight(2f)) {
                        Text(
                            "$tournamentGames partidas seguidas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Text1
                        )
                        Text(
                            "Colores alternos · resultados separados",
                            fontSize = 10.sp,
                            color = Muted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(if (tournamentMode) Color(0xFF1A1A14) else Color(0xFF111111), RoundedCornerShape(14.dp))
                        .border(1.2.dp, if (tournamentMode) Gold else Color(0xFF232323), RoundedCornerShape(14.dp))
                        .clickable { tournamentMode = !tournamentMode }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(Color(0xFF1A1A14), RoundedCornerShape(11.dp))
                                .border(1.dp, if (rivalOk) Color(0xFF3A3524) else Color(0xFF26261F), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (tournamentMode) Gold else Color(0xFF6B6244))
                        }
                        Column {
                            Text(
                                if (tournamentMode) "Torneo seleccionado" else "Seleccionar torneo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tournamentMode) Gold else Text1
                            )
                            Text(
                                "Iniciará $tournamentGames partidas con colores alternos",
                                fontSize = 10.sp,
                                color = Color(0xFFA8975E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        }

        if (isHost) {
            val actionLabel = when {
                !lobby.connected -> "Conectar rival"
                suspendedGame != null -> "Reanudar partida"
                tournamentMode -> "Iniciar torneo"
                else -> "Iniciar partida"
            }
            val actionDetail = when {
                !lobby.connected -> "Primero configura el reloj y luego conecta al rival"
                suspendedGame != null -> "Partida suspendida · ${suspendedGame.movesCount} jugadas · se continua con el turno guardado"
                !rivalOk -> "Bluetooth conectado · sincronizando perfil del rival"
                else -> "Ambos móviles comenzarán con una cuenta regresiva"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        if (lobby.connected) {
                            when {
                                suspendedGame != null -> suspendedGame.let(onResumeGame)
                                tournamentMode -> onStartTournament(tournamentGames)
                                else -> onStartGame()
                            }
                        } else {
                            onConnect()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A14), contentColor = Gold),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold)
                ) {
                    Text(
                        "$actionLabel  →",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    actionDetail,
                    modifier = Modifier.fillMaxWidth(),
                    color = Muted,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("Salir de la sala", color = Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Gold, modifier = Modifier.size(20.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Tú eres el invitado", color = Text1, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text("El anfitrión iniciará la partida", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RoomPlayerCard(
    name: String,
    role: String,
    color: ChessColor,
    photoBase64: String?,
    accent: Color,
    you: Boolean,
    onClick: (() -> Unit)?
) {
    val bmp = remember(photoBase64) {
        try {
            photoBase64?.substringAfter("base64,", photoBase64)?.let { raw ->
                val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
    val badge = when {
        color == ChessColor.WHITE -> "B"
        else -> "N"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A2433))
                .border(1.5.dp, accent.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text((name.firstOrNull() ?: '?').uppercaseChar().toString(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Text1, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Text(if (you) "Tú · $role" else "$role · rival", color = Muted, fontSize = 10.5.sp)
        }
        // Insignia de color (peoncito) — Lite
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (color == ChessColor.WHITE) Color(0xFFECECF4) else Color(0xFF141414), RoundedCornerShape(9.dp))
                .border(1.dp, Line2, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(badge, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (color == ChessColor.WHITE) Color(0xFF141414) else Color(0xFFECECF4))
        }
    }
}

@Composable
private fun ChecklistItem(label: String, value: String, ok: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (ok) Color(0xFF1E3B2B) else Color(0xFF2A2A2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (ok) {
                    Text("✓", fontSize = 9.sp, color = Green, fontWeight = FontWeight.ExtraBold)
                } else {
                    Text("·", fontSize = 9.sp, color = Muted, fontWeight = FontWeight.ExtraBold)
                }
            }
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Text(value, color = if (ok) Text1 else Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun ColorCard(
    title: String,
    sub: String,
    isWhite: ChessColor,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val white = isWhite == ChessColor.WHITE
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(16.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF0E1318), RoundedCornerShape(13.dp))
                .border(1.dp, Line, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.chessx.ui.components.pieceResource(
                        com.chessx.core.Piece(com.chessx.core.PieceType.PAWN, if (white) com.chessx.core.Color.WHITE else com.chessx.core.Color.BLACK)
                    )
                ),
                contentDescription = title,
                modifier = Modifier.size(44.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Gold else Text1)
            Text(sub, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun RandomColorCard(
    title: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(16.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF0E1318), RoundedCornerShape(13.dp))
                .border(1.dp, Line, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Gold else Text1)
            Text(sub, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

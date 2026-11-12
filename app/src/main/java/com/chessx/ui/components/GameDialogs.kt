/*
 * CHESS-X · © Elal Chico
 * Diálogos de finalización: fin de partida (mismo color / nuevo color / torneo),
 * fin de torneo (marcador) y selección de pieza al promover.
 */

package com.chessx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chessx.R
import com.chessx.core.Color as ChessColor
import com.chessx.core.GameEnd
import com.chessx.core.Piece
import com.chessx.core.PieceType
import com.chessx.data.PieceSet
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

private fun decodePhoto(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    val raw = base64.substringAfter("base64,", base64)
    return try {
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) { null }
}

// ---------- Fin de partida ----------

/** Presets de control de tiempo ofrecidos al repetir o cambiar de color. */
private val TIME_PRESETS = listOf(
    180_000L to 0L,      // 3 + 0
    180_000L to 2_000L,  // 3 + 2
    300_000L to 0L,      // 5 + 0
    600_000L to 0L,      // 10 + 0
    900_000L to 10_000L  // 15 + 10
)

@Composable
fun GameEndDialog(
    end: GameEnd,
    isHost: Boolean,
    tournamentActive: Boolean,
    tournamentFinished: Boolean,
    localName: String,
    remoteName: String,
    localPhotoBase64: String = "",
    remotePhotoBase64: String? = null,
    winnerName: String? = null,
    /** Control de tiempo de la partida que acaba de terminar (para "mismo tiempo"). */
    lastTimeMs: Long = 300_000L,
    lastIncMs: Long = 0L,
    onNewGameSame: (Long, Long) -> Unit,
    onNewGameRandom: (Long, Long) -> Unit,
    onTournamentNext: () -> Unit,
    onBackToRoom: () -> Unit,
    onReplayPgn: () -> Unit,
    onNewGameCustomTime: (Long, Long) -> Unit, // timeMs, incrementMs
    onDismiss: () -> Unit
) {
    val resultColorStr = if (end.winner == null) "draw" else
        (if (end.winner == ChessColor.WHITE) "white" else "black")

    val winnerDisplayName = winnerName ?: (when (resultColorStr) {
        "white" -> if (localName.isNotBlank()) localName else "Blancas"
        "black" -> if (remoteName.isNotBlank()) remoteName else "Negras"
        else -> "Empate"
    })

    val winnerPhoto = when (resultColorStr) {
        "white" -> localPhotoBase64
        "black" -> remotePhotoBase64
        else -> null
    }

    // Selector de tiempo: se despliega al pulsar "Repetir partida" o "Nuevo color".
    var expandSame by remember { mutableStateOf(false) }
    var expandRandom by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(Color(0xFF12141A), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF3A3524), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Winner announcement with photo
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Winner photo
                    if (winnerPhoto != null) {
                        val bmp = remember(winnerPhoto) { decodePhoto(winnerPhoto) }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF15251D), CircleShape)
                                .border(3.dp, Color(0xFFE2B94E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = winnerDisplayName,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    winnerDisplayName.firstOrNull()?.uppercaseChar().toString() ?: "?",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE2B94E)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = when (resultColorStr) {
                            "white" -> "¡BLANCAS GANAN!"
                            "black" -> "¡NEGRAS GANAN!"
                            else -> "¡TABLAS!"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (resultColorStr) {
                            "white" -> Color(0xFFFF6B6B)
                            "black" -> Color(0xFF5B9DF9)
                            else -> Color(0xFF3DDC97)
                        },
                        textAlign = TextAlign.Center
                    )
                    
                    if (resultColorStr != "draw") {
                        Text(
                            text = winnerDisplayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2B94E),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = end.message.ifBlank { "Partida finalizada" },
                    fontSize = 12.5.sp,
                    color = Color(0xFF8B96A5),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(2.dp))

                if (tournamentActive && !tournamentFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF26200F), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2B94E), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Torneo en curso · " + (if (isHost) "controlado por el anfitrión" else "controlado por el rival"),
                            fontSize = 11.5.sp,
                            color = Color(0xFFE2B94E),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }

                EndGameOption(
                    title = "Repetir partida",
                    subtitle = "Mismo color · elige el tiempo",
                    enabled = true,
                    highlight = false,
                    expanded = expandSame,
                    onClick = { expandSame = !expandSame }
                )
                if (expandSame) {
                    TimeChoicePanel(
                        lastTimeMs = lastTimeMs,
                        lastIncMs = lastIncMs,
                        onPick = { t, i ->
                            expandSame = false
                            onNewGameSame(t, i)
                        }
                    )
                }
                EndGameOption(
                    title = "Nuevo color",
                    subtitle = "Aleatorio · elige el tiempo",
                    enabled = true,
                    highlight = false,
                    expanded = expandRandom,
                    onClick = { expandRandom = !expandRandom }
                )
                if (expandRandom) {
                    TimeChoicePanel(
                        lastTimeMs = lastTimeMs,
                        lastIncMs = lastIncMs,
                        onPick = { t, i ->
                            expandRandom = false
                            onNewGameRandom(t, i)
                        }
                    )
                }

                // Atajo directo: control de tiempo libre (elige y arranca al instante)
                Text(
                    "O pulsa un tiempo para empezar directamente:",
                    color = Color(0xFF8B96A5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickTimeBtn("3+0", 180000, 0, Modifier.weight(1f)) { onNewGameCustomTime(180000, 0) }
                    QuickTimeBtn("3+2", 180000, 2000, Modifier.weight(1f)) { onNewGameCustomTime(180000, 2000) }
                    QuickTimeBtn("5+0", 300000, 0, Modifier.weight(1f)) { onNewGameCustomTime(300000, 0) }
                    QuickTimeBtn("10+0", 600000, 0, Modifier.weight(1f)) { onNewGameCustomTime(600000, 0) }
                    QuickTimeBtn("15+10", 900000, 10000, Modifier.weight(1f)) { onNewGameCustomTime(900000, 10000) }
                }
                Spacer(Modifier.height(4.dp))

                if (tournamentActive && !tournamentFinished) {
                    EndGameOption(
                        title = "Siguiente torneo",
                        subtitle = "Partida N+1 · color alternado",
                        enabled = true,
                        highlight = true,
                        onClick = onTournamentNext
                    )
                }
                EndGameOption(
                    title = "Reproducir PGN",
                    subtitle = "Ver la partida completa",
                    enabled = true,
                    highlight = false,
                    onClick = onReplayPgn
                )
                EndGameOption(
                    title = "Volver a la sala",
                    subtitle = "Continuar en el lobby del rival",
                    enabled = true,
                    highlight = false,
                    onClick = onBackToRoom
                )

                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0D10))
                ) {
                    Text("Cancelar", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
private fun EndGameOption(
    title: String,
    subtitle: String,
    enabled: Boolean,
    highlight: Boolean,
    expanded: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enabled) Color(0xFF151C25) else Color(0xFF12141A), RoundedCornerShape(13.dp))
            .border(1.dp, if (highlight || expanded) Color(0xFFE2B94E) else Color(0xFF232C38), RoundedCornerShape(13.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color(0xFFEEF1F5) else Color(0xFF6B6244)
            )
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF8B96A5))
        }
        Text(
            if (expanded) "▾" else "→",
            color = if (highlight || expanded) Color(0xFFE2B94E) else Color(0xFF232C38),
            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
        )
    }
}

/**
 * Panel de eleccion de tiempo al repetir la partida o cambiar de color:
 * "mismo tiempo" (el de la partida recien terminada), presets o personalizado.
 */
@Composable
private fun TimeChoicePanel(
    lastTimeMs: Long,
    lastIncMs: Long,
    onPick: (Long, Long) -> Unit
) {
    var minutes by remember { mutableIntStateOf((lastTimeMs / 60_000).toInt().coerceIn(1, 120)) }
    var increment by remember { mutableIntStateOf((lastIncMs / 1000).toInt().coerceIn(0, 60)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F141B), RoundedCornerShape(13.dp))
            .border(1.dp, Color(0xFF3A3524), RoundedCornerShape(13.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mismo tiempo que la partida anterior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A14), RoundedCornerShape(11.dp))
                .border(1.2.dp, Color(0xFFE2B94E), RoundedCornerShape(11.dp))
                .clickable { onPick(lastTimeMs, lastIncMs) }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Mismo tiempo (${if (lastTimeMs == 0L) "∞" else "${lastTimeMs / 60_000} + ${lastIncMs / 1000}"})",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE2B94E)
            )
        }

        Text("O elige otro", color = Color(0xFF8B96A5), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TIME_PRESETS.forEach { (t, i) ->
                val label = if (t == 0L) "∞" else "${t / 60_000}+${i / 1000}"
                QuickTimeBtn(label, t, i, Modifier.weight(1f)) { onPick(t, i) }
            }
        }

        // Personalizado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniStepper("min", minutes) { minutes = it.coerceIn(1, 120) }
            MiniStepper("+s", increment) { increment = it.coerceIn(0, 60) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF151C25), RoundedCornerShape(11.dp))
                    .border(1.dp, Color(0xFF2F3947), RoundedCornerShape(11.dp))
                    .clickable { onPick(minutes * 60_000L, increment * 1000L) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$minutes+$increment",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEEF1F5)
                )
            }
        }
    }
}

/** Stepper compacto para el tiempo personalizado. */
@Composable
private fun MiniStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0xFF151C25), RoundedCornerShape(11.dp))
            .border(1.dp, Color(0xFF2F3947), RoundedCornerShape(11.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(24.dp).background(Color(0xFF0F141B), RoundedCornerShape(7.dp)).clickable { onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) { Text("−", color = Color(0xFFEEF1F5), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text("$value", color = Color(0xFFEEF1F5), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color(0xFF8B96A5), fontSize = 8.sp)
        }
        Box(
            Modifier.size(24.dp).background(Color(0xFF0F141B), RoundedCornerShape(7.dp)).clickable { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color(0xFFEEF1F5), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun QuickTimeBtn(label: String, timeMs: Long, incMs: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val timeText = if (timeMs == 0L) "∞" else "${timeMs / 60000}"
    val incText = if (incMs == 0L) "+0" else "+${incMs / 1000}"
    Box(
        modifier = modifier
            .background(Color(0xFF151C25), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF232C38), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$timeText$incText", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2B94E))
    }
}

@Composable
fun TournamentEndDialog(
    totalGames: Int,
    localWins: Int,
    remoteWins: Int,
    draws: Int,
    timeControl: String,
    onNewTournament: () -> Unit,
    onBackToRoom: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = localWins + remoteWins + draws
    val winnerColor = when {
        localWins > remoteWins -> Color(0xFF3DDC97)
        remoteWins > localWins -> Color(0xFFFF6B6B)
        else -> Color(0xFF5B9DF9)
    }
    val winnerText = when {
        localWins > remoteWins -> "¡USTED GANA EL TORNEO!"
        remoteWins > localWins -> "¡GANA EL RIVAL!"
        else -> "TORNEO EMPATADO"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141A), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF3A3524), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF26200F), RoundedCornerShape(14.dp))
                        .border(1.dp, winnerColor, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TORNEO COMPLETADO", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = winnerColor, letterSpacing = 1.sp)
                        Text(
                            if (total == totalGames) "¡¡$winnerText $totalGames/$totalGames!!" else "Torneo finalizado",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE2B94E),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreRow("NEGRAS", remoteWins, Color(0xFF141414), Color(0xFFECECF4), remoteWins > localWins)
                    ScoreRow("BLANCAS", localWins, Color(0xFFECECF4), Color(0xFF141414), localWins > remoteWins)
                }
                Text("PARTIDAS: $total / $totalGames", fontSize = 12.sp, color = Color(0xFF8B96A5), fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onNewTournament,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Color(0xFFE2B94E))
                    ) { Text("Nuevo torneo", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2B94E)) }
                    Button(
                        onClick = onBackToRoom,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Color(0xFFEEF1F5))
                    ) { Text("Volver a la sala", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEEF1F5)) }
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(title: String, wins: Int, pieceColor: Color, badgeColor: Color, isWinning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isWinning) Color(0xFF26200F) else Color(0xFF151C25), RoundedCornerShape(12.dp))
            .border(1.dp, if (isWinning) Color(0xFFE2B94E) else Color(0xFF232C38), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B96A5), letterSpacing = 0.5.sp)
            Text(
                if (title == "BLANCAS") "Tus victorias: $wins" else "Tus victorias: $wins",
                fontSize = 10.5.sp,
                color = Color(0xFF8B96A5)
            )
        }
        Box(
            modifier = Modifier.size(44.dp).background(badgeColor, RoundedCornerShape(7.dp)).border(1.dp, Color(0xFF232C38), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(wins.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = pieceColor)
        }
    }
}

// ---------- Promoción ----------

private fun pieceRes(pieceType: PieceType, color: ChessColor, pieceSet: PieceSet): Int =
    pieceResource(Piece(pieceType, color), pieceSet)

@Composable
fun PromotionPieceDialog(
    localColor: ChessColor,
    pieceSet: PieceSet = PieceSet.DEFAULT,
    onPromote: (PieceType) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141A), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF3A3524), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (localColor == ChessColor.WHITE) "Peón BLANCO promueve" else "Peón NEGRO promueve",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (localColor == ChessColor.WHITE) Color(0xFFECECF4) else Color(0xFF141414),
                    textAlign = TextAlign.Center
                )
                Text("Elige la pieza", fontSize = 12.5.sp, color = Color(0xFF8B96A5), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(PieceType.BISHOP, PieceType.ROOK, PieceType.KNIGHT, PieceType.QUEEN).forEach { pt ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { onPromote(pt) }
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = pieceRes(pt, localColor, pieceSet)),
                                contentDescription = pt.name,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(
                                pt.name.take(1).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B96A5)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F))
                ) {
                    Text("Cancelar", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

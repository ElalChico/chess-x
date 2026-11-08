/*
 * CHESS-X · © Elal Chico
 * Diálogos de finalización: fin de partida (mismo color / nuevo color / torneo),
 * fin de torneo (marcador) y selección de pieza al promover.
 */

package com.chessx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chessx.R
import com.chessx.core.Color as ChessColor
import com.chessx.core.GameEnd
import com.chessx.core.PieceType

// ---------- Fin de partida ----------

@Composable
fun GameEndDialog(
    end: GameEnd,
    isHost: Boolean,
    tournamentActive: Boolean,
    tournamentFinished: Boolean,
    onNewGameSame: () -> Unit,
    onNewGameRandom: () -> Unit,
    onTournamentNext: () -> Unit,
    onBackToRoom: () -> Unit,
    onDismiss: () -> Unit
) {
    val resultColorStr = if (end.winner == null) "draw" else
        (if (end.winner == ChessColor.WHITE) "white" else "black")

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    subtitle = "Mismo color · misma cuenta 3-2-1",
                    enabled = true,
                    highlight = false,
                    onClick = onNewGameSame
                )
                EndGameOption(
                    title = "Nuevo color",
                    subtitle = "Aleatorio · siguiente partida",
                    enabled = true,
                    highlight = false,
                    onClick = onNewGameRandom
                )
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
private fun EndGameOption(title: String, subtitle: String, enabled: Boolean, highlight: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enabled) Color(0xFF151C25) else Color(0xFF12141A), RoundedCornerShape(13.dp))
            .border(1.dp, if (highlight) Color(0xFFE2B94E) else Color(0xFF232C38), RoundedCornerShape(13.dp))
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
        Text("→", color = if (highlight) Color(0xFFE2B94E) else Color(0xFF232C38), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
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

private fun pieceRes(pieceType: PieceType, color: ChessColor): Int = when (pieceType) {
    PieceType.PAWN -> if (color == ChessColor.WHITE) R.drawable.piece_wp else R.drawable.piece_bp
    PieceType.KNIGHT -> if (color == ChessColor.WHITE) R.drawable.piece_wn else R.drawable.piece_bn
    PieceType.BISHOP -> if (color == ChessColor.WHITE) R.drawable.piece_wb else R.drawable.piece_bb
    PieceType.ROOK -> if (color == ChessColor.WHITE) R.drawable.piece_wr else R.drawable.piece_br
    PieceType.QUEEN -> if (color == ChessColor.WHITE) R.drawable.piece_wq else R.drawable.piece_bq
    PieceType.KING -> if (color == ChessColor.WHITE) R.drawable.piece_wk else R.drawable.piece_bk
}

@Composable
fun PromotionPieceDialog(
    localColor: ChessColor,
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
                                painter = androidx.compose.ui.res.painterResource(id = pieceRes(pt, localColor)),
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

package com.chessx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.core.BoardSnapshot
import com.chessx.core.ChessEngine
import com.chessx.core.Color as ChessColor
import com.chessx.core.Move
import com.chessx.core.PieceType
import com.chessx.data.GameRecord
import com.chessx.data.formatClock
import com.chessx.ui.components.BoardSelection
import com.chessx.ui.components.ChessBoard

private val Bg2 = Color(0xFF0B0D10)
private val Card3 = Color(0xFF171D25)
private val Line3 = Color(0xFF252E39)
private val Text13 = Color(0xFFEEF1F5)
private val Muted3 = Color(0xFF8B96A5)
private val Gold3 = Color(0xFFE2B94E)
private val Green3 = Color(0xFF3DDC97)

/**
 * Replay of a finished game from its PGN data: names, colors and per-move clocks.
 */
@Composable
fun ReplayScreen(record: GameRecord, onBack: () -> Unit) {
    var idx by remember { mutableIntStateOf(0) }
    val total = record.uciMoves.size
    val snapshot: BoardSnapshot = remember(idx) {
        val engine = ChessEngine()
        engine.reset()
        repeat(idx) { i ->
            val uci = record.uciMoves.getOrNull(i) ?: return@repeat
            val promo = when (uci.lastOrNull()) {
                'q' -> PieceType.QUEEN; 'r' -> PieceType.ROOK; 'b' -> PieceType.BISHOP; 'n' -> PieceType.KNIGHT
                else -> null
            }
            val from = Move.algebraicToSquare(uci.substring(0, 2))
            val to = Move.algebraicToSquare(uci.substring(2, 4))
            if (from != null && to != null) engine.findMove(from, to, promo)?.let { engine.doMove(it) }
        }
        engine.snapshot
    }
    val localIsWhite = record.localColorWhite
    val localName = record.playerName
    val oppName = record.opponent
    val whiteClock = clockAfter(record, idx, true)
    val blackClock = clockAfter(record, idx, false)
    val localClock = if (localIsWhite) whiteClock else blackClock
    val remoteClock = if (localIsWhite) blackClock else whiteClock
    val san = record.movesSan.getOrNull(idx - 1)

    Column(
        modifier = Modifier.fillMaxSize().background(Bg2).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("< Volver al historial", color = Muted3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Reproducción · $localName vs $oppName",
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Text13, maxLines = 1
                )
                Text(
                    "${record.dateLabel()} · ${record.timeLabel} · ${if (localIsWhite) "Juegas blancas" else "Juegas negras"} · ${record.result}",
                    fontSize = 11.sp, color = Muted3, maxLines = 1
                )
            }
        }
        HudBar(oppName, false, remoteClock, record)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val boardSize = minOf(maxWidth, maxHeight)
            ChessBoard(
                snapshot = snapshot,
                perspective = if (localIsWhite) ChessColor.WHITE else ChessColor.BLACK,
                selection = BoardSelection(null),
                onSquareTap = {},
                modifier = Modifier.size(boardSize)
            )
        }
        HudBar(localName, true, localClock, record)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarBtn("Inicio") { idx = 0 }
            BarBtn("< Atrás", enabled = idx > 0) { if (idx > 0) idx-- }
            Text(
                "$idx/$total" + (san?.let { " · $it" } ?: ""),
                fontSize = 12.sp, color = Muted3, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            BarBtn("Adelante >", enabled = idx < total) { if (idx < total) idx++ }
            BarBtn("Final") { idx = total }
        }
    }
}

private fun clockAfter(record: GameRecord, idx: Int, white: Boolean): Long {
    var last: Long? = null
    for (i in 0 until minOf(idx, record.moveClocks.size)) {
        val isWhiteMove = i % 2 == 0
        if (isWhiteMove == white) last = record.moveClocks[i]
    }
    return last ?: 0L
}

@Composable
private fun HudBar(name: String, isYou: Boolean, clockMs: Long, record: GameRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Card3, RoundedCornerShape(16.dp))
            .border(1.dp, Line3, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).background(
                if (isYou) Color(0xFF15251D) else Color(0xFF251A1A), androidx.compose.foundation.shape.CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (name.firstOrNull() ?: 'T').uppercaseChar().toString(),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = if (isYou) Green3 else Color(0xFFFF9B9B)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(name, color = Text13, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
        Text(formatClock(clockMs), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Text13)
    }
}

@Composable
private fun BarBtn(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (enabled) Color(0xFF1A212B) else Color(0xFF14181F), RoundedCornerShape(12.dp))
            .border(1.dp, Line3, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (enabled) Text13 else Muted3)
    }
}

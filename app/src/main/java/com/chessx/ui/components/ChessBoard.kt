/*
 * CHESS-X · © Elal Chico
 * Tablero de ajedrez con piezas cburnett SVG y resaltados de juego.
 */

package com.chessx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.R
import com.chessx.core.BoardSnapshot
import com.chessx.core.Color as ChessColor
import com.chessx.core.Piece
import com.chessx.core.PieceType
import com.chessx.data.BoardTheme

private val LightClassic = Color(0xFFF0D9B5)
private val DarkClassic = Color(0xFFB58863)
private val LightDark = Color(0xFF5E7A96)
private val DarkDark = Color(0xFF2A3646)

private val GoldHl = Color(0xFFE2B94E)
private val GreenDot = Color(0xFF3DDC97)
private val RedKing = Color(0xFFFF6B6B)

/** Recurso drawable para cada pieza (versión cburnett SVG oficial de Lichess). */
internal fun pieceResource(piece: Piece): Int = when (piece.color) {
    ChessColor.WHITE -> when (piece.type) {
        PieceType.PAWN -> R.drawable.piece_wp
        PieceType.KNIGHT -> R.drawable.piece_wn
        PieceType.BISHOP -> R.drawable.piece_wb
        PieceType.ROOK -> R.drawable.piece_wr
        PieceType.QUEEN -> R.drawable.piece_wq
        PieceType.KING -> R.drawable.piece_wk
    }
    ChessColor.BLACK -> when (piece.type) {
        PieceType.PAWN -> R.drawable.piece_bp
        PieceType.KNIGHT -> R.drawable.piece_bn
        PieceType.BISHOP -> R.drawable.piece_bb
        PieceType.ROOK -> R.drawable.piece_br
        PieceType.QUEEN -> R.drawable.piece_bq
        PieceType.KING -> R.drawable.piece_bk
    }
}

/** Convierte coordenadas de pantalla (fila/columna mostradas) a índice de casilla 0..63. */
internal fun displayToSquare(row: Int, col: Int, perspective: ChessColor): Int = when (perspective) {
    ChessColor.WHITE -> (7 - row) * 8 + col
    ChessColor.BLACK -> row * 8 + (7 - col)
}

/**
 * Tablero cuadrado: las piezas se dibujan con los SVG oficiales; al pulsar una
 * casilla se invoca [onSquareTap] con el índice 0..63 (0 = a1).
 */
@Composable
fun ChessBoard(
    snapshot: BoardSnapshot,
    perspective: ChessColor,
    selection: BoardSelection,
    onSquareTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    theme: BoardTheme = BoardTheme.CLASSIC,
    showMoveIndicators: Boolean = true
) {
    Box(modifier = modifier.background(Color(0xFF0E1318))) {
        for (row in 0 until 8) {
            Row(modifier = Modifier.fillMaxSize()) {
                for (col in 0 until 8) {
                    val sq = displayToSquare(row, col, perspective)
                    BoardSquare(
                        index = sq,
                        piece = snapshot.board.getOrNull(sq),
                        isLight = (row + col) % 2 == 0,
                        theme = theme,
                        selected = selection.from == sq,
                        legalTarget = showMoveIndicators && selection.legalTargets.contains(sq),
                        lastMoveSq = selection.lastMove?.let { it.first == sq || it.second == sq } == true,
                        checkKing = selection.checkKingSquare == sq,
                        errorSq = selection.errorSquare == sq,
                        onSquareTap = onSquareTap,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BoardSquare(
    index: Int,
    piece: Piece?,
    isLight: Boolean,
    theme: BoardTheme,
    selected: Boolean,
    legalTarget: Boolean,
    lastMoveSq: Boolean,
    checkKing: Boolean,
    errorSq: Boolean,
    onSquareTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val base = when (theme) {
        BoardTheme.CLASSIC -> if (isLight) LightClassic else DarkClassic
        BoardTheme.DARK -> if (isLight) LightDark else DarkDark
    }
    Box(
        modifier = modifier
            .background(base)
            .clickable { onSquareTap(index) },
        contentAlignment = Alignment.Center
    ) {
        // Resaltados encima del color base
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (lastMoveSq) drawRect(color = GoldHl.copy(alpha = 0.28f), size = size)
            if (selected) drawRect(color = GoldHl.copy(alpha = 0.45f), size = size)
            if (checkKing) drawCircle(color = RedKing.copy(alpha = 0.55f), radius = size.minDimension / 2.4f, center = Offset(size.width / 2f, size.height / 2f))
            if (errorSq) drawRect(color = RedKing.copy(alpha = 0.55f), size = size)
            if (legalTarget) {
                if (piece != null) {
                    // Captura: anillo dorado alrededor
                    drawCircle(
                        color = GreenDot.copy(alpha = 0.75f),
                        radius = size.minDimension / 2f - 2.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                } else {
                    // Destino libre: punto central
                    drawCircle(
                        color = GreenDot.copy(alpha = 0.75f),
                        radius = size.minDimension / 6f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
        }
        piece?.let {
            Image(
                painter = painterResource(id = pieceResource(it)),
                contentDescription = it.symbol(),
                modifier = Modifier.fillMaxSize().padding(2.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/*
 * CHESS-X Â· Â© Elal Chico
 * Tablero de ajedrez con estilos vectoriales, coordenadas y resaltados de juego.
 */

package com.chessx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.chessx.R
import com.chessx.core.BoardSnapshot
import com.chessx.core.Color as ChessColor
import com.chessx.core.Piece
import com.chessx.core.PieceType
import com.chessx.data.BoardTheme
import com.chessx.data.PieceSet

private val LightClassic = Color(0xFFF0D9B5)
private val DarkClassic = Color(0xFFB58863)
private val LightDark = Color(0xFFB6BEC9)
private val DarkDark = Color(0xFF5D6775)

private val GoldHl = Color(0xFFE2B94E)
private val GreenDot = Color(0xFF3DDC97)
private val RedKing = Color(0xFFFF6B6B)

/**
 * Recurso drawable de cada pieza del conjunto elegido.
 * Todos los conjuntos son los oficiales de Lichess: `ps_<clave>_<w|b><pieza>`.
 */
internal fun pieceResource(piece: Piece, pieceSet: PieceSet = PieceSet.DEFAULT): Int {
    val c = if (piece.color == ChessColor.WHITE) "w" else "b"
    val t = when (piece.type) {
        PieceType.PAWN -> "p"
        PieceType.KNIGHT -> "n"
        PieceType.BISHOP -> "b"
        PieceType.ROOK -> "r"
        PieceType.QUEEN -> "q"
        PieceType.KING -> "k"
    }
    return when (pieceSet) {
        PieceSet.LICHESS -> when ("$c$t") {
            "wp" -> R.drawable.ps_cburnett_wp
            "bp" -> R.drawable.ps_cburnett_bp
            "wn" -> R.drawable.ps_cburnett_wn
            "bn" -> R.drawable.ps_cburnett_bn
            "wb" -> R.drawable.ps_cburnett_wb
            "bb" -> R.drawable.ps_cburnett_bb
            "wr" -> R.drawable.ps_cburnett_wr
            "br" -> R.drawable.ps_cburnett_br
            "wq" -> R.drawable.ps_cburnett_wq
            "bq" -> R.drawable.ps_cburnett_bq
            "wk" -> R.drawable.ps_cburnett_wk
            "bk" -> R.drawable.ps_cburnett_bk
            else -> R.drawable.ps_cburnett_bk
        }
        PieceSet.ALPHA -> when ("$c$t") {
            "wp" -> R.drawable.ps_alpha_wp
            "bp" -> R.drawable.ps_alpha_bp
            "wn" -> R.drawable.ps_alpha_wn
            "bn" -> R.drawable.ps_alpha_bn
            "wb" -> R.drawable.ps_alpha_wb
            "bb" -> R.drawable.ps_alpha_bb
            "wr" -> R.drawable.ps_alpha_wr
            "br" -> R.drawable.ps_alpha_br
            "wq" -> R.drawable.ps_alpha_wq
            "bq" -> R.drawable.ps_alpha_bq
            "wk" -> R.drawable.ps_alpha_wk
            "bk" -> R.drawable.ps_alpha_bk
            else -> R.drawable.ps_alpha_bk
        }
        PieceSet.ANARCANDY -> when ("$c$t") {
            "wp" -> R.drawable.ps_anarcandy_wp
            "bp" -> R.drawable.ps_anarcandy_bp
            "wn" -> R.drawable.ps_anarcandy_wn
            "bn" -> R.drawable.ps_anarcandy_bn
            "wb" -> R.drawable.ps_anarcandy_wb
            "bb" -> R.drawable.ps_anarcandy_bb
            "wr" -> R.drawable.ps_anarcandy_wr
            "br" -> R.drawable.ps_anarcandy_br
            "wq" -> R.drawable.ps_anarcandy_wq
            "bq" -> R.drawable.ps_anarcandy_bq
            "wk" -> R.drawable.ps_anarcandy_wk
            "bk" -> R.drawable.ps_anarcandy_bk
            else -> R.drawable.ps_anarcandy_bk
        }
        PieceSet.COMPANION -> when ("$c$t") {
            "wp" -> R.drawable.ps_companion_wp
            "bp" -> R.drawable.ps_companion_bp
            "wn" -> R.drawable.ps_companion_wn
            "bn" -> R.drawable.ps_companion_bn
            "wb" -> R.drawable.ps_companion_wb
            "bb" -> R.drawable.ps_companion_bb
            "wr" -> R.drawable.ps_companion_wr
            "br" -> R.drawable.ps_companion_br
            "wq" -> R.drawable.ps_companion_wq
            "bq" -> R.drawable.ps_companion_bq
            "wk" -> R.drawable.ps_companion_wk
            "bk" -> R.drawable.ps_companion_bk
            else -> R.drawable.ps_companion_bk
        }
        PieceSet.HORSEY -> when ("$c$t") {
            "wp" -> R.drawable.ps_horsey_wp
            "bp" -> R.drawable.ps_horsey_bp
            "wn" -> R.drawable.ps_horsey_wn
            "bn" -> R.drawable.ps_horsey_bn
            "wb" -> R.drawable.ps_horsey_wb
            "bb" -> R.drawable.ps_horsey_bb
            "wr" -> R.drawable.ps_horsey_wr
            "br" -> R.drawable.ps_horsey_br
            "wq" -> R.drawable.ps_horsey_wq
            "bq" -> R.drawable.ps_horsey_bq
            "wk" -> R.drawable.ps_horsey_wk
            "bk" -> R.drawable.ps_horsey_bk
            else -> R.drawable.ps_horsey_bk
        }
        PieceSet.LEIPZIG -> when ("$c$t") {
            "wp" -> R.drawable.ps_leipzig_wp
            "bp" -> R.drawable.ps_leipzig_bp
            "wn" -> R.drawable.ps_leipzig_wn
            "bn" -> R.drawable.ps_leipzig_bn
            "wb" -> R.drawable.ps_leipzig_wb
            "bb" -> R.drawable.ps_leipzig_bb
            "wr" -> R.drawable.ps_leipzig_wr
            "br" -> R.drawable.ps_leipzig_br
            "wq" -> R.drawable.ps_leipzig_wq
            "bq" -> R.drawable.ps_leipzig_bq
            "wk" -> R.drawable.ps_leipzig_wk
            "bk" -> R.drawable.ps_leipzig_bk
            else -> R.drawable.ps_leipzig_bk
        }
        PieceSet.MERIDA -> when ("$c$t") {
            "wp" -> R.drawable.ps_merida_wp
            "bp" -> R.drawable.ps_merida_bp
            "wn" -> R.drawable.ps_merida_wn
            "bn" -> R.drawable.ps_merida_bn
            "wb" -> R.drawable.ps_merida_wb
            "bb" -> R.drawable.ps_merida_bb
            "wr" -> R.drawable.ps_merida_wr
            "br" -> R.drawable.ps_merida_br
            "wq" -> R.drawable.ps_merida_wq
            "bq" -> R.drawable.ps_merida_bq
            "wk" -> R.drawable.ps_merida_wk
            "bk" -> R.drawable.ps_merida_bk
            else -> R.drawable.ps_merida_bk
        }
        PieceSet.PIXEL -> when ("$c$t") {
            "wp" -> R.drawable.ps_pixel_wp
            "bp" -> R.drawable.ps_pixel_bp
            "wn" -> R.drawable.ps_pixel_wn
            "bn" -> R.drawable.ps_pixel_bn
            "wb" -> R.drawable.ps_pixel_wb
            "bb" -> R.drawable.ps_pixel_bb
            "wr" -> R.drawable.ps_pixel_wr
            "br" -> R.drawable.ps_pixel_br
            "wq" -> R.drawable.ps_pixel_wq
            "bq" -> R.drawable.ps_pixel_bq
            "wk" -> R.drawable.ps_pixel_wk
            "bk" -> R.drawable.ps_pixel_bk
            else -> R.drawable.ps_pixel_bk
        }
        PieceSet.RIOHACHA -> when ("$c$t") {
            "wp" -> R.drawable.ps_riohacha_wp
            "bp" -> R.drawable.ps_riohacha_bp
            "wn" -> R.drawable.ps_riohacha_wn
            "bn" -> R.drawable.ps_riohacha_bn
            "wb" -> R.drawable.ps_riohacha_wb
            "bb" -> R.drawable.ps_riohacha_bb
            "wr" -> R.drawable.ps_riohacha_wr
            "br" -> R.drawable.ps_riohacha_br
            "wq" -> R.drawable.ps_riohacha_wq
            "bq" -> R.drawable.ps_riohacha_bq
            "wk" -> R.drawable.ps_riohacha_wk
            "bk" -> R.drawable.ps_riohacha_bk
            else -> R.drawable.ps_riohacha_bk
        }
        PieceSet.STAUNTY -> when ("$c$t") {
            "wp" -> R.drawable.ps_staunty_wp
            "bp" -> R.drawable.ps_staunty_bp
            "wn" -> R.drawable.ps_staunty_wn
            "bn" -> R.drawable.ps_staunty_bn
            "wb" -> R.drawable.ps_staunty_wb
            "bb" -> R.drawable.ps_staunty_bb
            "wr" -> R.drawable.ps_staunty_wr
            "br" -> R.drawable.ps_staunty_br
            "wq" -> R.drawable.ps_staunty_wq
            "bq" -> R.drawable.ps_staunty_bq
            "wk" -> R.drawable.ps_staunty_wk
            "bk" -> R.drawable.ps_staunty_bk
            else -> R.drawable.ps_staunty_bk
        }
        PieceSet.TATIANA -> when ("$c$t") {
            "wp" -> R.drawable.ps_tatiana_wp
            "bp" -> R.drawable.ps_tatiana_bp
            "wn" -> R.drawable.ps_tatiana_wn
            "bn" -> R.drawable.ps_tatiana_bn
            "wb" -> R.drawable.ps_tatiana_wb
            "bb" -> R.drawable.ps_tatiana_bb
            "wr" -> R.drawable.ps_tatiana_wr
            "br" -> R.drawable.ps_tatiana_br
            "wq" -> R.drawable.ps_tatiana_wq
            "bq" -> R.drawable.ps_tatiana_bq
            "wk" -> R.drawable.ps_tatiana_wk
            "bk" -> R.drawable.ps_tatiana_bk
            else -> R.drawable.ps_tatiana_bk
        }
        PieceSet.ICPIECES -> when ("$c$t") {
            "wp" -> R.drawable.ps_icpieces_wp
            "bp" -> R.drawable.ps_icpieces_bp
            "wn" -> R.drawable.ps_icpieces_wn
            "bn" -> R.drawable.ps_icpieces_bn
            "wb" -> R.drawable.ps_icpieces_wb
            "bb" -> R.drawable.ps_icpieces_bb
            "wr" -> R.drawable.ps_icpieces_wr
            "br" -> R.drawable.ps_icpieces_br
            "wq" -> R.drawable.ps_icpieces_wq
            "bq" -> R.drawable.ps_icpieces_bq
            "wk" -> R.drawable.ps_icpieces_wk
            "bk" -> R.drawable.ps_icpieces_bk
            else -> R.drawable.ps_icpieces_bk
        }
    }
}

/** Convierte coordenadas de pantalla (fila/columna mostradas) a Ã­ndice de casilla 0..63. */
internal fun displayToSquare(row: Int, col: Int, perspective: ChessColor): Int = when (perspective) {
    ChessColor.WHITE -> (7 - row) * 8 + col
    ChessColor.BLACK -> row * 8 + (7 - col)
}

/**
 * Tablero cuadrado: las piezas se dibujan con los SVG oficiales; al pulsar una
 * casilla se invoca [onSquareTap] con el Ã­ndice 0..63 (0 = a1).
 */
@Composable
fun ChessBoard(
    snapshot: BoardSnapshot,
    perspective: ChessColor,
    selection: BoardSelection,
    onSquareTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    theme: BoardTheme = BoardTheme.LICHESS,
    pieceSet: PieceSet = PieceSet.DEFAULT,
    showMoveIndicators: Boolean = true,
    showCoordinates: Boolean = true
) {
    Layout(
        modifier = modifier.background(theme.boardBackground),
        content = {
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val sq = displayToSquare(row, col, perspective)
                    BoardSquare(
                        index = sq,
                        piece = snapshot.board.getOrNull(sq),
                        isLight = (row + col) % 2 == 0,
                        theme = theme,
                        pieceSet = pieceSet,
                        showCoordinates = showCoordinates,
                        displayRow = row,
                        displayCol = col,
                        selected = selection.from == sq,
                        legalTarget = showMoveIndicators && selection.legalTargets.contains(sq),
                        lastMoveSq = selection.lastMove?.let { it.first == sq || it.second == sq } == true,
                        checkKing = selection.checkKingSquare == sq,
                        errorSq = selection.errorSquare == sq,
                        onSquareTap = onSquareTap
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val boardSize = minOf(constraints.maxWidth, constraints.maxHeight)
        val coordinates = IntArray(9) { index -> index * boardSize / 8 }
        val cells = measurables.mapIndexed { index, measurable ->
            val row = index / 8
            val col = index % 8
            measurable.measure(
                androidx.compose.ui.unit.Constraints.fixed(
                    coordinates[col + 1] - coordinates[col],
                    coordinates[row + 1] - coordinates[row]
                )
            )
        }
        layout(boardSize, boardSize) {
            cells.forEachIndexed { index, cell ->
                cell.place(coordinates[index % 8], coordinates[index / 8])
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
    pieceSet: PieceSet,
    showCoordinates: Boolean,
    displayRow: Int,
    displayCol: Int,
    selected: Boolean,
    legalTarget: Boolean,
    lastMoveSq: Boolean,
    checkKing: Boolean,
    errorSq: Boolean,
    onSquareTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val base = if (isLight) theme.lightSquare else theme.darkSquare
    BoxWithConstraints(
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
            if (showCoordinates) {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = (if (isLight) Color(0xFF70583F) else Color(0xFFFFE6B9)).toArgb()
                        textSize = size.minDimension * 0.17f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val inset = size.minDimension * 0.08f
                    if (displayCol == 0) {
                        val rank = (index / 8 + 1).toString()
                        native.drawText(rank, inset, paint.textSize + inset, paint)
                    }
                    if (displayRow == 7) {
                        val file = ('a'.code + index % 8).toChar().toString()
                        native.drawText(file, size.width - paint.measureText(file) - inset, size.height - inset, paint)
                    }
                }
            }
        }
        piece?.let {
            Image(
                painter = painterResource(id = pieceResource(it, pieceSet)),
                contentDescription = it.symbol(),
                modifier = Modifier.fillMaxSize().padding(2.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

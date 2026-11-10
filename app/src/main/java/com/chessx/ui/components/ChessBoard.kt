/*
 * CHESS-X · © Elal Chico
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

/** Recurso drawable para cada pieza del conjunto Lichess. */
internal fun pieceResource(piece: Piece, pieceSet: PieceSet = PieceSet.DEFAULT): Int = when (pieceSet) {
    PieceSet.LICHESS, PieceSet.CHESS_COM, PieceSet.OPEN_SOURCE -> when (piece.color) {
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
            if (pieceSet == PieceSet.LICHESS) {
                Image(
                    painter = painterResource(id = pieceResource(it, pieceSet)),
                    contentDescription = it.symbol(),
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                PieceIcon(it, Modifier.fillMaxSize().padding(1.dp), pieceSet)
            }
        }
    }
}

@Composable
fun PieceIcon(piece: Piece, modifier: Modifier = Modifier, pieceSet: PieceSet = PieceSet.CHESS_COM) {
    val modern = pieceSet == PieceSet.CHESS_COM
    val fill = when {
        modern && piece.color == ChessColor.WHITE -> Color(0xFFF9F7F0)
        modern -> Color(0xFF26384F)
        piece.color == ChessColor.WHITE -> Color(0xFFFFF4DC)
        else -> Color(0xFF252B34)
    }
    val outline = when {
        modern && piece.color == ChessColor.WHITE -> Color(0xFF929A9E)
        modern -> Color(0xFF9AB8D5)
        piece.color == ChessColor.WHITE -> Color(0xFF765B3A)
        else -> Color(0xFFD7B66B)
    }
    Canvas(modifier = modifier) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            val stroke = Stroke(width = if (modern) 2.2f else 3.2f)
            fun path(points: List<Offset>): Path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            fun silhouette(points: List<Offset>) {
                val p = path(points)
                if (modern) {
                    drawContext.transform.translate(2f, 3f)
                    drawPath(p, Color(0x66000000))
                    drawContext.transform.translate(-2f, -3f)
                }
                drawPath(p, fill)
                drawPath(p, outline, style = stroke)
            }
            drawRoundRect(fill, topLeft = Offset(14f, 83f), size = Size(72f, 9f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
            drawRoundRect(outline, topLeft = Offset(14f, 83f), size = Size(72f, 9f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f), style = stroke)
            when (piece.type) {
                PieceType.PAWN -> {
                    silhouette(listOf(Offset(24f, 79f), Offset(31f, 62f), Offset(39f, 54f), Offset(32f, 48f), Offset(34f, 43f), Offset(66f, 43f), Offset(68f, 48f), Offset(61f, 54f), Offset(69f, 62f), Offset(76f, 79f)))
                    drawCircle(fill, 12f, Offset(50f, 30f))
                    drawCircle(outline, 12f, Offset(50f, 30f), style = stroke)
                }
                PieceType.ROOK -> silhouette(listOf(
                    Offset(15f, 24f), Offset(29f, 24f), Offset(29f, 34f), Offset(40f, 34f),
                    Offset(40f, 24f), Offset(60f, 24f), Offset(60f, 34f), Offset(71f, 34f),
                    Offset(71f, 24f), Offset(85f, 24f), Offset(80f, 48f), Offset(73f, 56f),
                    Offset(78f, 79f), Offset(22f, 79f), Offset(27f, 56f), Offset(20f, 48f)
                ))
                PieceType.KNIGHT -> silhouette(listOf(
                    Offset(20f, 79f), Offset(27f, 61f), Offset(18f, 50f), Offset(23f, 37f),
                    Offset(46f, 20f), Offset(43f, 34f), Offset(62f, 29f), Offset(82f, 41f),
                    Offset(77f, 54f), Offset(65f, 61f), Offset(78f, 79f)
                )).also {
                    drawLine(outline, Offset(40f, 45f), Offset(47f, 45f), strokeWidth = 3f)
                }
                PieceType.BISHOP -> {
                    silhouette(listOf(Offset(21f, 79f), Offset(31f, 61f), Offset(40f, 51f), Offset(34f, 42f), Offset(50f, 24f), Offset(66f, 42f), Offset(60f, 51f), Offset(69f, 61f), Offset(79f, 79f)))
                    drawCircle(fill, 6f, Offset(50f, 28f))
                    drawCircle(outline, 6f, Offset(50f, 28f), style = stroke)
                    drawLine(outline, Offset(45f, 39f), Offset(55f, 49f), strokeWidth = 2.5f)
                }
                PieceType.QUEEN -> {
                    silhouette(listOf(Offset(19f, 79f), Offset(28f, 56f), Offset(23f, 34f), Offset(41f, 48f), Offset(50f, 27f), Offset(59f, 48f), Offset(77f, 34f), Offset(72f, 56f), Offset(81f, 79f)))
                    listOf(Offset(24f, 31f), Offset(50f, 21f), Offset(76f, 31f)).forEach { center ->
                        drawCircle(fill, 6f, center)
                        drawCircle(outline, 6f, center, style = stroke)
                    }
                }
                PieceType.KING -> {
                    silhouette(listOf(Offset(20f, 79f), Offset(30f, 58f), Offset(39f, 48f), Offset(35f, 39f), Offset(65f, 39f), Offset(61f, 48f), Offset(70f, 58f), Offset(80f, 79f)))
                    drawLine(outline, Offset(50f, 12f), Offset(50f, 36f), strokeWidth = 4f)
                    drawLine(outline, Offset(40f, 22f), Offset(60f, 22f), strokeWidth = 4f)
                }
            }
        }
    }
}

package com.chessx.core

enum class Color { WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING;

    fun isSliding() = this == BISHOP || this == ROOK || this == QUEEN
}

data class Piece(val type: PieceType, val color: Color) {
    fun symbol(): String = when (color) {
        Color.WHITE -> when (type) {
            PieceType.KING -> "♔"; PieceType.QUEEN -> "♕"; PieceType.ROOK -> "♖"
            PieceType.BISHOP -> "♗"; PieceType.KNIGHT -> "♘"; PieceType.PAWN -> "♙"
        }
        Color.BLACK -> when (type) {
            PieceType.KING -> "♚"; PieceType.QUEEN -> "♛"; PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"; PieceType.KNIGHT -> "♞"; PieceType.PAWN -> "♟"
        }
    }
}

data class Move(
    val from: Int, // 0..63, 0=a1, 7=h1, 56=a8
    val to: Int,
    val promotion: PieceType? = null,
    val san: String = "",
    val isCapture: Boolean = false,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false,
    val isEnPassant: Boolean = false,
    val isDoublePawnPush: Boolean = false
) {
    fun uci(): String {
        val base = squareToAlgebraic(from) + squareToAlgebraic(to)
        return if (promotion != null) base + when (promotion) {
            PieceType.QUEEN -> "q"
            PieceType.ROOK -> "r"
            PieceType.BISHOP -> "b"
            PieceType.KNIGHT -> "n"
            else -> "q"
        } else base
    }
    companion object {
        fun squareToAlgebraic(sq: Int): String {
            require(sq in 0..63) { "invalid square" }
            val file = 'a' + (sq % 8)
            val rank = '1' + (sq / 8)
            return "$file$rank"
        }
        fun algebraicToSquare(s: String): Int? {
            if (s.length != 2) return null
            val f = s[0] - 'a'
            val r = s[1] - '1'
            if (f !in 0..7 || r !in 0..7) return null
            return r * 8 + f
        }
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
)

data class BoardSnapshot(
    val board: Array<Piece?>, // size 64
    val sideToMove: Color = Color.WHITE,
    val castling: CastlingRights = CastlingRights(),
    val enPassantSquare: Int? = null,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val o = other as? BoardSnapshot ?: return false
        return sideToMove == o.sideToMove && castling == o.castling &&
            enPassantSquare == o.enPassantSquare && halfmoveClock == o.halfmoveClock &&
            fullmoveNumber == o.fullmoveNumber && board.contentEquals(o.board)
    }
    override fun hashCode(): Int {
        var r = board.contentHashCode()
        r = 31 * r + sideToMove.hashCode()
        r = 31 * r + castling.hashCode()
        r = 31 * r + (enPassantSquare ?: -1)
        r = 31 * r + halfmoveClock
        r = 31 * r + fullmoveNumber
        return r
    }
}

enum class GameEndReason {
    CHECKMATE, STALEMATE, DRAW_INSUFFICIENT, DRAW_FIFTY,
    RESIGNATION, TIMEOUT, DRAW_AGREED
}

data class GameEnd(
    val winner: Color?, // null = draw
    val reason: GameEndReason,
    val message: String
)

enum class MoveValidation {
    OK, NO_PIECE, WRONG_COLOR, ILLEGAL_MOVE, SELF_CHECK
}

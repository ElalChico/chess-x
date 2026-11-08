package com.chessx.core

/**
 * Pure-Kotlin chess engine, rules compatible with Lichess (FIDE).
 * Zero external dependencies, fully defensive (never throws on user input),
 * deterministic and thread-confined to Main/StateFlow.
 */
class ChessEngine {

    @Volatile var snapshot: BoardSnapshot = startingPosition()
        private set

    fun reset() { snapshot = startingPosition() }

    fun loadSnapshot(s: BoardSnapshot) { snapshot = copy(s) }

    fun pieceAt(sq: Int): Piece? =
        if (sq !in 0..63) null else snapshot.board[sq]

    fun sideToMove(): Color = snapshot.sideToMove

    // ---------- Setup / FEN ----------
    fun startingPosition(): BoardSnapshot {
        val b = arrayOfNulls<Piece>(64)
        val back = arrayOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
        for (f in 0..7) {
            b[0 * 8 + f] = Piece(back[f], Color.WHITE)
            b[1 * 8 + f] = Piece(PieceType.PAWN, Color.WHITE)
            b[6 * 8 + f] = Piece(PieceType.PAWN, Color.BLACK)
            b[7 * 8 + f] = Piece(back[f], Color.BLACK)
        }
        return BoardSnapshot(b)
    }

    fun parseFEN(fen: String): Boolean {
        try {
            val parts = fen.trim().split("\\s+".toRegex())
            if (parts.size < 4) return false
            val rows = parts[0].split("/")
            if (rows.size != 8) return false
            val b = arrayOfNulls<Piece>(64)
            for ((rankIdx, row) in rows.withIndex()) {
                val rank = 7 - rankIdx
                var file = 0
                for (ch in row) {
                    if (ch.isDigit()) {
                        file += ch.digitToInt()
                    } else {
                        if (file > 7) return false
                        val color = if (ch.isUpperCase()) Color.WHITE else Color.BLACK
                        val type = when (ch.lowercase()) {
                            "p" -> PieceType.PAWN
                            "n" -> PieceType.KNIGHT
                            "b" -> PieceType.BISHOP
                            "r" -> PieceType.ROOK
                            "q" -> PieceType.QUEEN
                            "k" -> PieceType.KING
                            else -> return false
                        }
                        b[rank * 8 + file] = Piece(type, color)
                        file++
                    }
                }
                if (file != 8) return false
            }
            val side = if (parts[1] == "w") Color.WHITE else Color.BLACK
            val c = parts[2]
            val castling = CastlingRights(
                whiteKingside = c.contains("K"),
                whiteQueenside = c.contains("Q"),
                blackKingside = c.contains("k"),
                blackQueenside = c.contains("q")
            )
            val ep = if (parts[3] == "-") null else Move.algebraicToSquare(parts[3])
            val half = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val full = parts.getOrNull(5)?.toIntOrNull() ?: 1
            snapshot = BoardSnapshot(b, side, castling, ep, half, full)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun toFEN(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = snapshot.board[rank * 8 + file]
                if (p == null) empty++
                else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(when (p.type) {
                        PieceType.PAWN -> if (p.color == Color.WHITE) "P" else "p"
                        PieceType.KNIGHT -> if (p.color == Color.WHITE) "N" else "n"
                        PieceType.BISHOP -> if (p.color == Color.WHITE) "B" else "b"
                        PieceType.ROOK -> if (p.color == Color.WHITE) "R" else "r"
                        PieceType.QUEEN -> if (p.color == Color.WHITE) "Q" else "q"
                        PieceType.KING -> if (p.color == Color.WHITE) "K" else "k"
                    })
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(if (snapshot.sideToMove == Color.WHITE) " w " else " b ")
        val c = StringBuilder()
        if (snapshot.castling.whiteKingside) c.append('K')
        if (snapshot.castling.whiteQueenside) c.append('Q')
        if (snapshot.castling.blackKingside) c.append('k')
        if (snapshot.castling.blackQueenside) c.append('q')
        sb.append(if (c.isEmpty()) "-" else c.toString())
        sb.append(' ').append(snapshot.enPassantSquare?.let { Move.squareToAlgebraic(it) } ?: "-")
        sb.append(' ').append(snapshot.halfmoveClock).append(' ').append(snapshot.fullmoveNumber)
        return sb.toString()
    }

    // ---------- Attack detection ----------
    fun findKing(color: Color): Int? {
        for (i in 0..63) {
            val p = snapshot.board[i]
            if (p != null && p.type == PieceType.KING && p.color == color) return i
        }
        return null
    }

    fun isSquareAttacked(sq: Int, by: Color, state: BoardSnapshot = snapshot): Boolean {
        if (sq !in 0..63) return false
        val f = sq % 8; val r = sq / 8
        // Pawns
        val pawnDir = if (by == Color.WHITE) -1 else 1
        for (df in listOf(-1, 1)) {
            val pf = f + df; val pr = r + pawnDir
            if (pf in 0..7 && pr in 0..7) {
                val p = state.board[pr * 8 + pf]
                if (p != null && p.color == by && p.type == PieceType.PAWN) return true
            }
        }
        // Knights
        for ((df, dr) in listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)) {
            val nf = f + df; val nr = r + dr
            if (nf in 0..7 && nr in 0..7) {
                val p = state.board[nr * 8 + nf]
                if (p != null && p.color == by && p.type == PieceType.KNIGHT) return true
            }
        }
        // King
        for (df in -1..1) for (dr in -1..1) {
            if (df == 0 && dr == 0) continue
            val nf = f + df; val nr = r + dr
            if (nf in 0..7 && nr in 0..7) {
                val p = state.board[nr * 8 + nf]
                if (p != null && p.color == by && p.type == PieceType.KING) return true
            }
        }
        // Sliding: rook/queen straight
        for ((df, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            var nf = f + df; var nr = r + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = state.board[nr * 8 + nf]
                if (p != null) {
                    if (p.color == by && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nf += df; nr += dr
            }
        }
        // Sliding: bishop/queen diagonal
        for ((df, dr) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
            var nf = f + df; var nr = r + dr
            while (nf in 0..7 && nr in 0..7) {
                val p = state.board[nr * 8 + nf]
                if (p != null) {
                    if (p.color == by && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nf += df; nr += dr
            }
        }
        return false
    }

    fun isInCheck(color: Color, state: BoardSnapshot = snapshot): Boolean {
        val k = state.board.indexOfFirst { it != null && it.type == PieceType.KING && it.color == color }
        if (k == -1) return false
        return isSquareAttacked(k, color.opposite(), state)
    }

    // ---------- Move generation ----------
    fun legalMoves(): List<Move> {
        val pseudo = pseudoLegalMoves(snapshot)
        return pseudo.filter { m ->
            val next = applyMove(snapshot, m, dryRun = true)
            !isInCheck(snapshot.sideToMove, next)
        }
    }

    fun legalMovesFrom(sq: Int): List<Move> =
        if (sq !in 0..63) emptyList() else legalMoves().filter { it.from == sq }

    fun validateMove(from: Int, to: Int, promotion: PieceType? = null): MoveValidation {
        if (from !in 0..63 || to !in 0..63) return MoveValidation.ILLEGAL_MOVE
        val p = snapshot.board[from]
        if (p == null) return MoveValidation.NO_PIECE
        if (p.color != snapshot.sideToMove) return MoveValidation.WRONG_COLOR
        val legal = legalMoves()
        val match = legal.firstOrNull { it.from == from && it.to == to }
        if (match == null) return MoveValidation.ILLEGAL_MOVE
        if (p.type == PieceType.PAWN && (to / 8 == 7 || to / 8 == 0)) {
            // promotion handled by UI; if no promo supplied, default QUEEN for validation only
        }
        return MoveValidation.OK
    }

    private fun pseudoLegalMoves(state: BoardSnapshot): List<Move> {
        val moves = mutableListOf<Move>()
        val side = state.sideToMove
        for (sq in 0..63) {
            val p = state.board[sq] ?: continue
            if (p.color != side) continue
            val f = sq % 8; val r = sq / 8
            when (p.type) {
                PieceType.PAWN -> {
                    val dir = if (side == Color.WHITE) 1 else -1
                    val startRank = if (side == Color.WHITE) 1 else 6
                    val promoRank = if (side == Color.WHITE) 7 else 0
                    // forward 1
                    val oneSq = (r + dir) * 8 + f
                    if ((r + dir) in 0..7 && state.board[oneSq] == null) {
                        addPawnMove(moves, sq, oneSq, state, promoRank)
                        // double
                        if (r == startRank) {
                            val twoSq = (r + 2 * dir) * 8 + f
                            if (state.board[twoSq] == null) {
                                moves.add(Move(sq, twoSq, isDoublePawnPush = true))
                            }
                        }
                    }
                    // captures
                    for (df in listOf(-1, 1)) {
                        val nf = f + df; val nr = r + dir
                        if (nf !in 0..7 || nr !in 0..7) continue
                        val target = nr * 8 + nf
                        val tp = state.board[target]
                        if (tp != null && tp.color != side) {
                            addPawnMove(moves, sq, target, state, promoRank, isCapture = true)
                        } else if (state.enPassantSquare == target) {
                            moves.add(Move(sq, target, isEnPassant = true, isCapture = true))
                        }
                    }
                }
                PieceType.KNIGHT -> {
                    for ((df, dr) in listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)) {
                        val nf = f + df; val nr = r + dr
                        if (nf !in 0..7 || nr !in 0..7) continue
                        val t = nr * 8 + nf
                        val tp = state.board[t]
                        if (tp == null || tp.color != side) {
                            moves.add(Move(sq, t, isCapture = tp != null))
                        }
                    }
                }
                PieceType.KING -> {
                    for (df in -1..1) for (dr in -1..1) {
                        if (df == 0 && dr == 0) continue
                        val nf = f + df; val nr = r + dr
                        if (nf !in 0..7 || nr !in 0..7) continue
                        val t = nr * 8 + nf
                        val tp = state.board[t]
                        if (tp == null || tp.color != side) moves.add(Move(sq, t, isCapture = tp != null))
                    }
                    // castling
                    if (side == Color.WHITE && sq == 4) {
                        if (state.castling.whiteKingside && state.board[5] == null && state.board[6] == null &&
                            !isSquareAttacked(4, Color.BLACK, state) && !isSquareAttacked(5, Color.BLACK, state) && !isSquareAttacked(6, Color.BLACK, state) &&
                            state.board[7]?.type == PieceType.ROOK && state.board[7]?.color == Color.WHITE) {
                            moves.add(Move(4, 6, isCastleKingside = true))
                        }
                        if (state.castling.whiteQueenside && state.board[3] == null && state.board[2] == null && state.board[1] == null &&
                            !isSquareAttacked(4, Color.BLACK, state) && !isSquareAttacked(3, Color.BLACK, state) && !isSquareAttacked(2, Color.BLACK, state) &&
                            state.board[0]?.type == PieceType.ROOK && state.board[0]?.color == Color.WHITE) {
                            moves.add(Move(4, 2, isCastleQueenside = true))
                        }
                    }
                    if (side == Color.BLACK && sq == 60) {
                        if (state.castling.blackKingside && state.board[61] == null && state.board[62] == null &&
                            !isSquareAttacked(60, Color.WHITE, state) && !isSquareAttacked(61, Color.WHITE, state) && !isSquareAttacked(62, Color.WHITE, state) &&
                            state.board[63]?.type == PieceType.ROOK && state.board[63]?.color == Color.BLACK) {
                            moves.add(Move(60, 62, isCastleKingside = true))
                        }
                        if (state.castling.blackQueenside && state.board[59] == null && state.board[58] == null && state.board[57] == null &&
                            !isSquareAttacked(60, Color.WHITE, state) && !isSquareAttacked(59, Color.WHITE, state) && !isSquareAttacked(58, Color.WHITE, state) &&
                            state.board[56]?.type == PieceType.ROOK && state.board[56]?.color == Color.BLACK) {
                            moves.add(Move(60, 58, isCastleQueenside = true))
                        }
                    }
                }
                PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                    val dirs = when (p.type) {
                        PieceType.BISHOP -> listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
                        PieceType.ROOK -> listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                        else -> listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
                    }
                    for ((df, dr) in dirs) {
                        var nf = f + df; var nr = r + dr
                        while (nf in 0..7 && nr in 0..7) {
                            val t = nr * 8 + nf
                            val tp = state.board[t]
                            if (tp == null) moves.add(Move(sq, t))
                            else {
                                if (tp.color != side) moves.add(Move(sq, t, isCapture = true))
                                break
                            }
                            nf += df; nr += dr
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addPawnMove(
        list: MutableList<Move>, from: Int, to: Int,
        state: BoardSnapshot, promoRank: Int, isCapture: Boolean = false
    ) {
        if (to / 8 == promoRank) {
            for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                list.add(Move(from, to, promotion = promo, isCapture = isCapture))
            }
        } else {
            list.add(Move(from, to, isCapture = isCapture))
        }
    }

    // ---------- Apply ----------
    fun applyMove(state: BoardSnapshot, move: Move, dryRun: Boolean = false): BoardSnapshot {
        val b = state.board.copyOf()
        val piece = b[move.from] ?: return state
        val captured = b[move.to]
        b[move.from] = null
        b[move.to] = if (move.promotion != null) Piece(move.promotion, piece.color) else piece

        if (move.isEnPassant) {
            val capSq = if (piece.color == Color.WHITE) move.to - 8 else move.to + 8
            if (capSq in 0..63) b[capSq] = null
        }
        if (move.isCastleKingside) {
            if (piece.color == Color.WHITE) { b[5] = b[7]; b[7] = null } else { b[61] = b[63]; b[63] = null }
        }
        if (move.isCastleQueenside) {
            if (piece.color == Color.WHITE) { b[3] = b[0]; b[0] = null } else { b[59] = b[56]; b[56] = null }
        }

        var wc = state.castling.whiteKingside; var wq = state.castling.whiteQueenside
        var bc = state.castling.blackKingside; var bq = state.castling.blackQueenside
        if (piece.type == PieceType.KING) {
            if (piece.color == Color.WHITE) { wc = false; wq = false } else { bc = false; bq = false }
        }
        if (move.from == 7 || move.to == 7) wc = false
        if (move.from == 0 || move.to == 0) wq = false
        if (move.from == 63 || move.to == 63) bc = false
        if (move.from == 56 || move.to == 56) bq = false

        val newEp = if (move.isDoublePawnPush) {
            if (piece.color == Color.WHITE) move.from + 8 else move.from - 8
        } else null

        val half = if (piece.type == PieceType.PAWN || captured != null || move.isEnPassant) 0 else state.halfmoveClock + 1
        val full = if (state.sideToMove == Color.BLACK) state.fullmoveNumber + 1 else state.fullmoveNumber
        return BoardSnapshot(
            board = b,
            sideToMove = state.sideToMove.opposite(),
            castling = CastlingRights(wc, wq, bc, bq),
            enPassantSquare = newEp,
            halfmoveClock = half,
            fullmoveNumber = full
        )
    }

    fun doMove(move: Move): Boolean {
        val next = applyMove(snapshot, move)
        // final safety: don't allow moving into check
        if (isInCheck(snapshot.sideToMove, next)) return false
        snapshot = next
        return true
    }

    fun findMove(from: Int, to: Int, promotion: PieceType? = null): Move? {
        val legals = legalMoves()
        return legals.firstOrNull { it.from == from && it.to == to && (promotion == null || it.promotion == promotion || (it.promotion != null && promotion != null)) }
            ?: legals.firstOrNull { it.from == from && it.to == to }
    }

    fun detectEnd(): GameEnd? {
        val legals = legalMoves()
        val inCheck = isInCheck(snapshot.sideToMove)
        if (legals.isEmpty()) {
            return if (inCheck) {
                GameEnd(snapshot.sideToMove.opposite(), GameEndReason.CHECKMATE,
                    if (snapshot.sideToMove == Color.WHITE) "Jaque mate. Ganan las negras" else "Jaque mate. Ganan las blancas")
            } else {
                GameEnd(null, GameEndReason.STALEMATE, "Rey ahogado. Empate")
            }
        }
        if (snapshot.halfmoveClock >= 100) return GameEnd(null, GameEndReason.DRAW_FIFTY, "Regla de los 50 movimientos. Empate")
        if (isInsufficientMaterial()) return GameEnd(null, GameEndReason.DRAW_INSUFFICIENT, "Material insuficiente. Empate")
        return null
    }

    private fun isInsufficientMaterial(): Boolean {
        val pieces = snapshot.board.filterNotNull()
        if (pieces.size == 2) return pieces.all { it.type == PieceType.KING }
        if (pieces.size == 3) {
            val nonKing = pieces.firstOrNull { it.type != PieceType.KING }
            if (nonKing != null && (nonKing.type == PieceType.BISHOP || nonKing.type == PieceType.KNIGHT)) return true
        }
        return false
    }

    fun toSAN(move: Move): String {
        val piece = snapshot.board[move.from] ?: return ""
        if (move.isCastleKingside) return "O-O"
        if (move.isCastleQueenside) return "O-O-O"
        val capture = if (move.isCapture) "x" else ""
        val dest = Move.squareToAlgebraic(move.to)
        val promo = if (move.promotion != null) "=" + when (move.promotion) {
            PieceType.QUEEN -> "Q"; PieceType.ROOK -> "R"; PieceType.BISHOP -> "B"; PieceType.KNIGHT -> "N"; else -> "Q"
        } else ""
        return when (piece.type) {
            PieceType.PAWN -> if (move.isCapture) "${'a' + (move.from % 8)}$capture$dest$promo" else "$dest$promo"
            PieceType.KING -> "K$capture$dest"
            PieceType.QUEEN -> "Q$capture$dest"
            PieceType.ROOK -> "R$capture$dest"
            PieceType.BISHOP -> "B$capture$dest"
            PieceType.KNIGHT -> "N$capture$dest"
        }
    }

    private fun copy(s: BoardSnapshot): BoardSnapshot =
        BoardSnapshot(s.board.copyOf(), s.sideToMove, s.castling, s.enPassantSquare, s.halfmoveClock, s.fullmoveNumber)
}

package com.chessx.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas básicas compatibles con Lichess/FIDE.
 * Coordenadas: 0 = a1, 63 = h8.
 */
class ChessEngineTest {

    @Test
    fun `starting position has 20 legal moves`() {
        val engine = ChessEngine()
        assertEquals(20, engine.legalMoves().size)
        assertEquals(Color.WHITE, engine.sideToMove())
    }

    @Test
    fun `e2e4 is legal and updates side to move`() {
        val engine = ChessEngine()
        val from = Move.algebraicToSquare("e2")!!
        val to = Move.algebraicToSquare("e4")!!
        assertEquals(MoveValidation.OK, engine.validateMove(from, to))
        val move = engine.findMove(from, to)!!
        assertTrue(engine.doMove(move))
        assertEquals(Color.BLACK, engine.sideToMove())
        assertNull(engine.pieceAt(from))
        assertNotNull(engine.pieceAt(to))
    }

    @Test
    fun `illegal move is rejected`() {
        val engine = ChessEngine()
        val from = Move.algebraicToSquare("e2")!!
        val to = Move.algebraicToSquare("e5")!!
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(from, to))
    }

    @Test
    fun `castling moves the rook`() {
        val engine = ChessEngine()
        // Clear the f1/g1 squares: Nf3, e5, Bc4?? -> use simple legal sequence.
        engine.parseFEN("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val kingFrom = Move.algebraicToSquare("e1")!!
        val kingTo = Move.algebraicToSquare("g1")!!
        val castle = engine.findMove(kingFrom, kingTo)
        assertNotNull(castle)
        assertTrue(castle!!.isCastleKingside)
        assertTrue(engine.doMove(castle))
        assertNotNull(engine.pieceAt(Move.algebraicToSquare("f1")!!))
        assertNotNull(engine.pieceAt(Move.algebraicToSquare("g1")!!))
        assertNull(engine.pieceAt(Move.algebraicToSquare("e1")!!))
        assertNull(engine.pieceAt(Move.algebraicToSquare("h1")!!))
    }

    @Test
    fun `cannot move into check (pinned piece)`() {
        // White king e1, white rook e2, black rook e8: moving the rook off the e-file is illegal.
        val engine = ChessEngine()
        engine.parseFEN("4r3/8/8/8/8/8/4R3/4K3 w - - 0 1")
        val e2 = Move.algebraicToSquare("e2")!!
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(e2, Move.algebraicToSquare("a2")!!))
        assertEquals(MoveValidation.OK, engine.validateMove(e2, Move.algebraicToSquare("e8")!!))
    }

    @Test
    fun `fools mate is detected as checkmate`() {
        val engine = ChessEngine()
        val moves = listOf(
            "f2" to "f3",
            "e7" to "e5",
            "g2" to "g4",
            "d8" to "h4"
        )
        moves.forEach { (a, b) ->
            val m = engine.findMove(
                Move.algebraicToSquare(a)!!,
                Move.algebraicToSquare(b)!!
            )
            assertNotNull("move $a$b should be legal", m)
            assertTrue(engine.doMove(m!!))
        }
        val end = engine.detectEnd()
        assertNotNull(end)
        assertEquals(GameEndReason.CHECKMATE, end!!.reason)
        assertEquals(Color.BLACK, end.winner)
    }

    @Test
    fun `stalemate is detected as a draw`() {
        val engine = ChessEngine()
        engine.parseFEN("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        val end = engine.detectEnd()
        assertNotNull(end)
        assertEquals(GameEndReason.STALEMATE, end!!.reason)
        assertNull(end.winner)
    }

    @Test
    fun `pawn promotion produces four choices`() {
        val engine = ChessEngine()
        engine.parseFEN("8/P7/8/8/8/8/8/K6k w - - 0 1")
        val from = Move.algebraicToSquare("a7")!!
        val to = Move.algebraicToSquare("a8")!!
        val promos = engine.legalMovesFrom(from).filter { it.to == to }
        assertEquals(4, promos.size)
        assertTrue(promos.map { it.promotion }.toSet().containsAll(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
        ))
    }

    @Test
    fun `fen roundtrip keeps the position`() {
        val engine = ChessEngine()
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        assertTrue(engine.parseFEN(fen))
        assertEquals(fen, engine.toFEN())
    }

    @Test
    fun `king cannot capture a defended piece`() {
        val engine = ChessEngine()
        // White king e4 can "see" d5, but black queen on d8 defends d5 (and pawn e5 blocks? use clear line)
        engine.parseFEN("3q4/8/8/3p4/4K3/8/8/7k w - - 0 1")
        val from = Move.algebraicToSquare("e4")!!
        val to = Move.algebraicToSquare("d5")!! // pawn is defended by queen on d8
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(from, to))
        assertFalse(engine.legalMovesFrom(from).any { it.to == to })
    }

    @Test
    fun `king cannot move into check`() {
        val engine = ChessEngine()
        engine.parseFEN("8/8/8/8/8/8/3r4/4K2k w - - 0 1")
        val from = Move.algebraicToSquare("e1")!!
        // Rook on d2 attacks d-file and rank 2; e2/f2/d1 are attacked.
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(from, Move.algebraicToSquare("d1")!!))
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(from, Move.algebraicToSquare("e2")!!))
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(from, Move.algebraicToSquare("f2")!!))
        // Capturing the undefended rook on d2 is legal.
        assertEquals(MoveValidation.OK, engine.validateMove(from, Move.algebraicToSquare("d2")!!))
        assertFalse(engine.legalMovesFrom(from).any { it.to == Move.algebraicToSquare("d1")!! })
    }

    @Test
    fun `double check forces king move`() {
        val engine = ChessEngine()
        // Black king h8 in double check from white queen g7 and bishop b2 (diagonal to h8 via? simpler setup)
        // K on e8, checked by rook e1 and bishop b5 => double check, only king moves
        engine.parseFEN("4k3/8/8/1B6/8/8/8/4R2K b - - 0 1")
        val kingSq = Move.algebraicToSquare("e8")!!
        val kingMoves = engine.legalMovesFrom(kingSq)
        assertTrue(kingMoves.isNotEmpty())
        // No other black piece exists; all legal moves must be from king
        assertTrue(engine.legalMoves().all { it.from == kingSq })
    }

    @Test
    fun `cannot castle through check`() {
        val engine = ChessEngine()
        // Black rook attacks f1, so white cannot castle kingside
        engine.parseFEN("5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        val e1 = Move.algebraicToSquare("e1")!!
        val g1 = Move.algebraicToSquare("g1")!!
        assertEquals(MoveValidation.ILLEGAL_MOVE, engine.validateMove(e1, g1))
        assertNull(engine.findMove(e1, g1))
    }

    @Test
    fun `cannot castle out of check`() {
        val engine = ChessEngine()
        engine.parseFEN("4r3/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        val e1 = Move.algebraicToSquare("e1")!!
        assertTrue(engine.isInCheck(Color.WHITE))
        assertNull(engine.findMove(e1, Move.algebraicToSquare("g1")!!))
        assertNull(engine.findMove(e1, Move.algebraicToSquare("c1")!!))
    }

    @Test
    fun `checkmate has no legal escape`() {
        val engine = ChessEngine()
        // Fool's mate final position: black to move? after Qh4# it's white to move and mated
        engine.parseFEN("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertTrue(engine.legalMoves().isEmpty())
        val end = engine.detectEnd()
        assertNotNull(end)
        assertEquals(GameEndReason.CHECKMATE, end!!.reason)
        assertEquals(Color.BLACK, end.winner)
    }

    @Test
    fun `adjacent kings never legal`() {
        val engine = ChessEngine()
        engine.parseFEN("8/8/8/3k4/8/4K3/8/8 w - - 0 1")
        val from = Move.algebraicToSquare("e3")!!
        assertFalse(engine.legalMovesFrom(from).any { it.to == Move.algebraicToSquare("d4")!! || it.to == Move.algebraicToSquare("e4")!! })
    }

    @Test
    fun `en passant removes the captured pawn`() {
        val engine = ChessEngine()
        engine.parseFEN("8/8/8/3pP3/8/8/8/4K2k w - d6 0 1")
        val move = engine.findMove(Move.algebraicToSquare("e5")!!, Move.algebraicToSquare("d6")!!)
        assertNotNull(move)
        assertTrue(move!!.isEnPassant)
        assertTrue(engine.doMove(move))
        assertNull(engine.pieceAt(Move.algebraicToSquare("d5")!!))
        assertNotNull(engine.pieceAt(Move.algebraicToSquare("d6")!!))
    }
}

package com.chessx.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GameRecord(
    val id: String = UUID.randomUUID().toString(),
    val dateMs: Long = System.currentTimeMillis(),
    val opponent: String,
    val result: String, // "1-0", "0-1", "1/2-1/2"
    val localWon: Boolean?,
    val localColorWhite: Boolean,
    val timeLabel: String,
    val movesSan: List<String>,
    val pgn: String,
    val finalFen: String,
    val playerName: String = "Jugador",
    val uciMoves: List<String> = emptyList(),
    val moveClocks: List<Long> = emptyList() // remaining ms of the mover after each move
) {
    fun dateLabel(): String = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(dateMs))
}

/** Resumen de una partida del rival (sólo para mostrar en su perfil). */
data class RemoteGame(
    val dateMs: Long = 0L,
    val result: String = "1/2-1/2",
    val won: Boolean? = null,
    val moves: Int = 0,
    val timeLabel: String = "",
    val opponent: String = "",
    val playerName: String = ""
) {
    fun dateLabel(): String = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(dateMs))
}

data class RemoteProfile(
    val name: String = "Rival",
    val color: String = "",
    val photoBase64: String? = null,
    val total: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val games: List<RemoteGame> = emptyList()
)

fun buildPgn(record: GameRecord, white: String, black: String): String {
    val sb = StringBuilder()
    sb.appendLine("[Event \"CHESS-X\"]")
    sb.appendLine("[Date \"${SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(record.dateMs))}\"]")
    sb.appendLine("[White \"$white\"]")
    sb.appendLine("[Black \"$black\"]")
    sb.appendLine("[Result \"${record.result}\"]")
    sb.appendLine("[TimeControl \"${record.timeLabel}\"]")
    sb.appendLine()
    record.movesSan.forEachIndexed { i, san ->
        if (i % 2 == 0) sb.append("${i / 2 + 1}. ")
        sb.append("$san ")
        val clock = record.moveClocks.getOrNull(i)
        if (clock != null) sb.append("{ %clk ${formatClock(clock)} } ")
    }
    sb.append(record.result)
    return sb.toString().trim()
}

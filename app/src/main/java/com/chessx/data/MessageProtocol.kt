package com.chessx.data

import com.chessx.core.Color
import org.json.JSONObject

/** Versioned, newline-framed JSON protocol. Every message is one line, crash-safe parsing. */
sealed interface BtMessage {
    val type: String
    fun toJsonLine(): String
}

object ProtocolVersion { const val V = 2 }

data class HelloMessage(
    val playerName: String,
    val isHost: Boolean,
    val ownerColor: String // "white" | "black"
) : BtMessage {
    override val type = "HELLO"
    override fun toJsonLine(): String = JSONObject()
        .put("v", ProtocolVersion.V).put("type", type)
        .put("name", playerName).put("isHost", isHost).put("color", ownerColor).toString()
}

data class LobbyConfigMessage(
    val hostName: String,
    val guestName: String,
    val timeMs: Long, // 0 = unlimited
    val incrementMs: Long,
    val hostColor: String
) : BtMessage {
    override val type = "LOBBY"
    override fun toJsonLine(): String = JSONObject()
        .put("v", ProtocolVersion.V).put("type", type)
        .put("hostName", hostName).put("guestName", guestName)
        .put("timeMs", timeMs).put("incrementMs", incrementMs)
        .put("hostColor", hostColor).toString()
}

data class StartGameMessage(
    val startFen: String,
    val timeMs: Long,
    val incrementMs: Long,
    val hostColor: String,
    val hostName: String,
    val guestName: String,
    val hostTimestamp: Long
) : BtMessage {
    override val type = "START"
    override fun toJsonLine(): String = JSONObject()
        .put("v", ProtocolVersion.V).put("type", type)
        .put("fen", startFen).put("timeMs", timeMs).put("incrementMs", incrementMs)
        .put("hostColor", hostColor).put("hostName", hostName).put("guestName", guestName)
        .put("ts", hostTimestamp).toString()
}

data class MoveMessage(
    val from: Int,
    val to: Int,
    val promotion: String, // "", "Q","R","B","N"
    val san: String,
    val uci: String,
    val fenAfter: String,
    val timestamp: Long
) : BtMessage {
    override val type = "MOVE"
    override fun toJsonLine(): String = JSONObject()
        .put("v", ProtocolVersion.V).put("type", type)
        .put("from", from).put("to", to).put("promotion", promotion)
        .put("san", san).put("uci", uci).put("fen", fenAfter)
        .put("ts", timestamp).toString()
}

data class PauseMessage(val paused: Boolean, val byName: String) : BtMessage {
    override val type = "PAUSE"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("paused", paused).put("by", byName).toString()
}

data class DrawOfferMessage(val byName: String) : BtMessage {
    override val type = "DRAW_OFFER"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("by", byName).toString()
}

data class DrawResponseMessage(val accepted: Boolean, val byName: String) : BtMessage {
    override val type = "DRAW_ACCEPT"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("accepted", accepted).put("by", byName).toString()
}

data class ResignMessage(val byName: String, val loserColor: String) : BtMessage {
    override val type = "RESIGN"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("by", byName).put("loserColor", loserColor).toString()
}

data class ClockSyncMessage(
    val whiteMs: Long, val blackMs: Long,
    val sideToMove: String, val serverTs: Long, val paused: Boolean
) : BtMessage {
    override val type = "CLOCK"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type)
        .put("whiteMs", whiteMs).put("blackMs", blackMs)
        .put("side", sideToMove).put("ts", serverTs).put("paused", paused).toString()
}

data class GameEndMessage(
    val winnerColor: String, // "white"|"black"|"draw"
    val reason: String,
    val message: String
) : BtMessage {
    override val type = "END"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("winner", winnerColor)
        .put("reason", reason).put("message", message).toString()
}

/** Petición de nueva partida al terminar: "same" | "random" | "lobby". */
data class RematchMessage(val mode: String, val byName: String) : BtMessage {
    override val type = "REMATCH"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("mode", mode).put("by", byName).toString()
}

/** El anfitrión arranca un torneo de N partidas con colores alternos. */
data class TournamentStartMessage(
    val games: Int,
    val timeMs: Long,
    val incrementMs: Long,
    val hostColorWhite: Boolean
) : BtMessage {
    override val type = "TOURNAMENT"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type)
        .put("games", games).put("timeMs", timeMs).put("incrementMs", incrementMs)
        .put("hostColorWhite", hostColorWhite).toString()
}

data class ProfileMessage(
    val name: String,
    val color: String,          // "white" | "black" | "" (host asigna)
    val photoBase64: String,    // "" si el usuario no comparte foto
    val total: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    /** Historial resumido (PGN en JSON): sólo si el usuario comparte historial/PGN */
    val gamesJson: String
) : BtMessage {
    override val type = "PROFILE"
    override fun toJsonLine(): String = JSONObject()
        .put("v", ProtocolVersion.V).put("type", type)
        .put("name", name).put("color", color).put("photo", photoBase64)
        .put("total", total).put("wins", wins).put("draws", draws).put("losses", losses)
        .put("games", gamesJson).toString()
}

data class PingMessage(val ts: Long) : BtMessage {
    override val type = "PING"
    override fun toJsonLine(): String = JSONObject().put("v", 2).put("type", type).put("ts", ts).toString()
}

data class PongMessage(val ts: Long) : BtMessage {
    override val type = "PONG"
    override fun toJsonLine(): String = JSONObject().put("v", 2).put("type", type).put("ts", ts).toString()
}

data class ErrorMessage(val code: String, val text: String) : BtMessage {
    override val type = "ERROR"
    override fun toJsonLine(): String = JSONObject()
        .put("v", 2).put("type", type).put("code", code).put("text", text).toString()
}

object MessageParser {
    fun parse(line: String): BtMessage? {
        return try {
            if (line.isBlank()) return null
            if (line.length > 65536) return null // anti-abuse (perfil con foto)
            val o = JSONObject(line)
            when (o.optString("type")) {
                "HELLO" -> HelloMessage(
                    o.optString("name").ifBlank { "Jugador" }.take(32),
                    o.optBoolean("isHost"),
                    o.optString("color").ifBlank { "white" }
                )
                "LOBBY" -> LobbyConfigMessage(
                    o.optString("hostName").take(32), o.optString("guestName").take(32),
                    o.optLong("timeMs").coerceAtLeast(0), o.optLong("incrementMs").coerceAtLeast(0),
                    o.optString("hostColor")
                )
                "START" -> StartGameMessage(
                    o.optString("fen"), o.optLong("timeMs"), o.optLong("incrementMs"),
                    o.optString("hostColor"), o.optString("hostName").take(32),
                    o.optString("guestName").take(32), o.optLong("ts")
                )
                "MOVE" -> MoveMessage(
                    o.optInt("from").coerceIn(-1, 63), o.optInt("to").coerceIn(-1, 63),
                    o.optString("promotion").take(1), o.optString("san").take(16),
                    o.optString("uci").take(8), o.optString("fen"), o.optLong("ts")
                )
                "PAUSE" -> PauseMessage(o.optBoolean("paused"), o.optString("by").take(32))
                "DRAW_OFFER" -> DrawOfferMessage(o.optString("by").take(32))
                "DRAW_ACCEPT" -> DrawResponseMessage(o.optBoolean("accepted"), o.optString("by").take(32))
                "RESIGN" -> ResignMessage(o.optString("by").take(32), o.optString("loserColor"))
                "CLOCK" -> ClockSyncMessage(
                    o.optLong("whiteMs").coerceAtLeast(0), o.optLong("blackMs").coerceAtLeast(0),
                    o.optString("side"), o.optLong("ts"), o.optBoolean("paused")
                )
                "END" -> GameEndMessage(o.optString("winner"), o.optString("reason"), o.optString("message").take(256))
                "PROFILE" -> ProfileMessage(
                    o.optString("name").take(32).ifBlank { "Jugador" },
                    o.optString("color").ifBlank { "" },
                    o.optString("photo").take(32768),
                    o.optInt("total").coerceAtLeast(0),
                    o.optInt("wins").coerceAtLeast(0),
                    o.optInt("draws").coerceAtLeast(0),
                    o.optInt("losses").coerceAtLeast(0),
                    o.optString("games").take(100000)
                )
                "PING" -> PingMessage(o.optLong("ts"))
                "PONG" -> PongMessage(o.optLong("ts"))
                "REMATCH" -> RematchMessage(o.optString("mode").take(8), o.optString("by").take(32))
                "TOURNAMENT" -> TournamentStartMessage(
                    o.optInt("games").coerceIn(1, 20),
                    o.optLong("timeMs").coerceAtLeast(0),
                    o.optLong("incrementMs").coerceAtLeast(0),
                    o.optBoolean("hostColorWhite", true)
                )
                "ERROR" -> ErrorMessage(o.optString("code"), o.optString("text").take(256))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun colorFromString(s: String): Color = if (s.equals("black", true)) Color.BLACK else Color.WHITE
    fun stringFromColor(c: Color): String = if (c == Color.BLACK) "black" else "white"
}

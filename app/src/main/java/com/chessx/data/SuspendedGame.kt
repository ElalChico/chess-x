package com.chessx.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Partida interrumpida por perdida de la conexion Bluetooth.
 *
 * Guarda TODO lo necesario para retomarla mas tarde sin perder el estado:
 * la posicion exacta (FEN, que ya incluye de quien es el turno), los movimientos
 * con sus relojes, el control de tiempo y los colores.
 *
 * Es un "historial especial": no cuenta como partida terminada ni suma a las
 * estadisticas, pero se puede reanudar cuando el rival vuelva a conectarse.
 */
data class SuspendedGame(
    val id: String = UUID.randomUUID().toString(),
    val dateMs: Long = System.currentTimeMillis(),
    val opponent: String = "Rival",
    val opponentPhotoBase64: String? = null,
    /** Posicion actual. El campo side-to-move del FEN define de quien es el turno. */
    val fen: String = "",
    val uciMoves: List<String> = emptyList(),
    val movesSan: List<String> = emptyList(),
    /** Milisegundos que le quedaban al que movia tras cada jugada. */
    val moveClocks: List<Long> = emptyList(),
    val whiteMs: Long = 0L,
    val blackMs: Long = 0L,
    val timeMs: Long = 0L,
    val incrementMs: Long = 0L,
    val localColorWhite: Boolean = true,
    /** Ultima jugada jugada, para resaltarla al reanudar. -1 si no hay ninguna. */
    val lastFrom: Int = -1,
    val lastTo: Int = -1
) {
    val movesCount: Int get() = uciMoves.size

    val sideToMoveWhite: Boolean
        get() {
            val parts = fen.trim().split(" ".toRegex())
            if (parts.size < 2) return true
            return parts[1].equals("w", ignoreCase = true)
        }

    fun dateLabel(): String =
        SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(dateMs))

    fun timeLabel(): String =
        if (timeMs == 0L) "Sin límite" else "${timeMs / 60_000} + ${incrementMs / 1_000}"
}

/**
 * Almacena la partida suspendida. Se guardan varias (hasta 5) para poder
 * reanudar cualquiera de ellas; la mas reciente se muestra primero.
 */
object SuspendedGameStore {

    private const val PREFS = "chessbt_suspended"
    private const val KEY = "games"
    private const val MAX = 5

    fun load(context: Context): List<SuspendedGame> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<SuspendedGame>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    SuspendedGame(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        dateMs = o.optLong("d", System.currentTimeMillis()),
                        opponent = o.optString("opp", "Rival"),
                        opponentPhotoBase64 = o.optString("oppPhoto", "").takeIf { it.isNotBlank() },
                        fen = o.optString("fen", ""),
                        uciMoves = strArray(o.optJSONArray("uci")),
                        movesSan = strArray(o.optJSONArray("san")),
                        moveClocks = longArray(o.optJSONArray("clocks")),
                        whiteMs = o.optLong("wms", 0L),
                        blackMs = o.optLong("bms", 0L),
                        timeMs = o.optLong("tc", 0L),
                        incrementMs = o.optLong("inc", 0L),
                        localColorWhite = o.optBoolean("lw", true),
                        lastFrom = o.optInt("lf", -1),
                        lastTo = o.optInt("lt", -1)
                    )
                )
            }
        } catch (_: Exception) { }
        return out
    }

    /** Guarda o actualiza (misma id) la partida suspendida. */
    fun save(context: Context, game: SuspendedGame) {
        try {
            val all = load(context).toMutableList()
            all.removeAll { it.id == game.id }
            all.add(0, game)
            writeAll(context, all)
        } catch (_: Exception) { }
    }

    fun remove(context: Context, id: String) {
        try {
            writeAll(context, load(context).filterNot { it.id == id })
        } catch (_: Exception) { }
    }

    fun clear(context: Context) {
        try { writeAll(context, emptyList()) } catch (_: Exception) { }
    }

    private fun strArray(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until a.length()) out.add(a.optString(i, ""))
        return out
    }

    private fun longArray(a: JSONArray?): List<Long> {
        if (a == null) return emptyList()
        val out = mutableListOf<Long>()
        for (i in 0 until a.length()) out.add(a.optLong(i, 0L))
        return out
    }

    private fun writeAll(context: Context, all: List<SuspendedGame>) {
        val arr = JSONArray()
        all.take(MAX).forEach { g ->
            val o = JSONObject()
            o.put("id", g.id)
            o.put("d", g.dateMs)
            o.put("opp", g.opponent)
            if (g.opponentPhotoBase64 != null) o.put("oppPhoto", g.opponentPhotoBase64)
            o.put("fen", g.fen)
            o.put("uci", JSONArray(g.uciMoves))
            o.put("san", JSONArray(g.movesSan))
            o.put("clocks", JSONArray(g.moveClocks))
            o.put("wms", g.whiteMs)
            o.put("bms", g.blackMs)
            o.put("tc", g.timeMs)
            o.put("inc", g.incrementMs)
            o.put("lw", g.localColorWhite)
            o.put("lf", g.lastFrom)
            o.put("lt", g.lastTo)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

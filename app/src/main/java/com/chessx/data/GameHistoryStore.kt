package com.chessx.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local match history (PGN). Captures opponent name, colors, time control,
 * per-move remaining clocks and UCI moves so games can be replayed.
 */
object GameHistoryStore {

    private const val PREFS = "chessbt_history"
    private const val KEY = "records"
    private const val MAX = 50

    fun load(context: Context): List<GameRecord> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<GameRecord>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val times = mutableListOf<Long>()
                val ta = o.optJSONArray("clocks")
                if (ta != null) for (j in 0 until ta.length()) times.add(ta.optLong(j, 0))
                val ucis = mutableListOf<String>()
                val ua = o.optJSONArray("uci")
                if (ua != null) for (j in 0 until ua.length()) ucis.add(ua.optString(j, ""))
                out.add(
                    GameRecord(
                        id = o.optString("id", "${o.optLong("d", 0)}"),
                        dateMs = o.optLong("d", System.currentTimeMillis()),
                        opponent = o.optString("opp", "Rival"),
                        result = o.optString("res", "1/2-1/2"),
                        localWon = if (o.has("won")) o.optBoolean("won") else null,
                        localColorWhite = o.optBoolean("lw", true),
                        timeLabel = o.optString("tc", "5 + 0"),
                        movesSan = ucisSan(o),
                        pgn = o.optString("pgn", ""),
                        finalFen = o.optString("fen", ""),
                        playerName = o.optString("me", "Jugador"),
                        uciMoves = ucis,
                        moveClocks = times
                    )
                )
            }
        } catch (_: Exception) { }
        return out
    }

    private fun ucisSan(o: JSONObject): List<String> {
        val san = mutableListOf<String>()
        val sa = o.optJSONArray("san")
        if (sa != null) for (j in 0 until sa.length()) san.add(sa.optString(j, ""))
        return san
    }

    fun remove(context: Context, id: String) {
        try {
            val all = load(context).filterNot { it.id == id }.toMutableList()
            writeAll(context, all)
        } catch (_: Exception) { }
    }

    fun clear(context: Context) {
        try { writeAll(context, emptyList()) } catch (_: Exception) { }
    }

    private fun writeAll(context: Context, all: List<GameRecord>) {
        val arr = JSONArray()
        all.take(MAX).forEach { r ->
            val o = JSONObject()
            o.put("id", r.id)
            o.put("d", r.dateMs)
            o.put("opp", r.opponent)
            o.put("me", r.playerName)
            o.put("res", r.result)
            if (r.localWon != null) o.put("won", r.localWon)
            o.put("lw", r.localColorWhite)
            o.put("tc", r.timeLabel)
            o.put("pgn", r.pgn)
            o.put("fen", r.finalFen)
            o.put("san", JSONArray(r.movesSan))
            o.put("uci", JSONArray(r.uciMoves))
            o.put("clocks", JSONArray(r.moveClocks))
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun append(context: Context, record: GameRecord) {
        try {
            val all = load(context).toMutableList()
            all.add(0, record)
            writeAll(context, all)
        } catch (_: Exception) { }
    }
}

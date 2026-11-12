/*
 * CHESS-X · © Elal Chico
 * Modo torneo: N partidas seguidas con colores alternos y marcador acumulado.
 */
package com.chessx.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Configuración de un torneo entre dos móviles. */
data class TournamentConfig(
    val games: Int = 4,
    val timeMs: Long = 300_000,
    val incrementMs: Long = 0
)

/** Estado vivo del torneo durante la sesión. */
data class TournamentState(
    val config: TournamentConfig = TournamentConfig(),
    val played: Int = 0,
    val localWins: Int = 0,
    val remoteWins: Int = 0,
    val draws: Int = 0,
    val localColorWhite: Boolean = true,
    val active: Boolean = false,
    /** Resultado de cada partida jugada: "V · Mate", "D · Tiempo", "E · Tablas" … */
    val gameResults: List<String> = emptyList()
) {
    val totalGames: Int get() = config.games
    val finished: Boolean get() = played >= totalGames
    val localScore: Double get() = localWins + draws * 0.5
    val remoteScore: Double get() = remoteWins + draws * 0.5
}

/** Registro persistente de un torneo completo. */
data class TournamentRecord(
    val id: String,
    val dateMs: Long,
    val opponent: String,
    val games: Int,
    val localWins: Int,
    val remoteWins: Int,
    val draws: Int,
    val timeLabel: String,
    val gameResults: List<String>
) {
    val localScore: Double get() = localWins + draws * 0.5
    val remoteScore: Double get() = remoteWins + draws * 0.5
    val resultLabel: String
        get() {
            val l = localScore; val r = remoteScore
            return when {
                l > r -> "Ganaste $l-$r"
                r > l -> "Perdiste $l-$r"
                else -> "Empate $l-$r"
            }
        }

    fun dateLabel(): String =
        SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(dateMs))
}

object TournamentStore {
    private const val PREFS = "chessx_tournaments"
    private const val KEY = "records"
    private const val MAX = 20

    fun load(context: Context): List<TournamentRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<TournamentRecord>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val results = mutableListOf<String>()
                val ra = o.optJSONArray("results")
                if (ra != null) for (j in 0 until ra.length()) results.add(ra.optString(j, ""))
                out.add(
                    TournamentRecord(
                        id = o.optString("id", "$i"),
                        dateMs = o.optLong("d", 0L),
                        opponent = o.optString("opp", "Rival"),
                        games = o.optInt("g", 0),
                        localWins = o.optInt("lw", 0),
                        remoteWins = o.optInt("rw", 0),
                        draws = o.optInt("dr", 0),
                        timeLabel = o.optString("tc", ""),
                        gameResults = results
                    )
                )
            }
        } catch (_: Exception) { }
        return out
    }

    fun append(context: Context, record: TournamentRecord) {
        try {
            val all = load(context).toMutableList()
            all.add(0, record)
            writeAll(context, all.take(MAX))
        } catch (_: Exception) { }
    }

    fun remove(context: Context, id: String) {
        try { writeAll(context, load(context).filterNot { it.id == id }) } catch (_: Exception) { }
    }

    fun clear(context: Context) {
        try { writeAll(context, emptyList()) } catch (_: Exception) { }
    }

    private fun writeAll(context: Context, all: List<TournamentRecord>) {
        val arr = JSONArray()
        all.forEach { r ->
            val o = JSONObject()
            o.put("id", r.id)
            o.put("d", r.dateMs)
            o.put("opp", r.opponent)
            o.put("g", r.games)
            o.put("lw", r.localWins)
            o.put("rw", r.remoteWins)
            o.put("dr", r.draws)
            o.put("tc", r.timeLabel)
            o.put("results", JSONArray(r.gameResults))
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

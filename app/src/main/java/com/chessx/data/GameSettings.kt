package com.chessx.data

import com.chessx.core.Color

data class TimeControlOption(val label: String, val timeMs: Long, val incrementMs: Long)

object PredefinedControls {
    val options = listOf(
        TimeControlOption("Ilimitado", 0, 0),
        TimeControlOption("1 + 0", 60_000, 0),
        TimeControlOption("3 + 0", 180_000, 0),
        TimeControlOption("3 + 2", 180_000, 2_000),
        TimeControlOption("5 + 0", 300_000, 0),
        TimeControlOption("5 + 3", 300_000, 3_000),
        TimeControlOption("10 + 0", 600_000, 0),
        TimeControlOption("10 + 5", 600_000, 5_000),
        TimeControlOption("15 + 10", 900_000, 10_000),
        TimeControlOption("30 + 0", 1_800_000, 0),
        TimeControlOption("60 + 0", 3_600_000, 0),
        TimeControlOption("90 + 30", 5_400_000, 30_000)
    )
}

data class LobbyState(
    val localName: String = "Jugador",
    val remoteName: String = "",
    val localColor: Color = Color.WHITE,
    val remoteColor: Color = Color.BLACK,
    val timeMs: Long = 300_000,
    val incrementMs: Long = 0,
    val isHost: Boolean = false,
    val connected: Boolean = false,
    val gameStarted: Boolean = false,
    val colorRandom: Boolean = true
)

data class PlayerUiState(
    val name: String,
    val color: Color,
    val isLocal: Boolean,
    val isThinking: Boolean,
    val captured: List<String>,
    val clockMs: Long
)

fun formatClock(ms: Long): String {
    if (ms <= 0) return "0:00"
    if (ms > 60 * 60 * 1000) {
        val h = ms / 3600000; val m = (ms % 3600000) / 60000
        return "${h}:${m.toString().padStart(2, '0')}"
    }
    val s = ms / 1000
    val m = s / 60; val sec = s % 60
    return if (ms < 10_000) "${m}:${sec.toString().padStart(2, '0')}.${(ms % 1000) / 100}"
    else "${m}:${sec.toString().padStart(2, '0')}"
}

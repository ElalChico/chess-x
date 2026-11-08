package com.chessx.data

import com.chessx.core.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClockState(
    val whiteMs: Long,
    val blackMs: Long,
    val sideToMove: Color,
    val paused: Boolean,
    val finished: Boolean
)

/**
 * Host-authoritative synchronized clock.
 * - Host ticks every 100ms, sends CLOCK sync every 1000ms.
 * - Guest interpolates locally between syncs (offset = serverTs - local) so both show same.
 * - Pause freezes both. Increment applied on move.
 * - No drift: both compute remaining from (lastSyncWallClock).
 */
class ClockManager(
    private val scope: CoroutineScope,
    isHost: Boolean,
    private val onSyncRequest: (ClockSyncMessage) -> Unit,
    private val onTimeout: (Color) -> Unit,
    private val onLowTime: (Color) -> Unit = {}
) {
    /** Host is the clock authority; can be switched without recreating the manager. */
    var isHost: Boolean = isHost
        set(value) {
            field = value
            lastTickWall = System.currentTimeMillis()
            lastSyncToHost = lastTickWall
        }

    private var _state = MutableStateFlow(ClockState(0, 0, Color.WHITE, false, false))
    val state: StateFlow<ClockState> = _state

    private var initialMs: Long = 0
    private var incrementMs: Long = 0
    private var lastTickWall: Long = 0L
    private var lastSyncToHost: Long = 0L
    private var tickJob: Job? = null
    private var syncJob: Job? = null
    private var running = false

    fun configure(timeMs: Long, incrementMs: Long) {
        initialMs = timeMs
        this.incrementMs = incrementMs
        _state.value = ClockState(timeMs, timeMs, Color.WHITE, false, false)
    }

    fun start() {
        if (running) return
        running = true
        lastTickWall = System.currentTimeMillis()
        lastSyncToHost = lastTickWall
        tickJob?.cancel()
        tickJob = scope.launch(Dispatchers.Default) {
            while (isActive && running) {
                delay(100)
                if (_state.value.finished || _state.value.paused) { lastTickWall = System.currentTimeMillis(); continue }
                val now = System.currentTimeMillis()
                val delta = (now - lastTickWall).coerceIn(0, 1000)
                lastTickWall = now
                val cur = _state.value
                val sideBefore = when (cur.sideToMove) { Color.WHITE -> cur.whiteMs; Color.BLACK -> cur.blackMs }
                val newState = when (cur.sideToMove) {
                    Color.WHITE -> cur.copy(whiteMs = (cur.whiteMs - delta).coerceAtLeast(0))
                    Color.BLACK -> cur.copy(blackMs = (cur.blackMs - delta).coerceAtLeast(0))
                }
                _state.value = newState
                // Alerta de tiempo bajo: al cruzar un segundo entero por debajo de 10 s
                val sideAfter = when (cur.sideToMove) { Color.WHITE -> newState.whiteMs; Color.BLACK -> newState.blackMs }
                if (initialMs > 0 && sideAfter in 1L..10_000L && sideBefore / 1000 != sideAfter / 1000) {
                    try { onLowTime(cur.sideToMove) } catch (_: Exception) {}
                }
                if (newState.whiteMs == 0L && newState.sideToMove == Color.WHITE && initialMs > 0) {
                    _state.value = newState.copy(finished = true)
                    onTimeout(Color.WHITE)
                } else if (newState.blackMs == 0L && newState.sideToMove == Color.BLACK && initialMs > 0) {
                    _state.value = newState.copy(finished = true)
                    onTimeout(Color.BLACK)
                }
                if (isHost && running && now - lastSyncToHost >= 1000) {
                    lastSyncToHost = now
                    onSyncRequest(ClockSyncMessage(
                        newState.whiteMs, newState.blackMs,
                        if (newState.sideToMove == Color.WHITE) "white" else "black",
                        now, newState.paused
                    ))
                }
            }
        }
    }

    fun stop() {
        running = false
        tickJob?.cancel()
    }

    fun setPaused(paused: Boolean) {
        _state.value = _state.value.copy(paused = paused)
        lastTickWall = System.currentTimeMillis()
        if (isHost) {
            val c = _state.value
            onSyncRequest(ClockSyncMessage(c.whiteMs, c.blackMs, if (c.sideToMove == Color.WHITE) "white" else "black", System.currentTimeMillis(), paused))
        }
    }

    fun onMovePlayed(mover: Color) {
        if (initialMs == 0L) return // unlimited
        val cur = _state.value
        val next = when (mover) {
            Color.WHITE -> cur.copy(whiteMs = cur.whiteMs + incrementMs, sideToMove = Color.BLACK)
            Color.BLACK -> cur.copy(blackMs = cur.blackMs + incrementMs, sideToMove = Color.WHITE)
        }
        _state.value = next
        lastTickWall = System.currentTimeMillis()
        if (isHost) {
            val c = _state.value
            onSyncRequest(ClockSyncMessage(c.whiteMs, c.blackMs, if (c.sideToMove == Color.WHITE) "white" else "black", System.currentTimeMillis(), c.paused))
        }
    }

    fun applyRemoteSync(msg: ClockSyncMessage) {
        if (isHost) return // host is source of truth
        val side = if (msg.sideToMove == "black") Color.BLACK else Color.WHITE
        _state.value = ClockState(msg.whiteMs, msg.blackMs, side, msg.paused, false)
        lastTickWall = System.currentTimeMillis()
    }

    fun finish() {
        _state.value = _state.value.copy(finished = true)
        stop()
    }
}

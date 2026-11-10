package com.chessx.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.chessx.R

enum class SoundType {
    MOVE, CAPTURE, CHECK, CHECKMATE, ILLEGAL, CLICK, WIN, LOSE, DRAW, PAUSE, LOWTIME, COUNTDOWN3, COUNTDOWN2, COUNTDOWN1
}

/**
 * Motor de sonidos real basado en los MP3 oficiales de Lichess.
 * Set "standard" (movimiento, captura, selección, error, confirmación, cuenta
 * atrás, tiempo bajo) completado con los avisos de fin de partida del set
 * "sfx" — sin sonidos de naturaleza ni de animales.
 * Cargados en un SoundPool: latencia baja, seguro en cualquier API 26+.
 */
class SoundManager(private val context: Context, pack: SoundPack = SoundPack.DEFAULT) {
    private var enabled: Boolean = true
    private var volumePercent: Int = 80
    private var soundPack: SoundPack = pack

    private val pool: SoundPool? = try {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
    } catch (e: Exception) {
        Log.w("Sound", "SoundPool unavailable", e)
        null
    }

    private var moveId = 0
    private var captureId = 0
    private var checkId = 0
    private var checkmateId = 0
    private var illegalId = 0
    private var selectId = 0
    private var winId = 0
    private var loseId = 0
    private var drawId = 0
    private var pauseId = 0
    private var confirmId = 0
    private var lowTimeId = 0
    private var countdown3Id = 0
    private var countdown2Id = 0
    private var countdown1Id = 0

    init {
        pool?.load(context, R.raw.move, 1)?.let { moveId = it }
        pool?.load(context, R.raw.capture, 1)?.let { captureId = it }
        pool?.load(context, R.raw.check, 1)?.let { checkId = it }
        pool?.load(context, R.raw.checkmate, 1)?.let { checkmateId = it }
        pool?.load(context, R.raw.error, 1)?.let { illegalId = it }
        pool?.load(context, R.raw.select, 1)?.let { selectId = it }
        pool?.load(context, R.raw.victory, 1)?.let { winId = it }
        pool?.load(context, R.raw.defeat, 1)?.let { loseId = it }
        pool?.load(context, R.raw.draw, 1)?.let { drawId = it }
        pool?.load(context, R.raw.confirmation, 1)?.let { confirmId = it }
        pool?.load(context, R.raw.lowtime, 1)?.let { lowTimeId = it }
        pool?.load(context, R.raw.countdown3, 1)?.let { countdown3Id = it }
        pool?.load(context, R.raw.countdown2, 1)?.let { countdown2Id = it }
        pool?.load(context, R.raw.countdown1, 1)?.let { countdown1Id = it }
        pauseId = moveId // pausa: sonido suave, no alarma
    }

    fun setEnabled(enabled: Boolean) { this.enabled = enabled }
    fun isEnabled(): Boolean = enabled
    fun setVolume(percent: Int) { volumePercent = percent.coerceIn(0, 100) }
    fun getVolume(): Int = volumePercent
    fun setPack(pack: SoundPack) { soundPack = pack }

    private fun vol(): Float = (volumePercent / 100f).coerceIn(0f, 1f)
    private fun pitch(): Float = when (soundPack) {
        SoundPack.LICHESS -> 1f
        SoundPack.FREE -> 1.08f
        SoundPack.CLASSIC -> 0.94f
    }

    fun play(type: SoundType) {
        if (!enabled) return
        val id = when (type) {
            SoundType.MOVE -> moveId
            SoundType.CAPTURE -> captureId
            SoundType.CHECK -> checkId
            SoundType.CHECKMATE -> checkmateId
            SoundType.ILLEGAL -> illegalId
            SoundType.CLICK -> selectId
            SoundType.WIN -> winId
            SoundType.LOSE -> loseId
            SoundType.DRAW -> drawId
            SoundType.PAUSE -> pauseId
            SoundType.LOWTIME -> lowTimeId
            SoundType.COUNTDOWN3 -> countdown3Id
            SoundType.COUNTDOWN2 -> countdown2Id
            SoundType.COUNTDOWN1 -> countdown1Id
        }
        if (id <= 0) return
        try {
            pool?.play(id, vol(), vol(), 1, 0, pitch())
        } catch (e: Exception) {
            Log.w("Sound", "play failed", e)
        }
    }

    /** Sonido de confirmación (aceptar conexiones / iniciar partida). */
    fun playConfirmation() {
        if (!enabled) return
        if (confirmId > 0) try { pool?.play(confirmId, vol(), vol(), 1, 0, pitch()) } catch (_: Exception) {}
    }

    fun release() {
        try { pool?.release() } catch (_: Exception) {}
    }
}

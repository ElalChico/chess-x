package com.chessx.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class BoardTheme(val labelEs: String, val labelEn: String) {
    CLASSIC("Clásico", "Classic"),
    DARK("Oscuro", "Dark")
}

enum class AppLanguage(val code: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English")
}

data class PrivacySettings(
    val sharePhoto: Boolean = true,
    val shareHistory: Boolean = true,
    val sharePgn: Boolean = true
)

/**
 * Lightweight persisted preferences (SharedPreferences) — no extra dependency.
 */
class AppPreferences(context: Context) {
    private val p = context.getSharedPreferences("chessbt_prefs", Context.MODE_PRIVATE)

    private var _soundEnabled by mutableStateOf(p.getBoolean("sound", true))
    val soundEnabled: Boolean get() = _soundEnabled

    private var _soundVolume by mutableIntStateOf(p.getInt("volume", 80))
    val soundVolume: Int get() = _soundVolume

    private var _boardTheme by mutableStateOf(
        runCatching { BoardTheme.valueOf(p.getString("theme", BoardTheme.CLASSIC.name)!!) }.getOrDefault(BoardTheme.CLASSIC)
    )
    val boardTheme: BoardTheme get() = _boardTheme

    private var _language by mutableStateOf(
        runCatching { AppLanguage.valueOf(p.getString("lang", AppLanguage.ES.name)!!) }.getOrDefault(AppLanguage.ES)
    )
    val language: AppLanguage get() = _language

    private var _privacy by mutableStateOf(
        PrivacySettings(
            sharePhoto = p.getBoolean("share_photo", true),
            shareHistory = p.getBoolean("share_history", true),
            sharePgn = p.getBoolean("share_pgn", true)
        )
    )
    val privacy: PrivacySettings get() = _privacy

    private var _profileName by mutableStateOf(p.getString("profile_name", "Jugador") ?: "Jugador")
    val profileName: String get() = _profileName

    private var _profilePhotoBase64 by mutableStateOf(p.getString("profile_photo", "") ?: "")
    val profilePhotoBase64: String get() = _profilePhotoBase64

    private var _defaultTimeMs by mutableLongStateOf(p.getLong("time_ms", 300_000L))
    val defaultTimeMs: Long get() = _defaultTimeMs

    private var _defaultIncrementMs by mutableLongStateOf(p.getLong("inc_ms", 0L))
    val defaultIncrementMs: Long get() = _defaultIncrementMs

    private var _checkSignal by mutableStateOf(p.getBoolean("check_sig", true))
    val checkSignal: Boolean get() = _checkSignal

    private var _moveIndicators by mutableStateOf(p.getBoolean("move_ind", true))
    val moveIndicators: Boolean get() = _moveIndicators

    fun setSoundEnabled(v: Boolean) { _soundEnabled = v; p.edit().putBoolean("sound", v).apply() }
    fun setSoundVolume(v: Int) { val c = v.coerceIn(0, 100); _soundVolume = c; p.edit().putInt("volume", c).apply() }
    fun setBoardTheme(v: BoardTheme) { _boardTheme = v; p.edit().putString("theme", v.name).apply() }
    fun setLanguage(v: AppLanguage) { _language = v; p.edit().putString("lang", v.name).apply() }
    fun setPrivacy(v: PrivacySettings) {
        _privacy = v
        p.edit().putBoolean("share_photo", v.sharePhoto)
            .putBoolean("share_history", v.shareHistory)
            .putBoolean("share_pgn", v.sharePgn).apply()
    }
    fun setProfileName(v: String) { val n = v.trim().ifBlank { "Jugador" }.take(20); _profileName = n; p.edit().putString("profile_name", n).apply() }
    fun setProfilePhotoBase64(v: String) { _profilePhotoBase64 = v.take(60_000); p.edit().putString("profile_photo", _profilePhotoBase64).apply() }
    fun setDefaultTime(timeMs: Long, incMs: Long) {
        _defaultTimeMs = timeMs.coerceAtLeast(0); _defaultIncrementMs = incMs.coerceIn(0, 60_000)
        p.edit().putLong("time_ms", _defaultTimeMs).putLong("inc_ms", _defaultIncrementMs).apply()
    }
    fun setCheckSignal(v: Boolean) { _checkSignal = v; p.edit().putBoolean("check_sig", v).apply() }
    fun setMoveIndicators(v: Boolean) { _moveIndicators = v; p.edit().putBoolean("move_ind", v).apply() }

    fun t(es: String, en: String): String = if (language == AppLanguage.EN) en else es
}

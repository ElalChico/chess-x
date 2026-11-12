package com.chessx.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import okio.BufferedSource
import okio.Buffer
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * UpdateManager - Manejo de actualizaciones in-app
 * 
 * Características:
 * - Verifica actualizaciones en GitHub Releases
 * - Descarga APK firmado con mismo keystore (datos persisten)
 * - Instala sin perder historial/perfil (mismo packageId + misma firma)
 * - Solo borra datos en: desinstalación manual, factory reset, o reinstall limpio
 * 
 * IMPORTANTE: El APK DEBE estar firmado con el MISMO keystore que la versión actual
 * para que Android preserve los datos al actualizar.
 */
class UpdateManager private constructor(context: Context) {

    private val ctx = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // GitHub repo para releases
    private val repoOwner = "ElalChico"
    private val repoName = "chess-x"
    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
    private val downloadUrlBase = "https://github.com/$repoOwner/$repoName/releases/download/"

    // Estados
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    companion object {
        @Volatile private var INSTANCE: UpdateManager? = null
        fun getInstance(context: Context): UpdateManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: UpdateManager(context).also { INSTANCE = it }
        }
    }

    /**
     * Versión actual de la app
     */
    fun getCurrentVersion(): VersionInfo {
        try {
            val pkg = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            return VersionInfo(
                versionCode = pkg.longVersionCode,
                versionName = pkg.versionName ?: "unknown"
            )
        } catch (e: Exception) {
            return VersionInfo(0, "unknown")
        }
    }

    /**
     * Verifica si hay actualización disponible en GitHub
     */
    suspend fun checkForUpdate(): Result<UpdateCheckResult> {
        _updateState.value = UpdateState.Checking
        return suspendCoroutine { cont ->
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "CHESS-X-Android")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("UpdateManager", "checkForUpdate failed", e)
                    _updateState.value = UpdateState.Error(e.message ?: "Error de red")
                    cont.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            _updateState.value = UpdateState.Error("HTTP ${response.code}")
                            cont.resume(Result.failure(Exception("HTTP ${response.code}")))
                        } else {
                            val json = response.body?.string() ?: "{}"
                            response.close()
                            val result = parseReleaseJson(json)
                            cont.resume(result)
                        }
                    } catch (e: Exception) {
                        Log.e("UpdateManager", "checkForUpdate parsing failed", e)
                        _updateState.value = UpdateState.Error(e.message ?: "Error parseando")
                        cont.resume(Result.failure(e))
                    }
                }
            })
        }
    }

    private fun parseReleaseJson(json: String): Result<UpdateCheckResult> {
        try {
            val obj = org.json.JSONObject(json)
            val tagName = obj.getString("tag_name") // ej: "X.52"
            val name = obj.optString("name", "Release $tagName")
            val body = obj.optString("body", "Sin notas de versión")
            val prerelease = obj.getBoolean("prerelease")
            val assets = obj.getJSONArray("assets")

            // Buscar APK en assets. Acepta CUALQUIER .apk (el nombre puede ser
            // "app-release.apk", "CHESS-X-X.55.apk", etc.) y prioriza el de mayor tamaño
            // por si hay varios assets. Evita fallos si el nombre no contiene "chess-x".
            var apkUrl: String? = null
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.getString("name")
                if (!assetName.endsWith(".apk", true)) continue
                val size = asset.optLong("size", 0L)
                val url = asset.optString("browser_download_url", "")
                if (url.isBlank()) continue
                // Prioridad: nombres con "chess-x"/"chessx" primero, si no, el más grande
                val preferred = assetName.contains("chess-x", true) || assetName.contains("chessx", true)
                val currentPreferred = apkUrl != null && (
                    apkUrl!!.substringAfterLast("/").contains("chess-x", true) ||
                    apkUrl!!.substringAfterLast("/").contains("chessx", true)
                )
                if (apkUrl == null || (preferred && !currentPreferred) || (preferred == currentPreferred && size >= apkSize)) {
                    apkUrl = url
                    apkSize = size
                }
            }

            val current = getCurrentVersion()
            val remoteVersion = parseVersionName(tagName)
            
            val hasUpdate = remoteVersion.versionCode > current.versionCode

            val result = UpdateCheckResult(
                hasUpdate = hasUpdate,
                currentVersion = current,
                remoteVersion = remoteVersion,
                releaseName = name,
                releaseNotes = body,
                apkUrl = apkUrl,
                apkSize = apkSize,
                isPrerelease = prerelease
            )

            _updateState.value = if (hasUpdate) UpdateState.UpdateAvailable(result) else UpdateState.UpToDate
            return Result.success(result)
        } catch (e: Exception) {
            Log.e("UpdateManager", "parseReleaseJson failed", e)
            _updateState.value = UpdateState.Error("Error parseando release")
            return Result.failure(e)
        }
    }

    private fun parseVersionName(tag: String): VersionInfo {
        // tag format: "X.52" or "vX.52" -> versionCode = 52, versionName = "X.52"
        val clean = tag.replace("v", "").replace("X.", "").trim()
        val code = clean.toIntOrNull() ?: 0
        return VersionInfo(code.toLong(), "X.$code")
    }

    /**
     * Borra APKs de actualización antiguos/fallidos para no acumular basura.
     * Se llama al arrancar la app y también antes de cada descarga.
     */
    fun cleanupStaleUpdates(maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        try {
            val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            val now = System.currentTimeMillis()
            dir.listFiles { f ->
                f.name.startsWith("chess-x-update") && f.name.endsWith(".apk")
            }?.forEach { f ->
                // Borra siempre los muy antiguos; los fallidos se re-crean con nombre único
                if (now - f.lastModified() > maxAgeMs || f.length() == 0L) {
                    try { f.delete() } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * Descarga e instala el APK (método directo sin Play Store)
     * Requiere: APK firmado con MISMO keystore
     */
    suspend fun downloadAndInstall(
        apkUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> {
        // Limpieza previa: borra descargas fallidas/viejas para no acumular basura
        cleanupStaleUpdates(0L)
        // Nombre único con marca de tiempo: evita colisiones con descargas fallidas
        val file = File(
            ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "chess-x-update-${System.currentTimeMillis()}.apk"
        )

        _updateState.value = UpdateState.Downloading(0f)

        return suspendCoroutine { cont ->
            val request = Request.Builder().url(apkUrl).build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("UpdateManager", "downloadAndInstall failed", e)
                    // Borra cualquier archivo parcial para no dejar basura
                    try { file.delete() } catch (_: Exception) { }
                    _updateState.value = UpdateState.Error(e.message ?: "Error descargando")
                    cont.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            _updateState.value = UpdateState.Error("Descarga falló: HTTP ${response.code}")
                            response.close()
                            try { file.delete() } catch (_: Exception) { }
                            cont.resume(Result.failure(Exception("HTTP ${response.code}")))
                            return
                        }

                        val total = response.body?.contentLength() ?: 0L
                        var downloaded = 0L
                        val source: BufferedSource = response.body?.source() ?: run {
                            try { file.delete() } catch (_: Exception) { }
                            _updateState.value = UpdateState.Error("Descarga fallida: respuesta vacía")
                            cont.resume(Result.failure(Exception("Respuesta vacía")))
                            return
                        }
                        val sink: BufferedSink = FileOutputStream(file).sink().buffer()
                        
                        val buffer = Buffer()
                        while (true) {
                            val read = source.read(buffer, 8192)
                            if (read == -1L) break
                            downloaded += read
                            sink.write(buffer, read)
                            if (total > 0) {
                                val progress = downloaded.toFloat() / total.toFloat()
                                onProgress(progress)
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                        sink.flush()
                        sink.close()
                        source.close()
                        response.close()

                        // Descarga incompleta: no intentar instalar, borrar basura
                        if (total > 0 && downloaded != total) {
                            try { file.delete() } catch (_: Exception) { }
                            _updateState.value = UpdateState.Error("Descarga interrumpida (${downloaded / 1024} KB de ${total / 1024} KB). Vuelve a intentarlo.")
                            cont.resume(Result.failure(Exception("Descarga incompleta")))
                            return
                        }

                        _updateState.value = UpdateState.Downloaded(file)
                        val installResult = installApk(file)
                        cont.resume(installResult)
                    } catch (e: Exception) {
                        Log.e("UpdateManager", "downloadAndInstall failed", e)
                        try { file.delete() } catch (_: Exception) { }
                        _updateState.value = UpdateState.Error(e.message ?: "Error descargando")
                        cont.resume(Result.failure(e))
                    }
                }
            })
        }
    }

    /**
     * Instala APK usando Intent (Android 8+ usa FileProvider)
     * El instalador de Android siempre REEMPLAZA la versión instalada
     * (mismo packageId + misma firma + versionCode mayor). No se conservan
     * copias duplicadas de la app; el APK descargado se borra en el próximo arranque.
     */
    private fun installApk(file: File): Result<Unit> {
        // Validar que la descarga quedó completa antes de intentar instalar
        if (!file.exists() || file.length() == 0L) {
            try { file.delete() } catch (_: Exception) { }
            _updateState.value = UpdateState.Error("Descarga incompleta o dañada. Vuelve a intentarlo.")
            return Result.failure(Exception("APK inválido"))
        }
        return try {
            // Android 8+: comprobar permiso para instalar apps desconocidas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pm2 = ctx.packageManager
                if (!pm2.canRequestPackageInstalls()) {
                    _updateState.value = UpdateState.Error("Activa «Instalar apps desconocidas» para CHESS-X y pulsa de nuevo")
                    try {
                        val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = android.net.Uri.parse("package:${ctx.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        ctx.startActivity(settingsIntent)
                    } catch (_: Exception) { }
                    return Result.failure(Exception("Permiso de instalación no concedido"))
                }
            }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Verificar que hay actividad para manejar el intent
            val pm = ctx.packageManager
            if (intent.resolveActivity(pm) != null) {
                ctx.startActivity(intent)
                _updateState.value = UpdateState.Installing
                Result.success(Unit)
            } else {
                _updateState.value = UpdateState.Error("No hay instalador de paquetes")
                Result.failure(Exception("No package installer"))
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "installApk failed", e)
            _updateState.value = UpdateState.Error(e.message ?: "Error instalando")
            Result.failure(e)
        }
    }

    /**
     * In-app updates via Play Core (requiere Play Store instalado)
     * Útil si la app está en Play Store (internal/closed testing)
     * NOTA: Requiere dependencias adicionales de Play Core
     */
    fun startPlayStoreUpdate(activity: Activity, requestCode: Int = 1001) {
        // Play Core in-app updates - disabled for now due to API complexity
        // Use direct APK download from GitHub Releases instead
        Log.d("UpdateManager", "Play Store update not implemented - use direct download")
    }
    
    /**
     * Abre GitHub Releases en navegador (fallback manual)
     */
    fun openReleasesPage(activity: Activity) {
        val uri = Uri.parse("https://github.com/$repoOwner/$repoName/releases")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    /**
     * Verifica si la app fue actualizada recientemente (para mostrar "novedades")
     */
    fun checkIfUpdated(): Boolean {
        val prefs = ctx.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastVersionCode = prefs.getLong("last_version_code", 0)
        val current = getCurrentVersion().versionCode
        
        if (current > lastVersionCode) {
            prefs.edit().putLong("last_version_code", current).apply()
            return true
        }
        return false
    }

    fun clearUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    /** Marca un error cuando la release no tiene un APK descargable. */
    fun reportMissingApk() {
        _updateState.value = UpdateState.Error("La release no incluye un APK descargable")
    }
}

// ===== Data Classes =====

data class VersionInfo(
    val versionCode: Long,
    val versionName: String
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class UpdateAvailable(val result: UpdateCheckResult) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Downloaded(val file: File) : UpdateState
    object Installing : UpdateState
    object Installed : UpdateState
    data class Error(val message: String) : UpdateState
}

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: VersionInfo,
    val remoteVersion: VersionInfo,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String?,
    val apkSize: Long,
    val isPrerelease: Boolean
) {
    fun formattedSize(): String {
        return when {
            apkSize >= 1_000_000 -> "${(apkSize / 1_000_000.0).toString().take(4)} MB"
            apkSize >= 1_000 -> "${(apkSize / 1_000.0).toString().take(4)} KB"
            else -> "$apkSize bytes"
        }
    }
}
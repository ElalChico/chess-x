package com.chessx.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.chessx.data.GameRecord
import com.chessx.data.LobbyState
import com.chessx.ui.components.PhotoCropDialog
import java.io.ByteArrayOutputStream

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF171D25)
private val Card2 = Color(0xFF1C232D)
private val Line = Color(0xFF252E39)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)

private fun decodeOrientedImage(resolver: ContentResolver, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (maxDimension / sampleSize > 2048) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val transform = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(270f)
                postScale(-1f, 1f)
            }
        }
    }
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
}

private fun encodeProfilePhoto(cropped: Bitmap): String {
    val side = minOf(cropped.width, cropped.height)
    var scaled = Bitmap.createScaledBitmap(cropped, minOf(side, 384), minOf(side, 384), true)
    for (quality in listOf(82, 72, 62, 52)) {
        val output = ByteArrayOutputStream()
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) continue
        if (output.size() <= 42_000) return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
    scaled = Bitmap.createScaledBitmap(scaled, 256, 256, true)
    val output = ByteArrayOutputStream()
    if (!scaled.compress(Bitmap.CompressFormat.JPEG, 60, output) || output.size() > 42_000) return ""
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun LobbyScreen(
    lobby: LobbyState,
    localName: String,
    remoteName: String,
    photoBase64: String = "",
    history: List<GameRecord> = emptyList(),
    remoteProfile: com.chessx.data.RemoteProfile? = null,
    connected: Boolean = false,
    onViewRivalProfile: () -> Unit = {},
    continueLabel: String = "Continuar",
    onContinue: () -> Unit = {},
    onNameChange: (String) -> Unit,
    onReplay: (GameRecord) -> Unit = {},
    onDeleteGame: (GameRecord) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onPhotoCropped: (String) -> Unit = {},
    /** Abre la pantalla de estadisticas. */
    onOpenStats: () -> Unit = {},
    /** Historial especial: partidas interrumpidas que se pueden retomar. */
    suspendedGames: List<com.chessx.data.SuspendedGame> = emptyList(),
    /** Hay rival conectado: se puede reanudar. */
    canResume: Boolean = false,
    onResumeSuspended: (com.chessx.data.SuspendedGame) -> Unit = {},
    onDiscardSuspended: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var pendingCrop by remember { mutableStateOf<Bitmap?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoError = null
        if (uri != null) {
            try {
                pendingCrop = decodeOrientedImage(context.contentResolver, uri)
                    ?.let { downscale(it, 2048) }
                if (pendingCrop == null && photoError == null) photoError = "No se pudo abrir la imagen"
            } catch (e: Exception) {
                pendingCrop = null
                photoError = "No se pudo abrir la imagen"
            }
        }
    }

    pendingCrop?.let { bmp ->
        PhotoCropDialog(
            bitmap = bmp,
            onConfirm = { cropped ->
                try {
                    val b64 = encodeProfilePhoto(cropped)
                    if (b64.isBlank()) {
                        photoError = "No se pudo preparar la foto. Prueba con otra imagen."
                    } else {
                        onPhotoCropped(b64)
                        photoError = null
                        pendingCrop = null
                    }
                } catch (e: Exception) {
                    photoError = "No se pudo guardar la foto. Prueba con otra imagen."
                }
            },
            onDismiss = { pendingCrop = null }
        )
    }

    // Inicializar nombre: vacío si primera vez, o nombre guardado para editar.
    // Mientras el campo tiene el foco manda lo que escribe el usuario (no se sobrescribe).
    var nameFocused by remember { mutableStateOf(false) }
    LaunchedEffect(localName) {
        if (!nameFocused) nameText = localName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        photoError?.let {
            Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
        }
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
            Text("← Volver al inicio", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Perfil", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
            if (remoteName.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF12241B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3B2B))
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(7.dp).background(Green, RoundedCornerShape(50)))
                        Text("Conectado", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9BE3BD))
                    }
                }
            }
        }
        Text(
            if (remoteName.isBlank()) "Foto, nombre e historial. Conecta para jugar."
            else "Rival: $remoteName · abre la sala para jugar.",
            color = Muted,
            fontSize = 12.sp
        )

        if (connected && remoteProfile != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF3A3524), RoundedCornerShape(14.dp))
                    .clickable { onViewRivalProfile() }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RemoteAvatarMini(remoteProfile.photoBase64, remoteProfile.name.ifBlank { "R" })
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Ver perfil de ${remoteProfile.name.ifBlank { remoteName.ifBlank { "Rival" } }}",
                            color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1
                        )
                        Text(
                            "${remoteProfile.total} jugadas · ${remoteProfile.wins}V · ${remoteProfile.losses}D · ${remoteProfile.draws}E",
                            color = Muted, fontSize = 11.sp
                        )
                    }
                    Text("→", color = Gold, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Profile photo (round avatar + crop)
        SectionLabel("FOTO DE PERFIL")
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(photoBase64, localName.ifBlank { "T" })
            Spacer(Modifier.width(14.dp))
            Column {
                Button(
                    onClick = { picker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Gold),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF3A3524))
                ) { Text("Cambiar foto", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Spacer(Modifier.height(4.dp))
                Text("Pellizca para acercar · arrastra para posicionar", color = Muted, fontSize = 11.sp)
                photoError?.let { Text(it, color = Color(0xFFFF9B9B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        // Name field
        SectionLabel("TU NOMBRE")
        var hasChanges by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = nameText,
            onValueChange = { if (it.length <= 18) { nameText = it; onNameChange(it); hasChanges = true } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { nameFocused = it.isFocused },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Ej: Magnus", color = Muted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Text1, unfocusedTextColor = Text1,
                cursorColor = Gold, focusedBorderColor = Gold, unfocusedBorderColor = Line2,
                focusedContainerColor = Color(0xFF0F141B), unfocusedContainerColor = Color(0xFF0F141B)
            )
        )
        if (hasChanges) {
            Text("✓ Nombre guardado automáticamente", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F), RoundedCornerShape(16.dp))
                .border(1.5.dp, Color(0xFF3A3524), RoundedCornerShape(16.dp))
                .clickable { onContinue() }
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(38.dp)
                        .background(Color(0xFF1A1A14), RoundedCornerShape(11.dp))
                        .border(1.dp, Color(0xFF3A3524), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("→", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                }
                Column {
                    Text(continueLabel, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                    Text("Tu nombre y foto se comparten según tus ajustes", fontSize = 10.sp, color = Color(0xFFA8975E))
                }
            }
        }

        // Historial especial: partidas suspendidas por perdida de conexion
        if (suspendedGames.isNotEmpty()) {
            SectionLabel("PARTIDAS SUSPENDIDAS")
            suspendedGames.forEach { g ->
                SuspendedGameCard(
                    game = g,
                    canResume = canResume,
                    onResume = { onResumeSuspended(g) },
                    onDiscard = { onDiscardSuspended(g.id) }
                )
            }
        }

        // Historial de torneos (separado del historial común)
        val tournamentRecords = remember { mutableStateOf(com.chessx.data.TournamentStore.load(context)) }
        var showTournaments by remember { mutableStateOf(false) }

        if (showTournaments) {
            TournamentHistoryDialog(
                records = tournamentRecords.value,
                onDismiss = { showTournaments = false },
                onClearAll = {
                    com.chessx.data.TournamentStore.clear(context)
                    tournamentRecords.value = emptyList()
                },
                onDelete = { id ->
                    com.chessx.data.TournamentStore.remove(context, id)
                    tournamentRecords.value = com.chessx.data.TournamentStore.load(context)
                }
            )
        }

        SectionLabel("HISTORIAL DE TORNEOS")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTournaments = true },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A160C),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6B5518))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2B94E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("T", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF151203))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Torneos" + if (tournamentRecords.value.isNotEmpty()) " · ${tournamentRecords.value.size}" else "",
                        color = Color(0xFFE8C766), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (tournamentRecords.value.isEmpty()) "Aún no has jugado torneos. Pulsa para ver el historial."
                        else "Partidas por torneo con colores alternos y marcador acumulado",
                        color = Color(0xFFA7966A), fontSize = 10.sp
                    )
                }
                Text("›", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }
        }

        // History (PGN) — each entry replays with its captured data
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("HISTORIAL (PGN)")
            Spacer(Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = onOpenStats) {
                    Text("Estadísticas", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onClearHistory) {
                    Text("Borrar todo", color = Color(0xFFFF9B9B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (history.isEmpty()) {
            Text("Sin partidas guardadas todavía. Al terminar una por Bluetooth se guardará aquí.", color = Muted, fontSize = 12.sp)
        } else {
            history.forEach { rec ->
                HistoryItem(rec = rec, onReplay = { onReplay(rec) }, onDelete = { onDeleteGame(rec) })
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

private fun downscale(src: Bitmap?, maxDim: Int): Bitmap? {
    if (src == null) return null
    val longest = maxOf(src.width, src.height)
    if (longest <= maxDim) return src
    val ratio = maxDim.toFloat() / longest.toFloat()
    val out = Bitmap.createScaledBitmap(src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true)
    if (out !== src) src.recycle()
    return out
}

fun decodePhoto(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    val raw = base64.substringAfter("base64,", base64)
    return runCatching {
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

/**
 * Tarjeta de partida suspendida (historial especial).
 * Permite retomarla con el rival conectado; respeta el turno guardado.
 */
@Composable
private fun SuspendedGameCard(
    game: com.chessx.data.SuspendedGame,
    canResume: Boolean,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1206), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF3A2A10), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Partida suspendida",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Gold
                )
                Text(
                    "${game.opponent} · ${game.dateLabel()}",
                    fontSize = 11.sp, color = Text1, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${game.movesCount} jugadas · ${game.timeLabel()} · " +
                        if (game.sideToMoveWhite) "turno de las blancas" else "turno de las negras",
                    fontSize = 10.sp, color = Muted
                )
            }
            Text("⏸", fontSize = 22.sp, color = Gold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onResume,
                enabled = canResume,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A14),
                    contentColor = Gold,
                    disabledContainerColor = Color(0xFF12141A),
                    disabledContentColor = Color(0xFF6B6244)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, if (canResume) Color(0xFF3A3524) else Color(0xFF24231C))
            ) {
                Text(
                    if (canResume) "Reanudar" else "Conecta al rival",
                    fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1
                )
            }
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A2626))
            ) {
                Text("Descartar", color = Color(0xFFFF9B9B), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HistoryItem(rec: GameRecord, onReplay: () -> Unit, onDelete: () -> Unit) {
    // Determine white and black player names based on who played white
    val (whiteName, blackName) = if (rec.localColorWhite) {
        rec.playerName to rec.opponent
    } else {
        rec.opponent to rec.playerName
    }
    // Resultado visto desde el jugador local (estilo chess.com)
    val won = rec.localWon == true
    val lost = rec.localWon == false
    val outcomeText = when {
        won -> "Victoria"
        lost -> "Derrota"
        else -> "Tablas"
    }
    val outcomeColor = when {
        won -> Green
        lost -> Color(0xFFFF6B6B)
        else -> Color(0xFF8B96A5)
    }
    val winnerName = when {
        won -> rec.playerName
        lost -> rec.opponent
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Etiqueta de resultado + color jugado
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(outcomeColor.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(outcomeText, color = outcomeColor, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    if (rec.localColorWhite) "Con blancas" else "Con negras",
                    color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                )
            }
            // Marcador: el ganador destacado
            Text(
                buildString {
                    append(if (rec.localColorWhite) whiteName else blackName)
                    append("  vs  ")
                    append(if (rec.localColorWhite) blackName else whiteName)
                },
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Text1, maxLines = 1
            )
            Text(
                "${rec.dateLabel()} · ${rec.result} · ${rec.movesSan.size} jugadas · ${rec.timeLabel}" +
                    if (winnerName != null) " · Ganó $winnerName" else "",
                fontSize = 11.sp, color = Muted, maxLines = 2
            )
        }
        Button(
            onClick = onReplay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Gold),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3524)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) { Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(
            onClick = onDelete,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A2626)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF3A2222), contentColor = Color(0xFFFF9B9B))
        ) { Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}

@Composable
internal fun RemoteAvatarMini(photo: String?, name: String) {
    val bmp = remember(photo) { decodePhoto(photo) }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A2433))
            .border(1.dp, Color(0xFF24324A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = name, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Text((name.firstOrNull() ?: 'R').uppercaseChar().toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
        }
    }
}

/**
 * Diálogo con el historial de torneos: marcador acumulado, resultado por partida
 * y opción de borrar torneos individuales o todo el historial.
 */
@Composable
private fun TournamentHistoryDialog(
    records: List<com.chessx.data.TournamentRecord>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onDelete: (String) -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F141B),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6B5518))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE2B94E), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("T", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF151203))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Historial de torneos", color = Text1, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${records.size} torneo${if (records.size == 1) "" else "s"} guardado${if (records.size == 1) "" else "s"}", color = Muted, fontSize = 11.sp)
                    }
                    Text(
                        "✕",
                        modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                        color = Muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (records.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141B24), RoundedCornerShape(14.dp))
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🏆", fontSize = 34.sp)
                            Text("Todavía no hay torneos", color = Text1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Crea un torneo desde la sala para jugar varias partidas con colores alternos.",
                                color = Muted, fontSize = 10.sp)
                        }
                    }
                } else {
                    records.forEach { rec ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141B24), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF2A313B), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            val won = rec.localScore > rec.remoteScore
                            val lost = rec.localScore < rec.remoteScore
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(rec.opponent, color = Text1, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                    Text("${rec.dateLabel()} · ${rec.games} partidas · ${rec.timeLabel}", color = Muted, fontSize = 10.sp)
                                }
                                val badgeColor = when {
                                    won -> Green
                                    lost -> Color(0xFFFF6B6B)
                                    else -> Color(0xFF8B96A5)
                                }
                                Box(
                                    Modifier
                                        .background(badgeColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(rec.resultLabel, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "🗑",
                                    modifier = Modifier
                                        .clickable { onDelete(rec.id) }
                                        .padding(6.dp),
                                    fontSize = 15.sp
                                )
                            }
                            // Marcador acumulado
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ScorePill("Tú", rec.localScore, won = won)
                                ScorePill("Rival", rec.remoteScore, won = lost)
                                ScorePill("Tablas", rec.draws.toDouble(), won = false, neutral = true)
                            }
                            // Resultados por partida
                            if (rec.gameResults.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rec.gameResults.forEach { r ->
                                        val wonGame = r.contains(": V")
                                        val lostGame = r.contains(": D")
                                        val chipColor = when {
                                            wonGame -> Green
                                            lostGame -> Color(0xFFFF6B6B)
                                            else -> Color(0xFF8B96A5)
                                        }
                                        Box(
                                            Modifier
                                                .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(r, color = chipColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!confirmClear) {
                        TextButton(onClick = { confirmClear = true }) {
                            Text("Borrar todo el historial de torneos", color = Color(0xFFFF9B9B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A1616), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("¿Borrar todos los torneos?", color = Color(0xFFFF9B9B), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { confirmClear = false }) { Text("Cancelar", color = Muted, fontSize = 12.sp) }
                            TextButton(onClick = { onClearAll(); confirmClear = false }) {
                                Text("Borrar", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorePill(label: String, score: Double, won: Boolean, neutral: Boolean = false) {
    val color = when {
        neutral -> Color(0xFF8B96A5)
        won -> Green
        else -> Color(0xFF8B96A5)
    }
    Column(
        Modifier
            .background(Color(0xFF1B232E), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (score % 1.0 == 0.0) score.toInt().toString() else score.toString(), color = if (won) Green else Text1, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProfileAvatar(photoBase64: String, name: String) {
    val bmp = remember(photoBase64) { decodePhoto(photoBase64) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFF15251D))
            .border(2.dp, Green.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                (name.firstOrNull() ?: 'T').uppercaseChar().toString(),
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Green
            )
        }
    }
}

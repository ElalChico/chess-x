package com.chessx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.data.RemoteProfile
import com.chessx.data.RemoteGame
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight

private val Bg = Color(0xFF161D26)
private val Line = Color(0xFF252E39)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)
private val Red = Color(0xFFFF6B6B)

/** Avatar circular con la foto remota o una inicial, centrado siempre. */
@Composable
private fun Avatar(photo: String?, name: String, size: Int) {
    val bmp = remember(photo) {
        try {
            if (photo.isNullOrBlank()) null else {
                val raw = photo.substringAfter("base64,", photo)
                val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A2433))
            .border(1.5.dp, Color(0xFF24324A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text((name.firstOrNull()?.uppercaseChar() ?: 'R').toString(), fontWeight = FontWeight.ExtraBold, color = Text1, fontSize = (size / 2.2).sp)
        }
    }
}

/** Diálogo centrado con el perfil del rival recibido por Bluetooth. */
@Composable
fun RivalProfileDialog(
    profile: RemoteProfile,
    onDismiss: () -> Unit,
    onReplay: (RemoteGame) -> Unit = {}
) {
    var visiblePgnIndex by remember(profile.name) { mutableIntStateOf(-1) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bg, RoundedCornerShape(20.dp))
                .border(1.dp, Line, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(profile.photoBase64, profile.name, 64)
                Column(Modifier.weight(1f)) {
                    Text(profile.name.ifBlank { "Rival" }, color = Text1, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1)
                    Text(
                        when (profile.color) {
                            "white" -> "Blancas"; "black" -> "Negras"; "random" -> "Color aleatorio (lo decide el anfitrión)"; else -> "Color por asignar"
                        },
                        color = Muted, fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Jugadas", profile.total.toString(), Modifier.weight(1f))
                StatCard("Victorias", profile.wins.toString(), Modifier.weight(1f), Green)
                StatCard("Empates", profile.draws.toString(), Modifier.weight(1f))
                StatCard("Derrotas", profile.losses.toString(), Modifier.weight(1f), Red)
            }

            Spacer(Modifier.height(14.dp))
            Text("HISTORIAL", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            if (profile.games.isEmpty()) {
                Text("El rival no ha compartido historial (revisa su Ajustes → Compartir).", color = Muted, fontSize = 12.sp)
            } else {
                Column(modifier = Modifier.heightIn(max = 230.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    profile.games.take(20).forEachIndexed { index, g ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF10151C), RoundedCornerShape(10.dp)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(g.dateLabel(), color = Text1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${g.moves} jugadas · ${g.timeLabel.ifBlank { "—" }} · vs ${g.opponent.ifBlank { "?" }}", color = Muted, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    when (g.won) { true -> "Victoria"; false -> "Derrota"; null -> "Empate" },
                                    color = when (g.won) { true -> Green; false -> Red; null -> Muted },
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                                )
                                Row {
                                    if (g.pgn.isNotBlank()) {
                                        TextButton(
                                            onClick = { visiblePgnIndex = if (visiblePgnIndex == index) -1 else index },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text(if (visiblePgnIndex == index) "Ocultar PGN" else "Ver PGN", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (g.uciMoves.isNotEmpty()) {
                                        TextButton(
                                            onClick = { onReplay(g) },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text("Reproducir", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        if (visiblePgnIndex == index && g.pgn.isNotBlank()) {
                            Text(
                                g.pgn,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                color = Muted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cerrar", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Text1) {
    Column(
        modifier = modifier
            .background(Color(0xFF10151C), RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        Text(label, color = Muted, fontSize = 9.5.sp)
    }
}

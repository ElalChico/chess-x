package com.chessx.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.ui.theme.Bg
import com.chessx.ui.theme.Card
import com.chessx.ui.theme.Gold
import com.chessx.ui.theme.Green
import com.chessx.ui.theme.Line
import com.chessx.ui.theme.Muted
import com.chessx.ui.theme.Text1

/** Repositorio oficial de CHESS-X. */
const val GITHUB_URL = "https://github.com/ElalChico"

@Composable
fun HomeScreen(
    inSession: Boolean,
    isGuestSession: Boolean,
    profileName: String,
    profilePhotoBase64: String,
    clockSummary: String,
    onProfile: () -> Unit,
    onCreate: () -> Unit,
    onSearch: () -> Unit,
    updateVersion: String? = null,
    onUpdateClick: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val openGitHub: () -> Unit = {
        try { uriHandler.openUri(GITHUB_URL) } catch (_: Exception) {}
    }
    val avatar = remember(profilePhotoBase64) {
        try {
            profilePhotoBase64.substringAfter("base64,", profilePhotoBase64).takeIf { it.isNotBlank() }?.let {
                val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.chessx.R.drawable.brand_chessx),
                    contentDescription = "CHESS-X",
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text("CHESS-X", color = Text1, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Ajedrez cerca, partida a partida", color = Muted, fontSize = 11.sp)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (inSession) Color(0xFF10251A) else Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (inSession) Color(0xFF24523A) else Line)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(7.dp).background(if (inSession) Green else Muted, CircleShape))
                    Text(if (inSession) "Conectado" else "Bluetooth", color = if (inSession) Green else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Aviso pequeño de actualización disponible
        if (updateVersion != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onUpdateClick),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1A0C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B5518))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFE2B94E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↑", color = Color(0xFF151203), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Actualización disponible", color = Color(0xFFE8C766), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("CHESS-X $updateVersion · pulsa para instalar", color = Color(0xFFA7966A), fontSize = 9.5.sp)
                    }
                    Text("›", color = Color(0xFFE2B94E), fontSize = 16.sp, fontWeight = FontWeight.Light)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121922),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF273341))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF1A2430), CircleShape)
                        .border(1.5.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null) {
                        Image(avatar, contentDescription = "Foto de $profileName", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(profileName.firstOrNull()?.uppercaseChar()?.toString() ?: "J", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Hola, ${profileName.ifBlank { "Jugador" }}", color = Text1, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text("Tu perfil · historial · foto", color = Muted, fontSize = 11.sp)
                }
                Text("›", color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Light)
            }
        }

        Text("PARTIDA", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeAction(
                title = "Crear",
                subtitle = "Configurar reloj",
                icon = "＋",
                modifier = Modifier.weight(1f),
                onClick = onCreate
            )
            HomeAction(
                title = "Unirse",
                subtitle = "Buscar rival",
                icon = "⌕",
                modifier = Modifier.weight(1f),
                onClick = onSearch
            )
        }

        Surface(shape = RoundedCornerShape(16.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (inSession) "Sala conectada" else "Reloj habitual", color = Text1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            inSession && isGuestSession -> "Esperando el inicio del anfitrión"
                            inSession -> "Listos para iniciar la partida"
                            else -> "Tu siguiente partida comienza aquí"
                        },
                        color = Muted, fontSize = 10.sp
                    )
                }
                Text(if (inSession) "●" else clockSummary, color = if (inSession) Green else Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Firma del autor + acceso al repositorio
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Creada por Elal Chico", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("CHESS-X · © Elal Chico", color = Muted.copy(alpha = 0.6f), fontSize = 9.5.sp)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF151C25),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2F3947)),
                modifier = Modifier.clickable { openGitHub() }
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⌘", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("GitHub", color = Text1, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeAction(title: String, subtitle: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF151911),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3B1A))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icon, color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
        }
    }
}

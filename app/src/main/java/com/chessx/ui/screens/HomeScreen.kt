package com.chessx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF12141A)
private val Line = Color(0xFF2A2E36)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)

/** Logo CHESS-X: caballero dorado (vector) sobre tile oscuro + X. */
@Composable
private fun ChessXLogo(size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size.dp * 0.26f))
            .background(Color(0xFF11161D))
            .border(1.dp, Color(0xFF1E2630), RoundedCornerShape(size.dp * 0.26f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.chessx.R.drawable.knight_gold),
            contentDescription = "CHESS-X",
            modifier = Modifier.size((size * 0.72).dp)
        )
        // X monogram bottom-right
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.dp.toPx()
            val stroke = s * 0.075f
            val cx = s * 0.78f; val cy = s * 0.76f; val r = s * 0.072f
            drawLine(Gold, androidx.compose.ui.geometry.Offset(cx - r, cy - r), androidx.compose.ui.geometry.Offset(cx + r, cy + r), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(Gold, androidx.compose.ui.geometry.Offset(cx - r, cy + r), androidx.compose.ui.geometry.Offset(cx + r, cy - r), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun HomeScreen(
    inSession: Boolean,
    isGuestSession: Boolean,
    onCreate: () -> Unit,
    onSearch: () -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChessXLogo(52)
                Text(
                    "CHESS-X",
                    color = Text1,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF101B14),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3B2B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(7.dp).background(Green, androidx.compose.foundation.shape.CircleShape))
                    Text("SPP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9BE3BD))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Crear partida — invitado/conectado deshabilitado (sólo el anfitrión crea sala)
        CxButton(
            title = if (inSession && isGuestSession) "Crear partida" else "Crear partida",
            subtitle = if (inSession && isGuestSession) "Desconéctate de la sala primero" else "Anfitrión · decide reloj y color",
            enabled = !inSession,
            onClick = onCreate
        )
        CxButton(
            title = "Unirse a partida",
            subtitle = "Busca rivales cercanos",
            enabled = !inSession,
            onClick = onSearch
        )
        if (inSession) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1206), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF3A2A10), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    if (isGuestSession) "Conectado como invitado · el anfitrión administra la partida" else "Conectado como anfitrión · la sala está activa",
                    color = Color(0xFFE2B94E), fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Sonidos — fila minimalista
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sonidos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Text1)
                Text("Conjunto estándar de ajedrez", fontSize = 11.sp, color = Muted)
            }
            Switch(
                checked = soundEnabled,
                onCheckedChange = { onToggleSound() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Color(0xFF3A341C),
                    uncheckedThumbColor = Color(0xFF8892A0),
                    uncheckedTrackColor = Color(0xFF2A2A32)
                )
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "CHESS-X · © Elal Chico",
            fontSize = 10.5.sp,
            color = Muted,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Botón único de la app: negro, texto dorado, borde fino, esquinas 14dp. */
@Composable
private fun CxButton(title: String, subtitle: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val textColor = if (enabled) Gold else Color(0xFF6B6244)
    val subColor = if (enabled) Color(0xFFA8975E) else Color(0xFF4A4636)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF0F0F0F), RoundedCornerShape(14.dp))
            .border(1.2.dp, if (enabled) Color(0xFF3A3524) else Color(0xFF24231C), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = subColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

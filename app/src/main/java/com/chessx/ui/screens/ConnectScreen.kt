package com.chessx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.data.BtState
import com.chessx.data.DiscoveredDevice
import com.chessx.data.RemoteProfile

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF12141A)
private val Card2 = Color(0xFF151C25)
private val Line = Color(0xFF232C38)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)
private val Red = Color(0xFFFF6B6B)

@Composable
fun ConnectScreen(
    isHostMode: Boolean,
    btState: BtState,
    devices: List<DiscoveredDevice>,
    onHostListen: () -> Unit,
    onSearch: () -> Unit,
    onChooseRole: (Boolean) -> Unit, // true = anfitrión, false = invitado
    onConnect: (String) -> Unit,
    onBack: () -> Unit,
    error: String?,
    myName: String = "Jugador",
    remoteName: String = "",
    remoteProfile: RemoteProfile? = null,
    onViewRivalProfile: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Volver al inicio", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF141A22), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Text(
                    "YO · ${myName.take(14)}",
                    color = Text1, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))

        Text("Conecta dos móviles", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
        Text(
            when (btState) {
                BtState.WAITING_HOST -> "Eres el anfitrión · el otro móvil debe pulsar «Unirse a partida» y tocar este dispositivo."
                BtState.SCANNING -> "Buscando salas cercanas… el anfitrión debe estar en «Crear partida»."
                BtState.CONNECTED -> "¡Conectado! ${remoteName.ifBlank { "Rival" }} está sincronizado."
                BtState.CONNECTING -> "Conectando con ${remoteName.ifBlank { "el rival" }}…"
                BtState.DISCONNECTED -> "Conexión perdida · reintentando automáticamente (x5)."
                BtState.ERROR -> "Error de conexión · revisa permisos y reintenta."
                else -> "Un móvil crea la sala y el otro se une. El anfitrión inicia la partida."
            },
            color = Muted, fontSize = 12.5.sp, lineHeight = 17.sp
        )
        error?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }

        Spacer(Modifier.height(12.dp))

        // Selector de rol explícito: quién crea la sala
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card, RoundedCornerShape(14.dp))
                .border(1.dp, Line, RoundedCornerShape(14.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RoleTab("Crear partida", "Eres el anfitrión", isHostMode, Modifier.weight(1f)) { onChooseRole(true) }
            RoleTab("Unirse a partida", "Eres el invitado", !isHostMode, Modifier.weight(1f)) { onChooseRole(false) }
        }

        Spacer(Modifier.height(12.dp))

        if (btState == BtState.CONNECTED && remoteProfile != null) {
            // Estado sincronizado: ver perfil del rival + regresar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card2, RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF3A3524), RoundedCornerShape(14.dp))
                    .clickable { onViewRivalProfile() }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RemoteAvatarMini(remoteProfile.photoBase64, remoteProfile.name.ifBlank { remoteName.ifBlank { "R" } })
                    Column(Modifier.weight(1f)) {
                        Text("Ver perfil de ${remoteProfile.name.ifBlank { remoteName.ifBlank { "Rival" } }}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Text("${remoteProfile.total} jugadas · ${remoteProfile.wins}V · ${remoteProfile.losses}D", color = Muted, fontSize = 11.sp)
                    }
                    Text("→", color = Gold, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Acción principal del modo elegido
        if (btState != BtState.CONNECTED) {
            if (isHostMode) {
                RoleButton(
                    title = "Escuchar conexiones",
                    subtitle = "Visible por Bluetooth 300s · el rival se conecta a ti",
                    icon = "BT",
                    active = btState == BtState.WAITING_HOST,
                    activeLabel = "Escuchando… (300s)",
                    onClick = onHostListen
                )
            } else {
                RoleButton(
                    title = "Buscar rivales",
                    subtitle = "Listar salas cercanas del otro móvil",
                    icon = "BT",
                    active = btState == BtState.SCANNING,
                    activeLabel = "Escaneando… (~12s)",
                    onClick = onSearch
                )
            }
        }

        if (btState == BtState.WAITING_HOST || btState == BtState.SCANNING || btState == BtState.CONNECTING) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                CircularProgressIndicator(color = Gold, modifier = Modifier.size(18.dp), strokeWidth = 3.dp)
                Text(
                    when (btState) {
                        BtState.WAITING_HOST -> "Esperando al invitado…"
                        BtState.SCANNING -> "Buscando dispositivos…"
                        else -> "Conectando…"
                    },
                    color = Text1, fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            if (isHostMode) "DISPOSITIVOS QUE TE BUSCAN (${devices.size})" else "DISPOSITIVOS (${devices.size})",
            color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (devices.isEmpty()) {
                item {
                    Text(
                        if (isHostMode)
                            "Cuando el otro móvil esté en «Unirse a partida» y toque este dispositivo, aparecerá aquí."
                        else
                            "Pulsa «Buscar rivales»; los móviles con CHESS-X escuchando aparecerán aquí.",
                        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
            items(devices) { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Card, RoundedCornerShape(14.dp))
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1A2433), RoundedCornerShape(11.dp))
                            .border(1.dp, Color(0xFF24324A), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(d.name.firstOrNull()?.uppercaseChar()?.toString() ?: "D", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B9DF9)) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = Text1, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Text(d.address, color = Muted, fontSize = 9.sp)
                        if (d.bonded) Text("VINCULADO", color = Color(0xFF8FD3FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!isHostMode) {
                        Button(
                            onClick = { onConnect(d.address) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Gold),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3524)),
                            shape = RoundedCornerShape(11.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                        ) { Text("Conectar", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    } else {
                        Text("Esperando…", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleTab(title: String, subtitle: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(if (active) Color(0xFF26200F) else Color.Transparent, RoundedCornerShape(10.dp))
            .border(1.dp, if (active) Gold else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = if (active) Gold else Text1, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(subtitle, color = if (active) Color(0xFFA8975E) else Muted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun RoleButton(
    title: String,
    subtitle: String,
    icon: String,
    active: Boolean,
    activeLabel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(Color(0xFF0F0F0F), RoundedCornerShape(14.dp))
            .border(1.2.dp, if (active) Color(0xFF3A3524) else Color(0xFF2A2A22), RoundedCornerShape(14.dp))
            .clickable(enabled = !active) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp)
                    .background(Color(0xFF1A1A14), RoundedCornerShape(11.dp))
                    .border(1.dp, Color(0xFF3A3524), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
            }
            Column {
                Text(if (active) activeLabel else title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                Text(if (active) "Mantén la pantalla abierta" else subtitle, fontSize = 10.5.sp, color = Color(0xFFA8975E), fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

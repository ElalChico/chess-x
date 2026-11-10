package com.chessx.ui.screens

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.core.Color as ChessColor
import com.chessx.core.PieceType
import com.chessx.data.formatClock
import com.chessx.ui.UiGameState
import com.chessx.ui.components.ChessBoard
import com.chessx.ui.components.BoardSelection
import com.chessx.ui.components.GameEndDialog
import com.chessx.ui.components.PromotionPieceDialog
import com.chessx.ui.components.TournamentEndDialog

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF171D25)
private val Line = Color(0xFF252E39)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val GoldLight = Color(0xFFF3D478)
private val GoldDeep = Color(0xFFDFA93C)
private val Green = Color(0xFF3DDC97)
private val Red = Color(0xFFFF6B6B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    ui: UiGameState,
    isFullscreen: Boolean,
    boardTheme: com.chessx.data.BoardTheme = com.chessx.data.BoardTheme.LICHESS,
    pieceSet: com.chessx.data.PieceSet = com.chessx.data.PieceSet.DEFAULT,
    moveIndicators: Boolean = true,
    showCoordinates: Boolean = true,
    localPhotoBase64: String = "",
    remoteProfile: com.chessx.data.RemoteProfile? = null,
    onViewRivalProfile: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    onSquareTap: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onOfferDraw: () -> Unit,
    onAcceptDraw: () -> Unit,
    onRejectDraw: () -> Unit,
    onResign: () -> Unit,
    onPromotion: (PieceType) -> Unit,
    onCancelPromotion: () -> Unit,
    onToggleSound: () -> Unit,
    onDisconnect: () -> Unit,
    onNewGameSame: () -> Unit = {},
    onNewGameRandom: () -> Unit = {},
    onTournamentNext: () -> Unit = {},
    onBackToRoom: () -> Unit = {},
    onGameEndDismiss: () -> Unit = {},
    onNewTournament: () -> Unit = {},
    onReplayPgn: () -> Unit = {},
    onNewGameCustomTime: (Long, Long) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = if (isFullscreen) 8.dp else 10.dp, vertical = 8.dp)
    ) {
        if (!isFullscreen) {
            // Top status line + fullscreen control
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(Green, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = statusText(ui),
                    color = Muted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFF161C24), RoundedCornerShape(10.dp))
                        .border(1.dp, Line2, RoundedCornerShape(10.dp))
                        .clickable { onToggleFullscreen() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Pantalla completa", color = Color(0xFFC9D2DE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))

            PlayerBar(
                name = ui.remoteName.ifBlank { remoteProfile?.name ?: "Rival" },
                isYou = false,
                isTurn = ui.snapshot.sideToMove == ui.remoteColor,
                clockMs = if (ui.remoteColor == ChessColor.WHITE) ui.clock.whiteMs else ui.clock.blackMs,
                totalMs = ui.lobby.timeMs,
                showClock = ui.lobby.timeMs > 0,
                inCheck = ui.inCheck && ui.snapshot.sideToMove == ui.remoteColor,
                sideLabel = if (ui.remoteColor == ChessColor.WHITE) "BLANCAS" else "NEGRAS",
                remotePhoto = remoteProfile?.photoBase64,
                onRivalTap = if (remoteProfile != null) onViewRivalProfile else null
            )
            Spacer(Modifier.height(6.dp))
        } else {
            CompactHud(ui = ui, onExit = onToggleFullscreen)
            Spacer(Modifier.height(4.dp))
        }

        // Board sized to the smallest available dimension -> never overflows
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardSize = minOf(maxWidth, maxHeight)
            ChessBoard(
                snapshot = ui.snapshot,
                perspective = ui.localColor,
                selection = BoardSelection(
                    from = ui.selection.from,
                    legalTargets = ui.selection.legalTargets,
                    lastMove = ui.selection.lastMove,
                    checkKingSquare = ui.checkKing,
                    errorSquare = ui.selection.errorSquare
                ),
                onSquareTap = onSquareTap,
                modifier = Modifier.size(boardSize).aspectRatio(1f),
                theme = boardTheme,
                pieceSet = pieceSet,
                showMoveIndicators = moveIndicators,
                showCoordinates = showCoordinates
            )
        }

        Spacer(Modifier.height(6.dp))

        if (!isFullscreen) {
            PlayerBar(
                name = ui.localName.ifBlank { "Jugador" },
                isYou = true,
                isTurn = ui.snapshot.sideToMove == ui.localColor,
                clockMs = if (ui.localColor == ChessColor.WHITE) ui.clock.whiteMs else ui.clock.blackMs,
                totalMs = ui.lobby.timeMs,
                showClock = ui.lobby.timeMs > 0,
                inCheck = ui.inCheck && ui.snapshot.sideToMove == ui.localColor,
                sideLabel = if (ui.localColor == ChessColor.WHITE) "BLANCAS" else "NEGRAS",
                photoBase64 = localPhotoBase64
            )

            ui.lastError?.let {
                Text(it, color = Red, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ActionBtn(
                label = if (ui.paused) "Reanudar" else "Pausar",
                weak = true,
                modifier = Modifier.weight(1f)
            ) { onTogglePause() }

            if (!isFullscreen) {
                ActionBtn("Tablas", weak = false, modifier = Modifier.weight(1f)) { onOfferDraw() }
            }

            ActionBtn("Abandonar", weak = true, modifier = Modifier.weight(1f)) { onResign() }
        }

        if (ui.lastError?.contains("ofrece tablas") == true) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAcceptDraw,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF0B0D10))
                ) { Text("Aceptar") }
                OutlinedButton(
                    onClick = onRejectDraw,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line2)
                ) { Text("Rechazar", color = Text1) }
            }
        }

        if (!isFullscreen) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sonido ${if (ui.soundEnabled) "ON" else "OFF"}", color = Muted, fontSize = 11.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToggleSound) {
                        Text(if (ui.soundEnabled) "Desactivar" else "Activar", color = Gold, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDisconnect) { Text("Salir", color = Red, fontSize = 12.sp) }
                }
            }
        }

        ui.promotionPending?.let {
            PromotionPieceDialog(
                localColor = ui.localColor,
                pieceSet = pieceSet,
                onPromote = onPromotion,
                onCancel = onCancelPromotion
            )
        }

        // Cuenta regresiva 3-2-1 — visible y sincronizada en los DOS móviles
        ui.countdown?.let { n ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB30B0D10))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("COMENZAR", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    Text("$n", color = Gold, fontSize = 150.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 150.sp)
                    Text(
                        if (ui.localColor == ChessColor.WHITE) "Blancas mueven primero" else "Negras mueven segundo",
                        color = Text1, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        ui.gameEnd?.let { end ->
            // Determine winner name and photo
            val resultColorStr = if (end.winner == null) "draw" else
                (if (end.winner == ChessColor.WHITE) "white" else "black")
            val winnerName = when (resultColorStr) {
                "white" -> ui.localName
                "black" -> ui.remoteName
                else -> null
            }
            val winnerPhoto = when (resultColorStr) {
                "white" -> localPhotoBase64
                "black" -> remoteProfile?.photoBase64
                else -> null
            }
            
            GameEndDialog(
                end = end,
                isHost = ui.lobby.isHost,
                tournamentActive = ui.tournament.active && !ui.tournament.finished,
                tournamentFinished = ui.tournament.finished,
                localName = ui.localName,
                remoteName = ui.remoteName,
                localPhotoBase64 = localPhotoBase64,
                remotePhotoBase64 = remoteProfile?.photoBase64,
                winnerName = winnerName,
                onNewGameSame = onNewGameSame,
                onNewGameRandom = onNewGameRandom,
                onTournamentNext = onTournamentNext,
                onBackToRoom = onBackToRoom,
                onReplayPgn = { /* handled in MainActivity */ },
                onNewGameCustomTime = { timeMs, incMs -> /* handled in MainActivity */ },
                onDismiss = onGameEndDismiss
            )
        }

        if (ui.tournament.finished && !ui.tournament.active) {
            TournamentEndDialog(
                totalGames = ui.tournament.totalGames,
                localWins = ui.tournament.localWins,
                remoteWins = ui.tournament.remoteWins,
                draws = ui.tournament.draws,
                timeControl = if (ui.tournament.config.timeMs == 0L) "Ilimitado" else "${ui.tournament.config.timeMs / 60000} + ${ui.tournament.config.incrementMs / 1000}",
                onNewTournament = onNewTournament,
                onBackToRoom = onBackToRoom,
                onDismiss = onGameEndDismiss
            )
        }
    }
}

private fun statusText(ui: UiGameState): String = when {
    ui.gameEnd != null -> ui.gameEnd.message
    ui.paused -> "En pausa por ${ui.pauseBy}"
    ui.inCheck -> if (ui.snapshot.sideToMove == ui.localColor) "¡Jaque! Protege a tu rey" else "¡Jaque! Ataca"
    ui.snapshot.sideToMove == ui.localColor -> "Tu turno · mueve una pieza"
    else -> "Turno del rival"
}

@Composable
private fun CompactHud(ui: UiGameState, onExit: () -> Unit) {
    val remoteTurn = ui.snapshot.sideToMove == ui.remoteColor
    val remoteClock = if (ui.remoteColor == ChessColor.WHITE) ui.clock.whiteMs else ui.clock.blackMs
    val localClock = if (ui.localColor == ChessColor.WHITE) ui.clock.whiteMs else ui.clock.blackMs

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11161D), RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(Color(0xFFFF8A8A), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text = ui.remoteName.ifBlank { "Rival" },
            color = Text1,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        HudClock(ms = remoteClock, active = remoteTurn, visible = ui.lobby.timeMs > 0)
        Spacer(Modifier.width(8.dp))
        Text("VS", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(8.dp))
        HudClock(ms = localClock, active = !remoteTurn, visible = ui.lobby.timeMs > 0)
        Spacer(Modifier.width(6.dp))
        Text(
            text = ui.localName.ifBlank { "Tú" },
            color = Text1,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0xFF161C24), RoundedCornerShape(9.dp))
                .border(1.dp, Line2, RoundedCornerShape(9.dp))
                .clickable { onExit() },
            contentAlignment = Alignment.Center
        ) {
            Text("Salir", color = Color(0xFFC9D2DE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LowTimeBlink(isLow: Boolean): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "ClockBlink")
    val blink by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut)),
        label = "ClockBlinkAlpha"
    )
    return if (isLow) 0.35f + 0.65f * (1f - blink) else 1f
}

@Composable
private fun HudClock(ms: Long, active: Boolean, visible: Boolean) {
    if (!visible) return
    val low = ms < 30_000
    Box(
        modifier = Modifier
            .background(
                if (active) Brush.linearGradient(listOf(Green, Color(0xFF7AF0BC)))
                else Brush.linearGradient(listOf(Color(0xFF0B1016), Color(0xFF0B1016))),
                RoundedCornerShape(9.dp)
            )
            .border(
                1.dp,
                when {
                    active && low -> Color(0xFF8A2E2E)
                    active -> Color(0xFF2FA37E)
                    low -> Color(0xFF4A2626)
                    else -> Color(0xFF2A3341)
                },
                RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = formatClock(ms),
            color = if (active) Color(0xFF06231A) else if (low) Red else Color(0xFFDCE3EC),
            modifier = Modifier.alpha(LowTimeBlink(low)),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ActionBtn(label: String, weak: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .background(
                if (weak) Brush.linearGradient(listOf(Color(0xFF1A212B), Color(0xFF12171F)))
                else Brush.linearGradient(listOf(Color(0xFF2A2412), Color(0xFF1D1A10))),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, if (weak) Line2 else Color(0xFF3A3120), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (weak) Text1 else Gold,
            maxLines = 1
        )
    }
}

@Composable
private fun PlayerBar(
    name: String,
    isYou: Boolean,
    isTurn: Boolean,
    clockMs: Long,
    totalMs: Long,
    showClock: Boolean,
    inCheck: Boolean,
    sideLabel: String,
    photoBase64: String = "",
    remotePhoto: String? = null,
    onRivalTap: (() -> Unit)? = null
) {
    val accentColor = if (isTurn) Gold else Color(0xFF2B3542)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141A22), RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isTurn) Color(0xFF2E3A26) else Color(0xFF232C38),
                RoundedCornerShape(14.dp)
            )
            .then(if (!isYou && onRivalTap != null) Modifier.clickable { onRivalTap() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active-turn accent bar
        Box(
            Modifier
                .width(3.dp)
                .height(30.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(9.dp))

        // Circular avatar + online dot
        val avatarPhoto = if (isYou) photoBase64 else (remotePhoto ?: "")
        val bmp = remember(avatarPhoto) { decodePhoto(avatarPhoto) }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isYou) Color(0xFF15251D) else Color(0xFF251A1A))
                .border(
                    2.dp,
                    if (isYou) Green.copy(alpha = 0.75f) else Color(0xFFFF8A8A).copy(alpha = 0.75f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    text = (name.firstOrNull() ?: if (isYou) 'T' else 'R').uppercaseChar().toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isYou) Green else Color(0xFFFF9B9B)
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .background(Green, CircleShape)
                    .border(2.dp, Color(0xFF12171F), CircleShape)
            )
        }
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = if (inCheck) Red else Text1,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = sideLabel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isYou) Color(0xFF9BE3BD) else Color(0xFF8FD3FF),
                    modifier = Modifier
                        .background(
                            if (isYou) Color(0xFF12241B) else Color(0xFF122033),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (isYou) Color(0xFF1E3B2B) else Color(0xFF1E3A54),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
            Text(
                text = if (isYou) "TÚ · ${if (isTurn) "Jugando" else "En espera"}"
                       else "RIVAL · ${if (isTurn) "Jugando" else "En espera"} · toca para ver su perfil",
                fontSize = 10.sp,
                color = if (isTurn) Green else Muted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        if (showClock) {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isTurn) Brush.linearGradient(listOf(Green, Color(0xFF7AF0BC)))
                            else Brush.linearGradient(listOf(Color(0xFF0B1016), Color(0xFF0B1016))),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isTurn) Color(0xFF2FA37E) else Color(0xFF2A3341),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatClock(clockMs),
                        color = if (isTurn) Color(0xFF06231A) else if (clockMs < 30_000) Red else Color(0xFFDCE3EC),
                        modifier = Modifier.alpha(LowTimeBlink(clockMs < 30_000)),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(84.dp)
                        .height(3.dp)
                        .background(Color(0xFF202834), RoundedCornerShape(2.dp))
                ) {
                    val fraction = if (totalMs > 0) (clockMs.toFloat() / totalMs).coerceIn(0f, 1f) else 1f
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(
                                if (isTurn) Brush.horizontalGradient(listOf(Green, Color(0xFF7AF0BC)))
                                else Brush.horizontalGradient(listOf(Color(0xFF202834), Color(0xFF202834))),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

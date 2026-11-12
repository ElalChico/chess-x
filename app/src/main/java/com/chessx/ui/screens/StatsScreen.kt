/*
 * CHESS-X · © Elal Chico
 * Estadísticas del jugador: gráfico de líneas (progreso), gráfico de barras
 * (resultados por color) y desglose numérico.
 */

package com.chessx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessx.data.GameRecord

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
private val Grey = Color(0xFF8B96A5)

/** Resumen de resultados calculado a partir del historial local. */
data class PlayerStats(
    val total: Int = 0,
    val winsWhite: Int = 0,
    val lossesWhite: Int = 0,
    val drawsWhite: Int = 0,
    val winsBlack: Int = 0,
    val lossesBlack: Int = 0,
    val drawsBlack: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    /** Puntuación acumulada por partida (victoria 1, tablas 0.5, derrota 0). */
    val points: List<Float> = emptyList(),
    /** Resultado de cada partida: 1 victoria, 0 tablas, -1 derrota. */
    val results: List<Int> = emptyList(),
    /** Etiqueta de cada partida (fecha corta). */
    val labels: List<String> = emptyList()
) {
    val winRate: Int get() = if (total == 0) 0 else (wins * 100) / total

    companion object {
        fun from(history: List<GameRecord>): PlayerStats {
            // El historial viene del mas reciente al mas antiguo: se invierte para el grafico.
            val ordered = history.sortedBy { it.dateMs }
            var acc = 0f
            val points = ArrayList<Float>(ordered.size)
            val results = ArrayList<Int>(ordered.size)
            val labels = ArrayList<String>(ordered.size)
            var ww = 0; var lw = 0; var dw = 0
            var wb = 0; var lb = 0; var db = 0
            for (g in ordered) {
                when (g.localWon) {
                    true -> { acc += 1f; results.add(1); if (g.localColorWhite) ww++ else wb++ }
                    false -> { results.add(-1); if (g.localColorWhite) lw++ else lb++ }
                    null -> { acc += 0.5f; results.add(0); if (g.localColorWhite) dw++ else db++ }
                }
                points.add(acc)
                labels.add(g.dateLabel().substringBefore(" "))
            }
            return PlayerStats(
                total = ordered.size,
                winsWhite = ww, lossesWhite = lw, drawsWhite = dw,
                winsBlack = wb, lossesBlack = lb, drawsBlack = db,
                wins = ww + wb, losses = lw + lb, draws = dw + db,
                points = points, results = results, labels = labels
            )
        }
    }
}

/**
 * Pantalla de estadisticas con navegacion por el historico.
 * @param history partidas guardadas (de la mas reciente a la mas antigua)
 */
@Composable
fun StatsScreen(
    history: List<GameRecord>,
    onBack: () -> Unit
) {
    val stats = remember(history) { PlayerStats.from(history) }
    // Ventana visible del grafico: siempre arranca en la parte mas reciente.
    var offset by remember { mutableIntStateOf(0) }
    var chartMode by remember { mutableIntStateOf(0) } // 0 = progreso, 1 = resultados

    val windowSize = 12
    val n = stats.points.size
    val maxOffset = (n - 1).coerceAtLeast(0)
    // La ventana muestra las ULTIMAS partidas; con offset > 0 se retrocede.
    val end = (n - 1 - offset).coerceAtLeast(0)
    val start = (end - windowSize + 1).coerceAtLeast(0)
    val visible = if (n == 0) emptyList() else stats.points.subList(start, end + 1)
    val visibleLabels = if (n == 0) emptyList() else stats.labels.subList(start, end + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
            Text("← Volver al perfil", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("Estadísticas", color = Text1, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Cómo llevas tus partidas: progreso, resultados y desglose por color.",
            color = Muted, fontSize = 11.5.sp
        )

        if (stats.total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(16.dp))
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Todavía no hay partidas en el historial.\nJuega una partida por Bluetooth para ver tus estadísticas.",
                    color = Muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Column
        }

        // ---- Resumen ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Partidas", "${stats.total}", Muted, Modifier.weight(1f))
            StatCard("Victorias", "${stats.wins}", Green, Modifier.weight(1f))
            StatCard("Derrotas", "${stats.losses}", Red, Modifier.weight(1f))
            StatCard("Tablas", "${stats.draws}", Grey, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("% victorias", "${stats.winRate}%", Gold, Modifier.weight(1f))
            StatCard(
                "Puntos",
                String.format("%.1f", stats.points.lastOrNull() ?: 0f),
                Gold, Modifier.weight(1f)
            )
            StatCard(
                "Racha",
                streakText(stats),
                if (streak(stats) >= 0) Green else Red, Modifier.weight(1f)
            )
            StatCard("Con blancas", "${stats.winsWhite}V ${stats.drawsWhite}E ${stats.lossesWhite}D", Muted, Modifier.weight(1f))
        }

        // ---- Grafico ----
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (chartMode == 0) "Progreso (puntos acumulados)" else "Resultados por color",
                    color = Text1, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                )
                // Cambio de vista
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniToggle("Progreso", chartMode == 0) { chartMode = 0 }
                    MiniToggle("Barras", chartMode == 1) { chartMode = 1 }
                }
            }

            if (chartMode == 0) {
                LineChart(
                    values = visible,
                    labels = visibleLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .padding(top = 10.dp)
                )
                // Navegacion por el historico (siempre se puede volver a la ultima)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NavBtn("◀") { if (offset < maxOffset) offset++ }
                    Text(
                        if (n == 0) "—" else "${stats.labels.getOrNull(start) ?: ""} → ${stats.labels.getOrNull(end) ?: ""}",
                        color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    NavBtn("▶") { if (offset > 0) offset-- }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF26200F), RoundedCornerShape(9.dp))
                            .border(1.dp, Gold, RoundedCornerShape(9.dp))
                            .clickable { offset = 0 }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text("Última", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (offset == 0) "Mostrando las últimas partidas."
                    else "Mostrando hace $offset partida(s). Pulsa «Última» para volver.",
                    color = Muted.copy(alpha = 0.75f), fontSize = 9.5.sp, modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                BarsChart(
                    whiteWins = stats.winsWhite, whiteDraws = stats.drawsWhite, whiteLosses = stats.lossesWhite,
                    blackWins = stats.winsBlack, blackDraws = stats.drawsBlack, blackLosses = stats.lossesBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .padding(top = 10.dp)
                )
            }
        }

        // ---- Desglose numerico ----
        SectionCard {
            Text("Desglose", color = Text1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            BreakdownRow("Tablas (total)", stats.draws, Grey)
            BreakdownRow("Ganadas con blancas", stats.winsWhite, Green)
            BreakdownRow("Perdidas con blancas", stats.lossesWhite, Red)
            BreakdownRow("Tablas con blancas", stats.drawsWhite, Grey)
            BreakdownRow("Ganadas con negras", stats.winsBlack, Green)
            BreakdownRow("Perdidas con negras", stats.lossesBlack, Red)
            BreakdownRow("Tablas con negras", stats.drawsBlack, Grey)
        }
    }
}

/** Racha actual de resultados (positiva = victorias, negativa = derrotas). */
private fun streak(stats: PlayerStats): Int {
    val r = stats.results
    if (r.isEmpty()) return 0
    val sign = r.last()
    var count = 0
    for (i in r.indices.reversed()) {
        if (r[i] != sign) break
        count++
    }
    return if (sign >= 0) count else -count
}

private fun streakText(stats: PlayerStats): String {
    val s = streak(stats)
    return when {
        s > 0 -> "+$s"
        s < 0 -> "$s"
        else -> "0"
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Card, RoundedCornerShape(13.dp))
            .border(1.dp, Line, RoundedCornerShape(13.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun BreakdownRow(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(9.dp))
        Text(label, color = Text1, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("$value", color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MiniToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(9.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (selected) Gold else Text1, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NavBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(Card2, RoundedCornerShape(9.dp))
            .border(1.dp, Line2, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Text1, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

/** Crea un Paint de Android para rotular los graficos. */
private fun chartPaint(
    color: Int,
    textSize: Float,
    align: android.graphics.Paint.Align = android.graphics.Paint.Align.LEFT,
    bold: Boolean = false
): android.graphics.Paint {
    val p = android.graphics.Paint()
    p.isAntiAlias = true
    p.textSize = textSize
    p.color = color
    p.textAlign = align
    p.isFakeBoldText = bold
    return p
}

/** Grafico de lineas: puntos acumulados partida a partida. */
@Composable
private fun LineChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val padL = 26f
        val padR = 8f
        val padT = 10f
        val padB = 20f
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        val canvas = drawContext.canvas.nativeCanvas

        // Rejilla
        for (i in 0..4) {
            val y = padT + h * i / 4f
            drawLine(Color(0xFF232C38), Offset(padL, y), Offset(padL + w, y), strokeWidth = 1f)
            val v = maxV * (4 - i) / 4f
            val txt = if (maxV <= 3f) String.format("%.1f", v) else String.format("%.0f", v)
            canvas.drawText(txt, 2f, y + 7f, chartPaint(0x968B96A5.toInt(), 22f))
        }

        val stepX = if (values.size > 1) w / (values.size - 1) else 0f
        fun px(i: Int) = padL + stepX * i
        fun py(v: Float) = padT + h - (v / maxV) * h

        // Area bajo la linea
        val area = Path().apply {
            moveTo(px(0), py(values[0]))
            values.forEachIndexed { i, v -> lineTo(px(i), py(v)) }
            lineTo(px(values.size - 1), padT + h)
            lineTo(px(0), padT + h)
            close()
        }
        drawPath(area, Color(0x22E2B94E))

        // Linea
        val path = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(px(i), py(v)) else path.lineTo(px(i), py(v))
        }
        drawPath(path, Gold, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Puntos
        values.forEachIndexed { i, v ->
            drawCircle(Gold, radius = 3.5f, center = Offset(px(i), py(v)))
        }

        // Etiquetas del eje X (primera y ultima)
        if (labels.isNotEmpty()) {
            val p = chartPaint(0x968B96A5.toInt(), 20f)
            canvas.drawText(labels.first(), padL, size.height - 4f, p)
            val last = labels.last()
            canvas.drawText(last, size.width - p.measureText(last) - 4f, size.height - 4f, p)
        }
    }
}

/** Grafico de barras: victorias, tablas y derrotas con blancas y con negras. */
@Composable
private fun BarsChart(
    whiteWins: Int, whiteDraws: Int, whiteLosses: Int,
    blackWins: Int, blackDraws: Int, blackLosses: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val groups = listOf(
            "Blancas" to listOf(whiteWins to Green, whiteDraws to Grey, whiteLosses to Red),
            "Negras" to listOf(blackWins to Green, blackDraws to Grey, blackLosses to Red)
        )
        val maxV = maxOf(
            whiteWins + whiteDraws + whiteLosses,
            blackWins + blackDraws + blackLosses,
            1
        )
        val padL = 26f
        val padR = 8f
        val padT = 14f
        val padB = 26f
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        val canvas = drawContext.canvas.nativeCanvas

        for (i in 0..4) {
            val y = padT + h * i / 4f
            drawLine(Color(0xFF232C38), Offset(padL, y), Offset(padL + w, y), strokeWidth = 1f)
            val v = maxV * (4 - i) / 4f
            canvas.drawText(v.toInt().toString(), 2f, y + 7f, chartPaint(0x968B96A5.toInt(), 22f))
        }

        val groupW = w / groups.size
        val barW = (groupW - 26f) / 3f
        groups.forEachIndexed { gi, group ->
            val (title, bars) = group
            val gx = padL + groupW * gi + 13f
            bars.forEachIndexed { bi, bar ->
                val value = bar.first
                val color = bar.second
                val bh = (value.toFloat() / maxV) * h
                val x = gx + barW * bi + 2f
                val y = padT + h - bh
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barW - 4f, bh.coerceAtLeast(if (value > 0) 3f else 0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                if (value > 0) {
                    canvas.drawText(
                        value.toString(), x + (barW - 4f) / 2f, y - 6f,
                        chartPaint(0xFFFFFFFF.toInt(), 26f, android.graphics.Paint.Align.CENTER, true)
                    )
                }
            }
            canvas.drawText(
                title, padL + groupW * gi + groupW / 2f, size.height - 6f,
                chartPaint(0xB8EEF1F5.toInt(), 26f, android.graphics.Paint.Align.CENTER, true)
            )
        }
    }
}

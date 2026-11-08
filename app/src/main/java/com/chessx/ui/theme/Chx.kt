/*
 * CHESS-X · © Elal Chico
 * Sistema de diseño unificado: botones, campos y tarjetas.
 */
package com.chessx.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta única de toda la app
val Bg = Color(0xFF0B0D10)
val Card = Color(0xFF12141A)
val Card2 = Color(0xFF151C25)
val Field = Color(0xFF0F141B)
val Line = Color(0xFF232C38)
val Line2 = Color(0xFF2F3947)
val Text1 = Color(0xFFEEF1F5)
val Muted = Color(0xFF8B96A5)
val Gold = Color(0xFFE2B94E)
val GoldSoft = Color(0xFFA8975E)
val GoldBorder = Color(0xFF3A3524)
val GoldDim = Color(0xFF26200F)
val Green = Color(0xFF3DDC97)
val Blue = Color(0xFF5B9DF9)
val Red = Color(0xFFFF6B6B)

const val APP_SIGN = "CHESS-X · © Elal Chico"

/** Botón primario: negro, texto dorado, borde fino dorado. */
@Composable
fun ChxButton(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val border = when {
        !enabled -> Color(0xFF24231C)
        highlight -> Gold
        else -> GoldBorder
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .background(Color(0xFF0F0F0F), RoundedCornerShape(14.dp))
            .border(1.2.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Gold else Color(0xFF6B6244)
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) GoldSoft else Color(0xFF4A4636),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Botón secundario compacto. */
@Composable
fun ChxChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (selected) GoldDim else Card2, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Gold else Text1,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Tarjeta de información con cabecera de sección. */
@Composable
fun ChxSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun ChxSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Muted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

/** Campo de texto profesional. */
@Composable
fun ChxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLength: Int = 18,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Field, RoundedCornerShape(12.dp))
            .border(1.dp, Line2, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = Text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Gold),
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}

/** Interruptor con estilo de la app. */
@Composable
fun ChxSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val track = if (checked) Color(0xFF173B28) else Color(0xFF232A33)
    val thumb = if (checked) Green else Color(0xFF8892A0)
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 26.dp)
            .background(track, CircleShape)
            .border(1.dp, if (checked) Color(0xFF2E5B3E) else Line2, CircleShape)
            .clickable { onChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(start = if (checked) 22.dp else 4.dp)
                .size(18.dp)
                .background(thumb, CircleShape)
        )
    }
}

/** Fila de ajuste: título, subtítulo y control a la derecha. */
@Composable
fun ChxToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Text1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        ChxSwitch(checked, onChange)
    }
}

/** Fila de acción (opciones de ajustes / menús). */
@Composable
fun ChxActionRow(
    title: String,
    subtitle: String,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(12.dp))
            .border(1.dp, if (danger) Color(0xFF3A2222) else Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) Color(0xFFFF9B9B) else Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Text("→", color = if (danger) Color(0xFFFF9B9B) else Gold, fontWeight = FontWeight.ExtraBold)
    }
}

/** Contador con botones − / +. */
@Composable
fun ChxStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    Column(
        modifier
            .background(Card2, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(30.dp).background(Color(0xFF10151C), RoundedCornerShape(8.dp))
                    .border(1.dp, Line2, RoundedCornerShape(8.dp)).clickable { if (value > min) onChange(value - 1) },
                contentAlignment = Alignment.Center
            ) { Text("−", color = Text1, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Text("$value", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
            Box(
                Modifier.size(30.dp).background(Color(0xFF10151C), RoundedCornerShape(8.dp))
                    .border(1.dp, Line2, RoundedCornerShape(8.dp)).clickable { if (value < max) onChange(value + 1) },
                contentAlignment = Alignment.Center
            ) { Text("+", color = Text1, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
    }
}

/** Insinia pequeña de texto. */
@Composable
fun ChxBadge(text: String, color: Color, bg: Color, border: Color) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Estado retenido simple para formularios. */
@Composable
fun rememberText(initial: String): MutableTextState {
    val state = remember { mutableStateOf(initial) }
    return MutableTextState(state)
}

class MutableTextState(private val backing: androidx.compose.runtime.MutableState<String>) {
    var value: String
        get() = backing.value
        set(v) { backing.value = v }
}

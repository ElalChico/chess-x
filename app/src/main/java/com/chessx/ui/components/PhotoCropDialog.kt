/*
 * CHESS-X · © Elal Chico
 * Diálogo de recorte de foto de perfil.
 * Permite acercar (pellizco) y arrastrar para posicionar la foto;
 * el recuadro de recorte se mantiene SIEMPRE centrado.
 * La foto inicial se centra y ajusta para caber completa dentro del recuadro.
 */

package com.chessx.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Diálogo para recortar la foto de perfil.
 * - Zoom con dos dedos y doble toque.
 * - Arrastrar para mover la foto y posicionarla.
 * - El recuadro de recorte está SIEMPRE centrado en pantalla.
 * - [onConfirm] recibe un Bitmap cuadrado ya recortado.
 */
@Composable
fun PhotoCropDialog(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val maxW = maxWidth
            val maxH = maxHeight

            // Recuadro cuadrado SIEMPRE centrado, con margen respecto a la pantalla
            val frameSide: Dp = (if (maxW < maxH) maxW else maxH) * 0.78f
            val frameSizePx = with(density) { frameSide.toPx() }

            // Zoom y desplazamiento de la foto (estado retenido)
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            // Límites de arrastre: la foto nunca se sale del recuadro.
            // ContentScale.Fit dentro de un área de frameSizePx: calculamos el
            // tamaño dibujado para permitir desplazar sólo el desbordamiento real.
            val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
            val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
            val fitScale = remember(frameSizePx, srcW, srcH) { min(frameSizePx / srcW, frameSizePx / srcH) }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Ajusta tu foto",
                    color = Color(0xFFEEF1F5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Pellizca para acercar · arrastra para posicionar",
                    color = Color(0xFF8B96A5),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Zona interactiva: mide EXACTAMENTE igual que el recuadro de
                // recorte, para que el recorte calculado sea exacto.
                Box(
                    modifier = Modifier
                        .size(frameSide)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B0D10)),
                    contentAlignment = Alignment.Center
                ) {
                    // La foto, escalada y desplazada por gestos
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto a recortar",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    // Límite: el recuadro nunca sale de la foto
                                    val drawnW = srcW * fitScale * scale
                                    val drawnH = srcH * fitScale * scale
                                    val maxX = max(0f, (drawnW - frameSizePx) / 2f)
                                    val maxY = max(0f, (drawnH - frameSizePx) / 2f)
                                    val newX = (offset.x + pan.x).coerceIn(-maxX, maxX)
                                    val newY = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    offset = Offset(newX, newY)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.05f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.2f
                                        }
                                    }
                                )
                            },
                        alpha = 1f
                    )

                    // Máscara oscura alrededor del recuadro centrado
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val half = frameSizePx / 2f
                        // 4 rectángulos cubriendo todo menos el cuadrado central
                        drawRect(color = Color(0x99000000), topLeft = Offset.Zero, size = Size(cx - half, size.height))
                        drawRect(color = Color(0x99000000), topLeft = Offset(cx + half, 0f), size = Size(size.width - cx - half, size.height))
                        drawRect(color = Color(0x99000000), topLeft = Offset(cx - half, 0f), size = Size(half * 2f, cy - half))
                        drawRect(color = Color(0x99000000), topLeft = Offset(cx - half, cy + half), size = Size(half * 2f, size.height - cy - half))
                        // Borde dorado del recuadro (siempre centrado)
                        drawRect(
                            color = Color(0xFFE2B94E),
                            topLeft = Offset(cx - half, cy - half),
                            size = Size(half * 2f, half * 2f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Círculo guía central (la foto se recorta cuadrada; el avatar es redondo)
                    Canvas(modifier = Modifier.size(frameSide)) {
                        drawCircle(
                            color = Color(0x33FFFFFF),
                            radius = size.minDimension / 2f - 1.dp.toPx(),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botones: recorte real centrado sobre la foto transformada
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C25), contentColor = Color(0xFFEEF1F5))
                    ) { Text("Cancelar", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = {
                            val cropped = buildCroppedBitmap(
                                source = bitmap,
                                viewSizePx = frameSizePx,
                                scale = scale,
                                offsetPx = offset
                            )
                            onConfirm(cropped)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2B94E), contentColor = Color(0xFF0B0D10))
                    ) { Text("Guardar", fontWeight = FontWeight.ExtraBold) }
                }
                Text(
                    "El recuadro se mantiene centrado",
                    color = Color(0xFF6B6244),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/**
 * Recorta el cuadrado centrado considerando zoom y desplazamiento.
 * La foto se dibuja con ContentScale.Fit dentro de [viewSizePx]; se reconstruye
 * la matriz equivalente para extraer exactamente esa región central.
 */
private fun buildCroppedBitmap(
    source: Bitmap,
    viewSizePx: Float,
    scale: Float,
    offsetPx: Offset
): Bitmap {
    // Área donde se dibujó la imagen (Fit): centrada, manteniendo relación de aspecto
    val bmpW = source.width.toFloat()
    val bmpH = source.height.toFloat()
    val fitScale = min(viewSizePx / bmpW, viewSizePx / bmpH)
    val drawnW = bmpW * fitScale * scale
    val drawnH = bmpH * fitScale * scale

    // Origen (esquina superior izq.) de la imagen dibujada, centrada + offset
    val left = viewSizePx / 2f - drawnW / 2f + offsetPx.x
    val top = viewSizePx / 2f - drawnH / 2f + offsetPx.y
    val right = left + drawnW
    val bottom = top + drawnH

    // Región visible del recuadro centrado
    val half = viewSizePx / 2f
    val visLeft = viewSizePx / 2f - half
    val visTop = viewSizePx / 2f - half
    val visRight = visLeft + viewSizePx
    val visBottom = visTop + viewSizePx

    // Coordenadas de la región visible relativas a la imagen dibujada
    val srcLeftAbs = ((visLeft - left) / (drawnW / bmpW)).coerceIn(0f, bmpW)
    val srcTopAbs = ((visTop - top) / (drawnH / bmpH)).coerceIn(0f, bmpH)
    val srcRightAbs = ((visRight - left) / (drawnW / bmpW)).coerceIn(0f, bmpW)
    val srcBottomAbs = ((visBottom - top) / (drawnH / bmpH)).coerceIn(0f, bmpH)

    var cropW = abs(srcRightAbs - srcLeftAbs)
    var cropH = abs(srcBottomAbs - srcTopAbs)
    // Si la imagen es más estrecha que el recuadro, ajustar a cuadrado sobre el ancho visible
    if (cropW < 1f || cropH < 1f) {
        // fallback: centro de la imagen
        val s = min(bmpW, bmpH)
        val cx = bmpW / 2f
        val cy = bmpH / 2f
        return Bitmap.createBitmap(source, (cx - s / 2f).toInt().coerceIn(0, max(0, source.width - s.toInt())), (cy - s / 2f).toInt().coerceIn(0, max(0, source.height - s.toInt())), s.toInt().coerceAtMost(source.width), s.toInt().coerceAtMost(source.height))
    }
    // Cuadrado exacto: usar el lado menor
    val side = min(cropW, cropH)
    cropW = side
    cropH = side

    val x = srcLeftAbs.toInt().coerceIn(0, max(0, source.width - side.toInt()))
    val y = srcTopAbs.toInt().coerceIn(0, max(0, source.height - side.toInt()))
    val s = side.toInt().coerceIn(1, min(source.width, source.height))
    return Bitmap.createBitmap(source, x, y, s, s)
}

/** Utilidad: reduce un bitmap grande antes de mostrar el diálogo. */
fun downscale(src: Bitmap, maxSide: Int): Bitmap {
    val w = src.width
    val h = src.height
    val m = max(w, h)
    if (m <= maxSide) return src
    val f = maxSide.toFloat() / m
    val matrix = Matrix().apply { postScale(f, f) }
    return Bitmap.createBitmap(src, 0, 0, w, h, matrix, true)
}

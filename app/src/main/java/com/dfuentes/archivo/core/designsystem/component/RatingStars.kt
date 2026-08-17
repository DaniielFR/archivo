package com.dfuentes.archivo.core.designsystem.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import kotlin.math.roundToInt

/**
 * Nota de 0 a 10 en medias estrellas (10 = 5 estrellas llenas).
 *
 * Se almacena como Int y no como Float a propósito: evita ambigüedades de
 * comparación y de redondeo en las estadísticas.
 *
 * @param rating 0..10, o null si la obra no está puntuada.
 * @param onRatingChange null ⇒ modo solo lectura (tarjetas, listas).
 */
@Composable
fun RatingStars(
    rating: Int?,
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,
    starSize: androidx.compose.ui.unit.Dp = 24.dp,
    emptyColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val filledColor = LocalContentColor.current
    val haptics = LocalHapticFeedback.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    val current = rating ?: 0

    fun ratingFromX(x: Float): Int {
        if (widthPx <= 0f) return current
        val raw = (x / widthPx * 10f).roundToInt().coerceIn(0, 10)
        return raw
    }

    // Se captura en un local: el smart cast no cruza la frontera de la lambda,
    // y esto evita los !! que ensuciaban el fichero.
    val onChange = onRatingChange
    val description = if (rating == null) {
        "Sin puntuar"
    } else {
        "Puntuación: ${rating / 2f} de 5 estrellas"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp) // objetivo táctil mínimo, aunque los iconos midan menos
            .then(
                if (onChange == null) Modifier else Modifier.pointerInput(Unit) {
                    widthPx = size.width.toFloat()
                    detectTapGestures { offset ->
                        val next = ratingFromX(offset.x)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChange(next)
                    }
                },
            )
            .then(
                if (onChange == null) Modifier else Modifier.pointerInput(Unit) {
                    widthPx = size.width.toFloat()
                    var last = current
                    detectHorizontalDragGestures { change, _ ->
                        val next = ratingFromX(change.position.x)
                        if (next != last) {
                            last = next
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onChange(next)
                        }
                    }
                },
            )
            .clearAndSetSemantics { contentDescription = description },
    ) {
        repeat(5) { index ->
            val halvesForThisStar = current - index * 2
            val icon = when {
                halvesForThisStar >= 2 -> Icons.Filled.Star
                halvesForThisStar == 1 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (halvesForThisStar >= 1) filledColor else emptyColor,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingStarsPreview() {
    ArchivoTheme {
        Row {
            RatingStars(rating = 7)
        }
    }
}

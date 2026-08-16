package com.dfuentes.archivo.core.designsystem.component

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status

/**
 * Acento por tipo de medio. Uso deliberadamente contenido —icono y detalle,
 * nunca fondo—: tres colores de fondo compitiendo en la misma rejilla es
 * exactamente el aspecto que queríamos evitar.
 */
@Composable
fun MediaType.accentColor(): Color = when (this) {
    MediaType.BOOK -> MaterialTheme.colorScheme.tertiary
    MediaType.MOVIE -> MaterialTheme.colorScheme.secondary
    MediaType.SERIES -> MaterialTheme.colorScheme.primary
}

@Composable
fun MediaTypeChip(
    type: MediaType?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(type?.displayName ?: "Todo") },
        modifier = modifier,
    )
}

@Composable
fun StatusChip(
    status: Status?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(status?.displayName ?: "Cualquier estado") },
        modifier = modifier,
    )
}

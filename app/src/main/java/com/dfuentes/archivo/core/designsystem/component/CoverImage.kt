package com.dfuentes.archivo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImage

/**
 * Portada con reserva elegante.
 *
 * `path` es la copia LOCAL (filesDir/covers). La URL remota nunca se pinta
 * directamente: si la API desaparece, la app tiene que seguir viéndose igual.
 * Mientras la descarga está en curso —o si nunca llegó— se muestra la inicial
 * sobre el color dominante, que es infinitamente mejor que un hueco gris.
 */
@Composable
fun CoverImage(
    path: String?,
    title: String,
    dominantColor: Int?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    initialStyle: TextStyle = MaterialTheme.typography.displaySmall,
) {
    val background = dominantColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = path,
                contentDescription = "Portada de $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title.take(1).uppercase(),
                style = initialStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Variante para resultados de búsqueda, que sí pintan la URL remota. */
@Composable
fun RemoteCoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = "Portada de $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

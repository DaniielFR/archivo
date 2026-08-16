package com.dfuentes.archivo.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfuentes.archivo.core.designsystem.component.EmptyState
import com.dfuentes.archivo.core.designsystem.component.MediaTypeChip
import com.dfuentes.archivo.core.designsystem.component.RatingStars
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.data.repository.WorkSummary

/**
 * Composable de RUTA: conoce el ViewModel, no dibuja nada.
 * Composable de CONTENIDO ([LibraryScreen]): dibuja todo, no conoce el ViewModel.
 *
 * Esa separación es la que permite previsualizar cada estado en el panel de
 * Android Studio y, en la fase 6, capturarlos automáticamente como tests visuales.
 */
@Composable
fun LibraryRoute(
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TypeFilterRow(
                selected = state.filter.type,
                onSelect = { onAction(LibraryAction.TypeFilterChanged(it)) },
            )

            when {
                state.isLoading -> Unit // fase 6: skeletons con la forma de las tarjetas

                state.isEmpty && state.hasActiveFilters -> EmptyState(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = "Nada con estos filtros",
                    description = "Prueba a quitarlos para ver toda tu biblioteca.",
                    actionLabel = "Quitar filtros",
                    onAction = { onAction(LibraryAction.FiltersCleared) },
                )

                state.isEmpty -> EmptyState(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = "Tu archivo está vacío",
                    description = "Aquí irá quedando todo lo que leas y veas. " +
                        "El alta de verdad llega en la fase 1.",
                    actionLabel = "Añadir datos de ejemplo",
                    onAction = { onAction(LibraryAction.SampleDataRequested) },
                )

                else -> WorkGrid(items = state.items)
            }
        }
    }
}

@Composable
private fun TypeFilterRow(
    selected: MediaType?,
    onSelect: (MediaType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaTypeChip(type = null, selected = selected == null, onClick = { onSelect(null) })
        MediaType.entries.forEach { type ->
            MediaTypeChip(
                type = type,
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
            )
        }
    }
}

@Composable
private fun WorkGrid(
    items: List<WorkSummary>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            WorkCardItem(item = item, onClick = { /* fase 1: navegar a la ficha */ })
        }
    }
}

@Composable
private fun WorkCardItem(
    item: WorkSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        // 2:3 es la proporción real tanto de las portadas de libro como de los
        // pósters de cine, así que la rejilla queda alineada sin recortar nada.
        // Cuando entren las portadas reales (fase 3), ContentScale.Crop aquí.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    item.dominantColor
                        ?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Fase 3: aquí va Coil leyendo item.coverPath.
            Text(
                text = item.title.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        item.creators?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.rating != null) {
            RatingStars(rating = item.rating, starSize = 14.dp)
        }
    }
}

// ── Previsualizaciones: una por estado. En la fase 6 estas mismas se convierten
//    automáticamente en tests de captura de pantalla. ────────────────────────
@Preview(name = "Vacío", showBackground = true)
@Composable
private fun LibraryEmptyPreview() {
    ArchivoTheme {
        LibraryScreen(state = LibraryUiState(isLoading = false), onAction = {})
    }
}

@Preview(name = "Con contenido", showBackground = true)
@Composable
private fun LibraryContentPreview() {
    ArchivoTheme {
        LibraryScreen(
            state = LibraryUiState(
                isLoading = false,
                items = listOf(
                    WorkSummary(1, MediaType.BOOK, "El nombre del viento", 2007, null, null,
                        "Patrick Rothfuss", Status.FINISHED, 9, null),
                    WorkSummary(2, MediaType.MOVIE, "La llegada", 2016, null, null,
                        "Denis Villeneuve", Status.FINISHED, 8, null),
                    WorkSummary(3, MediaType.SERIES, "Chernobyl", 2019, null, null,
                        "Craig Mazin", Status.FINISHED, 10, null),
                ),
            ),
            onAction = {},
        )
    }
}

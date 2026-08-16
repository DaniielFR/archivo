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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfuentes.archivo.core.designsystem.component.EmptyState
import com.dfuentes.archivo.core.designsystem.component.MediaTypeChip
import com.dfuentes.archivo.core.designsystem.component.RatingStars
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.SortOrder
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.data.repository.WorkSummary

/**
 * Composable de RUTA: conoce el ViewModel, no dibuja nada.
 * Composable de CONTENIDO ([LibraryScreen]): dibuja todo, no conoce el ViewModel.
 *
 * Esa separación permite previsualizar cada estado en el panel de Android Studio
 * y, en la fase 6, capturarlos automáticamente como tests visuales.
 */
@Composable
fun LibraryRoute(
    onOpenWork: (Long) -> Unit,
    onAddWork: (MediaType) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenWork = onOpenWork,
        onAddWork = onAddWork,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onOpenWork: (Long) -> Unit,
    onAddWork: (MediaType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Ordenar")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            SortOrder.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.displayName) },
                                    onClick = {
                                        onAction(LibraryAction.SortChanged(sort))
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir")
            }
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
                    description = "Aquí irá quedando todo lo que leas y veas.",
                    actionLabel = "Añadir el primero",
                    onAction = { showAddSheet = true },
                )

                else -> WorkGrid(items = state.items, onOpenWork = onOpenWork)
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            MediaType.entries.forEach { type ->
                ListItem(
                    headlineContent = { Text(type.singular) },
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        onAddWork(type)
                    },
                )
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
    onOpenWork: (Long) -> Unit,
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
            WorkCardItem(item = item, onClick = { onOpenWork(item.id) })
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
        LibraryScreen(
            state = LibraryUiState(isLoading = false),
            onAction = {}, onOpenWork = {}, onAddWork = {},
        )
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
            onAction = {}, onOpenWork = {}, onAddWork = {},
        )
    }
}

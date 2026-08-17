package com.dfuentes.archivo.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfuentes.archivo.core.designsystem.component.DateField
import com.dfuentes.archivo.core.designsystem.component.EmptyState
import com.dfuentes.archivo.core.designsystem.component.RatingStars
import com.dfuentes.archivo.core.designsystem.component.RemoteCoverImage
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import com.dfuentes.archivo.core.model.BookCandidate
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.MetadataSource
import com.dfuentes.archivo.core.model.Status

@Composable
fun SearchRoute(
    onClose: () -> Unit,
    onManualEntry: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Guardar cierra la pantalla: el objetivo son tres toques, no quedarse
    // admirando un mensaje de confirmación.
    LaunchedEffect(state.savedTitle) {
        if (state.savedTitle != null) {
            viewModel.onAction(SearchAction.SavedHandled)
            onClose()
        }
    }

    SearchScreen(
        state = state,
        onAction = viewModel::onAction,
        onClose = onClose,
        onManualEntry = onManualEntry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onClose: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Añadir libro") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onAction(SearchAction.QueryChanged(it)) },
                label = { Text("Título, autor o ISBN") },
                singleLine = true,
                trailingIcon = {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.width(20.dp))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onAction(SearchAction.SearchSubmitted) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focus),
            )

            when {
                state.offline -> EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "Sin conexión",
                    description = "No se puede buscar ahora, pero puedes añadirlo a mano " +
                        "y completar los datos más tarde.",
                    actionLabel = "Añadir a mano",
                    onAction = onManualEntry,
                )

                state.error != null -> EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = "Algo ha fallado",
                    description = state.error,
                    actionLabel = "Añadir a mano",
                    onAction = onManualEntry,
                )

                state.hasSearched && state.results.isEmpty() && !state.isSearching -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Sin resultados",
                    description = "Prueba con el ISBN, o añádelo a mano si no está en ningún catálogo.",
                    actionLabel = "Añadir a mano",
                    onAction = onManualEntry,
                )

                else -> LazyColumn {
                    items(items = state.results, key = { it.source.name + it.sourceId }) { candidate ->
                        CandidateRow(
                            candidate = candidate,
                            onClick = { onAction(SearchAction.CandidateSelected(candidate)) },
                        )
                    }
                    item {
                        TextButton(
                            onClick = onManualEntry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text("No lo encuentro · añadir a mano")
                        }
                    }
                }
            }
        }
    }

    state.selected?.let { candidate ->
        QuickLogSheet(
            candidate = candidate,
            log = state.quickLog,
            onAction = onAction,
            onDismiss = { onAction(SearchAction.SelectionDismissed) },
        )
    }
}

@Composable
private fun CandidateRow(
    candidate: BookCandidate,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(candidate.displayTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            if (candidate.subtitleLine.isNotBlank()) {
                Text(candidate.subtitleLine, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        leadingContent = {
            RemoteCoverImage(
                url = candidate.coverUrl,
                title = candidate.title,
                modifier = Modifier.width(44.dp),
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * Alta rápida. Aparece sobre los resultados sin navegar a otra pantalla: si esto
 * fuese una pantalla nueva, cada alta costaría dos toques más y una animación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogSheet(
    candidate: BookCandidate,
    log: QuickLogState,
    onAction: (SearchAction) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row {
                RemoteCoverImage(
                    url = candidate.coverUrl,
                    title = candidate.title,
                    modifier = Modifier.width(72.dp),
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(candidate.displayTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = candidate.subtitleLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Status.PENDING, Status.IN_PROGRESS, Status.FINISHED).forEach { status ->
                    FilterChip(
                        selected = log.status == status,
                        onClick = { onAction(SearchAction.StatusChanged(status)) },
                        label = { Text(status.displayName) },
                    )
                }
            }

            RatingStars(
                rating = log.rating,
                starSize = 34.dp,
                onRatingChange = { onAction(SearchAction.RatingChanged(it)) },
            )

            if (log.status == Status.FINISHED) {
                DateField(
                    label = "Terminado",
                    epochDay = log.finishedOn,
                    onChange = { onAction(SearchAction.FinishedOnChanged(it)) },
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Format.PAPER, Format.EBOOK, Format.AUDIO).forEach { format ->
                    FilterChip(
                        selected = log.format == format,
                        onClick = {
                            onAction(
                                SearchAction.FormatChanged(if (log.format == format) null else format),
                            )
                        },
                        label = { Text(format.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = log.notes,
                onValueChange = { onAction(SearchAction.NotesChanged(it)) },
                placeholder = { Text("Cuatro ideas…") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onAction(SearchAction.SaveRequested) },
                enabled = !log.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchPreview() {
    ArchivoTheme {
        SearchScreen(
            state = SearchUiState(
                query = "el nombre del viento",
                hasSearched = true,
                results = listOf(
                    BookCandidate(
                        sourceId = "1",
                        source = MetadataSource.GOOGLE_BOOKS,
                        title = "El nombre del viento",
                        authors = listOf("Patrick Rothfuss"),
                        year = 2007,
                        pageCount = 662,
                    ),
                ),
            ),
            onAction = {}, onClose = {}, onManualEntry = {},
        )
    }
}

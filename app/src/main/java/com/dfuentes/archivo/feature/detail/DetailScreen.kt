package com.dfuentes.archivo.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfuentes.archivo.core.designsystem.component.CoverImage
import com.dfuentes.archivo.core.designsystem.component.DateField
import com.dfuentes.archivo.core.designsystem.component.RatingStars
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work

@Composable
fun DetailRoute(
    workId: Long,
    onBack: () -> Unit,
    onEdit: (MediaType) -> Unit,
) {
    val viewModel: DetailViewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(workId) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Borrado con deshacer: la pantalla se cierra sola si el usuario no actúa.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is DetailEvent.Deleted) {
                val result = snackbarHostState.showSnackbar(
                    message = "Eliminado",
                    actionLabel = "Deshacer",
                    duration = androidx.compose.material3.SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onAction(DetailAction.UndoDelete)
                }
                onBack()
            }
        }
    }

    DetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onEdit = onEdit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (DetailAction) -> Unit,
    onBack: () -> Unit,
    onEdit: (MediaType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val work = state.work
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (work != null) {
                        val fav = work.currentEntry?.isFavourite == true
                        IconButton(onClick = { onAction(DetailAction.FavouriteToggled) }) {
                            Icon(
                                imageVector = if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (fav) "Quitar de favoritos" else "Marcar favorito",
                            )
                        }
                        IconButton(onClick = { onEdit(work.type) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onAction(DetailAction.DeleteRequested) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            work == null && state.isLoading -> Unit
            work == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No encontrado") }
            else -> DetailContent(
                work = work,
                onAction = onAction,
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun DetailContent(
    work: Work,
    onAction: (DetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = work.currentEntry
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        // ── Cabecera: portada + datos objetivos ──
        Row {
            CoverImage(
                path = work.coverPath,
                title = work.title,
                dominantColor = work.dominantColor,
                modifier = Modifier.width(120.dp),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(work.title, style = MaterialTheme.typography.headlineSmall)
                if (work.creators.isNotEmpty()) {
                    Text(
                        text = work.creators.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = listOfNotNull(
                        work.type.singular,
                        work.year?.toString(),
                        work.pageCount?.let { "$it pp." },
                        work.runtimeMinutes?.let { "$it min" },
                        work.seasonCount?.let { "$it temp." },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Mi registro")

        // ── Estado ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Status.entries.forEach { status ->
                FilterChip(
                    selected = entry?.status == status,
                    onClick = { onAction(DetailAction.StatusChanged(status)) },
                    label = { Text(status.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }

        // ── Nota ──
        RatingStars(
            rating = entry?.rating,
            starSize = 32.dp,
            onRatingChange = { onAction(DetailAction.RatingChanged(it)) },
            modifier = Modifier.padding(top = 8.dp),
        )

        // ── Fechas ──
        Spacer(Modifier.height(8.dp))
        DateField(
            label = "Empezado",
            epochDay = entry?.startedOn,
            onChange = { onAction(DetailAction.StartedOnChanged(it)) },
        )
        Spacer(Modifier.height(12.dp))
        DateField(
            label = "Terminado",
            epochDay = entry?.finishedOn,
            onChange = { onAction(DetailAction.FinishedOnChanged(it)) },
        )

        // ── Notas: se guardan al escribir, sin botón de guardar ──
        Spacer(Modifier.height(24.dp))
        SectionTitle("Mis notas")
        var notes by remember(entry?.id) { mutableStateOf(entry?.notes.orEmpty()) }
        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                onAction(DetailAction.NotesChanged(it))
            },
            placeholder = { Text("Cuatro ideas que no quieras olvidar…") },
            minLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        if (work.synopsis != null) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Sinopsis")
            Text(
                text = work.synopsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // ── Historial de vueltas ──
        if (work.entries.size > 1) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Historial")
            work.entries.sortedByDescending { it.round }.forEach { previous ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Vuelta ${previous.round} · ${previous.status.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { onAction(DetailAction.NewRoundRequested) }) {
                Icon(Icons.Filled.Replay, contentDescription = null)
            }
            Text(
                text = when (work.type) {
                    MediaType.BOOK -> "Registrar una relectura"
                    else -> "Registrar otro visionado"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailPreview() {
    ArchivoTheme {
        DetailScreen(
            state = DetailUiState(
                isLoading = false,
                work = Work(
                    id = 1,
                    type = MediaType.BOOK,
                    title = "El nombre del viento",
                    year = 2007,
                    pageCount = 662,
                    creators = listOf("Patrick Rothfuss"),
                    entries = listOf(
                        Entry(workId = 1, status = Status.FINISHED, rating = 9, notes = "Kvothe."),
                    ),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
            onEdit = {},
        )
    }
}

package com.dfuentes.archivo.feature.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfuentes.archivo.core.designsystem.component.DateField
import com.dfuentes.archivo.core.designsystem.component.RatingStars
import com.dfuentes.archivo.core.designsystem.theme.ArchivoTheme
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.navigation.AddEditKey

@Composable
fun AddEditRoute(
    navKey: AddEditKey,
    onDone: () -> Unit,
) {
    val viewModel: AddEditViewModel = hiltViewModel<AddEditViewModel, AddEditViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    AddEditScreen(state = state, onAction = viewModel::onAction, onClose = onDone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    state: AddEditUiState,
    onAction: (AddEditAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleFocus = remember { FocusRequester() }
    // Al abrir un alta nueva el teclado sale solo sobre el título: un toque menos
    // en el flujo que más veces vas a repetir.
    LaunchedEffect(Unit) { if (!state.isEditing) titleFocus.requestFocus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar" else "Añadir ${state.type.singular.lowercase()}") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!state.isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaType.entries.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { onAction(AddEditAction.TypeChanged(type)) },
                            label = { Text(type.singular) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = { onAction(AddEditAction.TitleChanged(it)) },
                label = { Text("Título") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.creators,
                onValueChange = { onAction(AddEditAction.CreatorsChanged(it)) },
                label = { Text(state.creatorLabel) },
                supportingText = { Text("Separa varios nombres con comas") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = state.year,
                    label = "Año",
                    onChange = { onAction(AddEditAction.YearChanged(it)) },
                    modifier = Modifier.weight(1f),
                )
                when (state.type) {
                    MediaType.BOOK -> NumberField(
                        value = state.pageCount,
                        label = "Páginas",
                        onChange = { onAction(AddEditAction.PageCountChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    MediaType.MOVIE -> NumberField(
                        value = state.runtimeMinutes,
                        label = "Minutos",
                        onChange = { onAction(AddEditAction.RuntimeChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    MediaType.SERIES -> NumberField(
                        value = state.seasonCount,
                        label = "Temporadas",
                        onChange = { onAction(AddEditAction.SeasonCountChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Mi registro")

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Status.entries.forEach { status ->
                    FilterChip(
                        selected = state.status == status,
                        onClick = { onAction(AddEditAction.StatusChanged(status)) },
                        label = { Text(status.displayName) },
                    )
                }
            }

            RatingStars(
                rating = state.rating,
                starSize = 32.dp,
                onRatingChange = { onAction(AddEditAction.RatingChanged(it)) },
            )

            DateField(
                label = "Empezado",
                epochDay = state.startedOn,
                onChange = { onAction(AddEditAction.StartedOnChanged(it)) },
            )
            Spacer(Modifier.height(12.dp))
            DateField(
                label = "Terminado",
                epochDay = state.finishedOn,
                onChange = { onAction(AddEditAction.FinishedOnChanged(it)) },
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formatsFor(state.type).forEach { format ->
                    FilterChip(
                        selected = state.format == format,
                        onClick = {
                            onAction(
                                AddEditAction.FormatChanged(
                                    if (state.format == format) null else format,
                                ),
                            )
                        },
                        label = { Text(format.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onAction(AddEditAction.NotesChanged(it)) },
                label = { Text("Notas") },
                placeholder = { Text("Cuatro ideas que no quieras olvidar…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onAction(AddEditAction.SaveRequested) },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEditing) "Guardar cambios" else "Guardar")
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun formatsFor(type: MediaType): List<Format> = when (type) {
    MediaType.BOOK -> listOf(Format.PAPER, Format.EBOOK, Format.AUDIO)
    MediaType.MOVIE -> listOf(Format.CINEMA, Format.STREAMING, Format.TV)
    MediaType.SERIES -> listOf(Format.STREAMING, Format.TV)
}

@Preview(showBackground = true)
@Composable
private fun AddEditPreview() {
    ArchivoTheme {
        AddEditScreen(
            state = AddEditUiState(title = "El nombre del viento", creators = "Patrick Rothfuss"),
            onAction = {},
            onClose = {},
        )
    }
}

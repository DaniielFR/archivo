package com.dfuentes.archivo.feature.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.data.repository.LibraryRepository
import com.dfuentes.archivo.navigation.AddEditKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@HiltViewModel(assistedFactory = AddEditViewModel.Factory::class)
class AddEditViewModel @AssistedInject constructor(
    @Assisted private val navKey: AddEditKey,
    private val repository: LibraryRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navKey: AddEditKey): AddEditViewModel
    }

    private val _uiState = MutableStateFlow(
        AddEditUiState(
            type = navKey.type,
            isEditing = navKey.workId != null,
            isLoading = navKey.workId != null,
            // En alta nueva se preselecciona "Terminado" y la fecha de hoy:
            // la inmensa mayoría de los registros son de cosas ya consumidas.
            finishedOn = if (navKey.workId == null) LocalDate.now().toEpochDay() else null,
        ),
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var editing: Work? = null

    init {
        navKey.workId?.let { id ->
            viewModelScope.launch {
                repository.getWork(id)?.let { work ->
                    editing = work
                    val entry = work.currentEntry
                    _uiState.value = AddEditUiState(
                        type = work.type,
                        isEditing = true,
                        isLoading = false,
                        title = work.title,
                        creators = work.creators.joinToString(", "),
                        year = work.year?.toString().orEmpty(),
                        pageCount = work.pageCount?.toString().orEmpty(),
                        runtimeMinutes = work.runtimeMinutes?.toString().orEmpty(),
                        seasonCount = work.seasonCount?.toString().orEmpty(),
                        synopsis = work.synopsis.orEmpty(),
                        status = entry?.status ?: Status.FINISHED,
                        rating = entry?.rating,
                        startedOn = entry?.startedOn,
                        finishedOn = entry?.finishedOn,
                        format = entry?.format,
                        notes = entry?.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onAction(action: AddEditAction) {
        when (action) {
            is AddEditAction.TypeChanged -> update { it.copy(type = action.type) }
            is AddEditAction.TitleChanged -> update { it.copy(title = action.value) }
            is AddEditAction.CreatorsChanged -> update { it.copy(creators = action.value) }
            is AddEditAction.YearChanged -> update { it.copy(year = action.value.digits(4)) }
            is AddEditAction.PageCountChanged -> update { it.copy(pageCount = action.value.digits(5)) }
            is AddEditAction.RuntimeChanged -> update { it.copy(runtimeMinutes = action.value.digits(4)) }
            is AddEditAction.SeasonCountChanged -> update { it.copy(seasonCount = action.value.digits(3)) }
            is AddEditAction.SynopsisChanged -> update { it.copy(synopsis = action.value) }
            is AddEditAction.StatusChanged -> update {
                val today = LocalDate.now().toEpochDay()
                it.copy(
                    status = action.status,
                    finishedOn = if (action.status == Status.FINISHED) it.finishedOn ?: today else it.finishedOn,
                )
            }
            is AddEditAction.RatingChanged -> update {
                it.copy(rating = if (it.rating == action.rating) null else action.rating)
            }
            is AddEditAction.StartedOnChanged -> update { it.copy(startedOn = action.epochDay) }
            is AddEditAction.FinishedOnChanged -> update { it.copy(finishedOn = action.epochDay) }
            is AddEditAction.FormatChanged -> update { it.copy(format = action.format) }
            is AddEditAction.NotesChanged -> update { it.copy(notes = action.value) }
            AddEditAction.SaveRequested -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val work = buildWork(state)
            if (state.isEditing) repository.updateWork(work) else repository.addWork(work)
            _uiState.update { it.copy(saved = true) }
        }
    }

    private fun buildWork(state: AddEditUiState): Work {
        val previous = editing
        val entry = Entry(
            id = previous?.currentEntry?.id ?: 0,
            workId = previous?.id ?: 0,
            status = state.status,
            rating = state.rating,
            startedOn = state.startedOn,
            finishedOn = state.finishedOn,
            notes = state.notes.ifBlank { null },
            format = state.format,
            isFavourite = previous?.currentEntry?.isFavourite ?: false,
            round = previous?.currentEntry?.round ?: 1,
        )
        return Work(
            id = previous?.id ?: 0,
            type = state.type,
            title = state.title.trim(),
            year = state.year.toIntOrNull(),
            synopsis = state.synopsis.ifBlank { null },
            pageCount = state.pageCount.toIntOrNull(),
            runtimeMinutes = state.runtimeMinutes.toIntOrNull(),
            seasonCount = state.seasonCount.toIntOrNull(),
            coverPath = previous?.coverPath,
            coverUrl = previous?.coverUrl,
            dominantColor = previous?.dominantColor,
            creators = state.creators.split(",").map(String::trim).filter(String::isNotEmpty),
            entries = listOf(entry),
        )
    }

    private fun update(transform: (AddEditUiState) -> AddEditUiState) = _uiState.update(transform)
}

/** Filtra a dígitos y recorta: evita que un pegado accidental meta basura. */
private fun String.digits(max: Int) = filter(Char::isDigit).take(max)

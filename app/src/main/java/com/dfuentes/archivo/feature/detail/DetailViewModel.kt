package com.dfuentes.archivo.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.data.repository.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Inyección asistida: el id de la obra llega desde la clave de navegación, no
 * desde un SavedStateHandle. Es el patrón oficial de Hilt con Navigation 3 y
 * hace que el ViewModel no pueda existir sin su argumento.
 */
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val workId: Long,
    private val repository: LibraryRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(workId: Long): DetailViewModel
    }

    private val deleted = MutableStateFlow(false)
    private var lastDeleted: Work? = null

    private val _events = MutableSharedFlow<DetailEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<DetailUiState> =
        combine(repository.workDetail(workId), deleted) { work, isDeleted ->
            DetailUiState(
                work = work,
                isLoading = false,
                notFound = work == null && !isDeleted,
                deleted = isDeleted,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    fun onAction(action: DetailAction) {
        when (action) {
            is DetailAction.StatusChanged -> launch { repository.setStatus(workId, action.status) }

            is DetailAction.RatingChanged -> launch {
                // Volver a tocar la misma nota la quita: es el gesto que espera
                // cualquiera que se haya equivocado al puntuar.
                val current = uiState.value.work?.currentEntry?.rating
                repository.setRating(workId, if (current == action.rating) null else action.rating)
            }

            is DetailAction.NotesChanged -> updateEntry { it.copy(notes = action.notes.ifBlank { null }) }

            is DetailAction.StartedOnChanged -> updateEntry { it.copy(startedOn = action.epochDay) }

            is DetailAction.FinishedOnChanged -> updateEntry { it.copy(finishedOn = action.epochDay) }

            DetailAction.FavouriteToggled -> updateEntry { it.copy(isFavourite = !it.isFavourite) }

            DetailAction.NewRoundRequested -> launch { repository.startNewRound(workId) }

            DetailAction.DeleteRequested -> launch {
                lastDeleted = repository.deleteWork(workId)
                deleted.value = true
                _events.emit(DetailEvent.Deleted)
            }

            DetailAction.UndoDelete -> launch {
                lastDeleted?.let { repository.restore(it) }
                lastDeleted = null
            }
        }
    }

    private fun updateEntry(transform: (com.dfuentes.archivo.core.model.Entry) -> com.dfuentes.archivo.core.model.Entry) =
        launch {
            uiState.value.work?.currentEntry?.let { repository.upsertEntry(transform(it)) }
        }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

sealed interface DetailEvent {
    data object Deleted : DetailEvent
}

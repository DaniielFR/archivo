package com.dfuentes.archivo.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dfuentes.archivo.core.model.BookCandidate
import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.data.repository.BookSearchRepository
import com.dfuentes.archivo.data.repository.LibraryRepository
import com.dfuentes.archivo.data.repository.SearchOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: BookSearchRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries
                // 350 ms: suficiente para no disparar una petición por tecla,
                // poco para que no se note al dejar de escribir.
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .filter { it.trim().length >= MIN_QUERY }
                .collect { runSearch(it) }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                _uiState.update { it.copy(query = action.query) }
                queries.value = action.query
            }

            SearchAction.SearchSubmitted -> {
                val query = _uiState.value.query
                if (query.trim().length >= MIN_QUERY) viewModelScope.launch { runSearch(query) }
            }

            is SearchAction.CandidateSelected -> selectCandidate(action.candidate)

            SearchAction.SelectionDismissed ->
                _uiState.update { it.copy(selected = null, quickLog = QuickLogState()) }

            is SearchAction.StatusChanged -> updateQuickLog {
                it.copy(
                    status = action.status,
                    finishedOn = if (action.status == Status.FINISHED) {
                        it.finishedOn ?: LocalDate.now().toEpochDay()
                    } else {
                        it.finishedOn
                    },
                )
            }

            is SearchAction.RatingChanged -> updateQuickLog {
                it.copy(rating = if (it.rating == action.rating) null else action.rating)
            }

            is SearchAction.FinishedOnChanged -> updateQuickLog { it.copy(finishedOn = action.epochDay) }

            is SearchAction.NotesChanged -> updateQuickLog { it.copy(notes = action.notes) }

            is SearchAction.FormatChanged -> updateQuickLog { it.copy(format = action.format) }

            SearchAction.SaveRequested -> save()

            SearchAction.SavedHandled -> _uiState.update { it.copy(savedTitle = null) }
        }
    }

    private suspend fun runSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null, offline = false) }
        when (val outcome = searchRepository.search(query)) {
            is SearchOutcome.Success -> _uiState.update {
                it.copy(results = outcome.results, isSearching = false, hasSearched = true)
            }
            SearchOutcome.Offline -> _uiState.update {
                it.copy(isSearching = false, hasSearched = true, offline = true)
            }
            is SearchOutcome.Error -> _uiState.update {
                it.copy(isSearching = false, hasSearched = true, error = outcome.message)
            }
        }
    }

    private fun selectCandidate(candidate: BookCandidate) {
        _uiState.update {
            it.copy(
                selected = candidate,
                quickLog = QuickLogState(finishedOn = LocalDate.now().toEpochDay()),
            )
        }
        // Completar contra Open Library en cuanto se abre la hoja: para cuando
        // el usuario haya puesto la nota, la portada y las páginas ya están.
        viewModelScope.launch {
            val enriched = searchRepository.enrich(candidate)
            _uiState.update { state ->
                if (state.selected?.sourceId == candidate.sourceId) {
                    state.copy(selected = enriched)
                } else {
                    state
                }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val candidate = state.selected ?: return
        updateQuickLog { it.copy(isSaving = true) }
        viewModelScope.launch {
            val log = state.quickLog
            libraryRepository.addWork(
                candidate.toWork().copy(
                    entries = listOf(
                        Entry(
                            workId = 0,
                            status = log.status,
                            rating = log.rating,
                            finishedOn = log.finishedOn,
                            notes = log.notes.ifBlank { null },
                            format = log.format,
                        ),
                    ),
                ),
            )
            _uiState.update {
                it.copy(
                    selected = null,
                    quickLog = QuickLogState(),
                    savedTitle = candidate.title,
                )
            }
        }
    }

    private fun updateQuickLog(transform: (QuickLogState) -> QuickLogState) =
        _uiState.update { it.copy(quickLog = transform(it.quickLog)) }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val MIN_QUERY = 3
    }
}

package com.dfuentes.archivo.feature.search

import com.dfuentes.archivo.core.model.BookCandidate
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.Status

data class SearchUiState(
    val query: String = "",
    val results: List<BookCandidate> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null,
    /** Candidato elegido: abre la hoja de alta rápida. */
    val selected: BookCandidate? = null,
    val quickLog: QuickLogState = QuickLogState(),
    val savedTitle: String? = null,
)

/**
 * Estado de la hoja de alta rápida. Deliberadamente mínimo: estado, nota, fecha
 * y un texto libre. Todo lo demás se puede editar después desde la ficha, y
 * meterlo aquí convertiría tres toques en un formulario.
 */
data class QuickLogState(
    val status: Status = Status.FINISHED,
    val rating: Int? = null,
    val finishedOn: Long? = null,
    val notes: String = "",
    val format: Format? = null,
    val isSaving: Boolean = false,
)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction

    data object SearchSubmitted : SearchAction

    data class CandidateSelected(val candidate: BookCandidate) : SearchAction

    data object SelectionDismissed : SearchAction

    data class StatusChanged(val status: Status) : SearchAction

    data class RatingChanged(val rating: Int) : SearchAction

    data class FinishedOnChanged(val epochDay: Long?) : SearchAction

    data class NotesChanged(val notes: String) : SearchAction

    data class FormatChanged(val format: Format?) : SearchAction

    data object SaveRequested : SearchAction

    data object SavedHandled : SearchAction
}

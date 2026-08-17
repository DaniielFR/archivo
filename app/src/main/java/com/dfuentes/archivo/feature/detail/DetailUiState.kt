package com.dfuentes.archivo.feature.detail

import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work

data class DetailUiState(
    val work: Work? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    /** Se pone a true cuando el borrado se confirma y la pantalla debe cerrarse. */
    val deleted: Boolean = false,
)

sealed interface DetailAction {
    data class StatusChanged(val status: Status) : DetailAction

    data class RatingChanged(val rating: Int) : DetailAction

    data class NotesChanged(val notes: String) : DetailAction

    data class StartedOnChanged(val epochDay: Long?) : DetailAction

    data class FinishedOnChanged(val epochDay: Long?) : DetailAction

    data object FavouriteToggled : DetailAction

    data object NewRoundRequested : DetailAction

    data object DeleteRequested : DetailAction

    data object UndoDelete : DetailAction
}

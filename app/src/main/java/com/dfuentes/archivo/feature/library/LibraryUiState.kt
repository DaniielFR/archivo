package com.dfuentes.archivo.feature.library

import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.LibraryLayout
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.SortOrder
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.data.repository.WorkSummary

/**
 * Estado inmutable y exhaustivo de la pantalla.
 * `isLoading` e `isEmpty` son campos explícitos y no derivaciones de
 * `items.isEmpty()`: "todavía no ha cargado" y "no hay nada" son estados
 * distintos que se pintan distinto, y confundirlos produce un parpadeo feo.
 */
data class LibraryUiState(
    val items: List<WorkSummary> = emptyList(),
    val inProgress: List<WorkSummary> = emptyList(),
    val filter: LibraryFilter = LibraryFilter(),
    val layout: LibraryLayout = LibraryLayout.GRID,
    val isLoading: Boolean = true,
    val totalWorks: Int = 0,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
    val hasActiveFilters: Boolean
        get() = filter.type != null || filter.status != null || filter.year != null
}

sealed interface LibraryAction {
    data class TypeFilterChanged(val type: MediaType?) : LibraryAction

    data class StatusFilterChanged(val status: Status?) : LibraryAction

    data class SortChanged(val sort: SortOrder) : LibraryAction

    data object FiltersCleared : LibraryAction

    data object LayoutToggled : LibraryAction

    /** TEMPORAL (fase 0): valida el ciclo escritura → Room → Flow → UI.
     *  Se elimina en la fase 1, cuando exista el alta manual de verdad. */
    data object SampleDataRequested : LibraryAction
}

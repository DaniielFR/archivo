package com.dfuentes.archivo.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.LibraryLayout
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter())
    private val layout = MutableStateFlow(LibraryLayout.GRID)

    val uiState: StateFlow<LibraryUiState> =
        filter
            .flatMapLatest { f ->
                combine(
                    repository.library(f),
                    repository.inProgress(),
                    repository.workCount(),
                    layout,
                ) { items, inProgress, count, layoutValue ->
                    LibraryUiState(
                        items = items,
                        inProgress = inProgress,
                        filter = f,
                        layout = layoutValue,
                        isLoading = false,
                        totalWorks = count,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                // WhileSubscribed(5s) y no Eagerly: al rotar la pantalla el flujo
                // sobrevive, pero al irse la app a segundo plano se cancela.
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState(),
            )

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.TypeFilterChanged ->
                filter.update { it.copy(type = action.type) }

            is LibraryAction.StatusFilterChanged ->
                filter.update { it.copy(status = action.status) }

            is LibraryAction.SortChanged ->
                filter.update { it.copy(sort = action.sort) }

            LibraryAction.FiltersCleared ->
                filter.update { LibraryFilter(sort = it.sort) }

            LibraryAction.LayoutToggled ->
                layout.update {
                    if (it == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID
                }

            LibraryAction.SampleDataRequested -> addSampleData()
        }
    }

    /** TEMPORAL — ver LibraryAction.SampleDataRequested. */
    private fun addSampleData() = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        listOf(
            Work(type = MediaType.BOOK, title = "El nombre del viento", year = 2007, pageCount = 662),
            Work(type = MediaType.MOVIE, title = "La llegada", year = 2016, runtimeMinutes = 116),
            Work(type = MediaType.SERIES, title = "Chernobyl", year = 2019, seasonCount = 1),
        ).forEachIndexed { index, work ->
            repository.addWork(
                work.copy(
                    entries = listOf(
                        Entry(
                            workId = 0,
                            status = Status.FINISHED,
                            rating = 8 + index % 3,
                            finishedOn = today - index * 30L,
                            format = if (index == 0) Format.PAPER else Format.STREAMING,
                            notes = "Nota de ejemplo para validar el ciclo de datos.",
                        ),
                    ),
                ),
            )
        }
    }
}

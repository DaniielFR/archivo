package com.dfuentes.archivo.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dfuentes.archivo.core.datastore.SettingsRepository
import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.LibraryLayout
import com.dfuentes.archivo.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Filtros: viven en memoria, se pierden al salir. Es lo correcto. */
    private val typeAndStatus = MutableStateFlow(LibraryFilter())

    /** Orden y disposición: persisten, porque son preferencias, no filtros. */
    private val preferences = settings.preferences

    val uiState: StateFlow<LibraryUiState> =
        combine(typeAndStatus, preferences) { filter, prefs ->
            filter.copy(sort = prefs.sort) to prefs.layout
        }.flatMapLatest { (filter, layout) ->
            combine(
                repository.library(filter),
                repository.inProgress(),
                repository.workCount(),
            ) { items, inProgress, count ->
                LibraryUiState(
                    items = items,
                    inProgress = inProgress,
                    filter = filter,
                    layout = layout,
                    isLoading = false,
                    totalWorks = count,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5s) y no Eagerly: al rotar la pantalla el flujo
            // sobrevive, pero al irse la app a segundo plano se cancela.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.TypeFilterChanged ->
                typeAndStatus.update { it.copy(type = action.type) }

            is LibraryAction.StatusFilterChanged ->
                typeAndStatus.update { it.copy(status = action.status) }

            is LibraryAction.SortChanged ->
                viewModelScope.launch { settings.setSort(action.sort) }

            LibraryAction.FiltersCleared ->
                typeAndStatus.update { LibraryFilter(sort = it.sort) }

            LibraryAction.LayoutToggled -> viewModelScope.launch {
                val current = settings.preferences.first().layout
                settings.setLayout(
                    if (current == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID,
                )
            }
        }
    }
}

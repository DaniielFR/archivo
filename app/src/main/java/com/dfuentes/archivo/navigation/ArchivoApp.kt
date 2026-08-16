package com.dfuentes.archivo.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dfuentes.archivo.feature.addedit.AddEditRoute
import com.dfuentes.archivo.feature.detail.DetailRoute
import com.dfuentes.archivo.feature.library.LibraryRoute
import com.dfuentes.archivo.feature.settings.SettingsRoute
import com.dfuentes.archivo.feature.stats.StatsRoute

/**
 * Raíz de la aplicación.
 *
 * Navigation 3: la pila es una lista de [NavKey] que poseemos nosotros. Navegar
 * es `backStack.add(...)`; volver es quitar el último. No hay rutas en String ni
 * parsing de argumentos, así que toda una categoría de errores en tiempo de
 * ejecución simplemente no existe.
 *
 * La barra de navegación solo se muestra en los destinos de primer nivel: en una
 * ficha de detalle estorba y roba altura.
 */
@Composable
fun ArchivoApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(LibraryKey)

    val currentKey = backStack.lastOrNull()
    val topLevel = currentKey?.let(TopLevelDestination::from)

    fun goTo(key: NavKey) {
        // Cambiar de pestaña reinicia la pila: no queremos que "atrás" desde
        // Ajustes lleve a una ficha de detalle abierta hace diez minutos.
        backStack.clear()
        backStack.add(key)
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            if (topLevel != null) {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = destination == topLevel
                    item(
                        selected = selected,
                        onClick = { if (!selected) goTo(destination.key) },
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) {
        // SharedTransitionLayout envuelve todo para que en la fase 6 la portada
        // pueda volar de la rejilla a la ficha sin reestructurar nada.
        SharedTransitionLayout {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<LibraryKey> {
                        LibraryRoute(
                            onOpenWork = { id -> backStack.add(DetailKey(id)) },
                            onAddWork = { type -> backStack.add(AddEditKey(type)) },
                        )
                    }
                    entry<StatsKey> { StatsRoute() }
                    entry<SettingsKey> { SettingsRoute() }
                    entry<DetailKey> { key ->
                        DetailRoute(
                            workId = key.workId,
                            onBack = { backStack.removeLastOrNull() },
                            onEdit = { type ->
                                backStack.add(AddEditKey(type, key.workId))
                            },
                        )
                    }
                    entry<AddEditKey> { key ->
                        AddEditRoute(
                            navKey = key,
                            onDone = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}

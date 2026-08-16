package com.dfuentes.archivo.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dfuentes.archivo.feature.library.LibraryRoute
import com.dfuentes.archivo.feature.settings.SettingsRoute
import com.dfuentes.archivo.feature.stats.StatsRoute

/**
 * Raíz de la aplicación.
 *
 * DECISIÓN DE FASE 0: los destinos de primer nivel NO usan librería de
 * navegación. Son tres pestañas hermanas sin pila de retroceso entre ellas;
 * un `when` sobre una variable de estado es más simple, más rápido y no
 * introduce una dependencia que todavía no aporta nada.
 *
 * Navigation 3 entra en la FASE 1, cuando aparece la ficha de detalle y con
 * ella una pila real (biblioteca → ficha → editar) y las transiciones de
 * elemento compartido de la portada.
 *
 * NavigationSuiteScaffold elige solo entre NavigationBar (móvil) y
 * NavigationRail (tablet o plegable abierto). Coste: cero. Beneficio: la app
 * no se ve rota en horizontal.
 */
@Composable
fun ArchivoApp() {
    var current by rememberSaveable { mutableStateOf(TopLevelDestination.LIBRARY) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == current
                item(
                    selected = selected,
                    onClick = { current = destination },
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
        },
    ) {
        when (current) {
            TopLevelDestination.LIBRARY -> LibraryRoute()
            TopLevelDestination.STATS -> StatsRoute()
            TopLevelDestination.SETTINGS -> SettingsRoute()
        }
    }
}

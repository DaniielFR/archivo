package com.dfuentes.archivo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

/**
 * Los tres destinos de primer nivel. Deliberadamente tres y no cinco:
 * cada pestaña adicional diluye las que importan.
 */
enum class TopLevelDestination(
    val key: NavKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LIBRARY(
        LibraryKey, "Biblioteca",
        Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook,
    ),
    STATS(StatsKey, "Estadísticas", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS(SettingsKey, "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings),
    ;

    companion object {
        fun from(key: NavKey): TopLevelDestination? = entries.firstOrNull { it.key == key }
    }
}

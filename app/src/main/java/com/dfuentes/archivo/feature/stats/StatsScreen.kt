package com.dfuentes.archivo.feature.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.dfuentes.archivo.core.designsystem.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsRoute(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Estadísticas") }) },
    ) { padding ->
        EmptyState(
            icon = Icons.Outlined.BarChart,
            title = "Aún no hay nada que medir",
            description = "Las estadísticas llegan en la fase 5: recuento anual, " +
                "páginas, horas, histograma de notas y géneros.",
            modifier = Modifier.padding(padding),
        )
    }
}

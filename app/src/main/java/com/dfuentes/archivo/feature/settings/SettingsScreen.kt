package com.dfuentes.archivo.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dfuentes.archivo.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Ajustes") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text("Tema") },
                supportingContent = { Text("Fase 1 · sistema / claro / oscuro y color dinámico") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Copia de seguridad") },
                supportingContent = { Text("Fase 2 · exportar, importar y copia automática semanal") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Claves de API") },
                supportingContent = { Text("Fase 3 · Google Books y TMDB") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Versión") },
                supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
            )
        }
    }
}

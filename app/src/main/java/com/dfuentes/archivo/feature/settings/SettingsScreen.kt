package com.dfuentes.archivo.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfuentes.archivo.BuildConfig
import com.dfuentes.archivo.core.backup.ImportMode
import com.dfuentes.archivo.core.designsystem.theme.ThemeMode
import com.dfuentes.archivo.data.repository.backupFileName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackupDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // SAF: el sistema pone el selector de ficheros, la app no pide permisos de
    // almacenamiento. El usuario elige dónde y la app no ve nada más.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::prepareImport) }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            // Sin takePersistable el permiso muere al reiniciar y la copia
            // automática dejaría de funcionar en silencio.
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setBackupFolder(it)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Ajustes") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            SectionHeader("Apariencia")
            ListItem(headlineContent = { Text("Tema") })
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.themeMode == mode,
                        onClick = { viewModel.setTheme(mode) },
                        label = { Text(mode.displayName) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text("Color dinámico") },
                supportingContent = { Text("Deriva la paleta del fondo de pantalla") },
                trailingContent = {
                    Switch(checked = prefs.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                },
            )

            HorizontalDivider()
            SectionHeader("Copia de seguridad")

            ListItem(
                headlineContent = { Text("Exportar ahora") },
                supportingContent = { Text("Genera un fichero .archivo con todo") },
                modifier = Modifier.clickableItem { exportLauncher.launch(backupFileName()) },
            )
            ListItem(
                headlineContent = { Text("Importar") },
                supportingContent = { Text("Restaura desde un fichero .archivo") },
                modifier = Modifier.clickableItem { importLauncher.launch(arrayOf("*/*")) },
            )
            ListItem(
                headlineContent = { Text("Carpeta de copias automáticas") },
                supportingContent = {
                    Text(
                        prefs.backupFolderUri?.let { "Configurada · elige otra para cambiarla" }
                            ?: "Sin configurar. Elige una carpeta sincronizada con tu nube.",
                    )
                },
                modifier = Modifier.clickableItem { folderLauncher.launch(null) },
            )
            ListItem(
                headlineContent = { Text("Copia automática semanal") },
                supportingContent = {
                    val last = prefs.lastBackupAt
                    Text(
                        when {
                            prefs.backupFolderUri == null -> "Elige antes una carpeta"
                            last != null -> "Última copia: " + Instant.ofEpochMilli(last)
                                .atZone(ZoneId.systemDefault())
                                .format(BackupDateFormat)
                            else -> "Aún no se ha hecho ninguna"
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = prefs.autoBackupEnabled,
                        enabled = prefs.backupFolderUri != null,
                        onCheckedChange = viewModel::setAutoBackup,
                    )
                },
            )

            HorizontalDivider()
            SectionHeader("Acerca de")
            ListItem(
                headlineContent = { Text("Claves de API") },
                supportingContent = { Text("Fase 3 · Google Books y TMDB") },
            )
            ListItem(
                headlineContent = { Text("Versión") },
                supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
            )
        }
    }

    pendingImport?.let { pending ->
        val p = pending.preview
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Importar copia") },
            text = {
                Column {
                    Text("${p.works} obras · ${p.entries} registros")
                    if (p.alreadyPresent > 0) {
                        Text(
                            text = "${p.alreadyPresent} ya están en tu biblioteca.",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        text = "«Fusionar» añade solo lo que falta. «Reemplazar» borra " +
                            "todo lo que tienes ahora y deja únicamente esta copia.",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(ImportMode.MERGE) }) {
                    Text("Fusionar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::cancelImport) { Text("Cancelar") }
                    TextButton(onClick = { viewModel.confirmImport(ImportMode.REPLACE) }) {
                        Text("Reemplazar")
                    }
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier = clickable(onClick = onClick)

package com.dfuentes.archivo.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dfuentes.archivo.core.backup.AutoBackupWorker
import com.dfuentes.archivo.core.backup.BackupPreview
import com.dfuentes.archivo.core.backup.BackupResult
import com.dfuentes.archivo.core.backup.ImportMode
import com.dfuentes.archivo.core.datastore.SettingsRepository
import com.dfuentes.archivo.core.datastore.UserPreferences
import com.dfuentes.archivo.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dfuentes.archivo.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val backup: BackupRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settings.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences(),
    )

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    // ── Copias de seguridad ─────────────────────────────────────────────────
    fun export(target: Uri) = withBusy {
        when (val result = backup.export(target)) {
            is BackupResult.Success -> {
                settings.setLastBackupAt(System.currentTimeMillis())
                _messages.emit("Copia guardada · ${result.works} obras")
            }
            is BackupResult.Failure -> _messages.emit("No se pudo exportar: ${result.reason}")
        }
    }

    /**
     * Importar nunca es directo: primero se lee la copia y se enseña qué hay
     * dentro. Sobrescribir años de registros por un toque accidental es
     * exactamente el desastre que esta fase existe para evitar.
     */
    fun prepareImport(source: Uri) = withBusy {
        val preview = backup.preview(source)
        if (preview == null) {
            _messages.emit("El fichero no parece una copia de Archivo")
        } else {
            _pendingImport.value = PendingImport(source, preview)
        }
    }

    fun confirmImport(mode: ImportMode) {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        withBusy {
            when (val result = backup.import(pending.uri, mode)) {
                is BackupResult.Success -> _messages.emit("Importadas ${result.works} obras")
                is BackupResult.Failure -> _messages.emit("No se pudo importar: ${result.reason}")
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun setBackupFolder(treeUri: Uri) {
        viewModelScope.launch {
            settings.setBackupFolder(treeUri.toString())
            _messages.emit("Carpeta de copias seleccionada")
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settings.setAutoBackup(enabled)
            if (enabled) AutoBackupWorker.schedule(context) else AutoBackupWorker.cancel(context)
        }
    }

    private fun withBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}

data class PendingImport(val uri: Uri, val preview: BackupPreview)

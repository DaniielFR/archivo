package com.dfuentes.archivo.core.backup

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dfuentes.archivo.core.datastore.SettingsRepository
import com.dfuentes.archivo.data.repository.BackupRepository
import com.dfuentes.archivo.data.repository.backupFileName
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Copia semanal a la carpeta que el usuario eligió una vez.
 *
 * No requiere red a propósito: escribe en local y es la app de sincronización
 * del usuario (Drive, Nextcloud, Syncthing) la que se encarga de subirlo cuando
 * pueda. Así la copia se hace igual aunque estés sin cobertura.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val settings: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = settings.preferences.first()
        if (!prefs.autoBackupEnabled) return Result.success()
        val treeUri = prefs.backupFolderUri?.let(Uri::parse) ?: return Result.success()

        val resolver = applicationContext.contentResolver
        val target = BackupFolder.createFile(resolver, treeUri, backupFileName())
            // Un fallo aquí suele ser un permiso revocado o la carpeta borrada:
            // reintentar en el siguiente ciclo, no marcar error permanente.
            ?: return Result.retry()

        return when (backupRepository.export(target)) {
            is BackupResult.Success -> {
                BackupFolder.rotate(resolver, treeUri)
                settings.setLastBackupAt(System.currentTimeMillis())
                Result.success()
            }
            is BackupResult.Failure -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "auto-backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP y no UPDATE: reprogramar en cada arranque reiniciaría el
                // contador y la copia no llegaría a ejecutarse nunca.
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

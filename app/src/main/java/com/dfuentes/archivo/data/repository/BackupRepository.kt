package com.dfuentes.archivo.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dfuentes.archivo.BuildConfig
import com.dfuentes.archivo.core.backup.BACKUP_EXTENSION
import com.dfuentes.archivo.core.backup.BackupData
import com.dfuentes.archivo.core.backup.BackupJson
import com.dfuentes.archivo.core.backup.BackupManifest
import com.dfuentes.archivo.core.backup.BackupPreview
import com.dfuentes.archivo.core.backup.BackupResult
import com.dfuentes.archivo.core.backup.BackupWork
import com.dfuentes.archivo.core.backup.CSV_ENTRY
import com.dfuentes.archivo.core.backup.DATA_ENTRY
import com.dfuentes.archivo.core.backup.ImportMode
import com.dfuentes.archivo.core.backup.MANIFEST_ENTRY
import com.dfuentes.archivo.core.backup.buildCsv
import com.dfuentes.archivo.core.backup.fingerprint
import com.dfuentes.archivo.core.backup.roleOrDefault
import com.dfuentes.archivo.core.backup.toBackup
import com.dfuentes.archivo.core.backup.toEntity
import com.dfuentes.archivo.core.database.ArchivoDatabase
import com.dfuentes.archivo.core.database.dao.BackupDao
import com.dfuentes.archivo.core.database.entity.GenreEntity
import com.dfuentes.archivo.core.database.entity.PersonEntity
import com.dfuentes.archivo.core.database.entity.TagEntity
import com.dfuentes.archivo.core.database.entity.WorkGenreCrossRef
import com.dfuentes.archivo.core.database.entity.WorkPersonCrossRef
import com.dfuentes.archivo.core.database.entity.WorkTagCrossRef
import com.dfuentes.archivo.core.di.IoDispatcher
import com.dfuentes.archivo.core.util.sortTitleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

fun backupFileName(date: LocalDate = LocalDate.now()) = "archivo-$date.$BACKUP_EXTENSION"

/**
 * Copias de seguridad.
 *
 * En una app 100 % local el fallo catastrófico no es un crash: es perder años de
 * registros. Por eso este fichero es de los que más cuidado llevan y por eso la
 * importación es transaccional: o entra todo o no entra nada.
 */
@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: ArchivoDatabase,
    private val dao: BackupDao,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    // ── EXPORTAR ────────────────────────────────────────────────────────────
    suspend fun export(target: Uri): BackupResult = withContext(io) {
        runCatching {
            val data = collect()
            context.contentResolver.openOutputStream(target)?.use { out ->
                writeZip(out, data)
            } ?: return@runCatching BackupResult.Failure("No se pudo escribir en el destino")
            BackupResult.Success(backupFileName(), data.works.size)
        }.getOrElse { BackupResult.Failure(it.message ?: "Error desconocido al exportar") }
    }

    /** Vuelca la base a la estructura anidada del formato de copia. */
    suspend fun collect(): BackupData = withContext(io) {
        val works = dao.allWorks()
        val entriesByWork = dao.allEntries().groupBy { it.workId }
        val people = dao.allPeople().associateBy { it.id }
        val tags = dao.allTags().associateBy { it.id }
        val genres = dao.allGenres().associateBy { it.id }
        val creditsByWork = dao.allWorkPeople().groupBy { it.workId }
        val tagsByWork = dao.allWorkTags().groupBy { it.workId }
        val genresByWork = dao.allWorkGenres().groupBy { it.workId }

        BackupData(
            works = works.map { work ->
                work.toBackup(
                    credits = creditsByWork[work.id].orEmpty().mapNotNull { ref ->
                        people[ref.personId]?.let {
                            com.dfuentes.archivo.core.backup.BackupCredit(
                                name = it.name, role = ref.role.name, position = ref.position,
                            )
                        }
                    }.sortedBy { it.position },
                    tags = tagsByWork[work.id].orEmpty().mapNotNull { tags[it.tagId]?.name },
                    genres = genresByWork[work.id].orEmpty().mapNotNull { genres[it.genreId]?.name },
                    entries = entriesByWork[work.id].orEmpty().sortedBy { it.round }.map { it.toBackup() },
                )
            },
            people = dao.allPeople().map { it.toBackup() },
            tags = dao.allTags().map { it.toBackup() },
            genres = dao.allGenres().map { it.toBackup() },
        )
    }

    private suspend fun writeZip(out: OutputStream, data: BackupData) {
        val manifest = BackupManifest(
            schemaVersion = SCHEMA_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            workCount = data.works.size,
            entryCount = data.works.sumOf { it.entries.size },
        )
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(BackupJson.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(BackupJson.encodeToString(BackupData.serializer(), data).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(CSV_ENTRY))
            zip.write(buildCsv(data).toByteArray())
            zip.closeEntry()

            // Fase 3: aquí entran las portadas de filesDir/covers.
        }
    }

    // ── LEER / PREVISUALIZAR ────────────────────────────────────────────────
    suspend fun preview(source: Uri): BackupPreview? = withContext(io) {
        runCatching {
            val (manifest, data) = read(source) ?: return@runCatching null
            val existing = dao.fingerprints().toSet()
            BackupPreview(
                manifest = manifest,
                works = data.works.size,
                entries = data.works.sumOf { it.entries.size },
                alreadyPresent = data.works.count { it.fingerprint() in existing },
            )
        }.getOrNull()
    }

    private fun read(source: Uri): Pair<BackupManifest, BackupData>? =
        context.contentResolver.openInputStream(source)?.use { readZip(it) }

    private fun readZip(input: InputStream): Pair<BackupManifest, BackupData>? {
        var manifest: BackupManifest? = null
        var data: BackupData? = null
        ZipInputStream(input.buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    MANIFEST_ENTRY -> manifest =
                        BackupJson.decodeFromString(BackupManifest.serializer(), zip.readBytes().decodeToString())
                    DATA_ENTRY -> data =
                        BackupJson.decodeFromString(BackupData.serializer(), zip.readBytes().decodeToString())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val m = manifest ?: return null
        val d = data ?: return null
        return m to d
    }

    // ── IMPORTAR ────────────────────────────────────────────────────────────
    suspend fun import(source: Uri, mode: ImportMode): BackupResult = withContext(io) {
        runCatching {
            val (manifest, data) = read(source)
                ?: return@runCatching BackupResult.Failure("El fichero no es una copia válida")

            if (manifest.formatVersion > com.dfuentes.archivo.core.backup.FORMAT_VERSION) {
                return@runCatching BackupResult.Failure(
                    "La copia es de una versión más nueva de la app (formato ${manifest.formatVersion})",
                )
            }

            var imported = 0
            // Una sola transacción: si algo falla a mitad, la base queda exactamente
            // como estaba. Importar a medias es peor que no importar.
            db.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    dao.deleteAllWorks()
                    dao.deleteAllPeople()
                    dao.deleteAllTags()
                    dao.deleteAllGenres()
                }
                val existing = dao.fingerprints().toMutableSet()
                val now = System.currentTimeMillis()

                data.works.forEach { work ->
                    val fingerprint = work.fingerprint()
                    if (mode == ImportMode.MERGE && fingerprint in existing) return@forEach
                    insertWork(work, now)
                    existing += fingerprint
                    imported++
                }
            }
            BackupResult.Success(fileName = "", works = imported)
        }.getOrElse { BackupResult.Failure(it.message ?: "Error desconocido al importar") }
    }

    private suspend fun insertWork(work: BackupWork, now: Long) {
        val entity = work.toEntity(now, sortTitleOf(work.title))
        val workId = dao.insertWork(entity)

        dao.insertEntries(work.entries.map { it.toEntity(workId, now) })

        work.creators.forEach { credit ->
            val personId = dao.findPerson(credit.name)?.id
                ?: dao.insertPerson(PersonEntity(name = credit.name, sortName = sortTitleOf(credit.name)))
                    .takeIf { it > 0 }
                ?: dao.findPerson(credit.name)?.id
                ?: return@forEach
            dao.linkPeople(
                listOf(WorkPersonCrossRef(workId, personId, credit.roleOrDefault(entity.type), credit.position)),
            )
        }
        work.tags.forEach { name ->
            val tagId = dao.findTag(name)?.id
                ?: dao.insertTag(TagEntity(name = name)).takeIf { it > 0 }
                ?: dao.findTag(name)?.id ?: return@forEach
            dao.linkTags(listOf(WorkTagCrossRef(workId, tagId)))
        }
        work.genres.forEach { name ->
            val genreId = dao.findGenre(name)?.id
                ?: dao.insertGenre(GenreEntity(name = name)).takeIf { it > 0 }
                ?: dao.findGenre(name)?.id ?: return@forEach
            dao.linkGenres(listOf(WorkGenreCrossRef(workId, genreId)))
        }
    }

    companion object {
        /** Debe seguir a @Database(version = …) de ArchivoDatabase. */
        const val SCHEMA_VERSION = 1
    }
}

package com.dfuentes.archivo.data.repository

import androidx.room.withTransaction
import com.dfuentes.archivo.core.database.ArchivoDatabase
import com.dfuentes.archivo.core.database.dao.EntryDao
import com.dfuentes.archivo.core.database.dao.LibraryDao
import com.dfuentes.archivo.core.database.dao.WorkDao
import com.dfuentes.archivo.core.database.entity.GenreEntity
import com.dfuentes.archivo.core.database.entity.PersonEntity
import com.dfuentes.archivo.core.database.entity.WorkGenreCrossRef
import com.dfuentes.archivo.core.database.entity.WorkPersonCrossRef
import com.dfuentes.archivo.core.di.ApplicationScope
import com.dfuentes.archivo.core.di.IoDispatcher
import com.dfuentes.archivo.core.files.CoverStore
import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.PersonRole
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.core.util.sortTitleOf
import com.dfuentes.archivo.data.mapper.toDomain
import com.dfuentes.archivo.data.mapper.toEntity
import com.dfuentes.archivo.data.mapper.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val db: ArchivoDatabase,
    private val libraryDao: LibraryDao,
    private val workDao: WorkDao,
    private val entryDao: EntryDao,
    private val coverStore: CoverStore,
    @param:ApplicationScope private val appScope: CoroutineScope,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : LibraryRepository {

    override fun library(filter: LibraryFilter): Flow<List<WorkSummary>> =
        libraryDao.library(
            type = filter.type?.name,
            status = filter.status?.name,
            year = filter.year,
            sort = filter.sort.name,
        ).map { cards -> cards.map { it.toSummary() } }.flowOn(io)

    override fun inProgress(): Flow<List<WorkSummary>> =
        libraryDao.inProgress().map { cards -> cards.map { it.toSummary() } }.flowOn(io)

    override fun workCount(): Flow<Int> = libraryDao.workCount().flowOn(io)

    override fun workDetail(id: Long): Flow<Work?> =
        libraryDao.workDetail(id).map { it?.toDomain() }.flowOn(io)

    override suspend fun getWork(id: Long): Work? = withContext(io) {
        libraryDao.workDetail(id).first()?.toDomain()
    }

    /**
     * Alta. Va en una transacción porque una obra sin sus autores, o con autores
     * pero sin registro, es un estado que no debe existir ni un instante:
     * la UI observa Room y pintaría una tarjeta a medias.
     */
    override suspend fun addWork(work: Work): Long = withContext(io) {
        val workId = db.withTransaction {
            val now = System.currentTimeMillis()
            val id = workDao.insert(work.toEntity(now))
            linkCreators(id, work)
            linkGenres(id, work)
            val entry = work.currentEntry ?: Entry(workId = id, status = Status.PENDING)
            entryDao.insert(entry.copy(workId = id).toEntity(now))
            id
        }
        // Fuera de la transacción y sin await: la red no debe retrasar el guardado.
        fetchCover(workId, work.coverUrl)
        workId
    }

    /**
     * Descarga en el scope de aplicación, no en el del ViewModel: el usuario
     * cierra la hoja de alta justo después de guardar, y con un scope de
     * pantalla la descarga moriría exactamente ahí.
     */
    private fun fetchCover(workId: Long, url: String?) {
        if (url.isNullOrBlank()) return
        appScope.launch {
            coverStore.download(url, workId)?.let { stored ->
                workDao.updateCover(
                    id = workId,
                    path = stored.relativePath,
                    color = stored.dominantColor,
                    now = System.currentTimeMillis(),
                )
            }
        }
    }

    private suspend fun linkGenres(workId: Long, work: Work) {
        work.genres.map(String::trim).filter(String::isNotEmpty).distinct().forEach { name ->
            val genreId = workDao.findGenre(name)?.id
                ?: workDao.insertGenre(GenreEntity(name = name)).takeIf { it > 0 }
                ?: workDao.findGenre(name)?.id
                ?: return@forEach
            workDao.linkGenre(WorkGenreCrossRef(workId, genreId))
        }
    }

    override suspend fun updateWork(work: Work) = withContext(io) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val existing = workDao.findById(work.id) ?: return@withTransaction
            workDao.update(
                work.toEntity(now).copy(
                    id = work.id,
                    sortTitle = sortTitleOf(work.title),
                    createdAt = existing.createdAt,
                    // No pisamos los metadatos externos al editar a mano.
                    source = existing.source,
                    sourceId = existing.sourceId,
                    coverPath = work.coverPath ?: existing.coverPath,
                ),
            )
            workDao.clearPeople(work.id)
            linkCreators(work.id, work)
            work.currentEntry?.let { entryDao.update(it.toEntity(now)) }
            Unit  // el bloque debe devolver Unit, no Unit? (el ?.let de arriba)
        }
    }

    override suspend fun setStatus(workId: Long, status: Status) = withContext(io) {
        val entry = currentEntryOf(workId) ?: return@withContext
        val today = java.time.LocalDate.now().toEpochDay()
        entryDao.update(
            entry.copy(
                status = status,
                // Rellenar la fecha sola es la diferencia entre una app que usas
                // y una que abandonas: al marcar "Terminado" nadie quiere abrir
                // un selector de fecha para poner hoy.
                finishedOn = if (status == Status.FINISHED) entry.finishedOn ?: today else entry.finishedOn,
                startedOn = if (status == Status.IN_PROGRESS) entry.startedOn ?: today else entry.startedOn,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setRating(workId: Long, rating: Int?) = withContext(io) {
        val entry = currentEntryOf(workId) ?: return@withContext
        entryDao.update(entry.copy(rating = rating, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun startNewRound(workId: Long, status: Status): Long = withContext(io) {
        val now = System.currentTimeMillis()
        entryDao.insert(
            Entry(workId = workId, status = status, round = entryDao.lastRound(workId) + 1)
                .toEntity(now),
        )
    }

    override suspend fun upsertEntry(entry: Entry) = withContext(io) {
        val now = System.currentTimeMillis()
        if (entry.id == 0L) {
            entryDao.insert(entry.toEntity(now))
            Unit
        } else {
            entryDao.update(entry.toEntity(now))
        }
    }

    override suspend fun deleteWork(id: Long): Work? = withContext(io) {
        val snapshot = getWork(id) ?: return@withContext null
        workDao.findById(id)?.let { workDao.delete(it) }
        // La portada NO se borra aquí: si el usuario pulsa Deshacer la queremos
        // de vuelta sin volver a bajarla. Se limpia en restore() si no vuelve.
        snapshot
    }

    override suspend fun restore(work: Work): Long = withContext(io) {
        // Se reinserta con id nuevo: recuperar el mismo id no aporta nada y sí
        // puede chocar con algo insertado mientras el Snackbar estaba visible.
        addWork(work.copy(id = 0, entries = work.entries.map { it.copy(id = 0) }))
    }

    // ── privados ────────────────────────────────────────────────────────────
    private suspend fun currentEntryOf(workId: Long) =
        entryDao.forWork(workId).first().maxByOrNull { it.round }

    private suspend fun linkCreators(workId: Long, work: Work) {
        val role = when (work.type) {
            com.dfuentes.archivo.core.model.MediaType.BOOK -> PersonRole.AUTHOR
            com.dfuentes.archivo.core.model.MediaType.MOVIE -> PersonRole.DIRECTOR
            com.dfuentes.archivo.core.model.MediaType.SERIES -> PersonRole.CREATOR
        }
        work.creators.filter { it.isNotBlank() }.forEachIndexed { index, name ->
            val clean = name.trim()
            // insertPerson usa OnConflictStrategy.IGNORE y devuelve -1 si la
            // persona ya existía: en ese caso hay que releerla para obtener su id.
            val personId = workDao.findPerson(clean)?.id
                ?: workDao.insertPerson(PersonEntity(name = clean, sortName = sortTitleOf(clean)))
                    .takeIf { it > 0 }
                ?: workDao.findPerson(clean)?.id
                ?: return@forEachIndexed
            workDao.linkPerson(WorkPersonCrossRef(workId, personId, role, index))
        }
    }
}

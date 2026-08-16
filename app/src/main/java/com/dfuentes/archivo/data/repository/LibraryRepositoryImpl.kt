package com.dfuentes.archivo.data.repository

import com.dfuentes.archivo.core.database.dao.EntryDao
import com.dfuentes.archivo.core.database.dao.LibraryDao
import com.dfuentes.archivo.core.database.dao.WorkDao
import com.dfuentes.archivo.core.di.IoDispatcher
import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.data.mapper.toDomain
import com.dfuentes.archivo.data.mapper.toEntity
import com.dfuentes.archivo.data.mapper.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao,
    private val workDao: WorkDao,
    private val entryDao: EntryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LibraryRepository {

    override fun library(filter: LibraryFilter): Flow<List<WorkSummary>> =
        libraryDao.library(
            type = filter.type?.name,
            status = filter.status?.name,
            year = filter.year,
            sort = filter.sort.name,
        )
            .map { cards -> cards.map { it.toSummary() } }
            .flowOn(io)

    override fun inProgress(): Flow<List<WorkSummary>> =
        libraryDao.inProgress().map { cards -> cards.map { it.toSummary() } }.flowOn(io)

    override fun workCount(): Flow<Int> = libraryDao.workCount().flowOn(io)

    override fun workDetail(id: Long): Flow<Work?> =
        libraryDao.workDetail(id).map { it?.toDomain() }.flowOn(io)

    override suspend fun addWork(work: Work): Long = withContext(io) {
        val now = System.currentTimeMillis()
        val workId = workDao.insert(work.toEntity(now))
        work.currentEntry?.let { entry ->
            entryDao.insert(entry.copy(workId = workId).toEntity(now))
        }
        workId
    }

    override suspend fun deleteWork(id: Long) = withContext(io) {
        workDao.findById(id)?.let { workDao.delete(it) } ?: Unit
    }
}

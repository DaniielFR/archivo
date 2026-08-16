package com.dfuentes.archivo.fake

import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work
import com.dfuentes.archivo.data.repository.LibraryRepository
import com.dfuentes.archivo.data.repository.WorkSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake en memoria, no un mock. Un fake permite afirmar sobre el ESTADO final
 * ("¿se guardó la obra con estos datos?") en vez de sobre las llamadas
 * ("¿se invocó addWork una vez?"), que es lo que de verdad importa y lo que no
 * se rompe al refactorizar.
 */
class FakeLibraryRepository : LibraryRepository {

    val works = MutableStateFlow<List<Work>>(emptyList())
    private var nextId = 1L

    override fun library(filter: LibraryFilter): Flow<List<WorkSummary>> =
        works.map { list -> list.map(::summaryOf) }

    override fun inProgress(): Flow<List<WorkSummary>> =
        works.map { list ->
            list.filter { it.currentEntry?.status == Status.IN_PROGRESS }.map(::summaryOf)
        }

    override fun workCount(): Flow<Int> = works.map { it.size }

    override fun workDetail(id: Long): Flow<Work?> = works.map { list -> list.find { it.id == id } }

    override suspend fun getWork(id: Long): Work? = works.value.find { it.id == id }

    override suspend fun addWork(work: Work): Long {
        val id = nextId++
        works.value += work.copy(id = id)
        return id
    }

    override suspend fun updateWork(work: Work) {
        works.value = works.value.map { if (it.id == work.id) work else it }
    }

    override suspend fun setStatus(workId: Long, status: Status) =
        mutateEntry(workId) { it.copy(status = status) }

    override suspend fun setRating(workId: Long, rating: Int?) =
        mutateEntry(workId) { it.copy(rating = rating) }

    override suspend fun startNewRound(workId: Long, status: Status): Long = 0

    override suspend fun upsertEntry(entry: Entry) = mutateEntry(entry.workId) { entry }

    override suspend fun deleteWork(id: Long): Work? {
        val removed = works.value.find { it.id == id }
        works.value = works.value.filterNot { it.id == id }
        return removed
    }

    override suspend fun restore(work: Work): Long = addWork(work.copy(id = 0))

    private fun mutateEntry(workId: Long, transform: (Entry) -> Entry) {
        works.value = works.value.map { work ->
            if (work.id != workId) work
            else work.copy(entries = work.entries.map(transform))
        }
    }

    private fun summaryOf(work: Work) = WorkSummary(
        id = work.id,
        type = work.type,
        title = work.title,
        year = work.year,
        coverPath = null,
        dominantColor = null,
        creators = work.creators.joinToString(", ").ifEmpty { null },
        status = work.currentEntry?.status,
        rating = work.currentEntry?.rating,
        finishedOn = work.currentEntry?.finishedOn,
    )
}

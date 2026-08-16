package com.dfuentes.archivo.data.repository

import com.dfuentes.archivo.core.model.Entry
import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.core.model.Work
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de la capa de datos. La interfaz vive aquí y la implementación en el
 * mismo paquete a propósito: cuando el proyecto se modularice, la interfaz sube
 * a :core:domain y la implementación se queda en :core:data sin tocar una línea
 * de los ViewModels.
 */
interface LibraryRepository {
    fun library(filter: LibraryFilter): Flow<List<WorkSummary>>

    fun inProgress(): Flow<List<WorkSummary>>

    fun workCount(): Flow<Int>

    fun workDetail(id: Long): Flow<Work?>

    suspend fun getWork(id: Long): Work?

    /** Alta: inserta la obra, sus autores y su primer registro. Devuelve el id. */
    suspend fun addWork(work: Work): Long

    /** Edición: actualiza obra, autores y el registro vigente. */
    suspend fun updateWork(work: Work)

    /** Cambia solo el estado del registro vigente (acción rápida desde la ficha). */
    suspend fun setStatus(workId: Long, status: Status)

    /** Cambia solo la puntuación del registro vigente. */
    suspend fun setRating(workId: Long, rating: Int?)

    /** Abre una nueva vuelta: relectura o revisionado. */
    suspend fun startNewRound(workId: Long, status: Status = Status.IN_PROGRESS): Long

    suspend fun upsertEntry(entry: Entry)

    /**
     * Borra la obra y devuelve una copia completa en memoria, para poder
     * restaurarla si el usuario pulsa Deshacer. Toda escritura destructiva de
     * esta app pasa por aquí.
     */
    suspend fun deleteWork(id: Long): Work?

    /** Restaura lo devuelto por [deleteWork]. */
    suspend fun restore(work: Work): Long
}

/** Modelo de dominio ligero para listas. Ver LibraryDao.WorkCard. */
data class WorkSummary(
    val id: Long,
    val type: MediaType,
    val title: String,
    val year: Int?,
    val coverPath: String?,
    val dominantColor: Int?,
    val creators: String?,
    val status: Status?,
    val rating: Int?,
    val finishedOn: Long?,
)

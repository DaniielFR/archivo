package com.dfuentes.archivo.data.repository

import com.dfuentes.archivo.core.model.LibraryFilter
import com.dfuentes.archivo.core.model.Work
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de la capa de datos. La interfaz vive aquí y la implementación en
 * el mismo paquete a propósito: cuando el proyecto se modularice, la interfaz
 * sube a :core:domain y la implementación se queda en :core:data sin tocar
 * ni una línea de los ViewModels.
 */
interface LibraryRepository {
    fun library(filter: LibraryFilter): Flow<List<WorkSummary>>

    fun inProgress(): Flow<List<WorkSummary>>

    fun workCount(): Flow<Int>

    fun workDetail(id: Long): Flow<Work?>

    /** Inserta una obra con su registro inicial. Devuelve el id de la obra. */
    suspend fun addWork(work: Work): Long

    suspend fun deleteWork(id: Long)
}

/** Modelo de dominio ligero para listas. Ver LibraryDao.WorkCard. */
data class WorkSummary(
    val id: Long,
    val type: com.dfuentes.archivo.core.model.MediaType,
    val title: String,
    val year: Int?,
    val coverPath: String?,
    val dominantColor: Int?,
    val creators: String?,
    val status: com.dfuentes.archivo.core.model.Status?,
    val rating: Int?,
    val finishedOn: Long?,
)

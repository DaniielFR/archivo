package com.dfuentes.archivo.core.model

enum class SortOrder(val displayName: String) {
    RECENT("Más reciente"),
    RATING("Mejor valorado"),
    TITLE("Título"),
    ADDED("Fecha de alta"),
}

enum class LibraryLayout { GRID, LIST }

/**
 * Un único objeto de filtro atravesando ViewModel → UseCase → DAO.
 * Los campos nulos significan "sin filtrar" y se neutralizan en el WHERE,
 * lo que evita tener N variantes de la misma consulta.
 */
data class LibraryFilter(
    val type: MediaType? = null,
    val status: Status? = null,
    val year: Int? = null,
    val sort: SortOrder = SortOrder.RECENT,
)

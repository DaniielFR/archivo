package com.dfuentes.archivo.core.model

/**
 * Resultado de búsqueda ya normalizado. La UI nunca ve un DTO de Google ni de
 * Open Library: si mañana cambiamos de proveedor, esta clase no se entera.
 */
data class BookCandidate(
    val sourceId: String,
    val source: MetadataSource,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val year: Int? = null,
    val publisher: String? = null,
    val pageCount: Int? = null,
    val description: String? = null,
    val categories: List<String> = emptyList(),
    val language: String? = null,
    val isbn13: String? = null,
    val isbn10: String? = null,
    val coverUrl: String? = null,
) {
    val displayTitle: String get() = listOfNotNull(title, subtitle).joinToString(". ")

    val subtitleLine: String
        get() = listOfNotNull(
            authors.joinToString(", ").takeIf { it.isNotBlank() },
            year?.toString(),
            pageCount?.let { "$it pp." },
        ).joinToString(" · ")

    fun toWork() = Work(
        type = MediaType.BOOK,
        title = displayTitle,
        year = year,
        synopsis = description,
        language = language,
        coverUrl = coverUrl,
        pageCount = pageCount,
        publisher = publisher,
        isbn13 = isbn13,
        creators = authors,
        genres = categories,
    )
}

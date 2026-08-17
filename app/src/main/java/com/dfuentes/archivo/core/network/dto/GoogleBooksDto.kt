package com.dfuentes.archivo.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Google Books. Todo opcional a propósito: la API omite campos con alegría y un
 * `title` ausente no debe tirar la búsqueda entera.
 */
@Serializable
data class GoogleBooksResponse(
    val totalItems: Int = 0,
    val items: List<GoogleBookItem> = emptyList(),
)

@Serializable
data class GoogleBookItem(
    val id: String,
    val volumeInfo: GoogleVolumeInfo = GoogleVolumeInfo(),
)

@Serializable
data class GoogleVolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String> = emptyList(),
    val language: String? = null,
    val imageLinks: GoogleImageLinks? = null,
    val industryIdentifiers: List<GoogleIdentifier> = emptyList(),
) {
    /** publishedDate llega como "2007", "2007-03" o "2007-03-27". */
    val year: Int? get() = publishedDate?.take(4)?.toIntOrNull()

    val isbn13: String? get() = industryIdentifiers.firstOrNull { it.type == "ISBN_13" }?.identifier
    val isbn10: String? get() = industryIdentifiers.firstOrNull { it.type == "ISBN_10" }?.identifier
}

@Serializable
data class GoogleIdentifier(val type: String = "", val identifier: String = "")

@Serializable
data class GoogleImageLinks(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
) {
    /**
     * Las miniaturas de Google llegan en http y con zoom=1 (muy pequeñas).
     * Forzar https es obligatorio (el tráfico en claro está bloqueado desde
     * Android 9) y subir el zoom da una portada decente sin cambiar de API.
     */
    val best: String?
        get() = (thumbnail ?: smallThumbnail)
            ?.replace("http://", "https://")
            ?.replace("&zoom=1", "&zoom=3")
            ?.replace("&edge=curl", "")
}

/** Open Library: solo se usa para rellenar huecos y portadas por ISBN. */
@Serializable
data class OpenLibrarySearchResponse(
    val numFound: Int = 0,
    val docs: List<OpenLibraryDoc> = emptyList(),
)

@Serializable
data class OpenLibraryDoc(
    val key: String? = null,
    val title: String? = null,
    @SerialName("author_name") val authorName: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("number_of_pages_median") val pageCount: Int? = null,
    @SerialName("cover_i") val coverId: Int? = null,
    val publisher: List<String> = emptyList(),
    val isbn: List<String> = emptyList(),
    val subject: List<String> = emptyList(),
) {
    val coverUrl: String? get() = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
}

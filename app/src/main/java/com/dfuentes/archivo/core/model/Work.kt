package com.dfuentes.archivo.core.model

/**
 * Modelo de dominio de una OBRA: la cosa en sí, objetiva y enriquecible desde
 * APIs externas. No contiene nada subjetivo — eso vive en [Entry].
 */
data class Work(
    val id: Long = 0,
    val type: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null,
    val synopsis: String? = null,
    val language: String? = null,
    val coverPath: String? = null,
    val coverUrl: String? = null,
    val dominantColor: Int? = null,
    // BOOK
    val pageCount: Int? = null,
    val publisher: String? = null,
    val isbn13: String? = null,
    // MOVIE
    val runtimeMinutes: Int? = null,
    // SERIES
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val creators: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val entries: List<Entry> = emptyList(),
) {
    /** El registro vigente: el de mayor número de vuelta (relectura). */
    val currentEntry: Entry? get() = entries.maxByOrNull { it.round }
}

/**
 * Modelo de dominio de un REGISTRO: tu experiencia con una obra.
 * Una obra puede tener varios (releída, revisionada) sin machacar lo anterior.
 */
data class Entry(
    val id: Long = 0,
    val workId: Long,
    val status: Status,
    /** 0..10 en medias estrellas; null = sin puntuar. */
    val rating: Int? = null,
    /** Días desde epoch. La hora del día no aporta nada y complica el formateo. */
    val startedOn: Long? = null,
    val finishedOn: Long? = null,
    val notes: String? = null,
    val format: Format? = null,
    val isFavourite: Boolean = false,
    val progressValue: Int? = null,
    val progressSeason: Int? = null,
    val round: Int = 1,
)

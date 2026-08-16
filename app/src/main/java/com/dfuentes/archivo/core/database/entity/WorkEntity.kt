package com.dfuentes.archivo.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.MetadataSource

@Entity(
    tableName = "work",
    indices = [
        Index("type"),
        Index("sort_title"),
        Index(value = ["source", "source_id"], unique = true),
    ],
)
data class WorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val type: MediaType,
    val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String? = null,
    /** Clave de ordenación: sin artículo inicial ni tildes. Ver [sortTitleOf]. */
    @ColumnInfo(name = "sort_title") val sortTitle: String,
    val year: Int? = null,
    val synopsis: String? = null,
    val language: String? = null,

    // ── Portadas: la copia local manda; la URL es solo respaldo (RNF-04) ──
    @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @ColumnInfo(name = "cover_path") val coverPath: String? = null,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String? = null,
    @ColumnInfo(name = "dominant_color") val dominantColor: Int? = null,

    // ── Específicos de BOOK ──
    @ColumnInfo(name = "page_count") val pageCount: Int? = null,
    val publisher: String? = null,
    val isbn13: String? = null,
    val isbn10: String? = null,

    // ── Específicos de MOVIE ──
    @ColumnInfo(name = "runtime_minutes") val runtimeMinutes: Int? = null,

    // ── Específicos de SERIES ──
    @ColumnInfo(name = "season_count") val seasonCount: Int? = null,
    @ColumnInfo(name = "episode_count") val episodeCount: Int? = null,
    @ColumnInfo(name = "episode_runtime") val episodeRuntime: Int? = null,

    // ── Identificadores externos: permiten re-enriquecer sin ambigüedad
    //    y son la clave de deduplicación al importar un backup ──
    val source: MetadataSource = MetadataSource.MANUAL,
    @ColumnInfo(name = "source_id") val sourceId: String? = null,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int? = null,
    @ColumnInfo(name = "imdb_id") val imdbId: String? = null,
    @ColumnInfo(name = "openlibrary_id") val openLibraryId: String? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

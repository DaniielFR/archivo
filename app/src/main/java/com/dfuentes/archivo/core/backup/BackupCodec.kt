package com.dfuentes.archivo.core.backup

import com.dfuentes.archivo.core.database.entity.EntryEntity
import com.dfuentes.archivo.core.database.entity.GenreEntity
import com.dfuentes.archivo.core.database.entity.PersonEntity
import com.dfuentes.archivo.core.database.entity.TagEntity
import com.dfuentes.archivo.core.database.entity.WorkEntity
import com.dfuentes.archivo.core.model.Format
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.MetadataSource
import com.dfuentes.archivo.core.model.PersonRole
import com.dfuentes.archivo.core.model.Status
import kotlinx.serialization.json.Json

/**
 * Conversión entidades ⇄ formato de copia. Kotlin puro y sin dependencias de
 * Android: por eso se puede probar el ida y vuelta en un test de JVM normal,
 * sin emulador.
 */
val BackupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true // un backup viejo debe poder leerse con un formato nuevo
    encodeDefaults = true
}

/**
 * Huella para deduplicar al importar, en orden de fiabilidad decreciente:
 * identificador externo > ISBN > (tipo, título, año). Debe coincidir
 * exactamente con la expresión SQL de [BackupDao.fingerprints].
 */
fun fingerprintOf(
    source: String,
    sourceId: String?,
    isbn13: String?,
    type: String,
    title: String,
    year: Int?,
): String = "$source|${sourceId.orEmpty()}|${isbn13.orEmpty()}|$type|$title|${year ?: 0}"

fun BackupWork.fingerprint() = fingerprintOf(source, sourceId, isbn13, type, title, year)

fun WorkEntity.toBackup(
    credits: List<BackupCredit>,
    tags: List<String>,
    genres: List<String>,
    entries: List<BackupEntry>,
) = BackupWork(
    type = type.name,
    title = title,
    originalTitle = originalTitle,
    year = year,
    synopsis = synopsis,
    language = language,
    coverUrl = coverUrl,
    coverFile = coverPath,
    dominantColor = dominantColor,
    pageCount = pageCount,
    publisher = publisher,
    isbn13 = isbn13,
    isbn10 = isbn10,
    runtimeMinutes = runtimeMinutes,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    episodeRuntime = episodeRuntime,
    source = source.name,
    sourceId = sourceId,
    tmdbId = tmdbId,
    imdbId = imdbId,
    openLibraryId = openLibraryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    creators = credits,
    tags = tags,
    genres = genres,
    entries = entries,
)

fun EntryEntity.toBackup() = BackupEntry(
    status = status.name,
    rating = rating,
    startedOn = startedOn,
    finishedOn = finishedOn,
    notes = notes,
    format = format?.name,
    isFavourite = isFavourite,
    progressValue = progressValue,
    progressSeason = progressSeason,
    round = round,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PersonEntity.toBackup() = BackupPerson(name, sortName)

fun TagEntity.toBackup() = BackupTag(name, color)

fun GenreEntity.toBackup() = BackupGenre(name)

/**
 * Un enum desconocido (backup de una versión futura, o fichero manipulado) no
 * debe tumbar la importación entera: cae al valor de reserva y el resto del
 * registro se conserva. Perder el formato de un libro es molesto; perder los
 * otros 300 libros es inaceptable.
 */
fun BackupWork.toEntity(now: Long, sortTitle: String) = WorkEntity(
    type = enumOr(type, MediaType.BOOK),
    title = title,
    originalTitle = originalTitle,
    sortTitle = sortTitle,
    year = year,
    synopsis = synopsis,
    language = language,
    coverUrl = coverUrl,
    coverPath = coverFile,
    dominantColor = dominantColor,
    pageCount = pageCount,
    publisher = publisher,
    isbn13 = isbn13,
    isbn10 = isbn10,
    runtimeMinutes = runtimeMinutes,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    episodeRuntime = episodeRuntime,
    source = enumOr(source, MetadataSource.MANUAL),
    sourceId = sourceId,
    tmdbId = tmdbId,
    imdbId = imdbId,
    openLibraryId = openLibraryId,
    createdAt = createdAt.takeIf { it > 0 } ?: now,
    updatedAt = updatedAt.takeIf { it > 0 } ?: now,
)

fun BackupEntry.toEntity(workId: Long, now: Long) = EntryEntity(
    workId = workId,
    status = enumOr(status, Status.FINISHED),
    rating = rating?.coerceIn(0, 10),
    startedOn = startedOn,
    finishedOn = finishedOn,
    notes = notes,
    format = format?.let { enumOrNull<Format>(it) },
    isFavourite = isFavourite,
    progressValue = progressValue,
    progressSeason = progressSeason,
    round = round.coerceAtLeast(1),
    createdAt = createdAt.takeIf { it > 0 } ?: now,
    updatedAt = updatedAt.takeIf { it > 0 } ?: now,
)

fun BackupCredit.roleOrDefault(type: MediaType): PersonRole =
    enumOrNull<PersonRole>(role) ?: when (type) {
        MediaType.BOOK -> PersonRole.AUTHOR
        MediaType.MOVIE -> PersonRole.DIRECTOR
        MediaType.SERIES -> PersonRole.CREATOR
    }

private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
    enumOrNull<T>(value) ?: fallback

inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
    runCatching { enumValueOf<T>(value) }.getOrNull()

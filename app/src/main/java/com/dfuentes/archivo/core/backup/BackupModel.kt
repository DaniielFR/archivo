package com.dfuentes.archivo.core.backup

import kotlinx.serialization.Serializable

/**
 * Formato de copia de seguridad.
 *
 * JSON y no el fichero .db binario a propósito: el binario te ata a la versión
 * exacta del esquema de Room, no es inspeccionable y no puedes recuperar un dato
 * suelto con un editor de texto cuando algo va mal. Este JSON lo puedes leer con
 * los ojos dentro de diez años.
 *
 * [FORMAT_VERSION] es independiente de la versión del esquema de Room: el
 * formato puede cambiar sin que cambie la base de datos y viceversa. Cualquier
 * lector futuro debe mirar este número, no el de Room.
 */
const val FORMAT_VERSION = 1

const val BACKUP_EXTENSION = "archivo"
const val MANIFEST_ENTRY = "manifest.json"
const val DATA_ENTRY = "data.json"
const val CSV_ENTRY = "data.csv"
const val COVERS_DIR = "covers/"

@Serializable
data class BackupManifest(
    val formatVersion: Int = FORMAT_VERSION,
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val workCount: Int,
    val entryCount: Int,
)

@Serializable
data class BackupData(
    val works: List<BackupWork> = emptyList(),
    val people: List<BackupPerson> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val genres: List<BackupGenre> = emptyList(),
)

/**
 * La obra lleva dentro sus registros, autores, etiquetas y géneros.
 *
 * Se anidan en vez de exportar ocho tablas planas con ids cruzados porque al
 * importar los ids cambian: anidando, la reconstrucción es local a cada obra y
 * no hay que mantener un mapa de traducción de claves foráneas — que es
 * exactamente donde fallan la mayoría de los importadores.
 */
@Serializable
data class BackupWork(
    val type: String,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null,
    val synopsis: String? = null,
    val language: String? = null,
    val coverUrl: String? = null,
    val coverFile: String? = null,
    val dominantColor: Int? = null,
    val pageCount: Int? = null,
    val publisher: String? = null,
    val isbn13: String? = null,
    val isbn10: String? = null,
    val runtimeMinutes: Int? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val episodeRuntime: Int? = null,
    val source: String = "MANUAL",
    val sourceId: String? = null,
    val tmdbId: Int? = null,
    val imdbId: String? = null,
    val openLibraryId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val creators: List<BackupCredit> = emptyList(),
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val entries: List<BackupEntry> = emptyList(),
)

@Serializable
data class BackupCredit(val name: String, val role: String, val position: Int = 0)

@Serializable
data class BackupEntry(
    val status: String,
    val rating: Int? = null,
    val startedOn: Long? = null,
    val finishedOn: Long? = null,
    val notes: String? = null,
    val format: String? = null,
    val isFavourite: Boolean = false,
    val progressValue: Int? = null,
    val progressSeason: Int? = null,
    val round: Int = 1,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupPerson(val name: String, val sortName: String)

@Serializable
data class BackupTag(val name: String, val color: Int? = null)

@Serializable
data class BackupGenre(val name: String)

/** Resumen que se enseña al usuario ANTES de importar nada. */
data class BackupPreview(
    val manifest: BackupManifest,
    val works: Int,
    val entries: Int,
    val alreadyPresent: Int,
)

enum class ImportMode { MERGE, REPLACE }

sealed interface BackupResult {
    data class Success(val fileName: String, val works: Int) : BackupResult

    data class Failure(val reason: String) : BackupResult
}

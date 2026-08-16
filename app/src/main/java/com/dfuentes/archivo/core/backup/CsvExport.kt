package com.dfuentes.archivo.core.backup

import java.time.LocalDate

/**
 * CSV de cortesía: para abrirlo en una hoja de cálculo o migrar a otra app.
 * El JSON sigue siendo el formato canónico — el CSV aplana los registros
 * múltiples (una fila por registro) y pierde la estructura anidada.
 */
private val HEADERS = listOf(
    "tipo", "titulo", "titulo_original", "autoria", "anio", "editorial",
    "isbn13", "paginas", "minutos", "temporadas", "generos", "etiquetas",
    "estado", "nota_sobre_5", "empezado", "terminado", "formato",
    "favorito", "vuelta", "notas",
)

fun buildCsv(data: BackupData): String = buildString {
    appendLine(HEADERS.joinToString(","))
    data.works.forEach { work ->
        val entries = work.entries.ifEmpty { listOf(BackupEntry(status = "PENDING")) }
        entries.forEach { entry ->
            appendLine(
                listOf(
                    work.type,
                    work.title,
                    work.originalTitle.orEmpty(),
                    work.creators.joinToString("; ") { it.name },
                    work.year?.toString().orEmpty(),
                    work.publisher.orEmpty(),
                    work.isbn13.orEmpty(),
                    work.pageCount?.toString().orEmpty(),
                    work.runtimeMinutes?.toString().orEmpty(),
                    work.seasonCount?.toString().orEmpty(),
                    work.genres.joinToString("; "),
                    work.tags.joinToString("; "),
                    entry.status,
                    entry.rating?.let { (it / 2.0).toString() }.orEmpty(),
                    entry.startedOn.toIsoDate(),
                    entry.finishedOn.toIsoDate(),
                    entry.format.orEmpty(),
                    if (entry.isFavourite) "si" else "no",
                    entry.round.toString(),
                    entry.notes.orEmpty(),
                ).joinToString(",") { it.csvEscaped() },
            )
        }
    }
}

private fun Long?.toIsoDate(): String =
    this?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()

/**
 * Comillas dobles si hay coma, comilla o salto de línea; las comillas internas
 * se duplican (RFC 4180). Una nota con una coma es lo más normal del mundo y
 * sin esto rompe el fichero entero.
 */
private fun String.csvEscaped(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else {
        this
    }

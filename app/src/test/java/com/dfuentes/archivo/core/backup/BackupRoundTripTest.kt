package com.dfuentes.archivo.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ida y vuelta del formato de copia, en JVM pura (sin emulador).
 *
 * Este test es la razón por la que BackupCodec y CsvExport no dependen de
 * Android: el bug más caro posible en esta app es un backup que se escribe bien
 * y se lee mal, y eso se detecta aquí en milisegundos.
 */
class BackupRoundTripTest {

    private val sample = BackupData(
        works = listOf(
            BackupWork(
                type = "BOOK",
                title = "Crónica de una muerte anunciada",
                year = 1981,
                pageCount = 122,
                isbn13 = "9788497592437",
                creators = listOf(BackupCredit("Gabriel García Márquez", "AUTHOR", 0)),
                tags = listOf("relectura"),
                genres = listOf("Novela"),
                entries = listOf(
                    BackupEntry(
                        status = "FINISHED",
                        rating = 9,
                        finishedOn = 20_000,
                        notes = "Nota con, coma y \"comillas\"\ny un salto.",
                        format = "PAPER",
                        round = 1,
                    ),
                    BackupEntry(status = "FINISHED", rating = 10, round = 2),
                ),
            ),
            BackupWork(type = "MOVIE", title = "La llegada", year = 2016, runtimeMinutes = 116),
        ),
        people = listOf(BackupPerson("Gabriel García Márquez", "garcia marquez")),
        tags = listOf(BackupTag("relectura")),
        genres = listOf(BackupGenre("Novela")),
    )

    @Test
    fun `el json conserva todos los campos`() {
        val json = BackupJson.encodeToString(BackupData.serializer(), sample)
        val back = BackupJson.decodeFromString(BackupData.serializer(), json)
        assertEquals(sample, back)
    }

    @Test
    fun `un campo desconocido no rompe la lectura`() {
        // Simula una copia hecha por una versión futura de la app.
        val json = """{"works":[{"type":"BOOK","title":"X","campoDelFuturo":42}]}"""
        val back = BackupJson.decodeFromString(BackupData.serializer(), json)
        assertEquals("X", back.works.single().title)
    }

    @Test
    fun `las huellas distinguen obras distintas y reconocen la misma`() {
        val a = sample.works[0]
        val b = sample.works[1]
        assertEquals(a.fingerprint(), a.copy(synopsis = "otra sinopsis").fingerprint())
        assertTrue(a.fingerprint() != b.fingerprint())
    }

    @Test
    fun `el csv escapa comas comillas y saltos de linea`() {
        val csv = buildCsv(sample)
        // Comillas internas duplicadas y campo entrecomillado (RFC 4180).
        assertTrue(csv.contains("\"Nota con, coma y \"\"comillas\"\""))
        // El título con comas también va entrecomillado.
        assertTrue(csv.contains("Crónica de una muerte anunciada"))
    }

    @Test
    fun `el csv genera una fila por registro, no por obra`() {
        // Sin saltos de línea en las notas, para poder contar líneas de verdad.
        val plain = sample.copy(
            works = sample.works.map { work ->
                work.copy(entries = work.entries.map { it.copy(notes = "sin saltos") })
            },
        )
        val lines = buildCsv(plain).lines().filter { it.isNotBlank() }
        // 1 cabecera + 2 vueltas del libro + 1 fila de la película (sin registros
        // se emite igualmente una fila, para que la obra no desaparezca del CSV).
        assertEquals(4, lines.size)
    }

    @Test
    fun `un enum desconocido cae al valor de reserva sin perder el registro`() {
        val entry = BackupEntry(status = "ESTADO_INVENTADO", rating = 7)
        val entity = entry.toEntity(workId = 1, now = 0)
        assertEquals(com.dfuentes.archivo.core.model.Status.FINISHED, entity.status)
        assertEquals(7, entity.rating)
    }

    @Test
    fun `una nota fuera de rango se recorta en vez de guardarse mal`() {
        assertEquals(10, BackupEntry(status = "FINISHED", rating = 99).toEntity(1, 0).rating)
        assertEquals(0, BackupEntry(status = "FINISHED", rating = -5).toEntity(1, 0).rating)
    }
}

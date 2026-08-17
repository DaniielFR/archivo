package com.dfuentes.archivo.data

import com.dfuentes.archivo.core.model.BookCandidate
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.MetadataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class BookCandidateTest {

    private fun candidate(
        title: String = "El nombre del viento",
        subtitle: String? = null,
        authors: List<String> = listOf("Patrick Rothfuss"),
        year: Int? = 2007,
        pageCount: Int? = 662,
    ) = BookCandidate(
        sourceId = "abc",
        source = MetadataSource.GOOGLE_BOOKS,
        title = title,
        subtitle = subtitle,
        authors = authors,
        year = year,
        pageCount = pageCount,
    )

    @Test
    fun `el subtitulo se une al titulo al convertir a obra`() {
        val work = candidate(title = "Dune", subtitle = "Las crónicas").toWork()
        assertEquals("Dune. Las crónicas", work.title)
    }

    @Test
    fun `sin subtitulo el titulo no lleva punto de mas`() {
        assertEquals("Dune", candidate(title = "Dune", subtitle = null).toWork().title)
    }

    @Test
    fun `la linea secundaria omite los campos ausentes sin dejar separadores sueltos`() {
        assertEquals(
            "Patrick Rothfuss · 2007 · 662 pp.",
            candidate().subtitleLine,
        )
        assertEquals("2007", candidate(authors = emptyList(), pageCount = null).subtitleLine)
        assertEquals("", candidate(authors = emptyList(), year = null, pageCount = null).subtitleLine)
    }

    @Test
    fun `un candidato siempre se convierte en un libro`() {
        assertEquals(MediaType.BOOK, candidate().toWork().type)
    }
}

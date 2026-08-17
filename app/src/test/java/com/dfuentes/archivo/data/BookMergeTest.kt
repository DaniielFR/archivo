package com.dfuentes.archivo.data

import com.dfuentes.archivo.core.network.dto.GoogleImageLinks
import com.dfuentes.archivo.core.network.dto.GoogleVolumeInfo
import com.dfuentes.archivo.core.network.dto.GoogleIdentifier
import com.dfuentes.archivo.core.network.dto.OpenLibraryDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lógica de normalización de metadatos, sin red.
 *
 * Estos son los detalles que hacen que la búsqueda se vea bien o se vea rota, y
 * ninguno se detecta mirando la pantalla un rato: hay que fijarlos con tests.
 */
class BookMergeTest {

    @Test
    fun `el año sale de publishedDate en sus tres formatos`() {
        assertEquals(2007, GoogleVolumeInfo(publishedDate = "2007").year)
        assertEquals(2007, GoogleVolumeInfo(publishedDate = "2007-03").year)
        assertEquals(2007, GoogleVolumeInfo(publishedDate = "2007-03-27").year)
        assertNull(GoogleVolumeInfo(publishedDate = null).year)
        assertNull(GoogleVolumeInfo(publishedDate = "s.f.").year)
    }

    @Test
    fun `la miniatura de Google se fuerza a https y a mayor zoom`() {
        val links = GoogleImageLinks(
            thumbnail = "http://books.google.com/books/content?id=X&zoom=1&edge=curl",
        )
        val best = links.best
        assertTrue("debe ser https", best!!.startsWith("https://"))
        assertTrue("debe subir el zoom", best.contains("zoom=3"))
        assertTrue("debe quitar el efecto de página curvada", !best.contains("edge=curl"))
    }

    @Test
    fun `si no hay thumbnail se usa la miniatura pequeña`() {
        assertEquals(
            "https://x/small",
            GoogleImageLinks(smallThumbnail = "http://x/small").best,
        )
        assertNull(GoogleImageLinks().best)
    }

    @Test
    fun `los ISBN se extraen por tipo, no por posición`() {
        val info = GoogleVolumeInfo(
            industryIdentifiers = listOf(
                GoogleIdentifier("OTHER", "xxx"),
                GoogleIdentifier("ISBN_10", "8401352835"),
                GoogleIdentifier("ISBN_13", "9788401352836"),
            ),
        )
        assertEquals("9788401352836", info.isbn13)
        assertEquals("8401352835", info.isbn10)
    }

    @Test
    fun `la portada de Open Library se construye desde cover_i`() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/12345-L.jpg",
            OpenLibraryDoc(coverId = 12345).coverUrl,
        )
        assertNull(OpenLibraryDoc().coverUrl)
    }
}

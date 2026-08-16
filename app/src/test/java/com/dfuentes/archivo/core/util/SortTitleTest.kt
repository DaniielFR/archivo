package com.dfuentes.archivo.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SortTitleTest {

    @Test
    fun `quita el articulo inicial en castellano`() {
        assertEquals("nombre del viento", sortTitleOf("El nombre del viento"))
        assertEquals("sombra del viento", sortTitleOf("La sombra del viento"))
    }

    @Test
    fun `quita el articulo inicial en ingles`() {
        assertEquals("road", sortTitleOf("The Road"))
    }

    @Test
    fun `elimina tildes y dieresis`() {
        assertEquals("cronica de una muerte anunciada", sortTitleOf("Crónica de una muerte anunciada"))
    }

    @Test
    fun `no quita palabras que solo parecen articulos`() {
        assertEquals("ellas hablan", sortTitleOf("Ellas hablan"))
    }

    @Test
    fun `un titulo de una sola palabra se mantiene`() {
        assertEquals("chernobyl", sortTitleOf("Chernobyl"))
        assertEquals("el", sortTitleOf("El"))
    }
}

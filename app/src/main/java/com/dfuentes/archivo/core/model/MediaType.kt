package com.dfuentes.archivo.core.model

/**
 * Discriminador de la tabla `work`. Añadir un tipo nuevo (videojuegos, cómics,
 * podcasts) es literalmente añadir una constante aquí: ese es el dividendo de
 * haber elegido tabla única con discriminador en vez de tres tablas.
 */
enum class MediaType(val displayName: String) {
    BOOK("Libros"),
    MOVIE("Películas"),
    SERIES("Series"),
    ;

    val singular: String
        get() = when (this) {
            BOOK -> "Libro"
            MOVIE -> "Película"
            SERIES -> "Serie"
        }
}

package com.dfuentes.archivo.core.model

enum class Status(val displayName: String) {
    PENDING("Pendiente"),
    IN_PROGRESS("En curso"),
    FINISHED("Terminado"),
    ABANDONED("Abandonado"),
}

enum class Format(val displayName: String) {
    PAPER("Papel"),
    EBOOK("Ebook"),
    AUDIO("Audiolibro"),
    CINEMA("Cine"),
    STREAMING("Streaming"),
    TV("TV"),
    OTHER("Otro"),
}

enum class PersonRole {
    AUTHOR,
    DIRECTOR,
    CREATOR,
    TRANSLATOR,
    ILLUSTRATOR,
    ;

    companion object {
        /** Roles que se muestran como "autoría" en tarjetas y cabeceras. */
        val PRIMARY = listOf(AUTHOR, DIRECTOR, CREATOR)
    }
}

enum class MetadataSource {
    MANUAL,
    GOOGLE_BOOKS,
    OPENLIBRARY,
    TMDB,
}
